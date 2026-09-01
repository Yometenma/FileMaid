package net.filemaid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.Logging;
import net.filemaid.Settings;
import net.filemaid.util.DefaultThreadFactory;
import net.filemaid.util.IntSet;

public final class Parallelism
implements Executor {
    public static final IntSet THREAD_POOL_SIZE = Settings.getPreferredParallelism();
    public static final IntSet THREAD_POOL_PRIORITY = Settings.getPreferredThreadPriority();
    private static final Parallelism commonPool = new Parallelism("Parallelism", THREAD_POOL_SIZE.max());
    protected final ThreadPoolExecutor executor;

    public static Parallelism commonPool() {
        return commonPool;
    }

    private static ThreadPoolExecutor newThreadPool(String string, int n, BlockingQueue<Runnable> blockingQueue) {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(n, n, 60L, TimeUnit.SECONDS, blockingQueue, new DefaultThreadFactory(string, THREAD_POOL_PRIORITY.min(), false));
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }

    public Parallelism(String string, int n) {
        this(string, n, new LinkedBlockingQueue<Runnable>());
    }

    public Parallelism(String string, int n, BlockingQueue<Runnable> blockingQueue) {
        this.executor = Parallelism.newThreadPool(string, n, blockingQueue);
    }

    public void clearQueue() {
        this.executor.getQueue().clear();
    }

    @Override
    public void execute(Runnable runnable) {
        this.async(() -> {
            runnable.run();
            return null;
        });
    }

    public <R> CompletableFuture<R> async(RunnableFuture<R> runnableFuture) {
        return this.async(() -> {
            runnableFuture.run();
            return runnableFuture.get();
        });
    }

    public <R> CompletableFuture<R> async(Callable<R> callable) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callable.call();
            }
            catch (Exception exception) {
                Logging.trace(exception);
                return null;
            }
        }, this.executor);
    }

    public <T, R> Map<R, List<T>> group(Collection<T> collection, CallableFunction<T, R> callableFunction) throws Exception {
        LinkedHashMap<R, List<T>> linkedHashMap = new LinkedHashMap<R, List<T>>();
        this.map(collection, callableFunction, (object2, object3) -> linkedHashMap.computeIfAbsent(object3, object -> new ArrayList<T>()).add(object2));
        return linkedHashMap;
    }

    public <T, R> void map(Collection<T> collection, CallableFunction<T, R> callableFunction, BiConsumer<T, R> biConsumer) throws Exception {
        Stream<Task<T, R>> taskStream = collection.stream().map(object -> new Task(object, callableFunction));
        List<Future<Task<T, R>>> list = taskStream.map(t -> this.executor.submit(t)).collect(Collectors.toList());
        for (Future<Task<T, R>> future : list) {
            try {
                Task<T, R> task = future.get();
                biConsumer.accept(task.getValue(), task.getResult());
            }
            catch (Exception exception) {
                Parallelism.cancel(list);
                throw exception;
            }
        }
    }

    public <T, R> List<R> map(Collection<T> collection, CallableFunction<T, R> callableFunction) throws Exception {
        Stream<Task<T, R>> taskStream = collection.stream().map(object -> new Task(object, callableFunction));
        List<Future<Task<T, R>>> list = taskStream.map(t -> this.executor.submit(t)).collect(Collectors.toList());
        ArrayList arrayList = new ArrayList(list.size());
        for (Future future : list) {
            try {
                arrayList.add(((Task)future.get()).getResult());
            }
            catch (Exception exception) {
                Parallelism.cancel(list);
                throw exception;
            }
        }
        return arrayList;
    }

    private static <R> void cancel(List<Future<R>> list) {
        for (int i = list.size() - 1; i >= 0; --i) {
            list.get(i).cancel(true);
        }
    }

    @FunctionalInterface
    public static interface CallableFunction<T, R> {
        public R call(T var1) throws Exception;
    }

    private static class Task<T, R>
    implements Callable<Task<T, R>> {
        private final T value;
        private final CallableFunction<T, R> callable;
        private R result;

        public Task(T t, CallableFunction<T, R> callableFunction) {
            this.value = t;
            this.callable = callableFunction;
        }

        @Override
        public Task<T, R> call() throws Exception {
            this.result = this.callable.call(this.value);
            return this;
        }

        public T getValue() {
            return this.value;
        }

        public R getResult() {
            return this.result;
        }
    }
}

