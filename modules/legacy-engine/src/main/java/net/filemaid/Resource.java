package net.filemaid;

import java.util.function.Function;

@FunctionalInterface
public interface Resource<R> {
    public R get() throws Exception;

    default public Memoized<R> memoize() {
        return new Memoized(this);
    }

    default public <T> Resource<T> transform(Function<R, T> function) {
        return new Transformed<R, T>(this, function);
    }

    public static <T> Memoized<T> lazy(Resource<T> resource) {
        return resource.memoize();
    }

    public static class Memoized<R>
    implements Resource<R> {
        private final Resource<R> resource;
        private R value;

        public Memoized(Resource<R> resource) {
            this.resource = resource;
        }

        @Override
        public synchronized R get() throws Exception {
            if (this.value == null) {
                this.value = this.resource.get();
            }
            return this.value;
        }

        public synchronized void clear() {
            this.value = null;
        }
    }

    public static class Transformed<R, T>
    implements Resource<T> {
        private final Resource<R> resource;
        private final Function<R, T> function;

        public Transformed(Resource<R> resource, Function<R, T> function) {
            this.resource = resource;
            this.function = function;
        }

        @Override
        public T get() throws Exception {
            return this.function.apply(this.resource.get());
        }
    }
}

