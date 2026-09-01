package net.filemaid;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.function.Function;
import net.filemaid.util.SystemProperty;

public class MemoryCache<K, V> {
    private final Cache<K, V> cache;
    public static final Duration MINUTES = SystemProperty.get("net.filemaid.MemoryCache.minutes", Duration::parse, Duration.ofMinutes(20L));
    public static final Duration DAYS = SystemProperty.get("net.filemaid.MemoryCache.days", Duration::parse, Duration.ofDays(2L));

    private MemoryCache(Cache<K, V> cache) {
        this.cache = cache;
    }

    public V getIfPresent(K k) {
        return (V)this.cache.getIfPresent(k);
    }

    public V get(K k, Function<K, V> function) {
        return (V)this.cache.get(k, function);
    }

    public void put(K k, V v) {
        this.cache.put(k, v);
    }

    public void invalidate(K k) {
        this.cache.invalidate(k);
    }

    public void invalidateAll() {
        this.cache.invalidateAll();
    }

    public static <K, V> MemoryCache weak() {
        return new MemoryCache<K, V>(Caffeine.newBuilder().weakKeys().softValues().build());
    }

    public static <K, V> MemoryCache forObject() {
        return new MemoryCache<K, V>(Caffeine.newBuilder().build());
    }

    public static <K, V> MemoryCache forMinutes() {
        return new MemoryCache<K, V>(Caffeine.newBuilder().expireAfterAccess(MINUTES).build());
    }

    public static <K, V> MemoryCache forDays() {
        return new MemoryCache<K, V>(Caffeine.newBuilder().expireAfterAccess(DAYS).build());
    }
}

