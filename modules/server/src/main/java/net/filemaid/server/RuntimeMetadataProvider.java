package net.filemaid.server;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import net.filemaid.application.port.MetadataProvider;
import net.filemaid.application.service.SettingsService;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;
import net.filemaid.infrastructure.metadata.AnidbHttpMetadataProvider;
import net.filemaid.infrastructure.metadata.OmdbHttpMetadataProvider;
import net.filemaid.infrastructure.metadata.TmdbHttpMetadataProvider;
import net.filemaid.infrastructure.metadata.TvMazeHttpMetadataProvider;
import net.filemaid.infrastructure.metadata.TvdbHttpMetadataProvider;

/** Rebuilds the lightweight provider for each operation so saved settings apply immediately. */
public final class RuntimeMetadataProvider implements MetadataProvider {
    private final String providerId;
    private final FileMaidProperties properties;
    private final SettingsService settings;

    public RuntimeMetadataProvider(String providerId, FileMaidProperties properties, SettingsService settings) {
        this.providerId = providerId; this.properties = properties; this.settings = settings;
    }

    @Override public String id() { return providerId; }
    @Override public boolean available() { return delegate().available(); }
    @Override public String status() { return delegate().status(); }

    @Override public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) throws Exception {
        int retries = integer("network.retryCount", 2, 0, 10);
        Exception last = null;
        for (int attempt = 0; attempt <= retries; attempt++) {
            try { return delegate().search(query, type, locale, limit); }
            catch (Exception failure) { last = failure; }
        }
        throw last;
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
            case "anidb" -> new AnidbHttpMetadataProvider(enabled, endpoint, client, timeout.compareTo(Duration.ofSeconds(120)) < 0 ? Duration.ofSeconds(120) : timeout);
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
