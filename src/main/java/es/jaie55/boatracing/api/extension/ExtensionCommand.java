package es.jaie55.boatracing.api.extension;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * A subcommand registered by an extension under {@code /boatracing}. For example the party
 * extension registers {@code party}, so players run {@code /boatracing party top}.
 *
 * Execution always happens on the main thread. Exceptions are caught and logged by BoatRacing.
 */
public interface ExtensionCommand {

    /** Subcommand name without the root label (e.g. {@code party}). */
    String name();

    /** Extra aliases that also route to this command (without the root label). */
    default List<String> aliases() {
        return Collections.emptyList();
    }

    /** Permission required to run it, or null/blank for everyone. */
    default String permission() {
        return null;
    }

    /** Whether the console may run it (default false, players only). */
    default boolean allowConsole() {
        return false;
    }

    /** Optional one-line description for listings and tooling (may be a raw message). */
    default String description() {
        return "";
    }

    /** Usage line shown when the extension does not handle the arguments (may contain {label}). */
    default String usage() {
        return "";
    }

    /**
     * @param args arguments after the subcommand name (never null, may be empty)
     * @return true when the command was handled (usage line is not printed)
     */
    boolean execute(CommandSender sender, String[] args);

    /** Tab completion for the arguments after the subcommand name. */
    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
