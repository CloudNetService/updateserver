package eu.cloudnetservice.cloudnet.repository.console;

import de.dytanic.cloudnet.common.concurrent.ITask;
import eu.cloudnetservice.cloudnet.repository.command.ITabCompleter;
import eu.cloudnetservice.cloudnet.repository.console.animation.AbstractConsoleAnimation;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface IConsole extends AutoCloseable {

    Collection<AbstractConsoleAnimation> getRunningAnimations();

    void startAnimation(AbstractConsoleAnimation animation);

    boolean isAnimationRunning();

    void togglePrinting(boolean enabled);

    boolean isPrintingEnabled();

    default boolean hasAnimationSupport() {
        return this.hasColorSupport();
    }

    List<String> getCommandHistory();

    void setCommandHistory(List<String> history);

    void setCommandInputValue(String commandInputValue);

    ITask<String> readLine();

    void enableAllHandlers();

    void disableAllHandlers();

    void enableAllTabCompletionHandlers();

    void disableAllTabCompletionHandlers();

    void enableAllCommandHandlers();

    void disableAllCommandHandlers();

    void addCommandHandler(UUID uniqueId, Consumer<String> inputConsumer);

    void removeCommandHandler(UUID uniqueId);

    void addTabCompletionHandler(UUID uniqueId, ITabCompleter completer);

    void removeTabCompletionHandler(UUID uniqueId);

    IConsole writeRaw(String rawText);

    IConsole forceWrite(String text);

    IConsole forceWriteLine(String text);

    IConsole write(String text);

    IConsole writeLine(String text);

    boolean hasColorSupport();

    void setPrompt(String prompt);

    String getPrompt();

    void resetPrompt();

    void clearScreen();

    String getScreenName();

    void setScreenName(String name);

}