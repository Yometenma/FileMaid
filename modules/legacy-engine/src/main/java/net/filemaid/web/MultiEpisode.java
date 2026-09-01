package net.filemaid.web;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeFormat;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SimpleDate;

public class MultiEpisode
extends Episode
implements Iterable<Episode> {
    protected Episode[] episodes;

    public MultiEpisode() {
    }

    public MultiEpisode(Episode ... episodeArray) {
        this.episodes = (Episode[])episodeArray.clone();
    }

    public MultiEpisode(Collection<Episode> collection) {
        this.episodes = collection.toArray(new Episode[0]);
    }

    public Episode[] getEpisodes() {
        return (Episode[])this.episodes.clone();
    }

    public int count() {
        return this.episodes.length;
    }

    public Episode getFirst() {
        return this.episodes[0];
    }

    public Stream<Episode> stream() {
        return Arrays.stream(this.episodes);
    }

    @Override
    public Iterator<Episode> iterator() {
        return this.stream().iterator();
    }

    @Override
    public String getSeriesName() {
        return this.getFirst().getSeriesName();
    }

    @Override
    public Integer getEpisode() {
        return this.getFirst().getEpisode();
    }

    @Override
    public Integer getSeason() {
        return this.getFirst().getSeason();
    }

    @Override
    public String getTitle() {
        return EpisodeFormat.DEFAULT.formatMultiTitle(this.episodes);
    }

    @Override
    public Integer getAbsolute() {
        return this.getFirst().getAbsolute();
    }

    @Override
    public Integer getSpecial() {
        return this.getFirst().getSpecial();
    }

    @Override
    public SimpleDate getAirdate() {
        return this.getFirst().getAirdate();
    }

    @Override
    public Integer getRuntime() {
        return this.stream().map(Episode::getRuntime).mapToInt(Integer::intValue).sum();
    }

    @Override
    public Integer getId() {
        return this.getFirst().getId();
    }

    @Override
    public SeriesInfo getSeriesInfo() {
        return this.getFirst().getSeriesInfo();
    }

    @Override
    public List<String> getSeriesNames() {
        return this.getFirst().getSeriesNames();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof MultiEpisode) {
            MultiEpisode multiEpisode = (MultiEpisode)object;
            return Arrays.equals(this.episodes, multiEpisode.episodes);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.episodes);
    }

    @Override
    public MultiEpisode clone() {
        return new MultiEpisode(this.episodes);
    }

    @Override
    public MultiEpisode derive(String string, Integer n, Integer n2, String string2, Integer n3, SimpleDate simpleDate) {
        Episode[] episodeArray = new Episode[this.episodes.length];
        for (int i = 0; i < this.episodes.length; ++i) {
            episodeArray[i] = this.episodes[i].derive(string, n, this.up(n2, i), string2, this.up(n3, i), simpleDate);
        }
        return new MultiEpisode(episodeArray);
    }

    private Integer up(Integer n, int n2) {
        return n == null ? null : Integer.valueOf(n + n2);
    }

    @Override
    public String toString() {
        return EpisodeFormat.DEFAULT.formatMultiEpisode(this.episodes);
    }
}

