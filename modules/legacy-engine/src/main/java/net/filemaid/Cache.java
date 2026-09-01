package net.filemaid;

import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.CacheType;
import net.filemaid.CachedResource;
import net.filemaid.DiskStore;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.Resource;
import net.filemaid.web.WebRequest;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import org.w3c.dom.Document;

public class Cache {
    public static final DiskStore DISK_STORE = new DiskStore();
    public static final Duration NEVER = Duration.ofDays(360L);
    public static final Duration ONE_DAY = Duration.ofHours(16L);
    public static final Duration ONE_WEEK = Duration.ofDays(4L);
    public static final Duration ONE_MONTH = Duration.ofDays(24L);
    public static final String DATA = "data";
    public static final String URL = "url";
    private final net.sf.ehcache.Cache cache;
    private final CacheType cacheType;

    public static Cache getCache(String string, CacheType cacheType) {
        return DISK_STORE.getCache(string, cacheType, false);
    }

    public static Cache getConcurrentCache(String string, CacheType cacheType) {
        return DISK_STORE.getCache(string, cacheType, true);
    }

    public <T> CachedResource<T, byte[]> bytes(T t, CachedResource.Transform<T, URL> transform) {
        return new CachedResource<T, byte[]>(t, transform, CachedResource.fetchIfModified(), CachedResource.pack(CachedResource.getBytes()), CachedResource.unpack(), NEVER, this);
    }

    public <T> CachedResource<T, byte[]> bytes(T t, CachedResource.Transform<T, URL> transform, CachedResource.Transform<InputStream, InputStream> transform2) {
        return new CachedResource<T, byte[]>(t, transform, CachedResource.fetchIfModified(), CachedResource.pack(CachedResource.getBytes(transform2)), CachedResource.unpack(), NEVER, this);
    }

    public <T, R> CachedResource<T, R> stream(T t, CachedResource.Transform<T, URL> transform, CachedResource.Transform<InputStream, R> transform2) {
        return new CachedResource<T, R>(t, transform, CachedResource.fetchIfModified(), CachedResource.pack(CachedResource.getBytes()), CachedResource.unpack(transform2), NEVER, this);
    }

    public <T, R> CachedResource<T, R> stream(T t, CachedResource.Transform<T, URL> transform, CachedResource.Transform<InputStream, InputStream> transform2, CachedResource.Transform<InputStream, R> transform3) {
        return new CachedResource<T, R>(t, transform, CachedResource.fetchIfModified(), CachedResource.pack(CachedResource.getBytes(transform2)), CachedResource.unpack(transform3), NEVER, this);
    }

    public <T> CachedResource<T, String> text(T t, CachedResource.Transform<T, URL> transform) {
        return new CachedResource<T, String>(t, transform, CachedResource.fetchIfModified(), CachedResource.pack(CachedResource.getBytes()), CachedResource.unpack(CachedResource.getText(StandardCharsets.UTF_8)), NEVER, this);
    }

    public <T> CachedResource<T, Document> xml(T t, CachedResource.Transform<T, URL> transform) {
        return new CachedResource<T, Document>(t, transform, CachedResource.fetchIfModified(), CachedResource.pack(CachedResource.validateXml(), CachedResource.getBytes()), CachedResource.unpack(CachedResource.getXml()), NEVER, this);
    }

    public <T> CachedResource<T, Object> json(T t, CachedResource.Transform<T, URL> transform) {
        return new CachedResource<T, Object>(t, transform, CachedResource.fetchIfModified(), CachedResource.pack(CachedResource.validateJson(), CachedResource.getBytes()), CachedResource.unpack(CachedResource.getJson()), NEVER, this);
    }

    public <T> CachedResource<T, byte[]> image(T t, CachedResource.Transform<T, URL> transform) {
        return new CachedResource<T, byte[]>(t, transform, CachedResource.fetchIfModified(), CachedResource.getBytes(), byte[].class::cast, NEVER, this);
    }

    public <T> CachedResource<T, byte[]> image(T t, CachedResource.Transform<T, URL> transform, CachedResource.Transform<ByteBuffer, byte[]> transform2) {
        return new CachedResource<T, byte[]>(t, transform, CachedResource.fetchIfModified(), transform2, byte[].class::cast, NEVER, this);
    }

    public Resource<byte[]> url(URL uRL) {
        if (MediaTypes.IMAGE_FILES.accept(uRL.getPath())) {
            return this.image(uRL.toExternalForm(), WebRequest::newURL).flush();
        }
        return this.bytes(uRL.toExternalForm(), WebRequest::newURL).flush();
    }

    public Cache(net.sf.ehcache.Cache cache, CacheType cacheType) {
        this.cache = cache;
        this.cacheType = cacheType;
    }

    public String getName() {
        return this.cache.getName();
    }

    public CacheType getCacheType() {
        return this.cacheType;
    }

    public boolean isAlive() {
        return this.cache.getStatus() == Status.STATUS_ALIVE;
    }

    public List<Object> getKeys() {
        return this.cache.getKeys();
    }

    public long getHeapSize() {
        return this.cache.getStatistics().getLocalHeapSize();
    }

    public boolean contains(Object object) {
        try {
            return this.getElementValue(this.cache.getQuiet(object)) != null;
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(this, "contains", object, exception));
            return false;
        }
    }

    public Object get(Object object) {
        try {
            return this.getElementValue(this.cache.get(object));
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(this, "get", object, exception));
            return null;
        }
    }

    public Object computeIf(Object object, Predicate<Element> predicate, Compute<?> compute) throws Exception {
        Element element = null;
        try {
            element = this.cache.get(object);
            if (element != null && !predicate.test(element)) {
                return this.getElementValue(element);
            }
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(this, "compute", object, exception));
        }
        Object obj = compute.apply(element);
        this.put(object, obj);
        return obj;
    }

    public Object computeIfAbsent(Object object, Compute<?> compute) throws Exception {
        return this.computeIf(object, element -> element == null, compute);
    }

    public void put(Object object, Object object2) {
        try {
            this.cache.put(this.createElement(object, object2));
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(this, "put", object, exception));
        }
    }

    protected Object getElementValue(Element element) {
        return element == null ? null : element.getObjectValue();
    }

    protected Element createElement(Object object, Object object2) {
        return new Element(object, object2);
    }

    public void remove(Object object) {
        try {
            this.cache.remove(object);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(this, "remove", object, exception));
        }
    }

    public void flush() {
        try {
            this.cache.flush();
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(this, "flush", exception));
        }
    }

    public void clear() {
        try {
            this.cache.removeAll();
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(this, "clear", exception));
        }
    }

    public String toString() {
        return this.cache.getName();
    }

    public static Predicate<Element> isStale(Duration duration) {
        return element -> System.currentTimeMillis() - element.getLatestOfCreationAndUpdateTime() > duration.toMillis();
    }

    public TypedCache<byte[]> bytes() {
        return this.typed(object -> {
            try {
                byte[] byArray = (byte[])object;
                if (byArray != null) {
                    return WebRequest.gunzip(byArray);
                }
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("gunzip", exception));
            }
            return null;
        }, byArray -> {
            try {
                return WebRequest.gzip(byArray);
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("gzip", exception));
                return null;
            }
        });
    }

    public <V> TypedCache<V> cast(Class<V> clazz) {
        return this.typed(object -> clazz.cast(object), object -> object);
    }

    public <V> TypedCache<List<V>> castList(Class<V> clazz) {
        return this.typed(object -> object == null ? null : Stream.of((Object[])object).map(clazz::cast).collect(Collectors.toList()), list -> list == null ? null : list.toArray());
    }

    public <V> TypedCache<V> typed(Function<Object, V> function, Function<V, Object> function2) {
        return new TypedCache<V>(this.cache, this.cacheType, function, function2);
    }

    public ConcurrentCache concurrent() {
        return new ConcurrentCache(this.cache, this.cacheType);
    }

    @FunctionalInterface
    public static interface Compute<R> {
        public R apply(Element var1) throws Exception;
    }

    public static class TypedCache<V>
    extends Cache {
        private final Function<Object, V> read;
        private final Function<V, Object> write;

        private TypedCache(net.sf.ehcache.Cache cache, CacheType cacheType, Function<Object, V> function, Function<V, Object> function2) {
            super(cache, cacheType);
            this.read = function;
            this.write = function2;
        }

        public V get(Object object) {
            return (V)super.get(object);
        }

        public V computeIf(Object object, Predicate<Element> predicate, Compute<?> compute) throws Exception {
            return (V)super.computeIf(object, predicate, compute);
        }

        public V computeIfAbsent(Object object, Compute<?> compute) throws Exception {
            return (V)super.computeIfAbsent(object, compute);
        }

        @Override
        protected Object getElementValue(Element element) {
            return this.read.apply(super.getElementValue(element));
        }

        @Override
        protected Element createElement(Object object, Object object2) {
            return super.createElement(object, this.write.apply((V)object2));
        }
    }

    public static class ConcurrentCache
    extends Cache {
        private final ConcurrentHashMap<Object, ReentrantLock> keyLock = new ConcurrentHashMap();

        public ConcurrentCache(net.sf.ehcache.Cache cache, CacheType cacheType) {
            super(cache, cacheType);
        }

        private Object withLock(Object object2, Callable<Object> callable) throws Exception {
            ReentrantLock reentrantLock = this.keyLock.computeIfAbsent(object2, (Object object) -> new ReentrantLock());
            reentrantLock.lock();
            try {
                Object object3 = callable.call();
                return object3;
            }
            finally {
                if (!reentrantLock.hasQueuedThreads()) {
                    this.keyLock.remove(object2);
                }
                reentrantLock.unlock();
            }
        }

        @Override
        public Object computeIf(Object object, Predicate<Element> predicate, Compute<?> compute) throws Exception {
            return this.withLock(object, () -> super.computeIf(object, predicate, compute));
        }
    }
}

