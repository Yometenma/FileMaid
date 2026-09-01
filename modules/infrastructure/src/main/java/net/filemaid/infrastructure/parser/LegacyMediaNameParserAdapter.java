package net.filemaid.infrastructure.parser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.filemaid.application.port.MediaNameParser;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.ParsedMediaName;

/**
 * Adapts the legacy engine's stronger name recognition. Uses
 * {@code SmartSeasonEpisodeMatcher} (which strips format info before matching)
 * plus {@code MediaDetection.stripFormatInfo} / {@code stripBatchInfo} for title
 * cleaning. Everything it calls is local string parsing — no network, no file
 * access — and it degrades to the built-in parser when the legacy engine is not
 * on the runtime classpath.
 */
public final class LegacyMediaNameParserAdapter implements MediaNameParser {
    private static final System.Logger LOG = System.getLogger(LegacyMediaNameParserAdapter.class.getName());
    private final RegexMediaNameParser fallback = new RegexMediaNameParser();
    private final Object seasonEpisodeMatcher;
    private final Method match;
    private final Method head;
    private final Method stripFormatInfo;
    private final Method stripBatchInfo;

    public LegacyMediaNameParserAdapter() {
        Object matcherInstance = null;
        Method matchMethod = null;
        Method headMethod = null;
        Method stripFormat = null;
        Method stripBatch = null;
        try {
            Class<?> filterType = Class.forName("net.filemaid.similarity.SeasonEpisodeMatcher$SeasonEpisodeFilter");
            Class<?> matcherType = Class.forName("net.filemaid.media.SmartSeasonEpisodeMatcher");
            matcherInstance = matcherType.getConstructor(filterType, boolean.class).newInstance(null, false);
            matchMethod = matcherType.getMethod("match", CharSequence.class);
            headMethod = matcherType.getMethod("head", String.class);
            Class<?> detection = Class.forName("net.filemaid.media.MediaDetection");
            stripFormat = detection.getMethod("stripFormatInfo", String.class);
            stripBatch = detection.getMethod("stripBatchInfo", String.class);
        } catch (Throwable failure) {
            LOG.log(System.Logger.Level.INFO, "Legacy parser is unavailable; using the built-in parser: {0}", failure.toString());
        }
        this.seasonEpisodeMatcher = matcherInstance;
        this.match = matchMethod;
        this.head = headMethod;
        this.stripFormatInfo = stripFormat;
        this.stripBatchInfo = stripBatch;
    }

    @Override
    public ParsedMediaName parse(String fileName) {
        ParsedMediaName baseline = fallback.parse(fileName);
        if (!available()) return baseline;
        try {
            String baseName = baseline.extension().isEmpty() ? fileName : fileName.substring(0, fileName.length() - baseline.extension().length());
            // 仲裁：内置解析器已识别出电影年份时优先信任，避免旧引擎把年份误判为季集。
            if (baseline.type() == MediaType.MOVIE) {
                return parseMovie(fileName, baseName, baseline);
            }
            List<?> legacyEpisodes = (List<?>) match.invoke(seasonEpisodeMatcher, baseName);
            if (legacyEpisodes != null && !legacyEpisodes.isEmpty()) {
                return parseEpisode(fileName, baseName, baseline, legacyEpisodes);
            }
            return withParser(baseline, "legacy-engine+fallback");
        } catch (Throwable failure) {
            LOG.log(System.Logger.Level.INFO, "Legacy parser failed for {0}; using the built-in parser: {1}", fileName, failure.toString());
            return baseline;
        }
    }

    private ParsedMediaName parseEpisode(String fileName, String baseName, ParsedMediaName baseline, List<?> legacyEpisodes) throws Exception {
        Field seasonField = legacyEpisodes.get(0).getClass().getField("season");
        Field episodeField = legacyEpisodes.get(0).getClass().getField("episode");
        int season = seasonField.getInt(legacyEpisodes.get(0));
        List<Integer> episodes = new ArrayList<>();
        for (Object item : legacyEpisodes) episodes.add(episodeField.getInt(item));
        String legacyHead = (String) head.invoke(seasonEpisodeMatcher, baseName);
        String title = cleanTitle(legacyHead, baseline.title());
        double confidence = season < 0 ? 0.78 : 0.96;
        return new ParsedMediaName(fileName, title, MediaType.EPISODE, baseline.year(), season < 0 ? null : season, episodes, baseline.extension(), confidence, "legacy-engine");
    }

    private ParsedMediaName parseMovie(String fileName, String baseName, ParsedMediaName baseline) throws Exception {
        String stripped = strip(baseName);
        String title = cleanTitle(stripped, baseline.title());
        return new ParsedMediaName(fileName, title, MediaType.MOVIE, baseline.year(), null, List.of(), baseline.extension(), baseline.confidence(), "legacy-engine+fallback");
    }

    private String strip(String value) {
        try {
            String result = value;
            if (stripBatchInfo != null) result = (String) stripBatchInfo.invoke(null, result);
            if (stripFormatInfo != null) result = (String) stripFormatInfo.invoke(null, result);
            return result;
        } catch (Throwable failure) {
            return value;
        }
    }

    public boolean available() {
        return seasonEpisodeMatcher != null && match != null && head != null;
    }

    private ParsedMediaName withParser(ParsedMediaName value, String parser) {
        return new ParsedMediaName(value.originalName(), value.title(), value.type(), value.year(), value.season(), value.episodes(), value.extension(), value.confidence(), parser);
    }

    private String cleanTitle(String value, String fallbackTitle) {
        if (value == null || value.isBlank()) return fallbackTitle;
        String title = value.replaceAll("\\b(?:19|20)\\d{2}\\b", " ")
                .replaceAll("^[\\[({]+|[\\])}]+$", "")
                .replaceAll("[._]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return title.isEmpty() ? fallbackTitle : title;
    }
}
