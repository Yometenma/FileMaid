package net.filemaid.web;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.filemaid.WebServices;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.MultiEpisode;
import net.filemaid.web.SeriesInfo;

public class DiscDB {
    private final Map<Integer, Series> ids = new HashMap<Integer, Series>(256);
    public static final Pattern SOURCE_FILE_PATTERN = DiscDB.compileWordPattern("(?:\\d{5})\\P{Alnum}+(?:t|title)\\d{1,2}");
    public static final Pattern DISC_INDEX_PATTERN = DiscDB.compileWordPattern("(?:Disc)\\P{Alnum}+\\d\\P{Alnum}+(?:t|title)\\d{1,2}");
    public static final Pattern TITLE_INDEX_PATTERN = DiscDB.compileWordPattern("title_t\\d{2}");

    public void add(int n, String string, int n2, String string2, String string3, int n4, String string4, int[] nArray, int n5, int n6, int[] nArray2, String string5) {
        Series series = this.ids.computeIfAbsent(n, n3 -> new Series(n, string, n2));
        Group group = series.computeIfAbsent(string2, string3);
        group.add(n4, string4, nArray, n5, n6, nArray2, string5);
    }

    public Episode match(SeriesInfo seriesInfo, File file) {
        Series series = this.ids.get(seriesInfo.getId());
        if (series == null) {
            return null;
        }
        ArrayList<Group> arrayList = new ArrayList<Group>();
        ArrayList<Episode> arrayList2 = new ArrayList<Episode>();
        block0: for (Group group : series.groups.values()) {
            for (EpisodeSourceFile episodeSourceFile : group.sourceFiles) {
                if (!episodeSourceFile.matches(file)) continue;
                arrayList.add(group);
                arrayList2.add(episodeSourceFile.episode);
                continue block0;
            }
        }
        if (arrayList2.size() > 1) {
            for (int i = 0; i < arrayList.size(); ++i) {
                if (!((Group)arrayList.get(i)).matches(file)) continue;
                return (Episode)arrayList2.get(i);
            }
        }
        if (arrayList2.size() > 0) {
            return (Episode)arrayList2.get(0);
        }
        return null;
    }

    private static Pattern compileWordSequence(String string) {
        return Pattern.compile("(?<!\\p{Alnum})(?:" + string.replaceAll("\\P{Alnum}+", "\\\\P{Alnum}*") + ")(?!\\p{Alnum})", 2);
    }

    private static Pattern compileWordPattern(String string) {
        return Pattern.compile("(?<!\\p{Alnum})(?:" + string + ")(?!\\p{Alnum})", 2);
    }

    private static boolean anyMatch(String string, Pattern ... patternArray) {
        return Stream.of(patternArray).anyMatch(pattern -> pattern.matcher(string).find());
    }

    private static boolean allMatch(String string, Pattern ... patternArray) {
        return Stream.of(patternArray).allMatch(pattern -> pattern.matcher(string).find());
    }

    public static boolean canMatch(SeriesInfo seriesInfo, File file) {
        return EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, seriesInfo) && DiscDB.anyMatch(file.getName(), SOURCE_FILE_PATTERN, DISC_INDEX_PATTERN, TITLE_INDEX_PATTERN);
    }

    public static class Series {
        protected final int id;
        protected final String name;
        protected final int year;
        private final Map<String, Group> groups = new LinkedHashMap<String, Group>(4);

        public Series(int n, String string, int n2) {
            this.id = n;
            this.name = string;
            this.year = n2;
        }

        public Group computeIfAbsent(String string, String string2) {
            return this.groups.computeIfAbsent(string2, (String string3) -> new Group(this, string, string2));
        }

        public String toString() {
            return this.name;
        }
    }

    public static class Group {
        protected final Series series;
        protected final String slug;
        protected final String name;
        private final List<EpisodeSourceFile> sourceFiles = new ArrayList<EpisodeSourceFile>();
        protected Pattern[] pattern;

        public Group(Series series, String string, String string2) {
            this.series = series;
            this.slug = string;
            this.name = string2;
        }

        public boolean matches(File file) {
            if (this.pattern == null) {
                this.pattern = this.slug == null || this.slug.isEmpty() ? new Pattern[]{DiscDB.compileWordSequence(this.name)} : new Pattern[]{DiscDB.compileWordSequence(this.slug), DiscDB.compileWordSequence(this.name)};
            }
            return DiscDB.anyMatch(file.getPath(), this.pattern);
        }

        private Episode episode(int n, int[] nArray, String string, int n2) {
            if (nArray.length > 1) {
                return IntStream.of(nArray).mapToObj(n3 -> new Episode(this.series.name, n, n3, string, null, null, null, n2, null, this.name, null)).collect(Collectors.collectingAndThen(Collectors.toList(), MultiEpisode::new));
            }
            return new Episode(this.series.name, n, nArray[0], string, null, null, null, n2, null, this.name, null);
        }

        public void add(int n, String string, int[] nArray, int n2, int n3, int[] nArray2, String string2) {
            this.sourceFiles.add(new EpisodeSourceFile(n, string, nArray, this.episode(n3, nArray2, string2, n2)));
        }

        public String toString() {
            return this.name;
        }
    }

    public static class EpisodeSourceFile {
        protected final int index;
        protected final String sourceFile;
        protected final int[] segmentMap;
        protected final Episode episode;
        protected Pattern[] pattern;

        public EpisodeSourceFile(int n, String string, int[] nArray, Episode episode) {
            this.index = n;
            this.sourceFile = string;
            this.segmentMap = nArray;
            this.episode = episode;
        }

        public boolean matches(File file) {
            if (this.pattern == null) {
                this.pattern = new Pattern[]{DiscDB.compileWordPattern(this.sourceFile), DiscDB.compileWordPattern("(?:t|title)[0]*" + this.index)};
            }
            if (SOURCE_FILE_PATTERN.matcher(file.getName()).find()) {
                return DiscDB.allMatch(file.getName(), this.pattern);
            }
            return DiscDB.anyMatch(file.getName(), this.pattern);
        }

        public String toString() {
            return this.episode + " [" + this.sourceFile + "_t" + this.index + "]";
        }
    }
}

