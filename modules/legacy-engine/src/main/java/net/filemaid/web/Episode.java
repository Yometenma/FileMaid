package net.filemaid.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.filemaid.web.EpisodeFormat;
import net.filemaid.web.MappedEpisode;
import net.filemaid.web.MultiEpisode;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SimpleDate;

public class Episode
implements Serializable {
    protected String seriesName;
    protected Integer season;
    protected Integer episode;
    protected String title;
    protected Integer absolute;
    protected Integer special;
    protected SimpleDate airdate;
    protected Integer runtime;
    protected Integer id;
    protected String group;
    protected SeriesInfo seriesInfo;

    public Episode() {
    }

    public Episode(Episode episode) {
        this(episode.seriesName, episode.season, episode.episode, episode.title, episode.absolute, episode.special, episode.airdate, episode.runtime, episode.id, episode.group, episode.seriesInfo);
    }

    public Episode(String string, Integer n, Integer n2, String string2) {
        this(string, n, n2, string2, null, null, null, null, null, null, null);
    }

    public Episode(String string, Integer n, Integer n2, String string2, Integer n3, Integer n4, SimpleDate simpleDate, Integer n5, Integer n6, String string3, SeriesInfo seriesInfo) {
        this.seriesName = string;
        this.season = n;
        this.episode = n2;
        this.title = string2;
        this.absolute = n3;
        this.special = n4;
        this.airdate = simpleDate == null ? null : simpleDate.clone();
        this.runtime = n5;
        this.id = n6;
        this.group = string3;
        this.seriesInfo = seriesInfo == null ? null : seriesInfo.clone();
    }

    public String getSeriesName() {
        return this.seriesName;
    }

    public Integer getEpisode() {
        return this.episode;
    }

    public Integer getSeason() {
        return this.season;
    }

    public String getTitle() {
        return this.title;
    }

    public Integer getAbsolute() {
        return this.absolute;
    }

    public Integer getSpecial() {
        return this.special;
    }

    public SimpleDate getAirdate() {
        return this.airdate;
    }

    public Integer getRuntime() {
        return this.runtime;
    }

    public Integer getId() {
        return this.id;
    }

    public String getGroup() {
        return this.group;
    }

    public SeriesInfo getSeriesInfo() {
        return this.seriesInfo;
    }

    public List<String> getSeriesNames() {
        ArrayList<String> arrayList = new ArrayList<String>();
        if (this.seriesName != null) {
            arrayList.add(this.seriesName);
        }
        if (this.seriesInfo != null) {
            if (this.seriesInfo.name != null && !this.seriesInfo.name.equals(this.seriesName)) {
                arrayList.add(this.seriesInfo.name);
            }
            if (this.seriesInfo.aliasNames != null) {
                arrayList.addAll(Arrays.asList(this.seriesInfo.aliasNames));
            }
        }
        return arrayList;
    }

    public boolean equals(Object object) {
        if (object instanceof Episode) {
            Episode episode = (Episode)object;
            if (this.id != null && episode.id != null) {
                return this.id.equals(episode.id) && Objects.equals(this.seriesInfo, episode.seriesInfo);
            }
            return this.isEquivalent(episode);
        }
        return false;
    }

    public int hashCode() {
        return this.id != null ? this.id : Objects.hash(this.season, this.episode, this.absolute, this.special, this.seriesName, this.title);
    }

    public boolean isEquivalent(Episode episode) {
        return Objects.equals(this.absolute, episode.absolute) && Objects.equals(this.season, episode.season) && Objects.equals(this.episode, episode.episode) && Objects.equals(this.special, episode.special) && Objects.equals(this.seriesName, episode.seriesName) && Objects.equals(this.title, episode.title) && Objects.equals(this.group, episode.group);
    }

    public Episode clone() {
        return new Episode(this);
    }

    public Episode number(Integer n) {
        return new Episode(this.getSeriesName(), null, n, this.getTitle(), null, null, this.getAirdate(), this.getRuntime(), this.getId(), this.getGroup(), this.getSeriesInfo());
    }

    public Episode number(Integer n, Integer n2) {
        return new Episode(this.getSeriesName(), n, n2, this.getTitle(), null, null, this.getAirdate(), this.getRuntime(), this.getId(), this.getGroup(), this.getSeriesInfo());
    }

    public Episode number(Integer n, Integer n2, Integer n3) {
        return new Episode(this.getSeriesName(), n, n2, this.getTitle(), n3, null, this.getAirdate(), this.getRuntime(), this.getId(), this.getGroup(), this.getSeriesInfo());
    }

    public Episode title(String string) {
        return new Episode(this.getSeriesName(), this.getSeason(), this.getEpisode(), string, this.getAbsolute(), this.getSpecial(), this.getAirdate(), this.getRuntime(), this.getId(), this.getGroup(), this.getSeriesInfo());
    }

    public Episode group(String string) {
        return new Episode(this.getSeriesName(), this.getSeason(), this.getEpisode(), this.getTitle(), this.getAbsolute(), this.getSpecial(), this.getAirdate(), this.getRuntime(), this.getId(), string, this.getSeriesInfo());
    }

    public Episode absolute(Integer n) {
        return new Episode(this.getSeriesName(), this.getSeason(), this.getEpisode(), this.getTitle(), n, this.getSpecial(), this.getAirdate(), this.getRuntime(), this.getId(), this.getGroup(), this.getSeriesInfo());
    }

    public Episode airdate(String string) {
        return new Episode(this.getSeriesName(), this.getSeason(), this.getEpisode(), this.getTitle(), this.getAbsolute(), this.getSpecial(), SimpleDate.parse(string), this.getRuntime(), this.getId(), this.getGroup(), this.getSeriesInfo());
    }

    public Episode airdate(int n, int n2, int n3) {
        return new Episode(this.getSeriesName(), this.getSeason(), this.getEpisode(), this.getTitle(), this.getAbsolute(), this.getSpecial(), SimpleDate.of(n, n2, n3), this.getRuntime(), this.getId(), this.getGroup(), this.getSeriesInfo());
    }

    public Episode runtime(Integer n) {
        return new Episode(this.getSeriesName(), this.getSeason(), this.getEpisode(), this.getTitle(), this.getAbsolute(), this.getSpecial(), this.getAirdate(), n, this.getId(), this.getGroup(), this.getSeriesInfo());
    }

    public Episode derive(Integer n, Integer n2) {
        return this.derive(this.getSeriesName(), n, n2, null, null, null);
    }

    public Episode derive(Integer n) {
        return this.derive(this.getSeriesName(), null, n, null, n, null);
    }

    public Episode derive(String string, Integer n, Integer n2, String string2, Integer n3, SimpleDate simpleDate) {
        return new Episode(string, n, n2, string2, n3, null, simpleDate, null, null, null, null);
    }

    public MappedEpisode map(Episode episode) {
        return new MappedEpisode(this, episode);
    }

    public MappedEpisode map(List<Episode> list) {
        switch (list.size()) {
            case 0: {
                return null;
            }
            case 1: {
                return new MappedEpisode(this, list.get(0));
            }
        }
        return new MappedEpisode(this, new MultiEpisode(list));
    }

    public String toString() {
        return EpisodeFormat.DEFAULT.format(this);
    }
}

