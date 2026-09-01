package net.filemaid.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultThreadFactory
implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final String name;
    private final int priority;
    private final boolean daemon;

    public DefaultThreadFactory(String string) {
        this(string, 5, false);
    }

    public DefaultThreadFactory(String string, int n, boolean bl) {
        this.name = string;
        this.priority = n;
        this.daemon = bl;
        this.group = Thread.currentThread().getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(this.group, runnable, this.name + "-" + this.threadNumber.getAndIncrement());
        thread.setDaemon(this.daemon);
        thread.setPriority(this.priority);
        return thread;
    }
}

