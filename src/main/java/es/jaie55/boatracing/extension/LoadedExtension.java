package es.jaie55.boatracing.extension;

import es.jaie55.boatracing.api.extension.BoatRacingExtension;
import es.jaie55.boatracing.api.extension.ExtensionCommand;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** One enabled extension loaded by BoatRacing. */
public final class LoadedExtension {

    private final ExtensionDescriptor descriptor;
    private final BoatRacingExtension extension;
    private final ExtensionContextImpl context;
    private final URLClassLoader classLoader;
    private final List<ExtensionCommand> commands = new ArrayList<>();
    private final Set<String> registeredPermissions = new LinkedHashSet<>();

    LoadedExtension(ExtensionDescriptor descriptor, BoatRacingExtension extension,
                    ExtensionContextImpl context, URLClassLoader classLoader) {
        this.descriptor = descriptor;
        this.extension = extension;
        this.context = context;
        this.classLoader = classLoader;
    }

    public ExtensionDescriptor descriptor() {
        return descriptor;
    }

    public BoatRacingExtension extension() {
        return extension;
    }

    public ExtensionContextImpl context() {
        return context;
    }

    URLClassLoader classLoader() {
        return classLoader;
    }

    void addCommand(ExtensionCommand command) {
        commands.add(command);
    }

    public List<ExtensionCommand> commands() {
        return Collections.unmodifiableList(commands);
    }

    Set<String> registeredPermissions() {
        return registeredPermissions;
    }

    ExtensionCommand commandFor(String token) {
        if (token == null) return null;
        for (ExtensionCommand command : commands) {
            if (command.name().equalsIgnoreCase(token)) return command;
            for (String alias : command.aliases()) {
                if (alias != null && alias.toLowerCase(Locale.ROOT).equals(token.toLowerCase(Locale.ROOT))) return command;
            }
        }
        return null;
    }
}
