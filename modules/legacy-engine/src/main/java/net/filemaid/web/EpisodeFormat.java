package net.filemaid.web;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.filemaid.similarity.Normalization;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.MultiEpisode;
import net.filemaid.web.SimpleDate;

public class EpisodeFormat
extends Format {
    public static final EpisodeFormat DEFAULT = new EpisodeFormat();
    private final Pattern sxePattern = Pattern.compile("- (?:(\\d{1,2})x)?(Special )?(\\d{1,3}) -");
    private final Pattern airdatePattern = Pattern.compile("\\[(\\d{4}-\\d{1,2}-\\d{1,2})\\]");

    @Override
    public StringBuffer format(Object object, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        if (object instanceof MultiEpisode) {
            return stringBuffer.append(this.formatMultiEpisode((MultiEpisode)object));
        }
        Episode episode = (Episode)object;
        String string = episode.getEpisode() != null ? String.format(Locale.ROOT, "%02d", episode.getEpisode()) : null;
        stringBuffer.append(episode.getSeriesName());
        if (episode.getSeason() != null) {
            stringBuffer.append(" - ").append(episode.getSeason()).append('x');
            if (episode.getEpisode() != null) {
                stringBuffer.append(String.format(Locale.ROOT, "%02d", episode.getEpisode()));
            } else if (episode.getSpecial() != null) {
                stringBuffer.append("Special " + episode.getSpecial());
            }
        } else if (episode.getEpisode() != null) {
            stringBuffer.append(" - ").append(string);
        } else if (episode.getSpecial() != null) {
            stringBuffer.append(" - ").append("Special " + episode.getSpecial());
        }
        if (episode.getTitle() != null) {
            stringBuffer.append(" - ").append(episode.getTitle());
        }
        return stringBuffer;
    }

    public String formatMultiEpisode(Episode ... episodeArray) {
        Function<Episode, String> function2 = Episode::getSeriesName;
        Function<Episode, String> function3 = this::formatSxE;
        Function<Episode, String> function4 = episode -> this.formatMultiTitle((Episode)episode);
        return Stream.of(function2, function3, function4).map(function -> EpisodeUtilities.streamMultiEpisode(episodeArray).map(function).filter(string -> string.length() > 0).distinct().collect(Collectors.joining(" & "))).collect(Collectors.joining(" - "));
    }

    public String formatSxE(Episode episode) {
        if (episode instanceof MultiEpisode) {
            return this.formatMultiRangeSxE((MultiEpisode)episode);
        }
        StringBuilder stringBuilder = new StringBuilder();
        if (episode.getSeason() != null || episode.getSpecial() != null) {
            stringBuilder.append(episode.getSpecial() == null ? episode.getSeason() : 0).append('x');
        }
        if (episode.getEpisode() != null || episode.getSpecial() != null) {
            stringBuilder.append(String.format(Locale.ROOT, "%02d", episode.getSpecial() == null ? episode.getEpisode() : episode.getSpecial()));
        }
        return stringBuilder.toString();
    }

    public String formatS00E00(Episode episode) {
        if (episode instanceof MultiEpisode) {
            return this.formatMultiRangeS00E00((MultiEpisode)episode);
        }
        StringBuilder stringBuilder = new StringBuilder();
        if (episode.getSeason() != null || episode.getSpecial() != null) {
            stringBuilder.append(String.format(Locale.ROOT, "S%02d", episode.getSpecial() == null ? episode.getSeason() : 0));
        }
        if (episode.getEpisode() != null || episode.getSpecial() != null) {
            stringBuilder.append(String.format(Locale.ROOT, "E%02d", episode.getSpecial() == null ? episode.getEpisode() : episode.getSpecial()));
        }
        return stringBuilder.toString();
    }

    public String formatMultiTitle(Episode ... episodeArray) {
        return EpisodeUtilities.streamMultiEpisode(episodeArray).map(Episode::getTitle).filter(Objects::nonNull).map(Normalization::removeTrailingBrackets).distinct().collect(Collectors.joining(" & "));
    }

    public String formatMultiRangeSxE(Iterable<Episode> iterable) {
        return this.formatMultiRangeNumbers(iterable, "%01dx", "%02d", "x", "-", " - ");
    }

    public String formatMultiRangeS00E00(Iterable<Episode> iterable) {
        return this.formatMultiRangeNumbers(iterable, "S%02d", "E%02d", "", "-", " - ");
    }

    public String formatMultiRangeNumbers(Iterable<Episode> iterable, String string, String string2, String string3, String string4, String string5) {
        return this.getSeasonEpisodeNumbers(iterable).entrySet().stream().map(entry -> {
            String string6 = (Integer)entry.getKey() >= 0 ? String.format(Locale.ROOT, string, entry.getKey()) : "";
            SortedSet<Integer> sortedSet = entry.getValue();
            if (IntStream.rangeClosed(sortedSet.first(), sortedSet.last()).allMatch(sortedSet::contains)) {
                return Stream.of(sortedSet.first(), sortedSet.last()).distinct().map(n -> String.format(Locale.ROOT, string2, n)).filter(s -> !s.isEmpty()).collect(Collectors.joining(string4, string6, ""));
            }
            return sortedSet.stream().map(n -> String.format(Locale.ROOT, string2, n)).filter(s -> !s.isEmpty()).collect(Collectors.joining(string3, string6, ""));
        }).collect(Collectors.joining(string5));
    }

    public String formatMultiSxE(Iterable<Episode> iterable) {
        return this.formatMultiNumbers(iterable, "%01dx", "%02d", "x");
    }

    public String formatMultiS00E00(Iterable<Episode> iterable) {
        return this.formatMultiNumbers(iterable, "S%02d", "E%02d", "-");
    }

    public String formatMultiNumbers(Iterable<Episode> iterable, String string, String string2, String string3) {
        return this.getSeasonEpisodeNumbers(iterable).entrySet().stream().map(entry -> {
            String string4 = (Integer)entry.getKey() >= 0 ? String.format(Locale.ROOT, string, entry.getKey()) : "";
            SortedSet<Integer> sortedSet = entry.getValue();
            return sortedSet.stream().map(n -> String.format(Locale.ROOT, string2, n)).collect(Collectors.joining(string3, string4, ""));
        }).collect(Collectors.joining(" - "));
    }

    private SortedMap<Integer, SortedSet<Integer>> getSeasonEpisodeNumbers(Iterable<Episode> iterable) {
        TreeMap<Integer, SortedSet<Integer>> treeMap = new TreeMap<Integer, SortedSet<Integer>>();
        for (Episode episode : iterable) {
            Integer n2 = episode.getSeason() == null || episode.getSpecial() != null ? (episode.getSpecial() == null ? -1 : 0) : episode.getSeason();
            Integer n3 = episode.getEpisode() == null ? (episode.getSpecial() == null ? -1 : episode.getSpecial()) : episode.getEpisode();
            treeMap.computeIfAbsent(n2, n -> new TreeSet()).add(n3);
        }
        return treeMap;
    }

    @Override
    public Episode parseObject(String string, ParsePosition parsePosition) {
        StringBuilder stringBuilder = new StringBuilder(string);
        Integer n = null;
        Integer n2 = null;
        Integer n3 = null;
        SimpleDate simpleDate = null;
        Matcher matcher = this.airdatePattern.matcher(stringBuilder);
        if (matcher.find()) {
            simpleDate = SimpleDate.parse(matcher.group(1));
            stringBuilder.replace(matcher.start(), matcher.end(), "");
        }
        if ((matcher = this.sxePattern.matcher(stringBuilder)).find()) {
            Integer n4 = n = matcher.group(1) == null ? null : Integer.valueOf(Integer.parseInt(matcher.group(1)));
            if (matcher.group(2) == null) {
                n2 = Integer.parseInt(matcher.group(3));
            } else {
                n3 = Integer.parseInt(matcher.group(3));
            }
            stringBuilder.replace(matcher.start(), matcher.end(), "");
            String string2 = stringBuilder.substring(0, matcher.start()).trim();
            String string3 = stringBuilder.substring(matcher.start()).trim();
            parsePosition.setIndex(stringBuilder.length());
            return new Episode(string2, n, n2, string3, n == null ? n2 : null, n3, simpleDate, null, null, null, null);
        }
        parsePosition.setErrorIndex(0);
        return null;
    }

    @Override
    public Episode parseObject(String string) throws ParseException {
        return (Episode)super.parseObject(string);
    }
}

