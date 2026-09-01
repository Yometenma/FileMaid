package net.filemaid.util;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import net.filemaid.Logging;
import net.filemaid.util.DefaultThreadFactory;

public class Timer {
    private final ThreadFactory threadFactory = new DefaultThreadFactory("Timer", 5, true);
    private final Runnable runnable;
    private ScheduledThreadPoolExecutor executor;
    private ScheduledFuture<?> scheduledFuture;
    private Thread shutdownHook;

    public Timer(Runnable runnable) {
        this.runnable = runnable;
    }

    public synchronized ScheduledFuture<?> set(long l, TimeUnit timeUnit, boolean bl) {
        if (this.executor == null) {
            this.executor = new ScheduledThreadPoolExecutor(1, this.threadFactory);
        }
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(true);
        }
        try {
            this.removeShutdownHook();
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
        if (!bl) {
            this.scheduledFuture = this.executor.schedule(this.runnable, l, timeUnit);
            return this.scheduledFuture;
        }
        try {
            this.addShutdownHook();
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
        this.scheduledFuture = this.executor.schedule(() -> {
            try {
                this.runnable.run();
            }
            finally {
                this.removeShutdownHook();
            }
        }, l, timeUnit);
        return this.scheduledFuture;
    }

    public synchronized void cancel() {
        if (this.executor != null) {
            this.executor.shutdownNow();
        }
        this.scheduledFuture = null;
        this.executor = null;
        this.removeShutdownHook();
    }

    private synchronized void addShutdownHook() {
        if (this.shutdownHook == null) {
            this.shutdownHook = new Thread(this.runnable);
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        }
    }

    private synchronized void removeShutdownHook() {
        if (this.shutdownHook != null && this.shutdownHook != Thread.currentThread()) {
            Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
            this.shutdownHook = null;
        }
    }
}

