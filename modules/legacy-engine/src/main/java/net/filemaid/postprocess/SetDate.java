package net.filemaid.postprocess;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import net.filemaid.WebServices;
import net.filemaid.postprocess.ApplyMetadata;
import net.filemaid.postprocess.Feedback;
import net.filemaid.web.Episode;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieDetails;
import net.filemaid.web.SimpleDate;

public class SetDate
implements ApplyMetadata {
    @Override
    public void apply(File file, File file2, Movie movie, Feedback feedback) throws Exception {
        MovieDetails movieDetails = WebServices.TheMovieDB.getMovieInfo(movie, movie.getLanguage(), false);
        if (movieDetails != null) {
            this.setTimeStamp(file2, movieDetails.getReleased(), feedback);
        }
    }

    @Override
    public void apply(File file, File file2, Episode episode, Feedback feedback) throws Exception {
        this.setTimeStamp(file2, episode.getAirdate(), feedback);
    }

    protected void setTimeStamp(File file, SimpleDate simpleDate, Feedback feedback) throws Exception {
        if (simpleDate == null) {
            return;
        }
        feedback.info(simpleDate, file);
        FileTime fileTime = FileTime.fromMillis(simpleDate.getTimeStamp());
        Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class, new LinkOption[0]).setTimes(fileTime, null, fileTime);
    }
}

