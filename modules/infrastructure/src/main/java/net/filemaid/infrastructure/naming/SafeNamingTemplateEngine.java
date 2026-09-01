package net.filemaid.infrastructure.naming;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.application.port.NamingTemplateEngine;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.ParsedMediaName;

public final class SafeNamingTemplateEngine implements NamingTemplateEngine {
    public static final String DEFAULT_SERIES = "TV Shows/{title}/Season {season:02}/{title} - S{season:02}{episodes}{extension}";
    public static final String DEFAULT_MOVIE = "Movies/{title} ({year})/{title} ({year}){extension}";
    public static final String DEFAULT_UNKNOWN = "Unsorted/{original}";
    private static final Pattern TOKEN = Pattern.compile("\\{([a-z]+)(?::(\\d+))?}");
    private final String seriesTemplate;
    private final String movieTemplate;
    private final String unknownTemplate;

    public SafeNamingTemplateEngine(String seriesTemplate, String movieTemplate, String unknownTemplate) {
        this.seriesTemplate = valueOrDefault(seriesTemplate, DEFAULT_SERIES);
        this.movieTemplate = valueOrDefault(movieTemplate, DEFAULT_MOVIE);
        this.unknownTemplate = valueOrDefault(unknownTemplate, DEFAULT_UNKNOWN);
        validateTemplate(this.seriesTemplate); validateTemplate(this.movieTemplate); validateTemplate(this.unknownTemplate);
    }

    @Override
    public String format(ParsedMediaName media) {
        String template = media.type() == MediaType.EPISODE ? seriesTemplate : media.type() == MediaType.MOVIE ? movieTemplate : unknownTemplate;
        Map<String, String> values = Map.of(
                "title", safe(media.title()),
                "year", media.year() == null ? "" : media.year().toString(),
                "season", media.season() == null ? "" : media.season().toString(),
                "episodes", episodeCode(media),
                "extension", safeExtension(media.extension()),
                "original", safe(media.originalName()));
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

    public Map<String, String> templates() {
        return Map.of("series", seriesTemplate, "movie", movieTemplate, "unknown", unknownTemplate);
    }

    private String episodeCode(ParsedMediaName media) {
        return media.episodes().stream().map(value -> String.format(Locale.ROOT, "E%02d", value)).reduce("", String::concat);
    }
    private String pad(String value, int width) { try { return String.format(Locale.ROOT, "%0" + width + "d", Integer.parseInt(value)); } catch (NumberFormatException ignored) { return value; } }
    private String safe(String value) { String cleaned = value == null ? "" : value.replaceAll("[\\\\/:*?\"<>|]", " ").replaceAll("\\s+", " ").trim(); return cleaned.isEmpty() ? "Unknown" : cleaned; }
    private String safeExtension(String value) { return value == null ? "" : value.replaceAll("[^.A-Za-z0-9]", ""); }
    private String valueOrDefault(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private void validateTemplate(String template) {
        if (template.length() > 500) throw new IllegalArgumentException("Naming template is too long");
        if (template.contains("..") || template.startsWith("/") || template.matches("^[A-Za-z]:.*")) throw new IllegalArgumentException("Naming template must be a relative path");
        Matcher matcher = TOKEN.matcher(template); while (matcher.find()) if (!SetHolder.ALLOWED.contains(matcher.group(1))) throw new IllegalArgumentException("Unknown naming token: " + matcher.group(1));
        if (template.replaceAll("\\{[a-z]+(?::\\d+)?}", "").contains("{")) throw new IllegalArgumentException("Invalid naming template syntax");
    }
    private static final class SetHolder { static final java.util.Set<String> ALLOWED = java.util.Set.of("title", "year", "season", "episodes", "extension", "original"); }
}
