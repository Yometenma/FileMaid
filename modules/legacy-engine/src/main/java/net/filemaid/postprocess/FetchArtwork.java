package net.filemaid.postprocess;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.function.Predicate;
import net.filemaid.Resource;
import net.filemaid.WebServices;
import net.filemaid.postprocess.ApplyFolder;
import net.filemaid.postprocess.Feedback;
import net.filemaid.util.FileUtilities;
import net.filemaid.web.Artwork;
import net.filemaid.web.ArtworkProvider;
import net.filemaid.web.Movie;
import net.filemaid.web.XDB;

public class FetchArtwork
extends ApplyFolder {
    @Override
    protected void applyMovieFolder(Movie movie, Locale locale, File file, Feedback feedback) throws Exception {
        this.fetchArtwork(WebServices.TheMovieDB, movie::getTmdbId, "posters", locale, this.any(new Object[0]), new File(file, "poster.jpg"), feedback);
        this.fetchArtwork(WebServices.TheMovieDB, movie::getTmdbId, "backdrops", Locale.ROOT, this.any(new Object[0]), new File(file, "backdrop.jpg"), feedback);
        this.fetchArtwork(WebServices.FanartTV.TheMovieDB, movie::getTmdbId, null, locale, this.any("hdmovieclearart", "movieart"), new File(file, "clearart.png"), feedback);
        this.fetchArtwork(WebServices.FanartTV.TheMovieDB, movie::getTmdbId, null, locale, this.any("hdmovielogo", "movielogo"), new File(file, "logo.png"), feedback);
        this.fetchArtwork(WebServices.FanartTV.TheMovieDB, movie::getTmdbId, null, locale, this.any("bluray", "dvd"), new File(file, "disc.png"), feedback);
    }

    @Override
    protected void applySeriesFolder(ApplyFolder.SID sID, Locale locale, File file, Feedback feedback) throws Exception {
        this.fetchArtwork(WebServices.TheMovieDB_TV, sID.id(XDB.TheMovieDB), "posters", locale, this.any(new Object[0]), new File(file, "poster.jpg"), feedback);
        this.fetchArtwork(WebServices.TheMovieDB_TV, sID.id(XDB.TheMovieDB), "backdrops", Locale.ROOT, this.any(new Object[0]), new File(file, "backdrop.jpg"), feedback);
        this.fetchArtwork(WebServices.FanartTV.TheTVDB, sID.id(XDB.TheTVDB), null, locale, this.any("hdclearart", "clearart"), new File(file, "clearart.png"), feedback);
        this.fetchArtwork(WebServices.FanartTV.TheTVDB, sID.id(XDB.TheTVDB), null, locale, this.any("hdtvlogo", "clearlogo"), new File(file, "logo.png"), feedback);
        this.fetchArtwork(WebServices.FanartTV.TheTVDB, sID.id(XDB.TheTVDB), null, locale, this.any("tvthumb"), new File(file, "landscape.png"), feedback);
    }

    @Override
    protected void applySeasonFolder(ApplyFolder.SID sID, int n, Locale locale, File file, Feedback feedback) throws Exception {
        this.fetchArtwork(WebServices.FanartTV.TheTVDB, sID.id(XDB.TheTVDB), null, locale, this.all("seasonthumb", n), new File(file, "landscape.png"), feedback);
    }

    private Predicate<Artwork> any(Object ... objectArray) {
        return objectArray.length == 0 ? artwork -> true : artwork -> Arrays.stream(objectArray).anyMatch(object -> artwork.matches(object));
    }

    private Predicate<Artwork> all(Object ... objectArray) {
        return artwork -> artwork.matches(objectArray);
    }

    private void fetchArtwork(ArtworkProvider artworkProvider, Resource<Integer> resource, String string, Locale locale, Predicate<Artwork> predicate, File file, Feedback feedback) throws Exception {
        if (resource == null || file.exists()) {
            return;
        }
        if (feedback.isCancelled()) {
            throw new CancellationException();
        }
        int n = resource.get();
        if (n <= 0) {
            return;
        }
        for (Artwork artwork : artworkProvider.getArtwork(n, string, locale)) {
            URL uRL;
            if (!predicate.test(artwork) || (uRL = artwork.getUrl()) == null) continue;
            feedback.file(uRL, file);
            FileUtilities.writeFile(this.cache(uRL), file);
            return;
        }
    }
}

