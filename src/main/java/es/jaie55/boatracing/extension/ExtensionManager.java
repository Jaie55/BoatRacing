package es.jaie55.boatracing.extension;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.api.BoatRacingAPI;
import es.jaie55.boatracing.api.extension.BoatRacingExtension;
import es.jaie55.boatracing.api.extension.ExtensionCommand;
import es.jaie55.boatracing.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

/**
 * Discovers, loads and disables BoatRacing extensions.
 *
 * Extensions are plain jars in {@code plugins/BoatRacing/extensions/} with an {@code extension.yml}
 * descriptor and a main class implementing {@link BoatRacingExtension}. They are not Bukkit plugins:
 * BoatRacing owns their config, language files, storage, commands and scheduling.
 */
public final class ExtensionManager {

    private static final String DESCRIPTOR_NAME = "extension.yml";
    private static final java.util.Set<String> RESERVED_COMMANDS = java.util.Set.of(
            "teams", "race", "stats", "setup", "admin", "cosmetics", "debug", "extensions",
            "reload", "version", "help");
    private static final java.util.Set<String> RESERVED_SETUP_COMMANDS = java.util.Set.of(
            "help", "addstart", "clearstarts", "removestart", "setfinish", "clearfinish", "setpit", "clearpit",
            "addcheckpoint", "addalt", "clearalt", "clearcheckpoints", "addlight", "removelight", "clearlights",
            "setlaps", "setpitstops", "setregtime", "setcosmetics", "setlobby", "clearlobby", "setpos", "clearpos",
            "show", "selinfo", "wand", "gates", "autotrace", "wizard", "select");

    private final BoatRacingPlugin plugin;
    private final File extensionsDir;
    private final Map<String, LoadedExtension> extensions = new LinkedHashMap<>();
    private final Map<String, ExtensionCommand> commands = new LinkedHashMap<>();
    private final Map<String, ExtensionCommand> setupCommands = new LinkedHashMap<>();
    private final Map<String, Function<Player, String>> placeholders = new ConcurrentHashMap<>();

    public ExtensionManager(BoatRacingPlugin plugin) {
        this.plugin = plugin;
        this.extensionsDir = new File(plugin.getDataFolder(), "extensions");
    }

    public File extensionsDirectory() {
        return extensionsDir;
    }

    public Collection<LoadedExtension> loadedExtensions() {
        return Collections.unmodifiableCollection(extensions.values());
    }

    public boolean hasExtensions() {
        return !extensions.isEmpty();
    }

    // ------------------------------------------------------------------ loading

    public void loadAll() {
        if (!extensionsDir.exists() && !extensionsDir.mkdirs()) {
            plugin.getLogger().warning("Could not create the extensions folder: " + extensionsDir.getAbsolutePath());
            return;
        }
        File[] jars = extensionsDir.listFiles((dir, name) -> name != null && name.toLowerCase(Locale.ROOT).endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            plugin.getLogger().info("No BoatRacing extensions found in " + extensionsDir.getPath() + ".");
            return;
        }
        Arrays.sort(jars, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File jar : jars) {
            try {
                load(jar);
            } catch (Throwable throwable) {
                plugin.getLogger().warning("Failed to load extension " + jar.getName() + ": " + throwable.getMessage());
                plugin.getLogger().log(Level.FINE, "Extension load failure", throwable);
            }
        }
    }

    private void load(File jar) throws Exception {
        ExtensionDescriptor descriptor;
        try (JarFile jarFile = new JarFile(jar)) {
            JarEntry entry = jarFile.getJarEntry(DESCRIPTOR_NAME);
            if (entry == null) throw new IOException("missing " + DESCRIPTOR_NAME);
            try (InputStream in = jarFile.getInputStream(entry)) {
                descriptor = ExtensionDescriptor.read(in);
            }
        }
        if (descriptor.apiVersion() > BoatRacingAPI.API_VERSION) {
            throw new IOException("requires extension API v" + descriptor.apiVersion()
                    + " but this build supports v" + BoatRacingAPI.API_VERSION);
        }
        String key = descriptor.name().toLowerCase(Locale.ROOT);
        if (extensions.containsKey(key)) {
            throw new IOException("duplicate extension name '" + descriptor.name() + "'");
        }
        String pluginVersion = plugin.getDescription().getVersion();
        if (!pluginVersion.equals(descriptor.version())) {
            plugin.getLogger().warning("Extension " + descriptor.name() + " targets BoatRacing " + descriptor.version()
                    + " but this server runs " + pluginVersion + "; loading because API v" + descriptor.apiVersion() + " is compatible.");
        }

        File dataFolder = new File(extensionsDir, descriptor.name());
        try {
            if (!dataFolder.getCanonicalFile().toPath().startsWith(extensionsDir.getCanonicalFile().toPath())) {
                throw new IOException("extension name resolves outside the extensions folder");
            }
        } catch (IOException canonicalFailure) {
            throw new IOException("invalid extension data folder: " + canonicalFailure.getMessage());
        }
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IOException("could not create data folder " + dataFolder.getAbsolutePath());
        }

        URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toURI().toURL()}, plugin.getClass().getClassLoader());
        try (JarFile jarFile = new JarFile(jar)) {
            extractDefaults(jarFile, dataFolder);
        } catch (IOException ignored) {
            // Defaults were already extracted earlier or the jar is being replaced.
        }
        migrateLegacyBundles(dataFolder);
        BoatRacingExtension extension = null;
        ExtensionContextImpl context = null;
        try {
            Class<?> mainClass = Class.forName(descriptor.main(), true, classLoader);
            if (!BoatRacingExtension.class.isAssignableFrom(mainClass)) {
                throw new IOException(descriptor.main() + " does not implement BoatRacingExtension");
            }
            extension = (BoatRacingExtension) mainClass.getDeclaredConstructor().newInstance();
            context = new ExtensionContextImpl(plugin, this, descriptor, dataFolder, jar);
            LoadedExtension loaded = new LoadedExtension(descriptor, extension, context, classLoader);
            context.bind(loaded);
            extension.onEnable(context);
            context.attach();
            registerPermissions(loaded);
            extensions.put(key, loaded);
            plugin.getLogger().info("Loaded extension " + descriptor.name() + " v" + descriptor.version()
                    + " (API v" + descriptor.apiVersion() + ", " + loaded.commands().size() + " command(s)).");
        } catch (Throwable throwable) {
            if (extension != null) {
                try {
                    extension.onDisable();
                } catch (Throwable disableFailure) {
                    plugin.getLogger().fine("Extension " + descriptor.name()
                            + " also failed to clean up after a load error: " + disableFailure.getMessage());
                }
            }
            if (context != null) context.dispose();
            try {
                classLoader.close();
            } catch (IOException ignored) {
            }
            if (throwable instanceof Exception exception) throw exception;
            throw new IOException(throwable.getMessage(), throwable);
        }
    }

    private void registerPermissions(LoadedExtension loaded) {
        for (ExtensionDescriptor.PermissionEntry entry : loaded.descriptor().permissions()) {
            try {
                if (Bukkit.getPluginManager().getPermission(entry.name()) != null) continue;
                Bukkit.getPluginManager().addPermission(new Permission(
                        entry.name(), entry.description(), entry.defaultLevel()));
                loaded.registeredPermissions().add(entry.name());
            } catch (Throwable throwable) {
                plugin.getLogger().warning("Could not register permission " + entry.name()
                        + " for extension " + loaded.descriptor().name() + ": " + throwable.getMessage());
            }
        }
    }

    private void extractDefaults(JarFile jarFile, File dataFolder) {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (entry.isDirectory()) continue;
            File target;
            if (name.equals("config.yml")) {
                target = new File(dataFolder, name);
            } else if (name.startsWith("lang/messages_") && name.endsWith(".yml")) {
                target = new File(dataFolder, name.replace('/', File.separatorChar));
            } else {
                continue;
            }
            if (target.exists()) continue;
            try {
                if (target.getParentFile() != null) Files.createDirectories(target.getParentFile().toPath());
                try (InputStream in = jarFile.getInputStream(entry)) {
                    Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not extract " + name + " from " + jarFile.getName() + ": " + exception.getMessage());
            }
        }
    }

    /** Moves message files that older versions extracted next to the extension config into its lang folder. */
    private void migrateLegacyBundles(File dataFolder) {
        File[] legacy = dataFolder.listFiles((dir, name) -> name.startsWith("messages_") && name.endsWith(".yml"));
        if (legacy == null || legacy.length == 0) return;
        File langFolder = new File(dataFolder, "lang");
        if (!langFolder.exists() && !langFolder.mkdirs()) return;
        for (File file : legacy) {
            File target = new File(langFolder, file.getName());
            try {
                if (!target.exists()) {
                    Files.move(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.move(file.toPath(), file.toPath().resolveSibling(file.getName() + ".migrated"),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ignored) {
                plugin.getLogger().fine("Could not move legacy language file " + file.getName());
            }
        }
    }

    // ------------------------------------------------------------- lifecycle

    public void disableAll() {
        List<LoadedExtension> snapshot = new ArrayList<>(extensions.values());
        Collections.reverse(snapshot);
        for (LoadedExtension loaded : snapshot) {
            try {
                loaded.extension().onDisable();
            } catch (Throwable throwable) {
                plugin.getLogger().warning("Extension " + loaded.descriptor().name()
                        + " failed to disable: " + throwable.getMessage());
            }
            loaded.context().dispose();
            for (String permission : loaded.registeredPermissions()) {
                try {
                    Bukkit.getPluginManager().removePermission(permission);
                } catch (Throwable ignored) {
                }
            }
            try {
                loaded.classLoader().close();
            } catch (IOException ignored) {
            }
        }
        extensions.clear();
        commands.clear();
        setupCommands.clear();
        placeholders.clear();
    }

    public void reloadAll() {
        for (LoadedExtension loaded : extensions.values()) {
            try {
                loaded.context().reloadConfig();
                loaded.context().reloadMessages();
                loaded.extension().onReload();
            } catch (Throwable throwable) {
                plugin.getLogger().warning("Extension " + loaded.descriptor().name()
                        + " failed to reload: " + throwable.getMessage());
            }
        }
    }

    public void reloadMessages() {
        for (LoadedExtension loaded : extensions.values()) {
            try {
                loaded.context().reloadMessages();
            } catch (Throwable ignored) {
                plugin.getLogger().fine("Could not reload messages for " + loaded.descriptor().name());
            }
        }
    }

    // -------------------------------------------------------------- commands

    void indexCommand(LoadedExtension extension, ExtensionCommand command) {
        if (isReservedCommandName(command.name())) {
            plugin.getLogger().warning("Extension " + extension.descriptor().name()
                    + " tried to register the reserved command '/boatracing " + command.name() + "'; ignoring it.");
            return;
        }
        String key = command.name().toLowerCase(Locale.ROOT);
        ExtensionCommand existing = commands.get(key);
        if (existing != null && existing != command) {
            plugin.getLogger().warning("Extension " + extension.descriptor().name()
                    + " tried to register the duplicate command '/boatracing " + command.name() + "'; ignoring it.");
            return;
        }
        if (!extension.commands().contains(command)) extension.addCommand(command);
        commands.putIfAbsent(key, command);
        for (String alias : command.aliases()) {
            if (alias == null || alias.isBlank() || isReservedCommandName(alias)) continue;
            commands.putIfAbsent(alias.toLowerCase(Locale.ROOT), command);
        }
    }

    private static boolean isReservedCommandName(String name) {
        return name == null || RESERVED_COMMANDS.contains(name.toLowerCase(Locale.ROOT));
    }

    void indexSetupCommand(LoadedExtension extension, ExtensionCommand command) {
        String key = command.name().toLowerCase(Locale.ROOT);
        if (RESERVED_SETUP_COMMANDS.contains(key)) {
            plugin.getLogger().warning("Extension " + extension.descriptor().name()
                    + " tried to register the reserved setup command '/boatracing setup " + command.name() + "'; ignoring it.");
            return;
        }
        ExtensionCommand existing = setupCommands.get(key);
        if (existing != null && existing != command) {
            plugin.getLogger().warning("Extension " + extension.descriptor().name()
                    + " tried to register the duplicate setup command '/boatracing setup " + command.name() + "'; ignoring it.");
            return;
        }
        setupCommands.putIfAbsent(key, command);
    }

    void removeSetupCommand(ExtensionCommand command) {
        if (command == null) return;
        setupCommands.values().removeIf(candidate -> candidate == command);
    }

    public boolean isSetupCommandName(String token) {
        return token != null && setupCommands.containsKey(token.toLowerCase(Locale.ROOT));
    }

    public List<String> setupCommandNames(CommandSender sender) {
        List<String> names = new ArrayList<>();
        for (ExtensionCommand command : setupCommands.values()) {
            String permission = command.permission();
            if (permission == null || permission.isBlank() || sender.hasPermission(permission)) {
                names.add(command.name());
            }
        }
        return names;
    }

    public List<String> setupCommandHelp(CommandSender sender, String label) {
        List<String> lines = new ArrayList<>();
        for (ExtensionCommand command : setupCommands.values()) {
            String permission = command.permission();
            if (permission != null && !permission.isBlank() && !sender.hasPermission(permission)) continue;
            String usage = command.usage();
            if (usage != null && !usage.isBlank()) {
                lines.add(usage.replace("{label}", label == null ? "boatracing" : label));
            } else if (command.description() != null && !command.description().isBlank()) {
                lines.add("&7 - &f/" + (label == null ? "boatracing" : label) + " setup " + command.name()
                        + " &7- " + command.description());
            }
        }
        return lines;
    }

    public boolean handleSetupCommand(CommandSender sender, String label, String[] args) {
        if (args == null || args.length < 2) return false;
        ExtensionCommand command = setupCommands.get(args[1].toLowerCase(Locale.ROOT));
        if (command == null) return false;
        String permission = command.permission();
        if (permission != null && !permission.isBlank() && !sender.hasPermission(permission)) {
            sender.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("general.no-permission")));
            return true;
        }
        if (!(sender instanceof Player) && !command.allowConsole()) {
            sender.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("general.players-only")));
            return true;
        }
        String[] rest = Arrays.copyOfRange(args, 2, args.length);
        try {
            boolean handled = command.execute(sender, rest);
            if (!handled && command.usage() != null && !command.usage().isBlank()) {
                sender.sendMessage(Text.colorize(plugin.pref()
                        + command.usage().replace("{label}", label == null ? "boatracing" : label)));
            }
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "Extension setup command '" + command.name() + "' failed: "
                    + throwable.getMessage(), throwable);
            sender.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("general.extension-error")));
        }
        return true;
    }

    public List<String> tabCompleteSetup(CommandSender sender, String[] args) {
        if (args == null || args.length < 2) return Collections.emptyList();
        ExtensionCommand command = setupCommands.get(args[1].toLowerCase(Locale.ROOT));
        if (command == null) return Collections.emptyList();
        String permission = command.permission();
        if (permission != null && !permission.isBlank() && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }
        String[] rest = Arrays.copyOfRange(args, 2, args.length);
        try {
            List<String> result = command.tabComplete(sender, rest);
            return result == null ? Collections.emptyList() : result;
        } catch (Throwable throwable) {
            return Collections.emptyList();
        }
    }

    void removeCommand(ExtensionCommand command) {
        if (command == null) return;
        commands.values().removeIf(candidate -> candidate == command);
    }

    public boolean isCommandName(String token) {
        return token != null && commands.containsKey(token.toLowerCase(Locale.ROOT));
    }

    public boolean isConsoleAllowed(String token) {
        ExtensionCommand command = token == null ? null : commands.get(token.toLowerCase(Locale.ROOT));
        return command != null && command.allowConsole();
    }

    public List<String> commandNames(CommandSender sender) {
        List<String> names = new ArrayList<>();
        for (LoadedExtension loaded : extensions.values()) {
            for (ExtensionCommand command : loaded.commands()) {
                String permission = command.permission();
                if (permission == null || permission.isBlank() || sender.hasPermission(permission)) {
                    names.add(command.name());
                }
            }
        }
        return names;
    }

    public boolean handleCommand(CommandSender sender, String label, String[] args) {
        if (args == null || args.length == 0) return false;
        ExtensionCommand command = commands.get(args[0].toLowerCase(Locale.ROOT));
        if (command == null) return false;
        if (!(sender instanceof Player) && !command.allowConsole()) {
            sender.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("general.players-only")));
            return true;
        }
        String permission = command.permission();
        if (permission != null && !permission.isBlank() && !sender.hasPermission(permission)) {
            sender.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("general.no-permission")));
            return true;
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        try {
            boolean handled = command.execute(sender, rest);
            if (!handled && command.usage() != null && !command.usage().isBlank()) {
                sender.sendMessage(Text.colorize(plugin.pref()
                        + command.usage().replace("{label}", label == null ? "boatracing" : label)));
            }
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING, "Extension command '" + command.name() + "' failed: "
                    + throwable.getMessage(), throwable);
            sender.sendMessage(Text.colorize(plugin.pref() + plugin.msg().get("general.extension-error")));
        }
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args == null || args.length == 0) return Collections.emptyList();
        ExtensionCommand command = commands.get(args[0].toLowerCase(Locale.ROOT));
        if (command == null) return Collections.emptyList();
        String permission = command.permission();
        if (permission != null && !permission.isBlank() && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        try {
            List<String> result = command.tabComplete(sender, rest);
            return result == null ? Collections.emptyList() : result;
        } catch (Throwable throwable) {
            return Collections.emptyList();
        }
    }

    // ----------------------------------------------------------- placeholders

    boolean registerPlaceholder(String identifier, Function<Player, String> resolver) {
        if (identifier == null || identifier.isBlank() || resolver == null) return false;
        return placeholders.putIfAbsent(identifier.toLowerCase(Locale.ROOT), resolver) == null;
    }

    void unregisterPlaceholder(String identifier) {
        if (identifier != null) placeholders.remove(identifier.toLowerCase(Locale.ROOT));
    }

    /** @return the placeholder value, or null when no extension provides it. */
    public String resolvePlaceholder(Player player, String identifier) {
        if (identifier == null) return null;
        Function<Player, String> resolver = placeholders.get(identifier.toLowerCase(Locale.ROOT));
        if (resolver == null) return null;
        try {
            return resolver.apply(player);
        } catch (Throwable throwable) {
            return null;
        }
    }
}
