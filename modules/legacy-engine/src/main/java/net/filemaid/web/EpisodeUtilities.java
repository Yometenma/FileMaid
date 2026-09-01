package net.filemaid.web;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.Logging;
import net.filemaid.WebServices;
import net.filemaid.web.AnimeLists;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.MultiEpisode;
import net.filemaid.web.Series;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SortOrder;
import net.filemaid.web.XDB;

public final class EpisodeUtilities {
    public static final Comparator<Episode> EPISODE_NUMBERS_COMPARATOR = new Comparator<Episode>(){

        @Override
        public int compare(Episode episode2, Episode episode3) {
            int n = this.compare(episode2, episode3, Episode::getSeason, episode -> EpisodeUtilities.isRegularEpisode(episode) ? Integer.valueOf(1) : null);
            if (n != 0) {
                return n;
            }
            n = this.compare(episode2, episode3, Episode::getEpisode);
            if (n != 0) {
                return n;
            }
            n = this.compare(episode2, episode3, Episode::getSpecial);
            if (n != 0) {
                return n;
            }
            return this.compare(episode2, episode3, Episode::getAbsolute);
        }

        private int compare(Episode episode, Episode episode2, Function<Episode, Integer> ... functionArray) {
            return Integer.compare(this.asInt(episode, functionArray), this.asInt(episode2, functionArray));
        }

        private int asInt(Episode episode, Function<Episode, Integer> ... functionArray) {
            return Arrays.stream(functionArray).map(function -> (Integer)function.apply(episode)).filter(Objects::nonNull).findFirst().orElse(Integer.MAX_VALUE);
        }
    };

    public static boolean isInstance(Datasource datasource, Episode episode) {
        return episode != null && EpisodeUtilities.isInstance(datasource, episode.getSeriesInfo());
    }

    public static boolean isInstance(Datasource datasource, SeriesInfo seriesInfo) {
        return seriesInfo != null && datasource.getIdentifier().equals(seriesInfo.getDatabase());
    }

    public static boolean isInstance(SortOrder sortOrder, Episode episode) {
        return episode != null && EpisodeUtilities.isInstance(sortOrder, episode.getSeriesInfo());
    }

    public static boolean isInstance(SortOrder sortOrder, SeriesInfo seriesInfo) {
        return seriesInfo != null && sortOrder.equals(seriesInfo.getOrder());
    }

    public static boolean isRegularEpisode(Episode episode) {
        return episode != null && episode.getEpisode() != null;
    }

    public static boolean isSpecialEpisode(Episode episode) {
        return episode != null && episode.getSpecial() != null;
    }

    public static boolean isAbsoluteEpisode(Episode episode) {
        if (EpisodeUtilities.isRegularEpisode(episode)) {
            if (episode.getSeason() == null) {
                return true;
            }
            if (episode.getEpisode().equals(episode.getAbsolute())) {
                return true;
            }
            if (episode.getAbsolute() != null && EpisodeUtilities.isInstance(SortOrder.Absolute, episode)) {
                return true;
            }
        }
        return false;
    }

    public static Episode mapEpisode(Episode episode, Function<Episode, Episode> function) {
        return EpisodeUtilities.createEpisode((Episode[])EpisodeUtilities.streamMultiEpisode(episode).map(function).filter(Objects::nonNull).sorted(EPISODE_NUMBERS_COMPARATOR).toArray(Episode[]::new));
    }

    public static Episode selectEpisode(List<Episode> list, Episode episode) {
        List list2 = EpisodeUtilities.streamMultiEpisode(episode).collect(Collectors.toList());
        return EpisodeUtilities.createEpisode((Episode[])list.stream().filter(list2::contains).sorted(EPISODE_NUMBERS_COMPARATOR).toArray(Episode[]::new));
    }

    private static Episode createEpisode(Episode ... episodeArray) {
        switch (episodeArray.length) {
            case 0: {
                return null;
            }
            case 1: {
                return episodeArray[0];
            }
        }
        return new MultiEpisode(episodeArray);
    }

    public static Stream<Episode> streamMultiEpisode(Episode ... episodeArray) {
        return Arrays.stream(episodeArray).flatMap(episode -> episode instanceof MultiEpisode ? ((MultiEpisode)episode).stream() : Stream.of(episode));
    }

    public static List<Episode> fetchEpisodeList(Episode episode) throws Exception {
        return EpisodeUtilities.fetchEpisodeList(episode, null, null);
    }

    public static List<Episode> fetchEpisodeList(Episode episode, SortOrder sortOrder, Locale locale) throws Exception {
        SeriesInfo seriesInfo = episode.getSeriesInfo();
        SortOrder sortOrder2 = Optional.ofNullable(sortOrder).orElseGet(() -> SortOrder.valueOf(seriesInfo.getOrder()));
        Locale locale2 = Optional.ofNullable(locale).orElseGet(() -> seriesInfo.getLanguage());
        return WebServices.getEpisodeListProvider(seriesInfo.getDatabase()).getEpisodeList(seriesInfo.getId(), sortOrder2, locale2);
    }

    public static Episode fetchEpisode(Episode episode, SortOrder sortOrder, Locale locale) throws Exception {
        Episode episode2 = EpisodeUtilities.selectEpisode(EpisodeUtilities.fetchEpisodeList(episode, sortOrder, locale), episode);
        if (episode2 != null) {
            return episode2;
        }
        throw new IllegalArgumentException(episode + " does not exist in " + sortOrder + " Order");
    }

    public static Episode reorderEpisode(Episode episode, SortOrder sortOrder) throws Exception {
        if (EpisodeUtilities.isInstance(sortOrder, episode)) {
            return episode;
        }
        return EpisodeUtilities.fetchEpisode(episode, sortOrder, null);
    }

    public static Episode hydrateEpisode(Episode episode, Locale locale) throws Exception {
        Episode[] episodeArray = (Episode[])EpisodeUtilities.fetchEpisodeList(episode, null, locale).stream().filter(episode3 -> EpisodeUtilities.streamMultiEpisode(episode).anyMatch(episode2 -> EpisodeUtilities.isNumbersMatch(episode2, episode3))).sorted(EPISODE_NUMBERS_COMPARATOR).toArray(Episode[]::new);
        if (episodeArray.length > 0) {
            return EpisodeUtilities.createEpisode(episodeArray);
        }
        throw new IllegalArgumentException("Invalid Episode: " + episode);
    }

    public static boolean isNumbersMatch(Episode episode, Episode episode2) {
        return Objects.equals(episode.episode, episode2.episode) && Objects.equals(episode.season, episode2.season) && Objects.equals(episode.special, episode2.special);
    }

    public static boolean isSeriesMatch(Object object, Object object2) {
        if (Objects.equals(object, object2)) {
            return true;
        }
        if (object instanceof Episode && object2 instanceof Episode) {
            return EpisodeUtilities.isSeriesMatch(((Episode)object).getSeriesInfo(), ((Episode)object2).getSeriesInfo());
        }
        return false;
    }

    public static boolean isAnimeType(Object object) {
        Episode episode;
        SeriesInfo seriesInfo;
        if (object instanceof Episode && (seriesInfo = (episode = (Episode)object).getSeriesInfo()) != null) {
            return "Anime".equals(seriesInfo.getType()) || EpisodeUtilities.isInstance((Datasource)WebServices.AniDB, seriesInfo);
        }
        return false;
    }

    public static Episode mapSeasonEpisodeNumbers(Episode episode2, AnimeLists.DB dB) {
        if (EpisodeUtilities.isInstance((Datasource)WebServices.AniDB, episode2) && EpisodeUtilities.isRegularEpisode(episode2)) {
            return EpisodeUtilities.mapEpisode(episode2, episode -> {
                try {
                    return WebServices.AnimeList.map((Episode)episode, AnimeLists.DB.get(episode), dB).orElse((Episode)episode);
                }
                catch (Exception exception) {
                    Logging.trace(exception);
                    return episode;
                }
            });
        }
        return episode2;
    }

    public static String mapRomajiPrimaryTitle(Episode episode) {
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, episode) || EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, episode)) {
            try {
                AnimeLists.DB dB = AnimeLists.DB.get(episode);
                episode = EpisodeUtilities.reorderEpisode(episode, SortOrder.Airdate);
                return WebServices.AnimeList.mapName(episode.getSeriesInfo().getId(), episode.getSeason() == null ? 1 : episode.getSeason(), episode.getEpisode() == null ? 1 : episode.getEpisode(), dB, AnimeLists.DB.AniDB).orElse(null);
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("Failed to find AnimeList mapping", exception));
            }
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.AniDB, episode)) {
            return episode.getSeriesInfo().getName();
        }
        return null;
    }

    public static List<Episode> filterBySeason(Collection<Episode> collection, int n) {
        return collection.stream().filter(episode -> episode.getSeason() != null && n == episode.getSeason()).collect(Collectors.toList());
    }

    public static int getLastSeason(Collection<Episode> collection) {
        return collection.stream().mapToInt(episode -> episode.getSeason() == null ? 0 : episode.getSeason()).max().orElse(0);
    }

    public static Series lookupExternalSeries(SeriesInfo seriesInfo) throws Exception {
        Series series = EpisodeUtilities.getExternalSeries(seriesInfo);
        for (XDB xDB : XDB.values()) {
            Series series2 = xDB.getExternalSeries(series.getExternalId(xDB));
            if (series2 == null) continue;
            return series2;
        }
        throw new IllegalArgumentException("XDB not found: " + seriesInfo);
    }

    public static Series getExternalSeries(SeriesInfo seriesInfo) {
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, seriesInfo)) {
            return Series.XDB(XDB.TheMovieDB, seriesInfo.getId(), seriesInfo.getName());
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, seriesInfo)) {
            return Series.XDB(XDB.TheTVDB, seriesInfo.getId(), seriesInfo.getName());
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.AniDB, seriesInfo)) {
            return Series.XDB(XDB.AniDB, seriesInfo.getId(), seriesInfo.getName());
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TVmaze, seriesInfo)) {
            return Series.XDB(XDB.TVmaze, seriesInfo.getId(), seriesInfo.getName());
        }
        return null;
    }

    public static Comparator<Episode> episodeComparator() {
        return EPISODE_NUMBERS_COMPARATOR;
    }

    private EpisodeUtilities() {
        throw new UnsupportedOperationException();
    }
}

