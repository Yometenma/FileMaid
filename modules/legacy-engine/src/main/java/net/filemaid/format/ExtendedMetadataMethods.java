package net.filemaid.format;

import groovy.lang.Script;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.filemaid.Cache;
import net.filemaid.WebServices;
import net.filemaid.web.Artwork;
import net.filemaid.web.ArtworkProvider;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeDetails;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.Extra;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieCollection;
import net.filemaid.web.MovieDetails;
import net.filemaid.web.Person;
import net.filemaid.web.Series;
import net.filemaid.web.SeriesDetails;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SortOrder;
import net.filemaid.web.XDB;

public class ExtendedMetadataMethods {
    public static MovieDetails getInfo(Movie movie) throws Exception {
        return WebServices.TheMovieDB.getMovieInfo(movie, movie.getLanguage(), true);
    }

    public static Map<String, String> getTranslations(Movie movie) throws Exception {
        if (movie.getTmdbId() > 0) {
            return WebServices.TheMovieDB.getTranslations(movie.getTmdbId());
        }
        return null;
    }

    public static Map<String, List<String>> getAlternativeTitles(Movie movie) throws Exception {
        if (movie.getTmdbId() > 0) {
            return WebServices.TheMovieDB.getAlternativeTitles(movie.getTmdbId());
        }
        return null;
    }

    public static MovieCollection getCollection(Movie movie) throws Exception {
        if (movie.getTmdbId() > 0) {
            return WebServices.TheMovieDB.getCollection(movie.getTmdbId(), movie.getLanguage());
        }
        return null;
    }

    public static List<Artwork> getArtwork(Movie movie) throws Exception {
        if (movie.getTmdbId() > 0) {
            return WebServices.TheMovieDB.getArtwork(movie.getTmdbId(), movie.getLanguage());
        }
        return null;
    }

    public static List<Artwork> getArtwork(Movie movie, String string, Locale locale) throws Exception {
        if (movie.getTmdbId() > 0) {
            return WebServices.TheMovieDB.getArtwork(movie.getTmdbId(), string, locale);
        }
        return null;
    }

    public static List<Artwork> getFanart(Movie movie) throws Exception {
        if (movie.getTmdbId() > 0) {
            return WebServices.FanartTV.TheMovieDB.getArtwork(movie.getTmdbId(), movie.getLanguage());
        }
        return null;
    }

    public static List<Extra> getExtras(Movie movie) throws Exception {
        if (movie.getTmdbId() > 0) {
            return WebServices.TheMovieDB.getExtras(movie.getTmdbId());
        }
        return null;
    }

    public static List<Episode> getEpisodes(SeriesInfo seriesInfo) throws Exception {
        for (EpisodeListProvider episodeListProvider : WebServices.getEpisodeListProviders()) {
            if (!EpisodeUtilities.isInstance((Datasource)episodeListProvider, seriesInfo)) continue;
            return episodeListProvider.getEpisodeList(seriesInfo.getId(), SortOrder.forName(seriesInfo.getOrder()), seriesInfo.getLanguage());
        }
        return null;
    }

    public static List<Artwork> getArtwork(SeriesInfo seriesInfo) throws Exception {
        for (EpisodeListProvider episodeListProvider : WebServices.getEpisodeListProviders()) {
            if (!EpisodeUtilities.isInstance((Datasource)episodeListProvider, seriesInfo) || !(episodeListProvider instanceof ArtworkProvider)) continue;
            return ((ArtworkProvider)((Object)episodeListProvider)).getArtwork(seriesInfo.getId(), seriesInfo.getLanguage());
        }
        return null;
    }

    public static List<Artwork> getArtwork(SeriesInfo seriesInfo, String string, Locale locale) throws Exception {
        for (EpisodeListProvider episodeListProvider : WebServices.getEpisodeListProviders()) {
            if (!EpisodeUtilities.isInstance((Datasource)episodeListProvider, seriesInfo) || !(episodeListProvider instanceof ArtworkProvider)) continue;
            return ((ArtworkProvider)((Object)episodeListProvider)).getArtwork(seriesInfo.getId(), string, locale);
        }
        return null;
    }

    public static List<Artwork> getFanart(SeriesInfo seriesInfo) throws Exception {
        Integer n = ExtendedMetadataMethods.getExternalId(seriesInfo, "TheTVDB");
        if (n != null) {
            return WebServices.FanartTV.TheTVDB.getArtwork(n, seriesInfo.getLanguage());
        }
        return null;
    }

    public static SeriesInfo getSeries(Episode episode) throws Exception {
        SeriesInfo seriesInfo = episode.getSeriesInfo();
        for (EpisodeListProvider episodeListProvider : WebServices.getEpisodeListProviders()) {
            if (!EpisodeUtilities.isInstance((Datasource)episodeListProvider, seriesInfo)) continue;
            return episodeListProvider.getSeriesInfo(seriesInfo.getId(), seriesInfo.getLanguage());
        }
        return null;
    }

    public static SeriesDetails getDetails(SeriesInfo seriesInfo) throws Exception {
        for (EpisodeListProvider episodeListProvider : WebServices.getEpisodeListProviders()) {
            if (!EpisodeUtilities.isInstance((Datasource)episodeListProvider, seriesInfo)) continue;
            return (SeriesDetails)episodeListProvider.getSeriesInfo(seriesInfo.getId(), seriesInfo.getLanguage());
        }
        return null;
    }

    public static EpisodeDetails getInfo(Episode episode) throws Exception {
        for (EpisodeListProvider episodeListProvider : WebServices.getEpisodeListProviders()) {
            if (!EpisodeUtilities.isInstance((Datasource)episodeListProvider, episode)) continue;
            return episodeListProvider.getEpisodeInfo(episode, episode.getSeriesInfo().getLanguage());
        }
        return null;
    }

    public static List<String> getActors(SeriesInfo seriesInfo) throws Exception {
        List<Person> list = ExtendedMetadataMethods.getCrew(seriesInfo);
        if (list != null) {
            return list.stream().filter(Person::isActor).map(Person::getName).collect(Collectors.toList());
        }
        return null;
    }

    public static List<Person> getCrew(SeriesInfo seriesInfo) throws Exception {
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, seriesInfo)) {
            return WebServices.TheMovieDB_TV.getCredits(seriesInfo.getId(), Locale.US);
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, seriesInfo)) {
            return WebServices.TheTVDB.getCharacters(seriesInfo.getId(), Locale.US);
        }
        return null;
    }

    public static Integer getExternalId(Series series, String string) throws Exception {
        return series.getExternalId(XDB.forName(string));
    }

    public static Integer getExternalId(SeriesInfo seriesInfo, String string) throws Exception {
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, seriesInfo)) {
            return XDB.TheMovieDB.getExternalId(seriesInfo.getId(), XDB.forName(string));
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, seriesInfo)) {
            return XDB.TheTVDB.getExternalId(seriesInfo.getId(), XDB.forName(string));
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.AniDB, seriesInfo)) {
            return XDB.AniDB.getExternalId(seriesInfo.getId(), XDB.forName(string));
        }
        return null;
    }

    public static SeriesInfo getExternalSeries(SeriesInfo seriesInfo, String string) throws Exception {
        switch (XDB.forName(string)) {
            case TheMovieDB: {
                return WebServices.TheMovieDB_TV.getSeriesInfo(ExtendedMetadataMethods.getExternalId(seriesInfo, string), seriesInfo.getLanguage());
            }
            case TheTVDB: {
                return WebServices.TheTVDB.getSeriesInfo(ExtendedMetadataMethods.getExternalId(seriesInfo, string), seriesInfo.getLanguage());
            }
            case AniDB: {
                return WebServices.AniDB.getSeriesInfo(ExtendedMetadataMethods.getExternalId(seriesInfo, string), seriesInfo.getLanguage());
            }
            case TVmaze: {
                return WebServices.TVmaze.getSeriesInfo(ExtendedMetadataMethods.getExternalId(seriesInfo, string), seriesInfo.getLanguage());
            }
        }
        return null;
    }

    public static Map<String, String> getExternalIds(SeriesInfo seriesInfo) throws Exception {
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, seriesInfo)) {
            return WebServices.TheMovieDB_TV.getExternalSeriesID(seriesInfo.getId());
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, seriesInfo)) {
            return WebServices.TheTVDB.getExternalIds(seriesInfo.getId());
        }
        return null;
    }

    public static Map<String, String> getExternalIds(Episode episode) throws Exception {
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, episode)) {
            episode = EpisodeUtilities.reorderEpisode(episode, SortOrder.Airdate);
            int n = episode.getSeriesInfo().getId();
            int n2 = EpisodeUtilities.isRegularEpisode(episode) ? episode.getSeason() : 0;
            int n3 = EpisodeUtilities.isRegularEpisode(episode) ? episode.getEpisode() : episode.getSpecial();
            return WebServices.TheMovieDB_TV.getExternalEpisodeID(n, n2, n3);
        }
        return null;
    }

    public static List<Extra> getExtras(SeriesInfo seriesInfo) throws Exception {
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, seriesInfo)) {
            return WebServices.TheMovieDB_TV.getExtras(seriesInfo.getId());
        }
        return null;
    }

    public static Object getRaw(SeriesInfo seriesInfo) throws Exception {
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, seriesInfo)) {
            return WebServices.TheMovieDB_TV.requestSeriesInfo(seriesInfo.getId(), seriesInfo.getLanguage(), Cache.ONE_MONTH);
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, seriesInfo)) {
            return WebServices.TheTVDB.requestSeriesInfo(seriesInfo.getId(), seriesInfo.getLanguage());
        }
        return null;
    }

    public static Object getRaw(EpisodeDetails episodeDetails) throws Exception {
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, (Episode)episodeDetails)) {
            return WebServices.TheMovieDB_TV.requestEpisodeInfo(episodeDetails.getSeriesInfo().getId(), episodeDetails.getSeason(), episodeDetails.getEpisode(), episodeDetails.getSeriesInfo().getLanguage());
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, (Episode)episodeDetails)) {
            return WebServices.TheTVDB.requestEpisodeInfo(episodeDetails.getId(), episodeDetails.getSeriesInfo().getLanguage());
        }
        return null;
    }

    public static Object getService(Script script, String string) {
        return WebServices.getService(string);
    }
}

