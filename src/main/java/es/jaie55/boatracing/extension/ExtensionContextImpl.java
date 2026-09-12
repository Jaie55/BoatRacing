package es.jaie55.boatracing.extension;

import es.jaie55.boatracing.BoatRacingPlugin;
import es.jaie55.boatracing.api.BoatRacingAPI;
import es.jaie55.boatracing.api.HudProvider;
import es.jaie55.boatracing.api.extension.ExtensionCommand;
import es.jaie55.boatracing.api.extension.ExtensionContext;
import es.jaie55.boatracing.api.extension.ExtensionScheduler;
import es.jaie55.boatracing.api.extension.ExtensionStorage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/** Implementation of {@link ExtensionContext} backed by the BoatRacing plugin. */
public final class ExtensionContextImpl implements ExtensionContext {

    private final BoatRacingPlugin plugin;
    private final ExtensionManager manager;
    private final ExtensionDescriptor descriptor;
    private final File dataFolder;
    private final File jarFile;
    private final Logger logger;
    private final ExtensionMessages messages;
    private final ExtensionStorageImpl storage;
    private final ExtensionSchedulerImpl scheduler;
    private final List<ExtensionCommand> commands = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();
    private final List<HudProvider> hudProviders = new ArrayList<>();
    private final Map<String, Function<Player, String>> placeholderResolvers = new LinkedHashMap<>();
    private final Set<String> registeredPlaceholders = new LinkedHashSet<>();
    private LoadedExtension loadedExtension;
    private YamlConfiguration config;
    private boolean attached;

    ExtensionContextImpl(BoatRacingPlugin plugin, ExtensionManager manager, ExtensionDescriptor descriptor,
                         File dataFolder, File jarFile) {
        this.plugin = plugin;
        this.manager = manager;
        this.descriptor = descriptor;
        this.dataFolder = dataFolder;
        this.jarFile = jarFile;
        this.logger = Logger.getLogger("BoatRacing/" + descriptor.name());
        loadConfig();
        this.messages = new ExtensionMessages(plugin, dataFolder, jarFile);
        this.storage = new ExtensionStorageImpl(plugin, descriptor.name());
        this.scheduler = new ExtensionSchedulerImpl(plugin);
    }

    void bind(LoadedExtension loadedExtension) {
        this.loadedExtension = loadedExtension;
    }

    private void loadConfig() {
        YamlConfiguration defaults = loadResource("config.yml");
        File file = new File(dataFolder, "config.yml");
        config = YamlConfiguration.loadConfiguration(file);
        if (defaults != null) config.addDefaults(defaults);
        config.options().copyDefaults(true);
        stripForeignKeys(defaults);
        try {
            config.save(file);
        } catch (IOException exception) {
            logger.warning("Could not save config.yml: " + exception.getMessage());
        }
    }

    /**
     * Removes keys that leaked from the base plugin config into an extension config (older builds
     * resolved the bundled config through the parent classloader). Keys the extension declares are
     * always kept, and so are any custom user keys that are not part of the base config.
     */
    private void stripForeignKeys(YamlConfiguration defaults) {
        Set<String> ownKeys = defaults == null ? Set.of() : defaults.getKeys(false);
        Set<String> baseKeys = plugin.getConfig().getKeys(false);
        if (baseKeys.isEmpty()) return;
        boolean changed = false;
        for (String key : new ArrayList<>(config.getKeys(false))) {
            if (!ownKeys.contains(key) && baseKeys.contains(key)) {
                config.set(key, null);
                changed = true;
            }
        }
        if (changed) {
            logger.info("Removed base plugin keys from config.yml (they belong to the BoatRacing config).");
        }
    }

    /** Reads a bundled resource from the extension jar itself, never through the parent classloader. */
    private YamlConfiguration loadResource(String name) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry(name);
            if (entry == null) return null;
            try (InputStream in = jar.getInputStream(entry)) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (Exception exception) {
            return null;
        }
    }

    @Override
    public String name() {
        return descriptor.name();
    }

    @Override
    public String version() {
        return descriptor.version();
    }

    @Override
    public File dataFolder() {
        return dataFolder;
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public BoatRacingAPI api() {
        return plugin.getExtensionApi();
    }

    @Override
    public FileConfiguration config() {
        return config;
    }

    @Override
    public void reloadConfig() {
        loadConfig();
    }

    @Override
    public String language() {
        return plugin.getConfig().getString("language", "en");
    }

    @Override
    public String message(String key, Object... replacements) {
        return messages.get(key, replacements);
    }

    @Override
    public List<String> messageList(String key) {
        List<String> list = messages.getList(key);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public void reloadMessages() {
        messages.reload();
    }

    @Override
    public ExtensionStorage storage() {
        return storage;
    }

    @Override
    public ExtensionScheduler scheduler() {
        return scheduler;
    }

    @Override
    public void registerCommand(ExtensionCommand command) {
        if (command == null || command.name() == null || command.name().isBlank()) return;
        commands.add(command);
    }

    @Override
    public void registerListener(Listener listener) {
        if (listener == null) return;
        listeners.add(listener);
    }

    @Override
    public void registerHudProvider(HudProvider provider) {
        if (provider == null) return;
        hudProviders.add(provider);
    }

    @Override
    public void registerPlaceholder(String identifier, Function<Player, String> resolver) {
        if (identifier == null || identifier.isBlank() || resolver == null) return;
        placeholderResolvers.putIfAbsent(identifier.toLowerCase(Locale.ROOT), resolver);
    }

    /**
     * Applies everything the extension registered during onEnable. Registrations are deferred until
     * the extension has enabled successfully, so a failing onEnable leaves nothing behind.
     */
    void attach() {
        if (attached) return;
        attached = true;
        for (Listener listener : listeners) {
            try {
                Bukkit.getPluginManager().registerEvents(listener, plugin);
            } catch (Throwable throwable) {
                logger.warning("Could not register extension listener: " + throwable.getMessage());
            }
        }
        for (HudProvider provider : hudProviders) {
            if (!plugin.getHudProviders().contains(provider)) plugin.getHudProviders().add(provider);
        }
        if (loadedExtension != null) {
            for (ExtensionCommand command : commands) manager.indexCommand(loadedExtension, command);
        }
        for (Map.Entry<String, Function<Player, String>> entry : placeholderResolvers.entrySet()) {
            if (manager.registerPlaceholder(entry.getKey(), entry.getValue())) {
                registeredPlaceholders.add(entry.getKey());
            }
        }
    }

    /** Unregisters everything the extension registered. Called when the extension is disabled. */
    void dispose() {
        scheduler.cancelAll();
        for (Listener listener : listeners) {
            try {
                HandlerList.unregisterAll(listener);
            } catch (Throwable ignored) {
                logger.fine("Could not unregister extension listener: " + ignored.getMessage());
            }
        }
        plugin.getHudProviders().removeAll(hudProviders);
        for (ExtensionCommand command : commands) manager.removeCommand(command);
        for (String placeholder : registeredPlaceholders) manager.unregisterPlaceholder(placeholder);
        listeners.clear();
        hudProviders.clear();
        placeholderResolvers.clear();
        registeredPlaceholders.clear();
        commands.clear();
        attached = false;
    }

    List<ExtensionCommand> commands() {
        return Collections.unmodifiableList(commands);
    }
}
