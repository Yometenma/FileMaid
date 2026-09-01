package net.filemaid.ui.rename;

import java.awt.Component;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.InvalidInputException;
import net.filemaid.Logging;
import net.filemaid.Parallelism;
import net.filemaid.WebServices;
import net.filemaid.media.AutoDetection;
import net.filemaid.similarity.Match;
import net.filemaid.ui.rename.AutoCompleteMatcher;
import net.filemaid.ui.rename.AutoDetectionMode;
import net.filemaid.ui.rename.AutoSelectionMode;
import net.filemaid.ui.rename.EpisodeListMatcher;
import net.filemaid.ui.rename.MatchMode;
import net.filemaid.ui.rename.MovieMatcher;
import net.filemaid.ui.rename.MusicMatcher;
import net.filemaid.ui.rename.OriginalOrder;
import net.filemaid.web.AnimeLists;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.MappedEpisode;
import net.filemaid.web.SortOrder;

class AutoDetectMatcher
implements AutoCompleteMatcher {
    private final AutoCompleteMatcher movie = new MovieMatcher(WebServices.getDefaultMovieDB());
    private final AutoCompleteMatcher episode = new EpisodeListMatcher(WebServices.getDefaultSeriesDB(), false);
    private final AutoCompleteMatcher anime = new AnimeListMatcher(WebServices.getDefaultAnimeDB());
    private final AutoCompleteMatcher music = new MusicMatcher(WebServices.getDefaultMusicDB());
    private static final Parallelism groupPool = new Parallelism("Group", Parallelism.THREAD_POOL_SIZE.max());

    AutoDetectMatcher() {
    }

    @Override
    public List<Match<File, ?>> match(Collection<File> collection, MatchMode matchMode, SortOrder sortOrder, Locale locale, AutoDetectionMode autoDetectionMode, final Set<AutoSelectionMode> set, Component component) throws Exception {
        AutoDetection autoDetection = new AutoDetection(collection, locale){

            @Override
            public AutoDetection.Group detectGroup(File file) {
                if (set.contains((Object)AutoSelectionMode.Cancel)) {
                    throw new CancellationException();
                }
                return super.detectGroup(file);
            }
        };
        List<Map.Entry<AutoDetection.Group, List<File>>> list = autoDetection.group(groupPool).entrySet().stream().filter(entry -> entry.getKey().types().length == 1).collect(Collectors.toList());
        if (list.isEmpty()) {
            throw new InvalidInputException("Failed to find and classify any media files.");
        }
        List<List<Match<File, ?>>> grouped = groupPool.map(list, entry -> {
            if (set.contains((Object)AutoSelectionMode.Cancel)) {
                throw new CancellationException();
            }
            return this.match((AutoDetection.Group)entry.getKey(), (Collection)entry.getValue(), matchMode, sortOrder, locale, autoDetectionMode, set, component);
        });
        Stream<Match<File, ?>> stream = grouped.stream().flatMap(Collection::stream);
        List<Match<File, ?>> list2 = stream.sorted(Comparator.comparing(Match::getValue, OriginalOrder.of(collection))).collect(Collectors.toList());
        return list2;
    }

    private List<Match<File, ?>> match(AutoDetection.Group group, Collection<File> collection, MatchMode matchMode, SortOrder sortOrder, Locale locale, AutoDetectionMode autoDetectionMode, Set<AutoSelectionMode> set, Component component) throws Exception {
        AutoCompleteMatcher autoCompleteMatcher = this.getMatcher(group);
        if (autoCompleteMatcher != null) {
            try {
                return autoCompleteMatcher.match(collection, matchMode, sortOrder, locale, autoDetectionMode, set, component);
            }
            catch (InvalidInputException invalidInputException) {
                Logging.debug.finest(Logging.cause(group, collection, invalidInputException));
            }
        }
        return Collections.emptyList();
    }

    private AutoCompleteMatcher getMatcher(AutoDetection.Group group) {
        for (AutoDetection.Type type : group.types()) {
            switch (type) {
                case Movie: {
                    return this.movie;
                }
                case Series: {
                    return this.episode;
                }
                case Anime: {
                    return this.anime;
                }
                case Music: {
                    return this.music;
                }
            }
        }
        return null;
    }

    public static Parallelism groupPool() {
        return groupPool;
    }

    private static class AnimeListMatcher
    extends EpisodeListMatcher {
        public AnimeListMatcher(EpisodeListProvider episodeListProvider) {
            super(episodeListProvider, true);
        }

        @Override
        protected Collection<Episode> map(Collection<Episode> collection) {
            return collection.stream().flatMap(episode -> {
                Episode episode2 = null;
                Episode episode3 = null;
                if (!EpisodeUtilities.isInstance(SortOrder.Absolute, episode)) {
                    try {
                        episode2 = EpisodeUtilities.reorderEpisode(episode, SortOrder.Absolute);
                    }
                    catch (Exception exception) {
                        Logging.debug.finest(Logging.cause("Absolute mapper failed", episode, exception));
                    }
                }
                if (EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, episode) || EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, episode)) {
                    try {
                        episode3 = WebServices.AnimeList.map((Episode)episode, AnimeLists.DB.get(episode), AnimeLists.DB.AniDB).orElse(null);
                    }
                    catch (Exception exception) {
                        Logging.debug.finest(Logging.cause("AnimeLists mapper failed", episode, exception));
                    }
                }
                return MappedEpisode.generate(episode, new Episode[]{episode, episode2, episode3}, MappedEpisode::new);
            }).collect(Collectors.toList());
        }

        @Override
        protected Episode unmap(Episode episode) {
            return MappedEpisode.unmap(episode, MappedEpisode::getOriginal);
        }
    }
}

