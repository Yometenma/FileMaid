package net.filemaid.web;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.filemaid.web.Crew;
import net.filemaid.web.Episode;
import net.filemaid.web.Person;

public class EpisodeDetails
extends Episode
implements Crew {
    protected String productionCode;
    protected String overview;
    protected Double rating;
    protected Integer votes;
    protected String page;
    protected URL image;
    protected Person[] people;

    public EpisodeDetails() {
    }

    public EpisodeDetails(EpisodeDetails episodeDetails) {
        super(episodeDetails);
        this.productionCode = episodeDetails.productionCode;
        this.overview = episodeDetails.overview;
        this.rating = episodeDetails.rating;
        this.votes = episodeDetails.votes;
        this.page = episodeDetails.page;
        this.image = episodeDetails.image;
        this.people = episodeDetails.people == null ? null : (Person[])episodeDetails.people.clone();
    }

    public EpisodeDetails(Episode episode, String string, String string2, Double d, Integer n, String string3, URL uRL, List<Person> list) {
        super(episode);
        this.productionCode = string;
        this.overview = string2;
        this.rating = d;
        this.votes = n;
        this.page = string3;
        this.image = uRL;
        this.people = list.toArray(new Person[0]);
    }

    public String getProductionCode() {
        return this.productionCode;
    }

    public String getOverview() {
        return this.overview;
    }

    public Double getRating() {
        return this.rating;
    }

    public Integer getVotes() {
        return this.votes;
    }

    public String getPage() {
        return this.page;
    }

    public URL getImage() {
        return this.image;
    }

    @Override
    public List<Person> getCrew() {
        return Collections.unmodifiableList(Arrays.asList(this.people));
    }

    @Override
    public EpisodeDetails clone() {
        return new EpisodeDetails(this);
    }

    @Override
    public String toString() {
        return this.seriesInfo + "::" + this.id;
    }
}

