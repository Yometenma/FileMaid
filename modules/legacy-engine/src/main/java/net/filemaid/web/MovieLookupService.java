package net.filemaid.web;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.filemaid.util.RegularExpressions;
import net.filemaid.web.Datasource;
import net.filemaid.web.Link;
import net.filemaid.web.LookupException;
import net.filemaid.web.Movie;

public interface MovieLookupService
extends Datasource {
    public List<Movie> searchMovie(String var1, Locale var2) throws Exception;

    public Movie getMovieDescriptor(Movie var1, Locale var2) throws Exception;

    public List<? extends Movie> getIndex() throws Exception;

    default public List<Movie> lookupMovie(File file, Locale locale) throws Exception {
        return null;
    }

    default public List<Movie> lookupMovie(String string, Locale locale) throws Exception {
        if (string.startsWith("\"") && string.endsWith("\"")) {
            return this.searchMovie(string.substring(1, string.length() - 1), locale);
        }
        Movie movie = this.grepMovie(string);
        if (movie != null) {
            if ((movie = this.getMovieDescriptor(movie, locale)) != null) {
                return Collections.singletonList(movie);
            }
            throw new LookupException((Object)"Bad ID", string);
        }
        return this.searchMovie(string, locale);
    }

    default public Movie grepMovie(String string) {
        if (RegularExpressions.DIGIT.matcher(string).matches()) {
            return Movie.IMDB(Integer.parseInt(string));
        }
        Integer n = Link.IMDb.parseID(string);
        if (n != null) {
            return Movie.IMDB(n);
        }
        return null;
    }
}

