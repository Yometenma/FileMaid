package net.filemaid.similarity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.StringUtilities;

public class SeasonEpisodeMatcher {
    public static final SeasonEpisodeFilter LENIENT_SANITY = new SeasonEpisodeFilter(300, 3000, 5000, 1920, 2040);
    public static final SeasonEpisodeFilter DEFAULT_SANITY = new SeasonEpisodeFilter(30, 50, 1000, 1920, 2040);
    public static final SeasonEpisodeFilter STRICT_SANITY = new SeasonEpisodeFilter(10, 30, -1, -1, -1);
    private final SeasonEpisodeParser[] patterns;
    private final Pattern seasonPattern;
    private final int depth;

    public SeasonEpisodeMatcher(SeasonEpisodeFilter seasonEpisodeFilter, boolean bl) {
        this(1, seasonEpisodeFilter, bl);
    }

    public SeasonEpisodeMatcher(int n, SeasonEpisodeFilter seasonEpisodeFilter, boolean bl) {
        this.depth = n;
        SeasonEpisodePattern seasonEpisodePattern = new SeasonEpisodePattern(null, "(?<!\\p{Alnum})(?i:Season|Series)[^\\p{Alnum}]{0,3}(\\d{1,4})[^\\p{Alnum}]{0,3}(?i:Episode|Part|Act)[^\\p{Alnum}]{0,3}((?:\\d{1,4}(?:\\D|$))+)[^\\p{Alnum}]{0,3}(?!\\p{Digit})", matchResult -> this.range(matchResult.group(1), matchResult.group(2)));
        SeasonEpisodePattern seasonEpisodePattern2 = new SeasonEpisodePattern(null, "(?<!\\p{Alnum}|[-])[Ss](\\d{1,4})[Ee](\\d{2,3})[-][Ee]?(\\d{2,4})(?!\\p{Alnum}|[-])", matchResult -> this.range(matchResult.group(1), matchResult.group(2), matchResult.group(3)));
        SeasonEpisodePattern seasonEpisodePattern3 = new SeasonEpisodePattern(null, "(?<!\\p{Digit})[Ss](\\d{1,4})[^\\p{Alnum}]{0,3}(?i:ep|e|p|-)((?:(?<=[^._ ])[Ee]?[Pp]?\\d{1,4}(?:\\D|$))+)", matchResult -> this.multi(matchResult.group(1), matchResult.group(2)));
        SeasonEpisodePattern seasonEpisodePattern4 = new SeasonEpisodePattern(seasonEpisodeFilter, "(?<!\\p{Alnum})(\\d{1,2}x\\d{2}(?:[-._ ]\\d{1,2}x\\d{2})+)(?!\\p{Digit})", matchResult -> this.pairs(matchResult.group()));
        SeasonEpisodePattern seasonEpisodePattern5 = new SeasonEpisodePattern(seasonEpisodeFilter, "(?<!\\p{Alnum})[Ss]?(\\d{1,4})[xXEe]((?:(?<=[^._ ])\\d{2,4}(?:\\D|$))+)", matchResult -> this.multi(matchResult.group(1), matchResult.group(2)));
        SeasonEpisodePattern seasonEpisodePattern6 = new SeasonEpisodePattern(null, "(?<!\\p{Digit})(?:S|s|Season|Series)[._ ]?(\\d{1,4})(?:[._ ]-[._ ])(\\d{1,4}(?:[-]\\d{1,4})*)(?:\\D|$)", matchResult -> this.multi(matchResult.group(1), matchResult.group(2)));
        SeasonEpisodePattern seasonEpisodePattern7 = new SeasonEpisodePattern(seasonEpisodeFilter, "(?<!\\p{Alnum}|\\d{4}[.])(\\d{1,2})[.]((?:(?<=[^._ ])\\d{2}(?:\\D|$))+)", matchResult -> this.multi(matchResult.group(1), matchResult.group(2)));
        SeasonEpisodePattern seasonEpisodePattern8 = new SeasonEpisodePattern(seasonEpisodeFilter, "(?<=\\p{Alnum})[\\P{Alnum}]{0,3}((?i:EP|Episode|Act)[\\P{Alnum}]{0,3}\\d{2,4})(?![-]?\\p{Digit})", matchResult -> this.single(null, matchResult.group(1)));
        SeasonEpisodePattern seasonEpisodePattern9 = new SeasonEpisodePattern(seasonEpisodeFilter, "(?<!\\p{Alnum}|[-])(\\d{2,3})[-](\\d{2,3})(?!\\p{Alnum}|[-])", matchResult -> this.range(null, matchResult.group(1), matchResult.group(2)));
        SeasonEpisodePattern seasonEpisodePattern10 = new SeasonEpisodePattern(null, "(?<!\\p{Alnum})SP(?:[._ ]-[._ ])?(\\d{1,2}(?:[-]\\d{1,2})*)(?:\\D|$)", matchResult -> this.special(matchResult.group(1)));
        SeasonEpisodePattern seasonEpisodePattern11 = new SeasonEpisodePattern(seasonEpisodeFilter, "(?<!\\p{Alnum})(\\d{2}|\\d{4})?[\\P{Alnum}]{0,3}((?:(?i:e|p|ep|episode|act)[\\P{Alnum}]{0,3}\\d{1,4})+)(?!\\p{Digit})", matchResult -> this.multi(matchResult.group(1), matchResult.group(2)));
        SeasonEpisodePattern seasonEpisodePattern12 = new SeasonEpisodePattern(seasonEpisodeFilter, "(?<!\\p{Alnum})([0-2]?\\d?)(\\d{2})(\\d{2})?(?!\\p{Alnum})", matchResult -> this.numbers(matchResult.group(1), (String[])StringUtilities.streamCapturingGroups(matchResult).skip(1L).toArray(String[]::new)));
        SeasonEpisodePattern seasonEpisodePattern13 = new SeasonEpisodePattern(seasonEpisodeFilter, "(?<!\\p{Alnum})(\\d{1,2})[^._ ]?(?i:of)[^._ ]?(\\d{1,2})(?!\\p{Digit})", matchResult -> this.single(null, matchResult.group(1)));
        SeasonEpisodePattern seasonEpisodePattern14 = new SeasonEpisodePattern(seasonEpisodeFilter, "(?<=[._ ][-][._ ])(\\d{1,4})(?!\\p{Alnum})", matchResult -> this.single(null, matchResult.group(1)));
        SeasonEpisodePattern seasonEpisodePattern15 = new SeasonEpisodePattern(seasonEpisodeFilter, "[Ss](\\d{2})[Ee](\\d{2})", matchResult -> this.single(matchResult.group(1), matchResult.group(2)));
        SeasonEpisodePattern seasonEpisodePattern16 = new SeasonEpisodePattern(STRICT_SANITY, "(?<!\\p{Digit})(\\d{1})(\\d{2})(?!\\p{Alnum})(?:.*)", matchResult -> this.single(matchResult.group(1), matchResult.group(2)));
        this.patterns = bl ? new SeasonEpisodeParser[]{seasonEpisodePattern, seasonEpisodePattern2, seasonEpisodePattern3, seasonEpisodePattern4, seasonEpisodePattern5, seasonEpisodePattern6, seasonEpisodePattern7} : new SeasonEpisodeParser[]{seasonEpisodePattern, seasonEpisodePattern2, seasonEpisodePattern3, seasonEpisodePattern4, seasonEpisodePattern5, seasonEpisodePattern6, seasonEpisodePattern7, seasonEpisodePattern8, seasonEpisodePattern9, seasonEpisodePattern10, new SeasonEpisodeUnion(seasonEpisodePattern11, seasonEpisodePattern12, seasonEpisodePattern13), seasonEpisodePattern14, seasonEpisodePattern15, seasonEpisodePattern16};
        this.seasonPattern = Pattern.compile("(?i:Season|Series)[-._ ]?(\\d{1,2})");
    }

    protected List<SxE> single(String string, String string2) {
        return Collections.singletonList(new SxE(string, string2));
    }

    protected List<SxE> multi(String string2, String ... stringArray) {
        Integer n = StringUtilities.matchInteger(string2);
        return Arrays.stream(stringArray).flatMap(string -> StringUtilities.matchIntegers(string).stream()).map(n2 -> new SxE(n, (Integer)n2)).collect(Collectors.toList());
    }

    protected List<SxE> special(String ... stringArray) {
        return Arrays.stream(stringArray).flatMap(string -> StringUtilities.matchIntegers(string).stream()).map(n -> new SxE(0, (Integer)n)).collect(Collectors.toList());
    }

    protected List<SxE> range(String string2, String ... stringArray) {
        IntSummaryStatistics intSummaryStatistics = Arrays.stream(stringArray).flatMap(string -> StringUtilities.matchIntegers(string).stream()).mapToInt(n -> n).summaryStatistics();
        if (string2 == null && intSummaryStatistics.getMax() - intSummaryStatistics.getMin() >= 9) {
            return Collections.emptyList();
        }
        Integer n3 = StringUtilities.matchInteger(string2);
        return IntStream.rangeClosed(intSummaryStatistics.getMin(), intSummaryStatistics.getMax()).boxed().map(n2 -> new SxE(n3, n2)).collect(Collectors.toList());
    }

    protected List<SxE> pairs(String string) {
        ArrayList<SxE> arrayList = new ArrayList<SxE>(2);
        String[] stringArray = RegularExpressions.NON_DIGIT.split(string);
        for (int i = 0; i < stringArray.length; i += 2) {
            arrayList.add(new SxE(stringArray[i], stringArray[i + 1]));
        }
        return arrayList;
    }

    protected List<SxE> numbers(String string, String ... stringArray) {
        ArrayList<SxE> arrayList = new ArrayList<SxE>(2);
        for (String string2 : stringArray) {
            SxE sxE = new SxE(string, string2);
            if (sxE.episode <= 0 || sxE.season <= 0) continue;
            arrayList.add(sxE);
        }
        if (stringArray.length == 1) {
            SxE sxE = new SxE(null, string + stringArray[0]);
            if (sxE.episode > 0 && !arrayList.contains(sxE)) {
                arrayList.add(sxE);
            }
        }
        return arrayList;
    }

    public List<SxE> match(CharSequence charSequence) {
        for (SeasonEpisodeParser seasonEpisodeParser : this.patterns) {
            List<SxE> list = seasonEpisodeParser.match(charSequence);
            if (list.isEmpty()) continue;
            return list;
        }
        return null;
    }

    public List<SxE> match(File file) {
        String[] stringArray = this.tokenizeTail(file);
        for (SeasonEpisodeParser seasonEpisodeParser : this.patterns) {
            for (int i = 0; i < stringArray.length; ++i) {
                List<SxE> list = seasonEpisodeParser.match(stringArray[i]);
                if (list.isEmpty()) continue;
                for (int j = 0; j < list.size(); ++j) {
                    Matcher matcher;
                    if (list.get((int)j).season >= 0 || i >= stringArray.length - 1 || !(matcher = this.seasonPattern.matcher(stringArray[i + 1])).find()) continue;
                    list.set(j, new SxE(Integer.parseInt(matcher.group(1)), list.get((int)j).episode));
                }
                return list;
            }
        }
        return null;
    }

    protected String[] tokenizeTail(File file2) {
        return (String[])FileUtilities.listPathTailReverse(file2, this.depth).stream().map(file -> FileUtilities.getName(file)).toArray(String[]::new);
    }

    public int find(CharSequence charSequence, int n) {
        for (SeasonEpisodeParser seasonEpisodeParser : this.patterns) {
            int n2 = seasonEpisodeParser.find(charSequence, n);
            if (n2 < 0) continue;
            return n2;
        }
        return -1;
    }

    public String head(String string) {
        int n = this.find(string, 0);
        if (n > 0) {
            return string.substring(0, n).trim();
        }
        return null;
    }

    public static class SeasonEpisodeFilter {
        public final int seasonLimit;
        public final int seasonEpisodeLimit;
        public final int absoluteEpisodeLimit;
        public final int seasonYearBegin;
        public final int seasonYearEnd;

        public SeasonEpisodeFilter(int n, int n2, int n3, int n4, int n5) {
            this.seasonLimit = n;
            this.seasonEpisodeLimit = n2;
            this.absoluteEpisodeLimit = n3;
            this.seasonYearBegin = n4;
            this.seasonYearEnd = n5;
        }

        public boolean filter(int n) {
            return n < this.seasonEpisodeLimit || n < this.absoluteEpisodeLimit;
        }

        public boolean filter(SxE sxE) {
            return sxE.season >= 0 && (sxE.season < this.seasonLimit || sxE.season > this.seasonYearBegin && sxE.season < this.seasonYearEnd) && sxE.episode < this.seasonEpisodeLimit || sxE.season < 0 && sxE.episode < this.absoluteEpisodeLimit;
        }

        public boolean filter(SxE sxE, List<SxE> list) {
            return this.filter(sxE) && list.stream().filter((SxE sxE2) -> sxE.season == -1 == (sxE2.season == -1) && sxE2.compareTo(sxE) > 0).count() == 0L;
        }
    }

    public static class SeasonEpisodePattern
    implements SeasonEpisodeParser {
        protected Pattern pattern;
        protected Function<MatchResult, List<SxE>> process;
        protected SeasonEpisodeFilter sanity;

        public SeasonEpisodePattern(SeasonEpisodeFilter seasonEpisodeFilter, String string) {
            this(seasonEpisodeFilter, string, matchResult -> Collections.singletonList(new SxE(matchResult.group(1), matchResult.group(2))));
        }

        public SeasonEpisodePattern(SeasonEpisodeFilter seasonEpisodeFilter, String string, Function<MatchResult, List<SxE>> function) {
            this.pattern = Pattern.compile(string);
            this.process = function;
            this.sanity = seasonEpisodeFilter;
        }

        public Matcher matcher(CharSequence charSequence) {
            return this.pattern.matcher(charSequence);
        }

        @Override
        public List<SxE> match(CharSequence charSequence) {
            ArrayList<SxE> arrayList = new ArrayList<SxE>(2);
            Matcher matcher = this.matcher(charSequence);
            while (matcher.find()) {
                for (SxE sxE : this.process.apply(matcher)) {
                    if (this.sanity != null && !this.sanity.filter(sxE, arrayList)) continue;
                    arrayList.add(sxE);
                }
            }
            return arrayList;
        }

        @Override
        public int find(CharSequence charSequence, int n) {
            Matcher matcher = this.matcher(charSequence).region(n, charSequence.length());
            while (matcher.find()) {
                for (SxE sxE : this.process.apply(matcher)) {
                    if (this.sanity != null && !this.sanity.filter(sxE)) continue;
                    return matcher.start();
                }
            }
            return -1;
        }

        public String toString() {
            return this.pattern.pattern();
        }
    }

    public static interface SeasonEpisodeParser {
        public List<SxE> match(CharSequence var1);

        public int find(CharSequence var1, int var2);
    }

    public static class SeasonEpisodeUnion
    implements SeasonEpisodeParser {
        private final SeasonEpisodeParser[] parsers;

        public SeasonEpisodeUnion(SeasonEpisodeParser ... seasonEpisodeParserArray) {
            this.parsers = seasonEpisodeParserArray;
        }

        @Override
        public List<SxE> match(CharSequence charSequence) {
            LinkedHashSet<SxE> linkedHashSet = new LinkedHashSet<SxE>();
            for (SeasonEpisodeParser seasonEpisodeParser : this.parsers) {
                linkedHashSet.addAll(seasonEpisodeParser.match(charSequence));
            }
            return new ArrayList<SxE>(linkedHashSet);
        }

        @Override
        public int find(CharSequence charSequence, int n) {
            int n2 = -1;
            for (SeasonEpisodeParser seasonEpisodeParser : this.parsers) {
                int n3 = seasonEpisodeParser.find(charSequence, n);
                if (n3 < 0 || n3 <= n2) continue;
                n2 = n3;
            }
            return n2;
        }
    }

    public static class SxE
    implements Comparable<SxE> {
        public static final int UNDEFINED = -1;
        public final int season;
        public final int episode;

        public SxE(Integer n, Integer n2) {
            this.season = n != null ? n : -1;
            this.episode = n2 != null ? n2 : -1;
        }

        public SxE(String string, String string2) {
            this.season = this.parse(string);
            this.episode = this.parse(string2);
        }

        protected int parse(String string) {
            try {
                return Integer.parseInt(string);
            }
            catch (Exception exception) {
                return -1;
            }
        }

        public boolean equals(Object object) {
            if (object instanceof SxE) {
                SxE sxE = (SxE)object;
                return this.season == sxE.season && this.episode == sxE.episode;
            }
            return false;
        }

        @Override
        public int compareTo(SxE sxE) {
            return this.season < sxE.season ? -1 : (this.season != sxE.season ? 1 : (this.episode < sxE.episode ? -1 : (this.episode != sxE.episode ? 1 : 0)));
        }

        public int hashCode() {
            return Objects.hash(this.season, this.episode);
        }

        public String toString() {
            return this.season >= 0 ? String.format(Locale.ROOT, "%dx%02d", this.season, this.episode) : String.format(Locale.ROOT, "%02d", this.episode);
        }
    }
}

