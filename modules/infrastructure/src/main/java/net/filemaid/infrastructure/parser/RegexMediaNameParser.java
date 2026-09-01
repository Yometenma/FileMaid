package net.filemaid.infrastructure.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.application.port.MediaNameParser;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.ParsedMediaName;

public final class RegexMediaNameParser implements MediaNameParser {
    private static final Pattern EPISODE = Pattern.compile("(?i)(?:^|[ ._-])S(\\d{1,2})[ ._-]*E(\\d{1,3}(?:[ ._-]*E\\d{1,3})*)");
    private static final Pattern X_EPISODE = Pattern.compile("(?i)(?:^|[ ._-])(\\d{1,2})x(\\d{1,3})");
    private static final Pattern YEAR = Pattern.compile("(?:^|\\D)((?:19|20)\\d{2})(?:\\D|$)");
    private static final Pattern RELEASE_TAGS = Pattern.compile("(?i)\\b(?:2160p|1080p|720p|480p|4k|uhd|bluray|blu-ray|web[- .]?dl|webrip|hdtv|x26[45]|hevc|av1|remux|proper|repack)\\b.*$");

    @Override
    public ParsedMediaName parse(String fileName) {
        if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("File name must not be blank");
        String extension = extension(fileName);
        String baseName = extension.isEmpty() ? fileName : fileName.substring(0, fileName.length() - extension.length());
        Matcher episodeMatcher = EPISODE.matcher(baseName);
        Matcher xMatcher = X_EPISODE.matcher(baseName);
        Matcher yearMatcher = YEAR.matcher(baseName);

        if (episodeMatcher.find()) {
            int season = Integer.parseInt(episodeMatcher.group(1));
            List<Integer> episodes = parseEpisodes(episodeMatcher.group(2));
            return result(fileName, cleanTitle(baseName.substring(0, episodeMatcher.start())), MediaType.EPISODE, findYear(baseName), season, episodes, extension, 0.92);
        }
        if (xMatcher.find()) {
            return result(fileName, cleanTitle(baseName.substring(0, xMatcher.start())), MediaType.EPISODE, findYear(baseName), Integer.parseInt(xMatcher.group(1)), List.of(Integer.parseInt(xMatcher.group(2))), extension, 0.88);
        }
        if (yearMatcher.find()) {
            return result(fileName, cleanTitle(baseName.substring(0, yearMatcher.start())), MediaType.MOVIE, Integer.parseInt(yearMatcher.group(1)), null, List.of(), extension, 0.82);
        }
        return result(fileName, cleanTitle(baseName), MediaType.UNKNOWN, null, null, List.of(), extension, 0.35);
    }

    private ParsedMediaName result(String original, String title, MediaType type, Integer year, Integer season, List<Integer> episodes, String extension, double confidence) {
        return new ParsedMediaName(original, title, type, year, season, episodes, extension, confidence, "filemaid-regex");
    }

    private List<Integer> parseEpisodes(String value) {
        Matcher matcher = Pattern.compile("(?i)E?(\\d{1,3})").matcher(value);
        List<Integer> episodes = new ArrayList<>();
        while (matcher.find()) episodes.add(Integer.parseInt(matcher.group(1)));
        return episodes;
    }

    private Integer findYear(String value) {
        Matcher matcher = YEAR.matcher(value);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private String extension(String value) {
        int dot = value.lastIndexOf('.');
        if (dot <= 0 || value.length() - dot > 9 || !value.substring(dot + 1).matches("[A-Za-z0-9]+")) return "";
        return value.substring(dot).toLowerCase(Locale.ROOT);
    }

    private String cleanTitle(String value) {
        String cleaned = RELEASE_TAGS.matcher(value).replaceFirst("");
        cleaned = cleaned.replaceAll("^[\\[({]+|[\\])}]+$", "").replaceAll("[._]+", " ").replaceAll("\\s+-\\s+", " ").replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? "Unknown" : cleaned;
    }
}
