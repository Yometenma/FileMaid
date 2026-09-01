package net.filemaid.util.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import net.filemaid.Logging;

class AccumulativeRunnable
implements Runnable {
    private final Executor executor;
    private final List<Runnable> queue = new ArrayList<Runnable>();

    public AccumulativeRunnable(Executor executor) {
        this.executor = executor;
    }

    public synchronized void submit(Runnable runnable) {
        if (this.queue.isEmpty()) {
            this.queue.add(runnable);
            this.executor.execute(this);
        } else {
            this.queue.add(runnable);
        }
    }

    private synchronized List<Runnable> flush() {
        ArrayList<Runnable> arrayList = new ArrayList<Runnable>(this.queue);
        this.queue.clear();
        return arrayList;
    }

    @Override
    public void run() {
        for (Runnable runnable : this.flush()) {
            try {
                runnable.run();
            }
            catch (Exception exception) {
                Logging.trace(exception);
            }
        }
    }
}

