package net.filemaid.server;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import net.filemaid.application.port.MetadataCacheRepository;
import net.filemaid.application.port.MetadataProvider;
import net.filemaid.application.service.SettingsService;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;
import net.filemaid.infrastructure.metadata.AnidbHttpMetadataProvider;
import net.filemaid.infrastructure.metadata.MetadataHttpException;
import net.filemaid.infrastructure.metadata.OmdbHttpMetadataProvider;
import net.filemaid.infrastructure.metadata.TmdbHttpMetadataProvider;
import net.filemaid.infrastructure.metadata.TvMazeHttpMetadataProvider;
import net.filemaid.infrastructure.metadata.TvdbHttpMetadataProvider;

/** Rebuilds the lightweight provider for each operation so saved settings apply immediately. */
public final class RuntimeMetadataProvider implements MetadataProvider {
    private final String providerId;
    private final FileMaidProperties properties;
    private final SettingsService settings;
    private final MetadataCacheRepository durableCache;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, Object> cacheLocks = new ConcurrentHashMap<>();
    private static final long SUCCESS_TTL_MS = Duration.ofHours(24).toMillis();
    private static final long EMPTY_TTL_MS = Duration.ofHours(1).toMillis();
    private static final long STALE_TTL_MS = Duration.ofDays(7).toMillis();

    public RuntimeMetadataProvider(String providerId, FileMaidProperties properties, SettingsService settings, MetadataCacheRepository durableCache) {
        this.providerId = providerId; this.properties = properties; this.settings = settings; this.durableCache = durableCache;
    }

    @Override public String id() { return providerId; }
    @Override public boolean available() { return delegate().available(); }
    @Override public String status() { return delegate().status(); }

    @Override public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) throws Exception {
        String titlePreference = settings.value("naming.titlePreference", "LOCALIZED");
        Locale requestedLocale = "ENGLISH".equals(titlePreference) ? Locale.ENGLISH : locale;
        String key = cacheKey(query, type, requestedLocale, limit) + "|" + titlePreference;
        CacheEntry existing = cache.computeIfAbsent(key, ignored -> durableCache.find(key)
                .map(entry -> new CacheEntry(entry.candidates(), entry.createdAt().toEpochMilli(), entry.ttlSeconds() * 1_000L)).orElse(null));
        long now = System.currentTimeMillis();
        if (existing != null && now - existing.createdAt() < existing.ttlMillis()) return existing.value();
        Object lock = cacheLocks.computeIfAbsent(key, ignored -> new Object());
        synchronized (lock) {
            try {
                existing = cache.get(key);
                now = System.currentTimeMillis();
                if (existing != null && now - existing.createdAt() < existing.ttlMillis()) return existing.value();
                int retries = integer("network.retryCount", 2, 0, 10);
                Exception last = null;
                for (int attempt = 0; attempt <= retries; attempt++) {
                    try {
                        consumeRequestBudget();
                        List<MetadataCandidate> result = delegate().search(query, type, requestedLocale, limit).stream()
                                .map(candidate -> applyTitlePreference(candidate, titlePreference)).toList();
                        if (cache.size() >= 1_000) cache.clear();
                        cache.put(key, new CacheEntry(result, now, result.isEmpty() ? EMPTY_TTL_MS : SUCCESS_TTL_MS));
                        durableCache.save(key, providerId, result, Instant.ofEpochMilli(now), (result.isEmpty() ? EMPTY_TTL_MS : SUCCESS_TTL_MS) / 1_000L);
                        if (java.util.concurrent.ThreadLocalRandom.current().nextInt(100) == 0) durableCache.deleteOlderThan(Instant.now().minus(Duration.ofDays(7)));
                        return result;
                    } catch (Exception failure) {
                        last = failure;
                        if (attempt >= retries || !retryable(failure)) break;
                        backoff(attempt, failure);
                    }
                }
                if (existing != null && now - existing.createdAt() < STALE_TTL_MS) return existing.value();
                throw last;
            } finally {
                cacheLocks.remove(key, lock);
            }
        }
    }

    private String cacheKey(String query, MetadataType type, Locale locale, int limit) {
        return String.join("|", providerId, value("endpoint", ""), type.name(), locale.toLanguageTag(), Integer.toString(limit), query.trim().toLowerCase(Locale.ROOT));
    }

    private void backoff(int attempt, Exception failure) throws InterruptedException {
        long instructed = failure instanceof MetadataHttpException http
                ? http.retryAfter().map(Duration::toMillis).orElse(0L) : 0L;
        long base = failure instanceof MetadataHttpException http && http.statusCode() == 429 ? 1_000L : 250L;
        long delay = instructed > 0 ? Math.min(30_000L, instructed) : Math.min(8_000L, base << Math.min(attempt, 5));
        Thread.sleep(delay + java.util.concurrent.ThreadLocalRandom.current().nextLong(100L));
    }

    private boolean retryable(Exception failure) {
        if (failure instanceof MetadataHttpException http) return http.statusCode() == 429 || http.statusCode() >= 500;
        return failure instanceof java.io.IOException;
    }

    private record CacheEntry(List<MetadataCandidate> value, long createdAt, long ttlMillis) { }

    static MetadataCandidate applyTitlePreference(MetadataCandidate candidate, String preference) {
        if (!"ORIGINAL".equals(preference) || !"tmdb".equals(candidate.provider()) || candidate.alternativeTitles().isEmpty()) {
            return candidate;
        }
        String original = candidate.alternativeTitles().get(0);
        if (original == null || original.isBlank() || original.equals(candidate.title())) return candidate;
        List<String> alternatives = new java.util.ArrayList<>();
        alternatives.add(candidate.title());
        alternatives.addAll(candidate.alternativeTitles().subList(1, candidate.alternativeTitles().size()));
        return new MetadataCandidate(candidate.provider(), candidate.id(), candidate.type(), original, alternatives,
                candidate.year(), candidate.overview(), candidate.artworkUrl(), candidate.fanartUrl());
    }

    private void consumeRequestBudget() {
        if (!"omdb".equals(providerId)) return;
        int limit = integer("provider.omdb.dailyLimit", 1_000, 0, 1_000_000);
        String apiKey = value("apiKey", properties.metadata().omdbApiKey());
        if (limit == 0 || apiKey.isBlank()) return;
        if (!durableCache.tryConsumeDaily(providerId, sha256(apiKey), LocalDate.now(ZoneOffset.UTC), limit)) {
            throw new IllegalStateException("OMDb 今日请求已达到本机设定上限（" + limit + "），可等待 UTC 次日或调整设置");
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception failure) { throw new IllegalStateException("无法计算请求配额标识", failure); }
    }

    private MetadataProvider delegate() {
        Duration timeout = Duration.ofSeconds(integer("network.timeoutSeconds", 20, 1, 300));
        HttpClient client = client(timeout);
        String endpoint = value("endpoint", "");
        boolean enabled = enabled();
        return switch (providerId) {
            case "tmdb" -> new TmdbHttpMetadataProvider(enabled ? value("apiKey", properties.metadata().tmdbApiKey()) : "", endpoint, client, timeout);
            case "tvdb" -> new TvdbHttpMetadataProvider(enabled ? value("apiKey", properties.metadata().tvdbApiKey()) : "", value("pin", properties.metadata().tvdbPin()), endpoint, client, timeout);
            case "omdb" -> new OmdbHttpMetadataProvider(enabled ? value("apiKey", properties.metadata().omdbApiKey()) : "", endpoint, client, timeout);
            case "tvmaze" -> new TvMazeHttpMetadataProvider(enabled, endpoint, client, timeout);
            case "anidb" -> new AnidbHttpMetadataProvider(enabled, endpoint, client,
                    timeout.compareTo(Duration.ofSeconds(120)) < 0 ? Duration.ofSeconds(120) : timeout,
                    Path.of(properties.dbPath()).toAbsolutePath().getParent().resolve("cache/anidb/anime-titles.dat"));
            default -> throw new IllegalArgumentException("Unknown metadata provider: " + providerId);
        };
    }

    private boolean enabled() {
        boolean fallback = switch (providerId) {
            case "tvmaze" -> properties.metadata().tvmazeEnabled();
            case "anidb" -> properties.metadata().anidbEnabled();
            default -> true;
        };
        return Boolean.parseBoolean(settings.value("provider." + providerId + ".enabled", Boolean.toString(fallback)));
    }
    private String value(String suffix, String fallback) { return settings.value("provider." + providerId + "." + suffix, fallback); }
    private int integer(String key, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(settings.value(key, Integer.toString(fallback))))); }
        catch (NumberFormatException ignored) { return fallback; }
    }
    private HttpClient client(Duration timeout) {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(timeout);
        String type = settings.value("network.proxyType", "NONE");
        String host = settings.value("network.proxyHost", "");
        if ("HTTP".equals(type) && !host.isBlank()) {
            int port = integer("network.proxyPort", 8080, 1, 65535);
            builder.proxy(ProxySelector.of(new InetSocketAddress(host, port)));
            String username = settings.value("network.proxyUsername", "");
            String password = settings.value("network.proxyPassword", "");
            if (!username.isBlank()) builder.authenticator(new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() { return new PasswordAuthentication(username, password.toCharArray()); }
            });
        }
        return builder.build();
    }
}
