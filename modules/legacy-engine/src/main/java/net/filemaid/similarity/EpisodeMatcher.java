package net.filemaid.similarity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.filemaid.MemoryCache;
import net.filemaid.media.SmartSeasonEpisodeMatcher;
import net.filemaid.similarity.Match;
import net.filemaid.similarity.Matcher;
import net.filemaid.similarity.SeasonEpisodeMatcher;
import net.filemaid.similarity.SimilarityMetric;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.MappedEpisode;
import net.filemaid.web.MultiEpisode;

public class EpisodeMatcher
extends Matcher<File, Episode> {
    private final SeasonEpisodeMatcher seasonEpisodeMatcher = new SmartSeasonEpisodeMatcher(SeasonEpisodeMatcher.LENIENT_SANITY, false);
    private final MemoryCache<File, Set<Integer>> cache = MemoryCache.forObject();

    public EpisodeMatcher(Collection<File> collection, Collection<Episode> collection2, boolean bl, SimilarityMetric[] similarityMetricArray) {
        super(collection, collection2, bl, similarityMetricArray);
    }

    @Override
    protected void deepMatch(Collection<Match<File, Episode>> collection, int n) {
        boolean bl = false;
        IdentityHashMap<File, List<Episode>> identityHashMap = new IdentityHashMap<File, List<Episode>>();
        for (Match match : collection) {
            identityHashMap.computeIfAbsent((File)match.getValue(), file -> new ArrayList()).add((Episode)match.getCandidate());
        }
        IdentityHashMap identityHashMap2 = new IdentityHashMap();
        identityHashMap.forEach((file, list) -> identityHashMap2.put(file, list.stream().map(episode -> {
            if (episode.getSpecial() != null) {
                return new SeasonEpisodeMatcher.SxE(0, episode.getSpecial());
            }
            return new SeasonEpisodeMatcher.SxE(episode.getSeason(), episode.getEpisode());
        }).collect(Collectors.toSet())));
        boolean bl2 = false;
        for (Match match : collection) {
            Episode[] episodeArray;
            List list2;
            File file2 = (File)match.getValue();
            if (!this.isMultiEpisodeMatch(file2, (Set)identityHashMap2.get(file2)) || (list2 = (List)identityHashMap.get(file2)).size() <= 1 || !this.isMultiEpisode(episodeArray = (Episode[])list2.stream().sorted(EpisodeUtilities.episodeComparator()).distinct().toArray(Episode[]::new))) continue;
            MultiEpisode multiEpisode = new MultiEpisode(episodeArray);
            this.disjointMatchCollection.add(Match.of(file2, multiEpisode));
            bl = true;
        }
        if (bl) {
            this.removeCollected(collection);
        }
        super.deepMatch(collection, n);
    }

    private Set<Integer> normalizeIdentifierSet(File file2) {
        return this.cache.get(file2, file -> {
            List<SeasonEpisodeMatcher.SxE> list = this.seasonEpisodeMatcher.match((File)file);
            return list == null ? Collections.emptySet() : this.normalizeIdentifierSet(list);
        });
    }

    private Set<Integer> normalizeIdentifierSet(Collection<SeasonEpisodeMatcher.SxE> collection) {
        int n = 100;
        for (SeasonEpisodeMatcher.SxE object : collection) {
            while (object.season > 0 && object.episode >= n) {
                n *= 10;
            }
        }
        HashSet hashSet = new HashSet(collection.size());
        for (SeasonEpisodeMatcher.SxE sxE : collection) {
            if (sxE.season > 0 && sxE.episode > 0 && sxE.episode < n) {
                hashSet.add(sxE.season * n + sxE.episode);
                continue;
            }
            if (sxE.season > 0 || sxE.episode <= 0) continue;
            hashSet.add(sxE.episode);
        }
        return hashSet;
    }

    private boolean isMultiEpisodeMatch(File file, Set<SeasonEpisodeMatcher.SxE> set) {
        Set<Integer> set2 = this.normalizeIdentifierSet(file);
        Set<Integer> set3 = this.normalizeIdentifierSet(set);
        return set2.equals(set3);
    }

    private boolean isMultiEpisode(Episode[] episodeArray) {
        if (episodeArray.length < 2) {
            return false;
        }
        Function<Episode, Integer> function = Arrays.stream(episodeArray).allMatch(episode -> episode.getSpecial() == null) ? episode -> episode.getEpisode() : episode -> episode.getSpecial();
        Integer n = null;
        for (Episode episode2 : episodeArray) {
            if (episode2 instanceof MappedEpisode || episode2 instanceof MultiEpisode) {
                return false;
            }
            Integer n2 = function.apply(episode2);
            if (n2 == null) {
                return false;
            }
            if (n != null && !n2.equals(n + 1) && !n2.equals(n)) {
                return false;
            }
            n = n2;
        }
        return Arrays.stream(episodeArray).skip(1L).allMatch(episode -> Objects.equals(episodeArray[0].getSeriesName(), episode.getSeriesName()) && Objects.equals(episodeArray[0].getSeriesInfo(), episode.getSeriesInfo()));
    }
}

