package net.filemaid.infrastructure.naming;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.application.port.NamingTemplateEngine;
import net.filemaid.core.model.MediaInfo;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.ParsedMediaName;

public final class SafeNamingTemplateEngine implements NamingTemplateEngine {
    public static final String DEFAULT_SERIES = "TV Shows/{title}/Season {season:02}/{title} - S{season:02}{episodes}{extension}";
    public static final String DEFAULT_MOVIE = "Movies/{title} ({year})/{title} ({year}){extension}";
    public static final String DEFAULT_UNKNOWN = "Unsorted/{original}";
    private static final Pattern TOKEN = Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9]*)(?::(\\d+))?}");
    private final String seriesTemplate;
    private final String movieTemplate;
    private final String unknownTemplate;
    private final String unknownTitle;

    public SafeNamingTemplateEngine(String seriesTemplate, String movieTemplate, String unknownTemplate) {
        this(seriesTemplate, movieTemplate, unknownTemplate, null);
    }

    public SafeNamingTemplateEngine(String seriesTemplate, String movieTemplate, String unknownTemplate, String unknownTitle) {
        this.seriesTemplate = valueOrDefault(seriesTemplate, DEFAULT_SERIES);
        this.movieTemplate = valueOrDefault(movieTemplate, DEFAULT_MOVIE);
        this.unknownTemplate = valueOrDefault(unknownTemplate, DEFAULT_UNKNOWN);
        String fallbackTitle = sanitize(valueOrDefault(unknownTitle, "Unknown"));
        this.unknownTitle = fallbackTitle.isEmpty() ? "Unknown" : fallbackTitle;
        validateTemplate(this.seriesTemplate); validateTemplate(this.movieTemplate); validateTemplate(this.unknownTemplate);
    }

    @Override
    public String format(ParsedMediaName media) {
        return format(media, null);
    }

    @Override
    public String format(ParsedMediaName media, MediaInfo mediaInfo) {
        String template = media.type() == MediaType.EPISODE ? seriesTemplate : media.type() == MediaType.MOVIE ? movieTemplate : unknownTemplate;
        Map<String, String> values = buildValues(media, mediaInfo);
        Matcher matcher = TOKEN.matcher(template);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            if (value == null) throw new IllegalArgumentException("Unknown naming token: " + matcher.group(1));
            if (matcher.group(2) != null && !value.isEmpty()) value = pad(value, Integer.parseInt(matcher.group(2)));
            matcher.appendReplacement(output, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(output);
        String normalized = output.toString().replace('\\', '/').replaceAll("/+", "/");
        Path path = Path.of(normalized).normalize();
        if (path.isAbsolute() || path.startsWith("..") || normalized.contains("../")) throw new IllegalArgumentException("Naming template must stay inside the media root");
        return path.toString().replace('\\', '/');
    }

    @Override public Map<String, String> templates() {
        return Map.of("series", seriesTemplate, "movie", movieTemplate, "unknown", unknownTemplate);
    }

    private Map<String, String> buildValues(ParsedMediaName media, MediaInfo info) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("title", safe(media.title()));
        values.put("year", media.year() == null ? "" : media.year().toString());
        values.put("season", media.season() == null ? "" : media.season().toString());
        values.put("episode", media.episodes().isEmpty() ? "" : media.episodes().get(0).toString());
        values.put("episodes", episodeCode(media));
        values.put("extension", safeExtension(media.extension()));
        values.put("original", safe(media.originalName()));
        if (info != null) {
            values.put("resolution", blank(info.resolution()));
            values.put("videoCodec", blank(info.videoCodec()));
            values.put("videoProfile", blank(info.videoProfile()));
            values.put("audioCodec", blank(info.audioCodec()));
            values.put("audioLanguage", blank(info.audioLanguage()));
            values.put("subtitleCodec", blank(info.subtitleCodec()));
            values.put("subtitleLanguage", blank(info.subtitleLanguage()));
            values.put("width", info.width() == null ? "" : info.width().toString());
            values.put("height", info.height() == null ? "" : info.height().toString());
            values.put("frameRate", blankNumber(info.frameRate()));
            values.put("bitRate", blankNumber(info.bitRate()));
            values.put("duration", blankNumber(info.durationSeconds()));
            values.put("fileSize", info.fileSize() == null ? "" : info.fileSize().toString());
        }
        return values;
    }

    private static String blank(String value) { return value == null ? "" : value; }

    private static String blankNumber(Number value) {
        if (value == null) return "";
        double d = value.doubleValue();
        if (d == Math.rint(d)) return String.valueOf((long) d);
        String formatted = String.format(Locale.ROOT, "%.3f", d);
        return formatted.contains(".") ? formatted.replaceAll("0+$", "").replaceAll("\\.$", "") : formatted;
    }

    private String episodeCode(ParsedMediaName media) {
        return media.episodes().stream().map(value -> String.format(Locale.ROOT, "E%02d", value)).reduce("", String::concat);
    }
    private String pad(String value, int width) { try { return String.format(Locale.ROOT, "%0" + width + "d", Integer.parseInt(value)); } catch (NumberFormatException ignored) { return value; } }
    private String safe(String value) { String cleaned = sanitize(value); return cleaned.isEmpty() ? unknownTitle : cleaned; }
    private static String sanitize(String value) { return (value == null ? "" : value).replaceAll("[\\\\/:*?\"<>|]", " ").replaceAll("\\s+", " ").trim(); }
    private String safeExtension(String value) { return value == null ? "" : value.replaceAll("[^.A-Za-z0-9]", ""); }
    private String valueOrDefault(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private void validateTemplate(String template) {
        if (template.length() > 500) throw new IllegalArgumentException("Naming template is too long");
        if (template.contains("..") || template.startsWith("/") || template.matches("^[A-Za-z]:.*")) throw new IllegalArgumentException("Naming template must be a relative path");
        Matcher matcher = TOKEN.matcher(template); while (matcher.find()) if (!ALLOWED.contains(matcher.group(1))) throw new IllegalArgumentException("Unknown naming token: " + matcher.group(1));
        if (template.replaceAll("\\{[a-zA-Z][a-zA-Z0-9]*(?::\\d+)?}", "").contains("{")) throw new IllegalArgumentException("Invalid naming template syntax");
    }

    private static final java.util.Set<String> ALLOWED = java.util.Set.of(
            "title", "year", "season", "episode", "episodes", "extension", "original",
            "resolution", "videoCodec", "videoProfile", "audioCodec", "audioLanguage",
            "subtitleCodec", "subtitleLanguage", "width", "height", "frameRate",
            "bitRate", "duration", "fileSize");
}
