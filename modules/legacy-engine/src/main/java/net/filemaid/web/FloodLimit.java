package net.filemaid.web;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import net.filemaid.util.DefaultThreadFactory;

public class FloodLimit {
    private static final ScheduledThreadPoolExecutor TIMER = new ScheduledThreadPoolExecutor(1, new DefaultThreadFactory("FloodLimit", 5, true));
    private final Semaphore permits;
    private final long releaseDelay;
    private final TimeUnit timeUnit;

    public FloodLimit(int n, long l, TimeUnit timeUnit) {
        this.permits = new Semaphore(n, true);
        this.releaseDelay = l;
        this.timeUnit = timeUnit;
    }

    public ScheduledFuture<?> acquirePermit() throws InterruptedException {
        this.permits.acquire();
        return TIMER.schedule(this::releasePermit, this.releaseDelay, this.timeUnit);
    }

    protected void releasePermit() {
        this.permits.release();
    }

    public int availablePermits() {
        return this.permits.availablePermits();
    }

    public String toString() {
        return String.format("FloodLimit(%s)", this.availablePermits());
    }
}

