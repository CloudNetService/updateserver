package eu.cloudnetservice.cloudnet.repository.console;

import de.dytanic.cloudnet.common.logging.AbstractLogHandler;
import de.dytanic.cloudnet.common.logging.LogEntry;
import de.dytanic.cloudnet.common.logging.LogLevel;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ConsoleLogHandler extends AbstractLogHandler {

    private OutputStream outputStream;
    private OutputStream errorStream;

    public ConsoleLogHandler(OutputStream outputStream, OutputStream errorStream) {
        this.outputStream = outputStream;
        this.errorStream = errorStream;
    }

    @Override
    public void handle(LogEntry logEntry) {
        try {
            if (logEntry.getLogLevel().getLevel() >= LogLevel.ERROR.getLevel()) {
                this.errorStream.write(super.getFormatter().format(logEntry).getBytes(StandardCharsets.UTF_8));
            } else {
                this.outputStream.write(super.getFormatter().format(logEntry).getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
