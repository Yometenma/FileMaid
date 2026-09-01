package net.filemaid.media;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import net.filemaid.Logging;
import net.filemaid.MemoryCache;
import net.filemaid.WebServices;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieDetails;

public class MovieCharacteristicsOrder
implements Comparator<Movie> {
    private File file;
    private Optional<MediaCharacteristics> mediaCharacteristics;
    private final MemoryCache<Movie, Integer> cache = MemoryCache.forObject();

    public MovieCharacteristicsOrder(File file) {
        this.file = file;
        this.mediaCharacteristics = null;
    }

    private synchronized Optional<MediaCharacteristics> mediaCharacteristics() {
        if (this.mediaCharacteristics == null) {
            this.mediaCharacteristics = CachedMediaCharacteristics.getMediaCharacteristics(this.file);
        }
        return this.mediaCharacteristics;
    }

    private <T> T mediaCharacteristics(Function<MediaCharacteristics, T> function) {
        return this.mediaCharacteristics().map(function).orElse(null);
    }

    @Override
    public int compare(Movie movie, Movie movie2) {
        int n = this.cache.get(movie, this::getScore);
        int n2 = this.cache.get(movie2, this::getScore);
        return Integer.compare(n2, n);
    }

    public int getScore(Movie movie) {
        int n = 0;
        try {
            Duration duration = this.mediaCharacteristics(MediaCharacteristics::getDuration);
            if (duration != null) {
                Logging.debug.finest(Logging.message("Fetch additional movie details", movie));
                MovieDetails movieDetails = WebServices.TheMovieDB.getMovieInfo(movie, movie.getLanguage(), false);
                if (movieDetails != null) {
                    Object object;
                    if (movieDetails.getOriginalLanguage() != null && (object = this.mediaCharacteristics(MediaCharacteristics::getAudioLanguage)) != null && ((String)object).contains(movieDetails.getOriginalLanguage())) {
                        n += 2;
                    }
                    if (movieDetails.getReleased() != null && (object = this.mediaCharacteristics(MediaCharacteristics::getCreationTime)) != null) {
                        long l = movieDetails.getReleased().toInstant().until((Temporal)object, ChronoUnit.DAYS);
                        if (l < 0L) {
                            n -= 2;
                        } else if (l < 365L) {
                            n += 2;
                        } else if (l < 3650L) {
                            ++n;
                        }
                    }
                    if (movieDetails.getRuntime() != null) {
                        n -= Math.abs(movieDetails.getRuntime() - (int)duration.toMinutes()) / 10;
                    }
                }
            }
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Movie Score", this.file, exception));
        }
        return n;
    }

    public static MovieCharacteristicsOrder compareTo(File file) {
        return file == null ? null : new MovieCharacteristicsOrder(file);
    }
}

