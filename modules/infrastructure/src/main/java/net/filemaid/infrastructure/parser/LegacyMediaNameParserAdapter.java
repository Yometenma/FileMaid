package net.filemaid.infrastructure.parser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.filemaid.application.port.MediaNameParser;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.ParsedMediaName;

public final class LegacyMediaNameParserAdapter implements MediaNameParser {
    private static final System.Logger LOG = System.getLogger(LegacyMediaNameParserAdapter.class.getName());
    private final RegexMediaNameParser fallback = new RegexMediaNameParser();
    private final Object seasonEpisodeMatcher;
    private final Method match;
    private final Method head;

    public LegacyMediaNameParserAdapter() {
        Object matcherInstance = null;
        Method matchMethod = null;
        Method headMethod = null;
        try {
            Class<?> filterType = Class.forName("net.filemaid.similarity.SeasonEpisodeMatcher$SeasonEpisodeFilter");
            Class<?> matcherType = Class.forName("net.filemaid.similarity.SeasonEpisodeMatcher");
            matcherInstance = matcherType.getConstructor(filterType, boolean.class).newInstance(null, false);
            matchMethod = matcherType.getMethod("match", CharSequence.class);
            headMethod = matcherType.getMethod("head", String.class);
        } catch (Throwable failure) {
            LOG.log(System.Logger.Level.INFO, "Legacy parser is unavailable; using the built-in parser: {0}", failure.toString());
        }
        this.seasonEpisodeMatcher = matcherInstance;
        this.match = matchMethod;
        this.head = headMethod;
    }

    @Override
    public ParsedMediaName parse(String fileName) {
        ParsedMediaName baseline = fallback.parse(fileName);
        if (!available()) return baseline;
        try {
            String baseName = baseline.extension().isEmpty() ? fileName : fileName.substring(0, fileName.length() - baseline.extension().length());
            List<?> legacyEpisodes = (List<?>) match.invoke(seasonEpisodeMatcher, baseName);
            if (baseline.type() == MediaType.MOVIE) return withParser(baseline, "legacy-engine+fallback");
            if (legacyEpisodes == null || legacyEpisodes.isEmpty()) return withParser(baseline, "legacy-engine+fallback");

            Field seasonField = legacyEpisodes.get(0).getClass().getField("season");
            Field episodeField = legacyEpisodes.get(0).getClass().getField("episode");
            int season = seasonField.getInt(legacyEpisodes.get(0));
            List<Integer> episodes = new ArrayList<>();
            for (Object item : legacyEpisodes) episodes.add(episodeField.getInt(item));
            String legacyHead = (String) head.invoke(seasonEpisodeMatcher, baseName);
            String title = cleanTitle(legacyHead, baseline.title());
            double confidence = season < 0 ? 0.78 : 0.96;
            return new ParsedMediaName(fileName, title, MediaType.EPISODE, baseline.year(), season < 0 ? null : season, episodes, baseline.extension(), confidence, "legacy-engine");
        } catch (Throwable failure) {
            LOG.log(System.Logger.Level.INFO, "Legacy parser failed for {0}; using the built-in parser: {1}", fileName, failure.toString());
            return baseline;
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
        String title = value.replaceAll("^[\\[({]+|[\\])}]+$", "").replaceAll("[._]+", " ").replaceAll("\\s+", " ").trim();
        return title.isEmpty() ? fallbackTitle : title;
    }
}
