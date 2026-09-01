package net.filemaid.web;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import net.filemaid.web.Episode;
import net.filemaid.web.SimpleDate;

public class MappedEpisode
extends Episode {
    protected Episode original;
    protected Episode mapping;

    public MappedEpisode() {
    }

    public MappedEpisode(Episode episode, Episode episode2) {
        super(episode);
        this.original = MappedEpisode.unmap(episode, MappedEpisode::getOriginal);
        this.mapping = MappedEpisode.unmap(episode2, MappedEpisode::getMapping);
    }

    public Episode getOriginal() {
        return this.original;
    }

    public Episode getMapping() {
        return this.mapping;
    }

    private <T> T getFirst(Function<Episode, T> function) {
        T t = function.apply(this.mapping);
        if (t != null) {
            return t;
        }
        return function.apply(this.original);
    }

    @Override
    public String getSeriesName() {
        return this.getFirst(Episode::getSeriesName);
    }

    @Override
    public Integer getEpisode() {
        return this.mapping.getEpisode();
    }

    @Override
    public Integer getSeason() {
        return this.mapping.getSeason();
    }

    @Override
    public String getTitle() {
        return this.mapping.getTitle();
    }

    @Override
    public Integer getAbsolute() {
        return this.mapping.getAbsolute();
    }

    @Override
    public Integer getSpecial() {
        return this.mapping.getSpecial();
    }

    @Override
    public SimpleDate getAirdate() {
        return this.mapping.getAirdate();
    }

    @Override
    public Integer getId() {
        return this.getFirst(Episode::getId);
    }

    @Override
    public List<String> getSeriesNames() {
        return this.getFirst(Episode::getSeriesNames);
    }

    @Override
    public boolean equals(Object object) {
        return super.equals(object);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public MappedEpisode clone() {
        return new MappedEpisode(this.original, this.mapping);
    }

    @Override
    public MappedEpisode derive(Integer n, Integer n2) {
        return new MappedEpisode(this.original, this.mapping.derive(n, n2));
    }

    @Override
    public MappedEpisode derive(String string, Integer n, Integer n2, String string2, Integer n3, SimpleDate simpleDate) {
        return new MappedEpisode(this.original, this.mapping.derive(string, n, n2, string2, n3, simpleDate));
    }

    public MappedEpisode reverse() {
        return new MappedEpisode(this.mapping, this.original);
    }

    public MappedEpisode flatten() {
        return new MappedEpisode(this.mapping, this.mapping);
    }

    @Override
    public String toString() {
        return this.mapping + " => " + this.original;
    }

    public static Stream<Episode> generate(Episode episode, Episode[] episodeArray, BiFunction<Episode, Episode, Episode> biFunction) {
        if (episodeArray == null) {
            return Stream.of(episode);
        }
        for (int i = 0; i < episodeArray.length; ++i) {
            for (int j = i + 1; j < episodeArray.length; ++j) {
                if (episodeArray[i] == null || episodeArray[j] == null || episodeArray[i] != episodeArray[j] && !episodeArray[i].isEquivalent(episodeArray[j])) continue;
                episodeArray[j] = null;
            }
        }
        return Stream.of(episodeArray).filter(Objects::nonNull).map((Episode episode2) -> {
            if (episode == episode2) {
                return episode;
            }
            return (Episode)biFunction.apply(episode, (Episode)episode2);
        });
    }

    public static Episode map(Episode episode, Episode episode2) {
        if (episode2 instanceof MappedEpisode) {
            return episode2;
        }
        return new MappedEpisode(episode, episode2);
    }

    public static Episode unmap(Episode episode, Function<MappedEpisode, Episode> function) {
        if (episode instanceof MappedEpisode) {
            MappedEpisode mappedEpisode = (MappedEpisode)episode;
            episode = function.apply(mappedEpisode);
        }
        return episode;
    }
}

