package net.filemaid.web;

import java.io.Serializable;
import java.net.URL;
import java.util.AbstractList;
import java.util.Collection;
import net.filemaid.web.Movie;
import net.filemaid.web.WebRequest;

public class MovieCollection
extends AbstractList<Movie>
implements Serializable {
    protected Movie[] parts;
    protected int id;
    protected String name;
    protected String overview;
    protected String poster;
    protected String backdrop;

    public MovieCollection(Collection<Movie> collection, int n, String string, String string2, URL uRL, URL uRL2) {
        this.parts = collection.toArray(new Movie[0]);
        this.id = n;
        this.name = string;
        this.overview = string2;
        this.poster = uRL == null ? null : uRL.toExternalForm();
        this.backdrop = uRL2 == null ? null : uRL2.toExternalForm();
    }

    @Override
    public Movie get(int n) {
        return this.parts[n];
    }

    @Override
    public int size() {
        return this.parts.length;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getOverview() {
        return this.overview;
    }

    public URL getPoster() {
        return WebRequest.parseURL(this.poster);
    }

    public URL getBackdrop() {
        return WebRequest.parseURL(this.backdrop);
    }
}

