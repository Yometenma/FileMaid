package net.filemaid.web;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.WebServices;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.StringUtilities;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.SortOrder;
import net.filemaid.web.WebRequest;

public class AnimeLists
implements Datasource {
    @Override
    public String getIdentifier() {
        return "AnimeLists";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    public Optional<Episode> map(Episode episode, DB dB, DB dB2) throws Exception {
        int n;
        int n2;
        int n3 = (episode = EpisodeUtilities.reorderEpisode(episode, dB.order())).getSeriesInfo().getId();
        Entry entry = this.find(dB, n3, n2 = this.getSeasonNumber(dB, episode), n = (EpisodeUtilities.isSpecialEpisode(episode) ? episode.getSpecial() : episode.getEpisode()).intValue()).findFirst().orElse(null);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.mapping != null && n2 >= 0) {
            for (Mapping mapping : entry.mapping) {
                int n4;
                if (n2 != this.getSeason(dB, mapping) || (n4 = this.getSeason(dB2, mapping)) < 0) continue;
                int n5 = this.getEpisodeNumber(dB2, mapping, n);
                if (n5 > 0) {
                    return Optional.of(this.derive(dB2, entry, n4, n5, episode.getAirdate(), this.getSeriesInfo(dB2, entry)));
                }
                if (n5 != 0) continue;
                return Optional.empty();
            }
        }
        if (entry.tmdbtv != null && entry.tmdbseason == null && dB2 == DB.TheMovieDB) {
            return this.mapAutoAligned(entry, episode, episode.getSeriesInfo().getLanguage(), dB, dB2).findFirst();
        }
        if (entry.tvdbid != null && entry.defaulttvdbseason == null) {
            return this.mapAutoAligned(entry, episode, episode.getSeriesInfo().getLanguage(), dB, dB2).findFirst();
        }
        n2 = this.getSeason(dB2, entry, episode);
        return Optional.of(this.derive(dB2, entry, n2, n += this.offsetEpisodeNumber(entry, dB, dB2), episode.getAirdate(), this.getSeriesInfo(dB2, entry)));
    }

    protected Episode derive(DB dB, Entry entry, int n, int n2, SimpleDate simpleDate, SeriesInfo seriesInfo) {
        switch (dB) {
            case AniDB: {
                if (n == 0) {
                    return new Episode(entry.name, null, null, null, null, n2, simpleDate, null, null, null, seriesInfo);
                }
                return new Episode(entry.name, null, n2, null, null, null, simpleDate, null, null, null, seriesInfo);
            }
        }
        if (n == 0) {
            return new Episode(entry.tvdbname, null, null, null, null, n2, simpleDate, null, null, null, seriesInfo);
        }
        return new Episode(entry.tvdbname, n, n2, null, null, null, simpleDate, null, null, null, seriesInfo);
    }

    public int map(int n, int n2, int n3, DB dB, DB dB2) throws Exception {
        SeriesInfo seriesInfo;
        Entry entry = this.find(dB, n, n2, n3).findFirst().orElse(null);
        if (entry != null && (seriesInfo = this.getSeriesInfo(dB2, entry)) != null) {
            return seriesInfo.getId();
        }
        return -1;
    }

    public Optional<String> mapName(int n, int n2, int n3, DB dB, DB dB2) throws Exception {
        return this.find(dB, n, n2, n3).map(entry -> this.getName(dB2, (Entry)entry)).findFirst();
    }

    protected Stream<Episode> mapAutoAligned(Entry entry, Episode episode2, Locale locale, DB dB, DB dB2) throws Exception {
        if (episode2.getAbsolute() != null) {
            Integer n = this.getId(dB2, entry);
            Integer n2 = episode2.getAbsolute() + this.offsetEpisodeNumber(entry, dB, dB2);
            if (n > 0 && n2 > 0) {
                switch (dB2) {
                    case AniDB: {
                        return WebServices.AniDB.getEpisodeList(n, SortOrder.Absolute, locale).stream().filter(episode -> n2.equals(episode.getEpisode()));
                    }
                    case TheTVDB: {
                        return WebServices.TheTVDB.getEpisodeList(n, SortOrder.Airdate, locale).stream().filter(episode -> n2.equals(episode.getAbsolute()));
                    }
                    case TheMovieDB: {
                        return WebServices.TheMovieDB_TV.getEpisodeList(n, SortOrder.Airdate, locale).stream().filter(episode -> n2.equals(episode.getAbsolute()));
                    }
                }
            }
        }
        return Stream.empty();
    }

    protected int offsetEpisodeNumber(Entry entry, DB dB, DB dB2) throws Exception {
        return this.getEpisodeNumberOffset(dB2, entry) - this.getEpisodeNumberOffset(dB, entry);
    }

    protected SeriesInfo getSeriesInfo(DB dB, Entry entry) throws Exception {
        int n = this.getId(dB, entry);
        if (n > 0) {
            switch (dB) {
                case AniDB: {
                    return this.createSeriesInfo(WebServices.AniDB, SortOrder.Absolute, n, "Anime");
                }
                case TheTVDB: {
                    return this.createSeriesInfo(WebServices.TheTVDB, SortOrder.Airdate, n, "TV Series");
                }
                case TheMovieDB: {
                    return this.createSeriesInfo(WebServices.TheMovieDB_TV, SortOrder.Airdate, n, "TV Series");
                }
            }
        }
        return null;
    }

    protected SeriesInfo createSeriesInfo(EpisodeListProvider episodeListProvider, SortOrder sortOrder, int n, String string) throws Exception {
        SeriesInfo seriesInfo = new SeriesInfo(episodeListProvider, sortOrder, Locale.ENGLISH, n, string);
        episodeListProvider.getIndex().stream().filter(searchResult -> searchResult.id == n).findFirst().ifPresent(searchResult -> {
            seriesInfo.setName(searchResult.getName());
            seriesInfo.setAliasNames(searchResult.getAliasNames());
        });
        return seriesInfo;
    }

    protected int getEpisodeNumber(DB dB, Mapping mapping, int n) {
        if (mapping.numbers != null) {
            switch (dB) {
                case AniDB: {
                    for (int[] nArray : mapping.numbers) {
                        if (n != nArray[1]) continue;
                        return nArray[0];
                    }
                    break;
                }
                default: {
                    for (int[] nArray : mapping.numbers) {
                        if (n != nArray[0]) continue;
                        return nArray[1];
                    }
                }
            }
        }
        if (mapping.start != null && mapping.end != null) {
            switch (dB) {
                case AniDB: {
                    int n2;
                    int n3 = n2 = mapping.offset != null ? n - mapping.offset : n;
                    if (n2 < mapping.start || n2 > mapping.end) break;
                    return n2;
                }
                default: {
                    if (n < mapping.start || n > mapping.end) break;
                    return mapping.offset != null ? n + mapping.offset : n;
                }
            }
        }
        return -1;
    }

    protected int getEpisodeNumberOffset(DB dB, Entry entry) {
        switch (dB) {
            case TheTVDB: {
                return entry.episodeoffset != null ? entry.episodeoffset : 0;
            }
            case TheMovieDB: {
                return entry.tmdboffset != null ? entry.tmdboffset : 0;
            }
        }
        return 0;
    }

    protected int getSeasonNumber(DB dB, Episode episode) {
        if (EpisodeUtilities.isSpecialEpisode(episode)) {
            return 0;
        }
        if (episode.getSeason() != null) {
            return episode.getSeason();
        }
        switch (dB) {
            case AniDB: {
                return 1;
            }
        }
        return -1;
    }

    protected int getSeason(DB dB, Mapping mapping) {
        switch (dB) {
            case AniDB: {
                return mapping.anidbseason != null ? mapping.anidbseason : -1;
            }
            case TheTVDB: {
                return mapping.tvdbseason != null ? mapping.tvdbseason : -1;
            }
            case TheMovieDB: {
                return mapping.tmdbseason != null ? mapping.tmdbseason : -1;
            }
        }
        return -1;
    }

    protected int getSeason(DB dB, Entry entry, Episode episode) {
        if (EpisodeUtilities.isSpecialEpisode(episode)) {
            return 0;
        }
        switch (dB) {
            case AniDB: {
                return 1;
            }
            case TheTVDB: {
                return entry.defaulttvdbseason != null ? entry.defaulttvdbseason : -1;
            }
            case TheMovieDB: {
                return entry.tmdbseason != null ? entry.tmdbseason : -1;
            }
        }
        return -1;
    }

    protected int getId(DB dB, Entry entry) {
        switch (dB) {
            case AniDB: {
                return entry.anidbid != null ? entry.anidbid : -1;
            }
            case TheTVDB: {
                return entry.tvdbid != null ? entry.tvdbid : -1;
            }
            case TheMovieDB: {
                return entry.tmdbtv != null ? entry.tmdbtv : (entry.tmdbid != null ? entry.tmdbid : -1);
            }
        }
        return -1;
    }

    protected String getName(DB dB, Entry entry) {
        switch (dB) {
            case AniDB: {
                return entry.name;
            }
        }
        return entry.tvdbname;
    }

    protected boolean isValid(Entry entry) {
        return entry.anidbid != null && (entry.tvdbid != null || entry.tmdbtv != null);
    }

    public Stream<Entry> find(DB dB, int n) throws Exception {
        return Arrays.stream(this.getModel().anime).filter(this::isValid).filter(entry -> n == this.getId(dB, (Entry)entry));
    }

    public Stream<Entry> find(DB dB, int n, int n2, int n3) throws Exception {
        switch (dB) {
            case AniDB: {
                return this.find(dB, n);
            }
            case TheTVDB: {
                return this.find(dB, n).filter(entry -> !(entry.defaulttvdbseason != null && n2 != entry.defaulttvdbseason || entry.episodeoffset != null && n3 <= entry.episodeoffset)).sorted(Comparator.comparingInt(entry -> -this.getEpisodeNumberOffset(dB, (Entry)entry)));
            }
            case TheMovieDB: {
                return this.find(dB, n).filter(entry -> !(entry.tmdbseason != null && n2 != entry.tmdbseason || entry.tmdboffset != null && n3 <= entry.tmdboffset)).sorted(Comparator.comparingInt(entry -> -this.getEpisodeNumberOffset(dB, (Entry)entry)));
            }
        }
        return Stream.empty();
    }

    public Cache getCache() {
        return Cache.getCache(this.getIdentifier(), CacheType.Monthly);
    }

    public Model getModel() throws Exception {
        return this.getCache().stream("anime-list.xml", string -> WebRequest.newURL("https://github.com/Anime-Lists/anime-lists/raw/master/" + string), AnimeLists::parseModel).expire(Cache.ONE_WEEK).get();
    }

    public static Model parseModel(InputStream inputStream) {
        return AnimeLists.unmarshal(inputStream, Model.class);
    }

    protected static <T> T unmarshal(InputStream inputStream, Class<T> clazz) {
        try {
            return (T)JAXBContext.newInstance((Class[])new Class[]{clazz}).createUnmarshaller().unmarshal(inputStream);
        }
        catch (Exception exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    protected static <T> String marshal(T t, Class<T> clazz) {
        try {
            StringWriter stringWriter = new StringWriter();
            Marshaller marshaller = JAXBContext.newInstance((Class[])new Class[]{clazz}).createMarshaller();
            marshaller.setProperty("jaxb.formatted.output", (Object)Boolean.FALSE);
            marshaller.setProperty("jaxb.fragment", (Object)Boolean.TRUE);
            marshaller.marshal(t, (Writer)stringWriter);
            return stringWriter.toString();
        }
        catch (Exception exception) {
            return exception.toString();
        }
    }

    public static enum DB {
        AniDB,
        TheTVDB,
        TheMovieDB;


        public SortOrder order() {
            return this == AniDB ? SortOrder.Absolute : SortOrder.Airdate;
        }

        public static List<String> names() {
            return Arrays.stream(DB.values()).map(Enum::name).collect(Collectors.toList());
        }

        public static DB get(Episode episode) {
            return DB.get(episode.getSeriesInfo());
        }

        public static DB get(SeriesInfo seriesInfo) {
            if (EpisodeUtilities.isInstance((Datasource)WebServices.AniDB, seriesInfo)) {
                return AniDB;
            }
            if (EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, seriesInfo)) {
                return TheTVDB;
            }
            if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, seriesInfo)) {
                return TheMovieDB;
            }
            throw new IllegalArgumentException("AnimeLists.DB not supported: " + seriesInfo);
        }

        public static DB get(String string) {
            for (DB dB : DB.values()) {
                if (!dB.name().equalsIgnoreCase(string)) continue;
                return dB;
            }
            throw new IllegalArgumentException(string + " not in " + DB.names());
        }
    }

    @XmlRootElement(name="anime")
    public static class Entry {
        @XmlAttribute
        public Integer anidbid;
        @XmlJavaTypeAdapter(value=NumberAdapter.class)
        @XmlAttribute
        public Integer tvdbid;
        @XmlJavaTypeAdapter(value=NumberAdapter.class)
        @XmlAttribute
        public Integer tmdbtv;
        @XmlJavaTypeAdapter(value=NumberAdapter.class)
        @XmlAttribute
        public Integer tmdbid;
        @XmlJavaTypeAdapter(value=NumberAdapter.class)
        @XmlAttribute
        public Integer defaulttvdbseason;
        @XmlJavaTypeAdapter(value=NumberAdapter.class)
        @XmlAttribute
        public Integer tmdbseason;
        @XmlJavaTypeAdapter(value=NumberAdapter.class)
        @XmlAttribute
        public Integer episodeoffset;
        @XmlJavaTypeAdapter(value=NumberAdapter.class)
        @XmlAttribute
        public Integer tmdboffset;
        @XmlElement
        public String name;
        @XmlElement
        public String tvdbname;
        @XmlElementWrapper(name="mapping-list")
        public Mapping[] mapping;

        public String toString() {
            return AnimeLists.marshal(this, Entry.class);
        }
    }

    @XmlRootElement(name="mapping")
    public static class Mapping {
        @XmlAttribute
        public Integer anidbseason;
        @XmlAttribute
        public Integer tvdbseason;
        @XmlAttribute
        public Integer tmdbseason;
        @XmlAttribute
        public Integer start;
        @XmlAttribute
        public Integer end;
        @XmlAttribute
        public Integer offset;
        @XmlJavaTypeAdapter(value=NumberMapAdapter.class)
        @XmlValue
        public int[][] numbers;

        public String toString() {
            return AnimeLists.marshal(this, Mapping.class);
        }
    }

    @XmlRootElement(name="anime-list")
    public static class Model {
        @XmlElement
        public Entry[] anime;

        public String toString() {
            return AnimeLists.marshal(this, Model.class);
        }
    }

    protected static class NumberMapAdapter
    extends XmlAdapter<String, int[][]> {
        protected NumberMapAdapter() {
        }

        public int[][] unmarshal(String string2) throws Exception {
            return (int[][])StringUtilities.tokenize(string2, RegularExpressions.SEMICOLON).map(string -> StringUtilities.matchIntegers(string)).filter(list -> list.size() == 2).map(list -> list.stream().mapToInt(n -> n).toArray()).toArray(n -> new int[n][]);
        }

        public String marshal(int[][] nArray2) throws Exception {
            return Arrays.stream(nArray2).map(nArray -> StringUtilities.join(IntStream.of(nArray).boxed(), (CharSequence)"-")).collect(Collectors.joining(";"));
        }
    }

    protected static class NumberAdapter
    extends XmlAdapter<String, Integer> {
        protected NumberAdapter() {
        }

        public Integer unmarshal(String string) throws Exception {
            return StringUtilities.matchInteger(string);
        }

        public String marshal(Integer n) throws Exception {
            return n == null ? null : Integer.toString(n);
        }
    }
}

