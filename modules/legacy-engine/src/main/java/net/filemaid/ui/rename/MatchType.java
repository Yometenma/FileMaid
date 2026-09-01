package net.filemaid.ui.rename;

import java.io.File;
import java.io.Serializable;
import javax.swing.Icon;
import net.filemaid.ResourceManager;
import net.filemaid.WebServices;
import net.filemaid.media.LocalDatasource;
import net.filemaid.similarity.Match;
import net.filemaid.web.AudioTrack;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.Link;
import net.filemaid.web.Movie;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SeriesInfo;

public enum MatchType {
    Movie,
    Episode,
    Music,
    Photo,
    File,
    String,
    Object;


    public boolean canFormat() {
        switch (this) {
            case Movie: 
            case Episode: 
            case Music: 
            case Photo: 
            case File: {
                return true;
            }
        }
        return false;
    }

    public boolean canEdit() {
        switch (this) {
            case Movie: 
            case Episode: 
            case Music: 
            case File: {
                return true;
            }
        }
        return false;
    }

    public boolean canLink() {
        switch (this) {
            case Movie: 
            case Episode: {
                return true;
            }
        }
        return false;
    }

    public static MatchType getType(Match<?, ?> match) {
        return MatchType.getType(match.getValue());
    }

    public static MatchType getType(Object object) {
        if (object instanceof Movie) {
            return Movie;
        }
        if (object instanceof Episode) {
            return Episode;
        }
        if (object instanceof AudioTrack) {
            return Music;
        }
        if (object instanceof LocalDatasource.PhotoFile) {
            return Photo;
        }
        if (object instanceof File) {
            return File;
        }
        if (object instanceof String) {
            return String;
        }
        return Object;
    }

    public static Icon getIcon(Match<?, ?> match) {
        return MatchType.getIcon(match.getValue());
    }

    public static Icon getIcon(Object object) {
        if (object instanceof Movie) {
            Movie movie = (Movie)object;
            if (movie.getTmdbId() > 0) {
                return WebServices.TheMovieDB.getIcon();
            }
            return WebServices.OMDb.getIcon();
        }
        if (object instanceof Episode) {
            Episode episode = (Episode)object;
            if (episode.getSeriesInfo() != null) {
                return WebServices.getEpisodeListProvider(episode.getSeriesInfo().getDatabase()).getIcon();
            }
            return WebServices.MediaInfoID3.getIcon();
        }
        if (object instanceof AudioTrack) {
            AudioTrack audioTrack = (AudioTrack)object;
            return WebServices.getMusicLookupService(audioTrack.getDatabase()).getIcon();
        }
        if (object instanceof LocalDatasource.PhotoFile) {
            return ResourceManager.getIcon("search.exif");
        }
        if (object instanceof File) {
            return ResourceManager.getIcon("search.generic");
        }
        if (object instanceof String) {
            return ResourceManager.getIcon("search.literal");
        }
        return ResourceManager.getIcon("search.generic");
    }

    public static String getLink(Object object) {
        EpisodeListProvider episodeListProvider;
        SeriesInfo seriesInfo;
        Serializable serializable;
        if (object instanceof Movie) {
            serializable = (Movie)object;
            if (((Movie)serializable).getTmdbId() > 0) {
                return Link.TheMovieDB.getURL((Movie)serializable);
            }
            if (((Movie)serializable).getImdbId() > 0) {
                return Link.IMDb.getURL((Movie)serializable);
            }
        }
        if (object instanceof Episode && (seriesInfo = ((Episode)(serializable = (Episode)object)).getSeriesInfo()) != null && (episodeListProvider = WebServices.getEpisodeListProvider(seriesInfo.getDatabase())) != null) {
            return episodeListProvider.getEpisodeListLink(new SearchResult(seriesInfo.getId())).toString();
        }
        return null;
    }
}

