package net.filemaid.media;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.WebServices;
import net.filemaid.similarity.Normalization;
import net.filemaid.util.FileUtilities;
import net.filemaid.web.AudioTrack;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeFormat;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.Link;
import net.filemaid.web.Movie;
import net.filemaid.web.MoviePart;
import net.filemaid.web.MultiEpisode;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SimpleDate;

public enum NamingStandard {
    Default,
    Plex{

        @Override
        public String getSeriesNameYear(Episode episode) {
            return this.formatSeriesName(episode);
        }
    }
    ,
    Kodi{

        @Override
        public String formatSeasonNumber(Integer n) {
            return "Season " + n;
        }

        @Override
        public String getEpisodeNumber(Episode episode) {
            if (episode instanceof MultiEpisode) {
                return EpisodeFormat.DEFAULT.formatMultiSxE((MultiEpisode)episode);
            }
            return EpisodeFormat.DEFAULT.formatSxE(episode);
        }

        @Override
        public String formatID(String string, Object object) {
            return string == null || object == null ? null : " {" + string + "=" + object + "}";
        }
    }
    ,
    Emby{

        @Override
        public String formatID(String string, Object object) {
            return string == null || object == null ? null : " [" + string + "id=" + object + "]";
        }
    }
    ,
    Jellyfin{

        @Override
        public String formatID(String string, Object object) {
            return string == null || object == null ? null : " [" + string + "id-" + object + "]";
        }
    };

    public static final int TITLE_MAX_LENGTH = 150;
    public static final Pattern MULTI_EPISODE_PART;
    public static final Pattern NAME_YEAR_PATTERN;

    public Structure getPath(Object object) {
        if (object instanceof Episode) {
            return this.getPath((Episode)object);
        }
        if (object instanceof Movie) {
            return this.getPath((Movie)object);
        }
        if (object instanceof AudioTrack) {
            return this.getPath((AudioTrack)object);
        }
        return null;
    }

    public Structure getPath(Episode episode) {
        return new Structure(EpisodeUtilities.isAnimeType(episode) ? this.getAnimeFolder() : this.getSeriesFolder(), this.getSeriesNameYear(episode), this.getSeasonFolder(episode), this.getSeriesNameYear(episode), this.getEpisodeNumber(episode), this.getEpisodeTitle(episode), null);
    }

    public Structure getPath(Movie movie) {
        return new Structure(this.getMovieFolder(), this.getMovieName(movie), null, this.getMovieName(movie), null, null, this.getMoviePart(movie));
    }

    public Structure getPath(AudioTrack audioTrack) {
        return new Structure(this.getMusicFolder(), this.getArtistFolder(audioTrack), this.getAlbumFolder(audioTrack), this.getTrackName(audioTrack), null, null, null);
    }

    public String getMovieFolder() {
        return "Movies";
    }

    public String getSeriesFolder() {
        return "TV Shows";
    }

    public String getAnimeFolder() {
        return "Anime";
    }

    public String getMusicFolder() {
        return "Music";
    }

    public String getMovieName(Movie movie) {
        return movie.getName() + " (" + movie.getYear() + ")";
    }

    public String getMoviePart(Movie movie) {
        if (movie instanceof MoviePart) {
            MoviePart moviePart = (MoviePart)movie;
            return " - CD" + moviePart.getPartIndex();
        }
        return null;
    }

    public String getSeasonFolder(Episode episode) {
        if (episode.getSpecial() != null) {
            return "Specials";
        }
        if (episode.getSeason() != null) {
            return this.formatSeasonNumber(episode.getSeason());
        }
        return null;
    }

    public String formatSeasonNumber(Integer n) {
        return "Season " + this.pad(n);
    }

    public String getSeriesNameYear(Episode episode) {
        String string = this.formatSeriesName(episode);
        Integer n = Optional.ofNullable(episode).map(Episode::getSeriesInfo).map(SeriesInfo::getStartDate).map(SimpleDate::getYear).orElse(null);
        if (n == null || NamingStandard.isNameYear(string)) {
            return string;
        }
        return string + " (" + n + ")";
    }

    public String formatSeriesName(Episode episode) {
        if (EpisodeUtilities.isAnimeType(episode) && episode.getSeriesInfo() != null && episode.getSeriesInfo().getName() != null) {
            return episode.getSeriesInfo().getName();
        }
        return episode.getSeriesName();
    }

    public String getEpisodeNumber(Episode episode) {
        if (episode.getSpecial() != null && EpisodeUtilities.isAnimeType(episode)) {
            return EpisodeFormat.DEFAULT.formatMultiRangeNumbers(EpisodeUtilities.streamMultiEpisode(episode)::iterator, "", "S%02d", "", "-", "");
        }
        return EpisodeFormat.DEFAULT.formatS00E00(episode);
    }

    public String getEpisodeTitle(Episode episode) {
        return Normalization.truncateText(NamingStandard.replaceEpisodePart(episode.getTitle(), ""), 150);
    }

    public String getArtistFolder(AudioTrack audioTrack) {
        int n;
        String string = audioTrack.getAlbumArtist();
        if (string == null) {
            string = audioTrack.getArtist();
        }
        if ((n = string.indexOf(" feat. ")) > 0) {
            string = string.substring(0, n);
        }
        return string;
    }

    public String getAlbumFolder(AudioTrack audioTrack) {
        return audioTrack.getAlbum();
    }

    public String getTrackName(AudioTrack audioTrack) {
        String object = audioTrack.getTrackTitle();
        if (object == null) {
            object = audioTrack.getTitle();
        }
        if (audioTrack.getTrack() != null) {
            object = this.pad(audioTrack.getTrack()) + " - " + (String)object;
        }
        return object;
    }

    public String formatID(String string, Object object) {
        return string == null || object == null ? null : " {" + string + "-" + object + "}";
    }

    public String getID(SeriesInfo seriesInfo) {
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, seriesInfo)) {
            return this.formatID("tmdb", seriesInfo.getId());
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, seriesInfo)) {
            return this.formatID("tvdb", seriesInfo.getId());
        }
        return this.formatID(seriesInfo.getDatabase().toLowerCase(Locale.ROOT), seriesInfo.getId());
    }

    public String getID(Movie movie) {
        if (movie.getTmdbId() > 0) {
            return this.formatID("tmdb", movie.getTmdbId());
        }
        if (movie.getImdbId() > 0) {
            return this.formatID("imdb", Link.IMDb.getID(movie));
        }
        return null;
    }

    private String pad(Integer n) {
        return n < 10 ? "0" + n.toString() : n.toString();
    }

    public static boolean isNameYear(CharSequence charSequence) {
        return charSequence == null ? false : NAME_YEAR_PATTERN.matcher(charSequence).matches();
    }

    public static String replaceEpisodePart(CharSequence charSequence, String string) {
        return charSequence == null ? null : MULTI_EPISODE_PART.matcher(charSequence).replaceAll(string);
    }

    static {
        MULTI_EPISODE_PART = Pattern.compile("[ ]+[(][0-9/]{1,3}[)]$");
        NAME_YEAR_PATTERN = Pattern.compile("(.+?)[ ][(]((?:19|20)\\d{2})[)]");
    }

    public static class Structure {
        private String category;
        private String library;
        private String collection;
        private String group;
        private String name;
        private String number;
        private String title;
        private String tag;
        private String part;
        private String suffix;
        private boolean validate = true;

        public Structure(String string, String string2, String string3, String string4, String string5, String string6, String string7) {
            this.category = string;
            this.library = null;
            this.collection = string2;
            this.group = string3;
            this.name = string4;
            this.number = string5;
            this.title = string6;
            this.tag = null;
            this.part = string7;
            this.suffix = null;
        }

        private String apply(String string, String string2) {
            return string2 == null ? null : (string == null ? string2 : string.concat(string2));
        }

        public Structure category(String string) {
            this.category = this.apply(this.category, string);
            return this;
        }

        public Structure library(String string) {
            this.library = this.apply(this.library, string);
            return this;
        }

        public Structure collection(String string) {
            this.collection = this.apply(this.collection, string);
            return this;
        }

        public Structure group(String string) {
            this.group = this.apply(this.group, string);
            return this;
        }

        public Structure name(String string) {
            this.name = this.apply(this.name, string);
            return this;
        }

        public Structure number(String string) {
            this.number = this.apply(this.number, string);
            return this;
        }

        public Structure title(String string) {
            this.title = this.apply(this.title, string);
            return this;
        }

        public Structure tag(String string) {
            this.tag = this.apply(this.tag, string);
            return this;
        }

        public Structure part(String string) {
            this.part = this.apply(this.part, string);
            return this;
        }

        public Structure suffix(String string) {
            this.suffix = this.apply(this.suffix, string);
            return this;
        }

        public Structure validate(boolean bl) {
            this.validate = bl;
            return this;
        }

        private String component(String string) {
            return FileUtilities.replacePathSeparators(string, " ").trim();
        }

        private String validate(String string) {
            if (this.validate) {
                string = Normalization.replaceColon(string, " - ", " ", "");
                string = Normalization.replaceSlash(string, " - ", " ", ".", "");
                string = Normalization.normalizeQuotationMarks(string);
                string = Normalization.trimTrailingPunctuation(string);
                string = FileUtilities.validateFileName(string);
            }
            return string;
        }

        public String getPath() {
            return Stream.of(this.category, this.library, this.collection, this.group, this.getName()).filter(Objects::nonNull).map(this::validate).map(this::component).collect(Collectors.joining("/"));
        }

        public String getName() {
            return Stream.of(this.getObject(), this.tag, this.part, this.suffix).filter(Objects::nonNull).collect(Collectors.joining());
        }

        public String getObject() {
            return Stream.of(this.name, this.number, this.title).filter(Objects::nonNull).collect(Collectors.joining(" - "));
        }
    }
}

