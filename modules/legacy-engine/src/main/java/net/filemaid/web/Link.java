package net.filemaid.web;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.util.StringUtilities;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieDetails;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SeriesInfo;

public enum Link {
    TheTVDB,
    AniDB,
    TheMovieDB_TV,
    TheMovieDB,
    IMDb;

    private static final Pattern IMDB_ID;
    private static final Pattern TMDB_MOVIE_ID;
    private static final Pattern TMDB_TV_ID;
    private static final Pattern TVDB_ID;
    private static final Pattern ANIDB_ID;

    public Integer matchID(CharSequence charSequence) {
        return StringUtilities.streamMatches(charSequence, this.pattern(), matchResult -> matchResult.group(1)).reduce((string, string2) -> string2).map(Integer::parseInt).orElse(null);
    }

    public Integer parseID(CharSequence charSequence) {
        Matcher matcher = this.pattern().matcher(charSequence);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    public boolean findID(CharSequence charSequence) {
        return this.pattern().matcher(charSequence).find();
    }

    public String getURL(Movie movie) {
        switch (this) {
            case TheMovieDB: {
                return this.getURL(movie.getTmdbId());
            }
            case IMDb: {
                return this.getURL(movie.getImdbId());
            }
        }
        return null;
    }

    public String getURL(MovieDetails movieDetails) {
        switch (this) {
            case TheMovieDB: {
                return this.getURL(movieDetails.getId());
            }
            case IMDb: {
                return this.getURL(movieDetails.getImdbId());
            }
        }
        return null;
    }

    public String getURL(SearchResult searchResult) {
        switch (this) {
            case TheMovieDB_TV: {
                return this.getURL(searchResult.getId());
            }
            case TheTVDB: {
                return this.getURL(searchResult.getId());
            }
            case AniDB: {
                return this.getURL(searchResult.getId());
            }
        }
        return null;
    }

    public String getURL(SeriesInfo seriesInfo) {
        switch (this) {
            case TheMovieDB_TV: {
                return this.getURL(seriesInfo.getId());
            }
            case TheTVDB: {
                return this.getURL(seriesInfo.getId());
            }
            case AniDB: {
                return this.getURL(seriesInfo.getId());
            }
        }
        return null;
    }

    public String getID(Movie movie) {
        switch (this) {
            case TheMovieDB: {
                return this.getID(movie.getTmdbId());
            }
            case IMDb: {
                return this.getID(movie.getImdbId());
            }
        }
        return null;
    }

    public String getID(MovieDetails movieDetails) {
        switch (this) {
            case TheMovieDB: {
                return this.getID(movieDetails.getId());
            }
            case IMDb: {
                return this.getID(movieDetails.getImdbId());
            }
        }
        return null;
    }

    public String getID(SeriesInfo seriesInfo) {
        return this.toString().equals(seriesInfo.getDatabase()) ? this.getID(seriesInfo.getId()) : null;
    }

    public String getID(Integer n) {
        if (n == null || n <= 0) {
            return null;
        }
        switch (this) {
            case IMDb: {
                return String.format(Locale.ROOT, "tt%07d", n);
            }
        }
        return Integer.toString(n);
    }

    public String getURL(Integer n) {
        if (n == null || n <= 0) {
            return null;
        }
        switch (this) {
            case TheMovieDB: {
                return "https://www.themoviedb.org/movie/" + n;
            }
            case IMDb: {
                return "https://www.imdb.com/title/" + this.getID(n);
            }
            case TheMovieDB_TV: {
                return "https://www.themoviedb.org/tv/" + n;
            }
            case TheTVDB: {
                return "https://www.thetvdb.com/dereferrer/series/" + n;
            }
            case AniDB: {
                return "https://anidb.net/anime/" + n;
            }
        }
        return null;
    }

    private static Pattern pattern(String string) {
        return Pattern.compile("(?<!\\p{Alnum})(?:" + string + ")(?!\\p{Alnum})", 2);
    }

    private Pattern pattern() {
        switch (this) {
            case TheMovieDB: {
                return TMDB_MOVIE_ID;
            }
            case IMDb: {
                return IMDB_ID;
            }
            case TheMovieDB_TV: {
                return TMDB_TV_ID;
            }
            case TheTVDB: {
                return TVDB_ID;
            }
            case AniDB: {
                return ANIDB_ID;
            }
        }
        return null;
    }

    public String tag() {
        switch (this) {
            case TheMovieDB: {
                return "tmdb";
            }
            case TheTVDB: {
                return "tvdb";
            }
            case AniDB: {
                return "anidb";
            }
            case TheMovieDB_TV: {
                return "tmdb";
            }
            case IMDb: {
                return "imdb";
            }
        }
        return null;
    }

    public String toString() {
        switch (this) {
            case TheMovieDB: {
                return "TheMovieDB";
            }
            case TheMovieDB_TV: {
                return "TheMovieDB::TV";
            }
        }
        return this.name();
    }

    static {
        IMDB_ID = Link.pattern("tt(\\d{7,11})");
        TMDB_MOVIE_ID = Link.pattern("(?:tmdb[->\":=]+|(?<!\" |<director |    <)tmdbid[->\":=]+|themoviedb[.]org[/]movie[/]|^movie[/])(\\d+)");
        TMDB_TV_ID = Link.pattern("(?:tmdb[->\":=]+|(?<!\" |<director |    <)tmdbid[->\":=]+|themoviedb[.]org[/]tv[/]|^tv[/])(\\d+)");
        TVDB_ID = Link.pattern("(?:tvdb[->\":=]+|(?<!\" |<director |    <)tvdbid[->\":=]+|fanart[/]tv[/]|thetvdb[.]com[\\p{Graph}]*?[\\p{Punct}](?:id[=]|series[/]))(\\d+)");
        ANIDB_ID = Link.pattern("(?:anidb[->\":=]+|anidbid[->\":=]+|anidb.net[/]anime[/]|anidb.net[/]a|^a)(\\d+)");
    }
}

