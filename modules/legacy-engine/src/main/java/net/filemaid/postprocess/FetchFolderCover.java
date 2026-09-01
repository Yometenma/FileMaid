package net.filemaid.postprocess;

import java.io.File;
import java.net.URL;
import java.util.Locale;
import net.filemaid.WebServices;
import net.filemaid.postprocess.ApplyFolder;
import net.filemaid.postprocess.Feedback;
import net.filemaid.util.FileUtilities;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieDetails;
import net.filemaid.web.SeriesDetails;

public class FetchFolderCover
extends ApplyFolder {
    private String name;

    public FetchFolderCover(String string) {
        this.name = string;
    }

    @Override
    protected void applyMovieFolder(Movie movie, Locale locale, File file, Feedback feedback) throws Exception {
        File file2 = new File(file, this.name);
        if (file2.exists()) {
            return;
        }
        MovieDetails movieDetails = WebServices.TheMovieDB.getMovieInfo(movie, locale, true);
        if (movieDetails == null) {
            return;
        }
        URL uRL = movieDetails.getPoster();
        if (uRL == null) {
            return;
        }
        feedback.file(uRL, file2);
        FileUtilities.writeFile(this.cache(uRL), file2);
    }

    @Override
    protected void applySeriesFolder(ApplyFolder.SID sID, Locale locale, File file, Feedback feedback) throws Exception {
        File file2 = new File(file, this.name);
        if (file2.exists()) {
            return;
        }
        SeriesDetails seriesDetails = sID.getSeriesDetails(locale);
        if (seriesDetails == null) {
            return;
        }
        URL uRL = seriesDetails.getPoster();
        if (uRL == null) {
            return;
        }
        feedback.file(uRL, file2);
        FileUtilities.writeFile(this.cache(uRL), file2);
    }

    @Override
    protected void applySeasonFolder(ApplyFolder.SID sID, int n, Locale locale, File file, Feedback feedback) throws Exception {
    }
}

