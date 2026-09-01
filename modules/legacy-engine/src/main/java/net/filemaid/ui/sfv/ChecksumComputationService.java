package net.filemaid.ui.sfv;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.filemaid.Logging;
import net.filemaid.Parallelism;
import net.filemaid.util.DefaultThreadFactory;

class ChecksumComputationService {
    public static final String TASK_COUNT_PROPERTY = "taskCount";
    private final List<ThreadPoolExecutor> executors = new ArrayList<ThreadPoolExecutor>();
    private final AtomicInteger completedTaskCount = new AtomicInteger(0);
    private final AtomicInteger totalTaskCount = new AtomicInteger(0);
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    ChecksumComputationService() {
    }

    public ExecutorService newExecutor() {
        return new ChecksumComputationExecutor(Parallelism.THREAD_POOL_SIZE.max());
    }

    public void reset() {
        List<ThreadPoolExecutor> list = this.executors;
        synchronized (list) {
            for (ExecutorService executorService : this.executors) {
                executorService.shutdownNow();
            }
            this.totalTaskCount.set(0);
            this.completedTaskCount.set(0);
            this.executors.clear();
        }
        this.pcs.firePropertyChange(TASK_COUNT_PROPERTY, -1, this.getTaskCount());
    }

    public int getActiveCount() {
        List<ThreadPoolExecutor> list = this.executors;
        synchronized (list) {
            return this.executors.size();
        }
    }

    public int getTaskCount() {
        return this.totalTaskCount.get() - this.completedTaskCount.get();
    }

    public int getTotalTaskCount() {
        return this.totalTaskCount.get();
    }

    public int getCompletedTaskCount() {
        return this.completedTaskCount.get();
    }

    public void purge() {
        List<ThreadPoolExecutor> list = this.executors;
        synchronized (list) {
            for (ThreadPoolExecutor threadPoolExecutor : this.executors) {
                threadPoolExecutor.purge();
            }
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.pcs.addPropertyChangeListener(propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.pcs.removePropertyChangeListener(propertyChangeListener);
    }

    private class ChecksumComputationExecutor
    extends ThreadPoolExecutor {
        public ChecksumComputationExecutor(int n) {
            super(1, n, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new DefaultThreadFactory("ChecksumComputation", Parallelism.THREAD_POOL_PRIORITY.min(), false));
            List<ThreadPoolExecutor> list = ChecksumComputationService.this.executors;
            synchronized (list) {
                if (ChecksumComputationService.this.executors.add(this) && ChecksumComputationService.this.executors.size() == 1) {
                    ChecksumComputationService.this.totalTaskCount.set(0);
                    ChecksumComputationService.this.completedTaskCount.set(0);
                }
            }
            this.prestartAllCoreThreads();
        }

        private int getPreferredPoolSize() {
            return (int)Math.max(1L, Math.round(Math.sqrt(this.getMaximumPoolSize()) + Math.log10(this.getQueue().size()) - 1.0));
        }

        @Override
        public void execute(Runnable runnable) {
            int n = this.getPreferredPoolSize();
            if (n > 0 && n <= this.getMaximumPoolSize() && this.getCorePoolSize() < n) {
                try {
                    this.setCorePoolSize(n);
                }
                catch (Exception exception) {
                    Logging.trace("Failed to set core pool size: " + n, exception);
                }
            }
            ChecksumComputationExecutor checksumComputationExecutor = this;
            synchronized (checksumComputationExecutor) {
                super.execute(runnable);
            }
            ChecksumComputationService.this.totalTaskCount.incrementAndGet();
            ChecksumComputationService.this.pcs.firePropertyChange(ChecksumComputationService.TASK_COUNT_PROPERTY, this.getTaskCount() - 1L, this.getTaskCount());
        }

        @Override
        public void purge() {
            int n = 0;
            ChecksumComputationExecutor checksumComputationExecutor = this;
            synchronized (checksumComputationExecutor) {
                n += this.getQueue().size();
                super.purge();
            }
            if ((n -= this.getQueue().size()) > 0) {
                ChecksumComputationService.this.totalTaskCount.addAndGet(-n);
                ChecksumComputationService.this.pcs.firePropertyChange(ChecksumComputationService.TASK_COUNT_PROPERTY, this.getTaskCount() + (long)n, this.getTaskCount());
            }
        }

        @Override
        protected void afterExecute(Runnable runnable, Throwable throwable) {
            super.afterExecute(runnable, throwable);
            if (this.isValid()) {
                if (runnable instanceof Future && ((Future)((Object)runnable)).isCancelled()) {
                    ChecksumComputationService.this.totalTaskCount.decrementAndGet();
                } else {
                    ChecksumComputationService.this.completedTaskCount.incrementAndGet();
                }
                ChecksumComputationService.this.pcs.firePropertyChange(ChecksumComputationService.TASK_COUNT_PROPERTY, this.getTaskCount() + 1L, this.getTaskCount());
            }
        }

        protected boolean isValid() {
            List<ThreadPoolExecutor> list = ChecksumComputationService.this.executors;
            synchronized (list) {
                return ChecksumComputationService.this.executors.contains(this);
            }
        }

        @Override
        protected void terminated() {
            List<ThreadPoolExecutor> list = ChecksumComputationService.this.executors;
            synchronized (list) {
                ChecksumComputationService.this.executors.remove(this);
            }
        }

        @Override
        public List<Runnable> shutdownNow() {
            List<Runnable> list = super.shutdownNow();
            for (Runnable runnable : list) {
                if (!(runnable instanceof Future)) continue;
                ((Future)((Object)runnable)).cancel(true);
            }
            return list;
        }
    }
}

