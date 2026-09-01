package net.filemaid;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import net.filemaid.Cache;
import net.filemaid.InvalidResponseException;
import net.filemaid.Logging;
import net.filemaid.Resource;
import net.filemaid.util.ByteBufferInputStream;
import net.filemaid.util.ByteBufferOutputStream;
import net.filemaid.util.JsonUtilities;
import net.filemaid.util.SystemProperty;
import net.filemaid.web.HttpClientError;
import net.filemaid.web.WebRequest;
import org.w3c.dom.Document;

public class CachedResource<K, R>
implements Resource<R> {
    public static final int DEFAULT_RETRY_LIMIT = SystemProperty.get("net.filemaid.CachedResource.retryLimit", Integer::parseInt, 2);
    public static final Duration DEFAULT_RETRY_DELAY = SystemProperty.get("net.filemaid.CachedResource.retryDelay", Duration::parse, Duration.ofSeconds(2L));
    public static final int DEFAULT_RETRY_MULTIPLIER = SystemProperty.get("net.filemaid.CachedResource.retryMultiplier", Integer::parseInt, 4);
    private K key;
    private Transform<K, URL> resource;
    private Fetch fetch;
    private Transform<ByteBuffer, ? extends Object> parse;
    private Transform<? super Object, R> cast;
    private Duration expirationTime;
    private int retryLimit;
    private Duration retryDelay;
    private int retryMultiplier;
    private final Cache cache;

    public CachedResource(K k, Transform<K, URL> transform, Fetch fetch, Transform<ByteBuffer, ? extends Object> transform2, Transform<? super Object, R> transform3, Duration duration, Cache cache) {
        this(k, transform, fetch, transform2, transform3, DEFAULT_RETRY_LIMIT, DEFAULT_RETRY_DELAY, DEFAULT_RETRY_MULTIPLIER, duration, cache);
    }

    public CachedResource(K k, Transform<K, URL> transform, Fetch fetch, Transform<ByteBuffer, ? extends Object> transform2, Transform<? super Object, R> transform3, int n, Duration duration, int n2, Duration duration2, Cache cache) {
        this.key = k;
        this.resource = transform;
        this.fetch = fetch;
        this.parse = transform2;
        this.cast = transform3;
        this.expirationTime = duration2;
        this.retryLimit = n;
        this.retryDelay = duration;
        this.retryMultiplier = n2;
        this.cache = cache;
    }

    public CachedResource<K, R> fetch(Fetch fetch) {
        this.fetch = fetch;
        return this;
    }

    public CachedResource<K, R> expire(Duration duration) {
        this.expirationTime = duration;
        return this;
    }

    public CachedResource<K, R> retry(int n) {
        this.retryLimit = n;
        return this;
    }

    @Override
    public R get() throws Exception {
        Object object = this.cache.computeIf(this.key, Cache.isStale(this.expirationTime), element -> {
            URL uRL = this.resource.transform(this.key);
            Object cached = element == null ? null : element.getObjectValue();
            long l = cached == null ? 0L : element.getLatestOfCreationAndUpdateTime();
            try {
                Object object2 = this.retry(() -> {
                    ByteBuffer byteBuffer = this.fetch.fetch(uRL, l);
                    Logging.debug.finest(WebRequest.log(byteBuffer));
                    if (byteBuffer == null) {
                        return cached;
                    }
                    return this.parse.transform(byteBuffer);
                }, this.retryLimit, this.retryDelay);
                if (object2 == null) {
                    throw new InvalidResponseException("Response data is null: " + this.key + " => " + WebRequest.getFilePath(uRL));
                }
                return object2;
            }
            catch (InterruptedException | CancellationException exception) {
                throw exception;
            }
            catch (Exception exception) {
                Logging.debug.severe(Logging.cause("Failed to fetch resource", WebRequest.getFilePath(uRL), exception));
                if (cached == null) {
                    throw exception;
                }
                return cached;
            }
        });
        try {
            return this.cast.transform(object);
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to cast cached value: " + this.key + " => " + object + " (" + this.cache + ")", exception);
        }
    }

    public boolean available() {
        return this.cache.contains(this.key);
    }

    public void invalidate() {
        this.cache.remove(this.key);
    }

    protected <T> T retry(Callable<T> callable, int n, Duration duration) throws Exception {
        try {
            return callable.call();
        }
        catch (InterruptedException | UnknownHostException | CancellationException exception) {
            throw exception;
        }
        catch (IOException iOException) {
            if (n <= 0) {
                throw iOException;
            }
            Logging.debug.warning(Logging.format("Fetch failed: Try again in %s seconds (%s more) => %s", duration.getSeconds(), n, iOException));
            Thread.sleep(duration.toMillis());
            return this.retry(callable, n - 1, duration.multipliedBy(this.retryMultiplier));
        }
    }

    public Resource<R> flush() {
        return this.transform(object -> {
            this.cache.flush();
            return object;
        });
    }

    public static Transform<ByteBuffer, byte[]> getBytes() {
        return byteBuffer -> {
            byte[] byArray = new byte[byteBuffer.remaining()];
            byteBuffer.get(byArray, 0, byArray.length);
            return byArray;
        };
    }

    public static Transform<ByteBuffer, byte[]> getBytes(Transform<InputStream, InputStream> transform) {
        return byteBuffer -> {
            ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(byteBuffer.remaining());
            if (byteBuffer.hasRemaining()) {
                try (InputStream inputStream = (InputStream)transform.transform(new ByteBufferInputStream((ByteBuffer)byteBuffer));){
                    byteBufferOutputStream.transferFully(inputStream);
                }
            }
            return byteBufferOutputStream.getByteArray();
        };
    }

    public static Transform<ByteBuffer, ByteBuffer> validateXml() {
        return byteBuffer -> {
            if (byteBuffer.hasRemaining()) {
                try {
                    WebRequest.validateXml(new ByteBufferInputStream(byteBuffer.duplicate()));
                    return byteBuffer;
                }
                catch (Exception exception) {
                    throw new InvalidResponseException("Invalid XML", WebRequest.getTextContent(byteBuffer, "application/xml"), exception);
                }
            }
            return StandardCharsets.UTF_8.encode("<null/>");
        };
    }

    public static Transform<ByteBuffer, ByteBuffer> validateJson() {
        return byteBuffer -> {
            if (byteBuffer.hasRemaining()) {
                try {
                    JsonUtilities.readJson(new ByteBufferInputStream(byteBuffer.duplicate()));
                    return byteBuffer;
                }
                catch (Exception exception) {
                    throw new InvalidResponseException("Invalid JSON", WebRequest.getTextContent(byteBuffer, "application/json"), exception);
                }
            }
            return StandardCharsets.UTF_8.encode("null");
        };
    }

    public static Transform<InputStream, String> getText(Charset charset) {
        return inputStream -> {
            ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
            byteBufferOutputStream.transferFully((InputStream)inputStream);
            return new String(byteBufferOutputStream.getByteArray(), charset);
        };
    }

    public static Transform<InputStream, Document> getXml() {
        return inputStream -> WebRequest.getDocument(inputStream);
    }

    public static Transform<InputStream, Object> getJson() {
        return inputStream -> JsonUtilities.readJson(inputStream);
    }

    public static Transform<ByteBuffer, byte[]> pack(Transform<ByteBuffer, byte[]> transform) {
        return CachedResource.pack(byteBuffer -> byteBuffer, transform);
    }

    public static Transform<ByteBuffer, byte[]> pack(Transform<ByteBuffer, ByteBuffer> transform, Transform<ByteBuffer, byte[]> transform2) {
        return byteBuffer -> {
            byte[] byArray = (byte[])transform2.transform((ByteBuffer)transform.transform((ByteBuffer)byteBuffer));
            return WebRequest.gzip(byArray);
        };
    }

    public static Transform<Object, byte[]> unpack() {
        return CachedResource.unpack(inputStream -> {
            ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
            byteBufferOutputStream.transferFully((InputStream)inputStream);
            return byteBufferOutputStream.getByteArray();
        });
    }

    public static <R> Transform<Object, R> unpack(Transform<InputStream, R> transform) {
        return object -> {
            byte[] byArray = (byte[])object;
            try (GZIPInputStream gZIPInputStream = new GZIPInputStream(new ByteArrayInputStream(byArray));){
                R r = transform.transform(gZIPInputStream);
                return r;
            }
        };
    }

    public static Fetch fetchIfModified() {
        return CachedResource.fetchIfModified(Collections::emptyMap);
    }

    public static Fetch fetchIfModified(Callable<Map<String, String>> callable) {
        return (uRL, l) -> {
            Logging.debug.fine(WebRequest.log(uRL, l, null));
            try {
                return WebRequest.fetch(uRL, l, null, (Map)callable.call(), null);
            }
            catch (HttpClientError httpClientError) {
                return CachedResource.error(httpClientError);
            }
        };
    }

    public static Fetch fetchIfNoneMatch(Object object, Cache cache) {
        Resource.Memoized<Cache> memoized = Resource.lazy(() -> Cache.getCache(cache.getName() + "_etag", cache.getCacheType()));
        return CachedResource.fetchIfNoneMatch(() -> {
            try {
                return (String)((Cache)memoized.get()).get(object);
            }
            catch (Exception exception) {
                Logging.debug.severe(Logging.cause("Retrieve ETag", exception));
                return null;
            }
        }, (String string) -> {
            try {
                ((Cache)memoized.get()).put(object, string);
            }
            catch (Exception exception) {
                Logging.debug.severe(Logging.cause("Store ETag", exception));
            }
        });
    }

    public static Fetch fetchIfNoneMatch(Supplier<String> supplier, Consumer<String> consumer) {
        return (uRL, l) -> {
            String string = l > 0L ? (String)supplier.get() : null;
            Logging.debug.fine(WebRequest.log(uRL, l, string));
            try {
                return WebRequest.fetch(uRL, string == null ? l : 0L, string, null, (string2, string3) -> {
                    if ("etag".equals(string2) && !string3.equals(string)) {
                        Logging.debug.finest(Logging.message("Store ETag", string3));
                        consumer.accept((String)string3);
                    }
                });
            }
            catch (HttpClientError httpClientError) {
                return CachedResource.error(httpClientError);
            }
        };
    }

    public static Fetch post(Callable<byte[]> callable, Callable<String> callable2, Callable<Map<String, String>> callable3) {
        return (uRL, l) -> {
            Logging.debug.fine(WebRequest.log(uRL, 0L, null));
            try {
                return WebRequest.post(uRL, (byte[])callable.call(), (String)callable2.call(), (Map)callable3.call());
            }
            catch (HttpClientError httpClientError) {
                CachedResource.error(httpClientError);
                throw httpClientError;
            }
        };
    }

    private static ByteBuffer error(HttpClientError httpClientError) {
        if (httpClientError.isUnauthorized()) {
            Logging.debug.warning(Logging.message("Request not authorized", httpClientError.getMessage()));
            throw new CancellationException(httpClientError.getMessage());
        }
        if (httpClientError.isPermanent()) {
            Logging.debug.warning(Logging.message("Resource not found", httpClientError.getMessage()));
            return ByteBuffer.allocate(0);
        }
        Logging.debug.severe(Logging.message("Resource not available", httpClientError.getMessage()));
        if (httpClientError.isErrorResponse()) {
            Logging.debug.severe(httpClientError::getResponseContent);
        }
        return null;
    }

    public static Fetch withPermit(Fetch fetch, Permit permit) {
        return (uRL, l) -> {
            permit.acquire(uRL);
            return fetch.fetch(uRL, l);
        };
    }

    @FunctionalInterface
    public static interface Transform<T, R> {
        public R transform(T var1) throws Exception;
    }

    @FunctionalInterface
    public static interface Fetch {
        public ByteBuffer fetch(URL var1, long var2) throws Exception;
    }

    @FunctionalInterface
    public static interface Permit {
        public void acquire(URL var1) throws Exception;
    }
}

