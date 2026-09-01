package net.filemaid.application.service;

import java.net.URI;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.filemaid.application.port.SettingsRepository;

/** Owns the supported settings catalogue, defaults and input validation. */
public final class SettingsService {
    public static final String LANGUAGE_PRIORITY = "metadata.languagePriority";
    private static final int MAX_TEXT = 2_000;
    private static final Map<String, Definition> DEFINITIONS = buildDefinitions();
    private final SettingsRepository repository;

    public SettingsService(SettingsRepository repository) { this.repository = repository; }

    public Map<String, String> findAll() {
        Map<String, String> result = new LinkedHashMap<>();
        DEFINITIONS.forEach((key, definition) -> result.put(key, definition.defaultValue()));
        result.putAll(repository.findAll());
        return result;
    }

    public List<Definition> definitions() { return List.copyOf(DEFINITIONS.values()); }
    public String value(String key, String fallback) { return repository.findAll().getOrDefault(key, fallback == null ? "" : fallback); }
    public Set<String> secretKeys() { return DEFINITIONS.entrySet().stream().filter(e -> e.getValue().secret()).map(Map.Entry::getKey).collect(java.util.stream.Collectors.toUnmodifiableSet()); }

    public void update(Map<String, String> values) {
        if (values == null) return;
        Map<String, String> accepted = new LinkedHashMap<>();
        values.forEach((key, rawValue) -> {
            Definition definition = DEFINITIONS.get(key);
            if (definition == null) throw new IllegalArgumentException("不支持的设置项: " + key);
            String value = rawValue == null ? "" : rawValue.trim();
            validate(definition, value);
            accepted.put(key, value);
        });
        repository.saveAll(accepted);
    }

    public List<Locale> languagePriority() {
        List<Locale> locales = Arrays.stream(findAll().get(LANGUAGE_PRIORITY).split(","))
                .map(String::trim).filter(value -> !value.isBlank())
                .map(Locale::forLanguageTag).filter(locale -> !locale.getLanguage().isBlank())
                .distinct().toList();
        return locales.isEmpty() ? List.of(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH, Locale.JAPANESE) : locales;
    }

    private void validate(Definition definition, String value) {
        if (value.length() > MAX_TEXT) invalid(definition, "内容过长");
        switch (definition.type()) {
            case BOOLEAN -> { if (!Set.of("true", "false").contains(value)) invalid(definition, "必须是 true 或 false"); }
            case INTEGER -> validateInteger(definition, value);
            case DECIMAL -> validateDecimal(definition, value);
            case ENUM -> { if (!definition.options().contains(value)) invalid(definition, "可选值为 " + definition.options()); }
            case URI -> validateUri(definition, value);
            case TIMEZONE -> { try { ZoneId.of(value); } catch (Exception ignored) { invalid(definition, "不是有效时区"); } }
            case TEMPLATE -> { if (value.startsWith("/") || value.contains("..") || value.matches("^[A-Za-z]:.*")) invalid(definition, "必须是安全的相对路径模板"); }
            case STRING -> { }
        }
    }

    private void validateInteger(Definition d, String value) {
        try { long number = Long.parseLong(value); if (number < d.min() || number > d.max()) invalid(d, "范围应为 " + d.min() + "–" + d.max()); }
        catch (NumberFormatException ignored) { invalid(d, "必须是整数"); }
    }
    private void validateDecimal(Definition d, String value) {
        try { double number = Double.parseDouble(value); if (!Double.isFinite(number) || number < d.min() || number > d.max()) invalid(d, "范围应为 " + d.min() + "–" + d.max()); }
        catch (NumberFormatException ignored) { invalid(d, "必须是数字"); }
    }
    private void validateUri(Definition d, String value) {
        if (value.isBlank()) return;
        try { URI uri = URI.create(value); if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null) invalid(d, "必须是 HTTP(S) 地址"); }
        catch (Exception ignored) { invalid(d, "不是有效地址"); }
    }
    private void invalid(Definition definition, String reason) { throw new IllegalArgumentException(definition.key() + " " + reason); }

    private static Map<String, Definition> buildDefinitions() {
        Map<String, Definition> map = new LinkedHashMap<>();
        add(map, "network.proxyType", "network", Type.ENUM, "NONE", false, false, 0, 0, "NONE", "HTTP");
        add(map, "network.proxyHost", "network", Type.STRING, "", false, false, 0, 0);
        add(map, "network.proxyPort", "network", Type.INTEGER, "8080", false, false, 1, 65535);
        add(map, "network.proxyUsername", "network", Type.STRING, "", false, false, 0, 0);
        add(map, "network.proxyPassword", "network", Type.STRING, "", true, false, 0, 0);
        add(map, "network.timeoutSeconds", "network", Type.INTEGER, "20", false, false, 1, 300);
        add(map, "network.retryCount", "network", Type.INTEGER, "2", false, false, 0, 10);
        provider(map, "tmdb"); provider(map, "tvdb"); provider(map, "omdb"); provider(map, "tvmaze"); provider(map, "anidb");
        add(map, "provider.tmdb.apiKey", "providers", Type.STRING, "", true, false, 0, 0);
        add(map, "provider.tvdb.apiKey", "providers", Type.STRING, "", true, false, 0, 0);
        add(map, "provider.tvdb.pin", "providers", Type.STRING, "", true, false, 0, 0);
        add(map, "provider.omdb.apiKey", "providers", Type.STRING, "", true, false, 0, 0);
        add(map, "provider.anidb.clientName", "providers", Type.STRING, "filemaid", false, false, 0, 0);
        add(map, "naming.preset", "naming", Type.ENUM, "JELLYFIN", false, false, 0, 0, "JELLYFIN", "EMBY", "PLEX", "CUSTOM");
        add(map, "naming.seriesTemplate", "naming", Type.TEMPLATE, "TV Shows/{title}/Season {season:02}/{title} - S{season:02}{episodes}{extension}", false, false, 0, 0);
        add(map, "naming.movieTemplate", "naming", Type.TEMPLATE, "Movies/{title} ({year})/{title} ({year}){extension}", false, false, 0, 0);
        add(map, "naming.unknownTemplate", "naming", Type.TEMPLATE, "Unsorted/{original}", false, false, 0, 0);
        add(map, "naming.titlePreference", "naming", Type.ENUM, "LOCALIZED", false, false, 0, 0, "LOCALIZED", "ORIGINAL", "ENGLISH");
        add(map, "naming.unknownTitle", "naming", Type.STRING, "Unknown", false, false, 0, 0);
        add(map, LANGUAGE_PRIORITY, "metadata", Type.STRING, "zh-CN,en,ja", false, true, 0, 0);
        add(map, "metadata.matchThreshold", "metadata", Type.DECIMAL, "0.72", false, false, 0, 1);
        add(map, "metadata.candidateLimit", "metadata", Type.INTEGER, "10", false, false, 1, 50);
        add(map, "metadata.defaultMatchMode", "metadata", Type.ENUM, "MANUAL", false, false, 0, 0, "MANUAL", "AUTO");
        add(map, "postprocess.generateNfo", "postprocess", Type.BOOLEAN, "false", false, false, 0, 0);
        add(map, "postprocess.downloadArtwork", "postprocess", Type.BOOLEAN, "false", false, false, 0, 0);
        add(map, "postprocess.artworkType", "postprocess", Type.ENUM, "POSTER", false, false, 0, 0, "POSTER", "FANART", "BOTH");
        add(map, "postprocess.cleanEmptyDirectories", "postprocess", Type.BOOLEAN, "false", false, false, 0, 0);
        add(map, "files.defaultOperation", "files", Type.ENUM, "MOVE", false, false, 0, 0, "MOVE", "COPY", "HARDLINK");
        add(map, "files.conflictPolicy", "files", Type.ENUM, "FAIL", false, false, 0, 0, "FAIL", "SKIP");
        add(map, "files.historyRetentionDays", "files", Type.INTEGER, "90", false, false, 1, 3650);
        add(map, "scan.maxDepth", "scan", Type.INTEGER, "16", false, false, 1, 128);
        add(map, "scan.maxFiles", "scan", Type.INTEGER, "10000", false, false, 1, 1_000_000);
        add(map, "scan.ignorePatterns", "scan", Type.STRING, "@eaDir,.git,@Recycle", false, false, 0, 0);
        add(map, "scan.minimumFileSizeMb", "scan", Type.INTEGER, "0", false, false, 0, 1_000_000);
        add(map, "scan.extensions", "scan", Type.STRING, "mkv,mp4,avi,mov,m4v,ts,m2ts,ass,srt,ssa,vtt,nfo,jpg,jpeg,png", false, false, 0, 0);
        add(map, "system.timezone", "system", Type.TIMEZONE, "Asia/Shanghai", false, false, 0, 0);
        add(map, "system.logLevel", "system", Type.ENUM, "INFO", false, false, 0, 0, "ERROR", "WARN", "INFO", "DEBUG");
        add(map, "system.databaseBackupRetention", "system", Type.INTEGER, "7", false, false, 1, 365);
        add(map, "notification.webhookUrl", "notification", Type.URI, "", false, false, 0, 0);
        Set<String> runtimeKeys = Set.of(
                "network.proxyType", "network.proxyHost", "network.proxyPort", "network.proxyUsername", "network.proxyPassword",
                "network.timeoutSeconds", "network.retryCount", "provider.tmdb.enabled", "provider.tmdb.apiKey", "provider.tmdb.endpoint",
                "provider.tvdb.enabled", "provider.tvdb.apiKey", "provider.tvdb.pin", "provider.tvdb.endpoint",
                "provider.omdb.enabled", "provider.omdb.apiKey", "provider.omdb.endpoint", "provider.tvmaze.enabled", "provider.tvmaze.endpoint",
                "provider.anidb.enabled", "provider.anidb.endpoint", LANGUAGE_PRIORITY);
        runtimeKeys = new java.util.HashSet<>(runtimeKeys);
        runtimeKeys.addAll(Set.of("naming.seriesTemplate", "naming.movieTemplate", "naming.unknownTemplate", "scan.maxDepth", "scan.maxFiles",
                "metadata.matchThreshold", "metadata.candidateLimit", "postprocess.generateNfo", "postprocess.downloadArtwork", "postprocess.artworkType", "files.defaultOperation",
                "naming.preset", "naming.titlePreference", "naming.unknownTitle", "scan.ignorePatterns", "scan.minimumFileSizeMb", "scan.extensions",
                "files.conflictPolicy", "files.historyRetentionDays", "postprocess.cleanEmptyDirectories", "notification.webhookUrl"));
        runtimeKeys.forEach(key -> map.computeIfPresent(key, (ignored, value) -> new Definition(value.key(), value.category(), value.type(), value.defaultValue(), value.secret(), true, value.min(), value.max(), value.options())));
        return java.util.Collections.unmodifiableMap(map);
    }

    private static void provider(Map<String, Definition> map, String id) {
        add(map, "provider." + id + ".enabled", "providers", Type.BOOLEAN, "true", false, false, 0, 0);
        add(map, "provider." + id + ".endpoint", "providers", Type.URI, "", false, false, 0, 0);
    }
    private static void add(Map<String, Definition> map, String key, String category, Type type, String defaultValue,
                            boolean secret, boolean active, double min, double max, String... options) {
        map.put(key, new Definition(key, category, type, defaultValue, secret, active, min, max, List.of(options)));
    }

    public enum Type { STRING, BOOLEAN, INTEGER, DECIMAL, ENUM, URI, TIMEZONE, TEMPLATE }
    public record Definition(String key, String category, Type type, String defaultValue, boolean secret,
                             boolean active, double min, double max, List<String> options) { }
}
