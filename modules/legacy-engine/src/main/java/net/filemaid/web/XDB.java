package net.filemaid.web;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import net.filemaid.WebServices;
import net.filemaid.media.MediaDetection;
import net.filemaid.similarity.SimilarityComparator;
import net.filemaid.web.AnimeLists;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.Series;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SortOrder;

public enum XDB {
    TheMovieDB,
    TheTVDB,
    IMDb,
    AniDB,
    TVmaze;


    public Integer getExternalId(Integer n, XDB xDB) throws Exception {
        if (this == xDB) {
            return n;
        }
        Series series = this.getExternalSeries(n);
        if (series != null) {
            return series.getExternalId(xDB);
        }
        return null;
    }

    public Series getExternalSeries(Integer n) throws Exception {
        if (n == null || n < 1) {
            return null;
        }
        Series series = this.queryLocalIndex(n);
        if (series != null) {
            return series;
        }
        Series series2 = this.queryExternalDatabase(n);
        if (series2 != null) {
            return series2;
        }
        return this.querySourceDatabase(n);
    }

    private Series queryLocalIndex(Integer n) throws Exception {
        switch (this) {
            case TheMovieDB: 
            case TheTVDB: 
            case IMDb: {
                return MediaDetection.releaseInfo.getSeriesIndex().stream().filter(series -> n.equals(series.getExternalId(this))).findFirst().orElse(null);
            }
            case AniDB: {
                return WebServices.AnimeList.find(AnimeLists.DB.AniDB, n).map(entry -> Series.XDB(entry.tvdbname, null, null, Series.XID(entry.tmdbid, entry.tvdbid, null, entry.anidbid))).findFirst().orElse(null);
            }
        }
        return null;
    }

    private Series queryExternalDatabase(Integer n) throws Exception {
        switch (this) {
            case TheMovieDB: 
            case TheTVDB: 
            case IMDb: {
                return WebServices.TheMovieDB_TV.getExternalSeries(this, n);
            }
            case AniDB: {
                return WebServices.AniDB.getIndex().stream().filter(searchResult -> n.equals(searchResult.getId())).map(searchResult -> Series.XDB(AniDB, searchResult.getId(), searchResult.getName())).findFirst().orElse(null);
            }
        }
        return null;
    }

    private Series querySourceDatabase(Integer n) throws Exception {
        switch (this) {
            case TheMovieDB: {
                return Series.XDB(TheMovieDB, n, WebServices.TheMovieDB_TV.getSeriesInfo(n, Locale.US).getName());
            }
            case TheTVDB: {
                return Series.XDB(TheTVDB, n, WebServices.TheTVDB.getSeriesInfo(n, Locale.US).getName());
            }
            case AniDB: {
                return Series.XDB(AniDB, n, WebServices.AniDB.getSeriesInfo(n, Locale.US).getName());
            }
        }
        return null;
    }

    public static Episode map(XDB xDB, Episode episode, XDB xDB2, EpisodeListProvider episodeListProvider) throws Exception {
        SeriesInfo seriesInfo = episode.getSeriesInfo();
        if (episode.getAirdate() == null && episode.getAbsolute() == null) {
            throw new IllegalArgumentException("Cannot map " + seriesInfo + " episode: airdate and absolute episode number are undefined");
        }
        Integer n = xDB.getExternalId(seriesInfo.getId(), xDB2);
        if (n == null) {
            throw new NoSuchElementException("Corresponding " + xDB2 + " series for " + seriesInfo + " not found");
        }
        return episodeListProvider.getEpisodeList(n, SortOrder.valueOf(seriesInfo.getOrder()), seriesInfo.getLanguage()).stream().filter(episode2 -> episode.getAirdate() != null && episode.getAirdate().equals(episode2.getAirdate()) || episode.getAbsolute() != null && episode.getAbsolute().equals(episode2.getAbsolute())).sorted(SimilarityComparator.compareTo(episode.getTitle(), Episode::getTitle)).findFirst().orElseThrow(() -> new NoSuchElementException("Corresponding " + xDB2 + "::" + n + " episode #" + episode.getAbsolute() + " aired on " + episode.getAirdate() + " for " + seriesInfo + " not found"));
    }

    public static List<String> names() {
        return Arrays.stream(XDB.values()).map(Enum::name).collect(Collectors.toList());
    }

    public static XDB forName(String string) {
        for (XDB xDB : XDB.values()) {
            if (!xDB.name().equalsIgnoreCase(string)) continue;
            return xDB;
        }
        throw new IllegalArgumentException(string + " not in " + XDB.names());
    }
}

