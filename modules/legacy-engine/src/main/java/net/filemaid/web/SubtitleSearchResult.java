package net.filemaid.web;

import java.util.List;
import java.util.Locale;
import net.filemaid.web.Movie;

public class SubtitleSearchResult
extends Movie {
    private Kind kind;
    private int score;

    public SubtitleSearchResult() {
    }

    public SubtitleSearchResult(int n, String string, int n2, String string2) {
        this(n, string, null, n2, n, 0, Locale.ENGLISH, Kind.forName(string2), 0);
    }

    public SubtitleSearchResult(int n, String string, String[] stringArray, int n2, int n3, int n4, Locale locale, Kind kind, int n5) {
        super(n, string, stringArray, n2, n3, n4, locale);
        this.kind = kind;
        this.score = n5;
    }

    public SubtitleSearchResult(Movie movie) {
        super(movie);
        this.kind = Kind.Movie;
        this.score = Integer.MAX_VALUE;
    }

    @Override
    public int getId() {
        return this.getImdbId();
    }

    public int getFeatureId() {
        return this.id;
    }

    public Kind getKind() {
        return this.kind;
    }

    public int getScore() {
        return this.score;
    }

    public boolean isMovie() {
        return this.kind == Kind.Movie;
    }

    public boolean isSeries() {
        return this.kind == Kind.Series;
    }

    @Override
    public List<String> getEffectiveNames() {
        switch (this.kind) {
            case Series: {
                return super.getEffectiveNamesWithoutYear();
            }
        }
        return super.getEffectiveNames();
    }

    @Override
    public String toString() {
        switch (this.kind) {
            case Series: {
                return super.getName();
            }
        }
        return super.toString();
    }

    public static enum Kind {
        Movie,
        Series,
        Episode,
        Other,
        Unkown;


        public static Kind forName(String string) {
            if (string == null || string.isEmpty()) {
                return Unkown;
            }
            if (string.equalsIgnoreCase("m") || string.equalsIgnoreCase("movie")) {
                return Movie;
            }
            if (string.equalsIgnoreCase("s") || string.equalsIgnoreCase("tv series") || string.equalsIgnoreCase("tvshow")) {
                return Series;
            }
            if (string.equalsIgnoreCase("e") || string.equalsIgnoreCase("episode")) {
                return Episode;
            }
            return Other;
        }
    }
}

