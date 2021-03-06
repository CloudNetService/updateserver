package eu.cloudnetservice.cloudnet.repository.command;

import de.dytanic.cloudnet.common.Properties;

import java.util.Collection;

/**
 * A interface, for all commands, an additional completer for that.
 *
 * @see Command
 */
public interface ITabCompleter {

    /**
     * This method allows on a Command implementation to complete the tab requests from the console
     * or a supported command sender
     *
     * @param commandLine the commandLine, that is currently written
     * @param args        the command line split into arguments
     * @param properties  the parsed properties from the command line
     * @return all available results. It does not necessarily depend on the actual input, which is already given
     */
    Collection<String> complete(String commandLine, String[] args, Properties properties);
}