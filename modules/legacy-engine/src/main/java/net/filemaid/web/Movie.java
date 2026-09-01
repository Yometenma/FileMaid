package net.filemaid.web;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.util.FunctionList;
import net.filemaid.web.SearchResult;

public class Movie
extends SearchResult {
    public static final int UNDEFINED = 0;
    protected int year;
    protected int imdbId;
    protected int tmdbId;
    protected String language;
    private static final Pattern NAME_YEAR_PATTERN = Pattern.compile("(.+?)[ _.][(]?((?:19|20)\\d{2})[)]?");

    public Movie() {
    }

    public Movie(int n, String string, String[] stringArray, int n2, int n3, int n4, Locale locale) {
        super(n, string, stringArray);
        this.year = n2;
        this.imdbId = n3;
        this.tmdbId = n4;
        this.language = locale == null ? null : locale.toLanguageTag();
    }

    public Movie(Movie movie) {
        super(movie.id, movie.name, movie.aliasNames);
        this.year = movie.year;
        this.imdbId = movie.imdbId;
        this.tmdbId = movie.tmdbId;
        this.language = movie.language;
    }

    public int getYear() {
        return this.year;
    }

    public int getImdbId() {
        return this.imdbId;
    }

    public int getTmdbId() {
        return this.tmdbId;
    }

    public Locale getLanguage() {
        return this.language == null ? null : Locale.forLanguageTag(this.language);
    }

    public String getNameWithYear() {
        return Movie.toString(this.name, this.year);
    }

    @Override
    public List<String> getEffectiveNames() {
        if (this.name == null || this.name.length() == 0) {
            return Collections.emptyList();
        }
        if (this.aliasNames == null || this.aliasNames.length == 0) {
            return Collections.singletonList(Movie.toString(this.name, this.year));
        }
        return FunctionList.of(n -> n == 0 ? Movie.toString(this.name, this.year) : Movie.toString(this.aliasNames[n - 1], this.year), 1 + this.aliasNames.length);
    }

    public List<String> getEffectiveNamesWithoutYear() {
        return super.getEffectiveNames();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Movie) {
            Movie movie = (Movie)object;
            if (this.tmdbId > 0 && movie.tmdbId > 0) {
                return this.tmdbId == movie.tmdbId;
            }
            if (this.imdbId > 0 && movie.imdbId > 0) {
                return this.imdbId == movie.imdbId;
            }
            return this.year == movie.year && this.name.equals(movie.name);
        }
        return false;
    }

    @Override
    public Movie clone() {
        return new Movie(this.id, this.name, this.aliasNames, this.year, this.imdbId, this.tmdbId, this.getLanguage());
    }

    @Override
    public String toString() {
        if (this.name != null && this.year > 0) {
            return Movie.toString(this.name, this.year);
        }
        if (this.tmdbId > 0) {
            return "TheMovieDB::" + this.tmdbId;
        }
        if (this.imdbId > 0) {
            return "IMDB::" + this.imdbId;
        }
        return super.toString();
    }

    public static Movie IMDB(int n) {
        return new Movie(n, null, null, 0, n, 0, null);
    }

    public static Movie IMDB(String string, int n, int n2) {
        return new Movie(n2, string, null, n, n2, 0, null);
    }

    public static Movie TMDB(int n) {
        return new Movie(n, null, null, 0, 0, n, null);
    }

    public static Movie TMDB(String string, int n, int n2) {
        return new Movie(n2, string, null, n, 0, n2, null);
    }

    public static Movie NameYear(String string, int n) {
        return new Movie(0, string, null, n, 0, 0, null);
    }

    public static Movie ID(Integer n, Integer n2, String string, Integer n3) {
        if (n != null && n > 0 || n2 != null && n2 > 0 || string != null && n3 != null && !string.isEmpty() && n3 > 0) {
            int n4 = n != null && n > 0 ? n : (n2 != null && n2 > 0 ? n2 : 0);
            return new Movie(n4, string, null, n3 == null ? 0 : n3, n2 == null ? 0 : n2, n == null ? 0 : n, null);
        }
        return null;
    }

    public static Movie matchNameYear(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        Matcher matcher = NAME_YEAR_PATTERN.matcher(string.trim());
        if (matcher.matches()) {
            return Movie.NameYear(matcher.group(1).trim(), Integer.parseInt(matcher.group(2)));
        }
        return null;
    }

    public static String toString(String string, int n) {
        return n > 0 ? string + " (" + n + ")" : string;
    }
}

