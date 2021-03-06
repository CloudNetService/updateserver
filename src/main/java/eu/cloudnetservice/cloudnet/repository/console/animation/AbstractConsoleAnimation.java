package eu.cloudnetservice.cloudnet.repository.console.animation;

import eu.cloudnetservice.cloudnet.repository.console.IConsole;
import org.fusesource.jansi.Ansi;

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractConsoleAnimation implements Runnable {
    private IConsole console;
    private int updateInterval = 25;
    private long startTime;
    private int cursorUp = 1;
    private boolean staticCursor;
    private Collection<Runnable> finishHandler = new ArrayList<>();

    public long getTimeElapsed() {
        return System.currentTimeMillis() - this.startTime;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public void setStaticCursor(boolean staticCursor) {
        this.staticCursor = staticCursor;
    }

    public boolean isStaticCursor() {
        return this.staticCursor;
    }

    public void setConsole(IConsole console) {
        if (this.console != null) {
            throw new IllegalStateException("Cannot set console twice");
        }
        this.console = console;
    }

    public IConsole getConsole() {
        return this.console;
    }

    public void addToCursor(int cursor) {
        if (!this.isStaticCursor()) {
            this.cursorUp += cursor;
        }
    }

    public void setCursor(int cursor) {
        this.cursorUp = cursor;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public int getUpdateInterval() {
        return this.updateInterval;
    }

    public void addFinishHandler(Runnable finishHandler) {
        this.finishHandler.add(finishHandler);
    }

    protected void print(String... input) {
        if (input.length == 0) {
            return;
        }
        Ansi ansi = Ansi
                .ansi()
                .saveCursorPosition()
                .cursorUp(this.cursorUp)
                .eraseLine(Ansi.Erase.ALL);
        for (String a : input) {
            ansi.a(a);
        }
        this.console.forceWrite(ansi.restoreCursorPosition().toString());
    }

    protected void eraseLastLine() {
        this.console.writeRaw(
                Ansi.ansi()
                        .reset()
                        .cursorUp(1)
                        .eraseLine()
                        .toString()
        );
    }

    /**
     * @return if the animation is finished and should be cancelled
     */
    protected abstract boolean handleTick();

    @Override
    public final void run() {
        this.startTime = System.currentTimeMillis();
        while (!Thread.interrupted() && !handleTick()) {
            try {
                Thread.sleep(this.updateInterval);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
        for (Runnable runnable : this.finishHandler) {
            runnable.run();
        }
    }

}
