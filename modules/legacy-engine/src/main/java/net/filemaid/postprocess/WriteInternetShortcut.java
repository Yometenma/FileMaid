package net.filemaid.postprocess;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Function;
import net.filemaid.Resource;
import net.filemaid.postprocess.ApplyFolder;
import net.filemaid.postprocess.Feedback;
import net.filemaid.util.FileUtilities;
import net.filemaid.web.Link;
import net.filemaid.web.Movie;
import net.filemaid.web.XDB;

public class WriteInternetShortcut
extends ApplyFolder {
    @Override
    protected void applyMovieFolder(Movie movie, Locale locale, File file, Feedback feedback) throws Exception {
        this.writeInternetShortcut(movie::getTmdbId, Link.TheMovieDB::getURL, new File(file, "tmdb.url"), feedback);
    }

    @Override
    protected void applySeriesFolder(ApplyFolder.SID sID, Locale locale, File file, Feedback feedback) throws Exception {
        this.writeInternetShortcut(sID.id(XDB.TheMovieDB), Link.TheMovieDB_TV::getURL, new File(file, "tmdb.url"), feedback);
        this.writeInternetShortcut(sID.id(XDB.TheTVDB), Link.TheTVDB::getURL, new File(file, "tvdb.url"), feedback);
        this.writeInternetShortcut(sID.id(XDB.AniDB), Link.AniDB::getURL, new File(file, "anidb.url"), feedback);
    }

    @Override
    protected void applySeasonFolder(ApplyFolder.SID sID, int n, Locale locale, File file, Feedback feedback) throws Exception {
    }

    protected void writeInternetShortcut(Resource<Integer> resource, Function<Integer, String> function, File file, Feedback feedback) throws Exception {
        if (resource == null || file.exists()) {
            return;
        }
        try {
            String string = function.apply(resource.get());
            if (string == null) {
                return;
            }
            feedback.file(string, file);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[InternetShortcut]").append("\r\n");
            stringBuilder.append("URL=").append(string).append("\r\n");
            FileUtilities.writeFile(stringBuilder.toString().getBytes(StandardCharsets.UTF_8), file);
        }
        catch (Exception exception) {
            feedback.warning(exception, file);
        }
    }
}

