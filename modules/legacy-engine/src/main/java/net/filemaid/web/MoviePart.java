package net.filemaid.web;

import net.filemaid.web.Movie;

public class MoviePart
extends Movie {
    protected int partIndex;
    protected int partCount;

    public MoviePart() {
    }

    public MoviePart(MoviePart moviePart) {
        this(moviePart, moviePart.partIndex, moviePart.partCount);
    }

    public MoviePart(Movie movie, int n, int n2) {
        super(movie);
        this.partIndex = n;
        this.partCount = n2;
    }

    public int getPartIndex() {
        return this.partIndex;
    }

    public int getPartCount() {
        return this.partCount;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof MoviePart) {
            MoviePart moviePart = (MoviePart)object;
            return super.equals(moviePart) && this.partIndex == moviePart.partIndex && this.partCount == moviePart.partCount;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id + this.partCount + this.partIndex;
    }

    @Override
    public MoviePart clone() {
        return new MoviePart(this);
    }

    @Override
    public String toString() {
        return super.toString() + " [CD" + this.partIndex + "]";
    }
}

