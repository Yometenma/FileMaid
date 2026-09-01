package net.filemaid.postprocess;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.filemaid.WebServices;
import net.filemaid.format.ExpressionFormatMethods;
import net.filemaid.format.ExtendedMetadataMethods;
import net.filemaid.media.MediaCharacteristicsParser;
import net.filemaid.media.MediaInfoTable;
import net.filemaid.mediainfo.StreamKind;
import net.filemaid.postprocess.ApplyFolder;
import net.filemaid.postprocess.Feedback;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.XmlUtilities;
import net.filemaid.web.Artwork;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.Link;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieCollection;
import net.filemaid.web.MovieDetails;
import net.filemaid.web.Person;
import net.filemaid.web.SeriesDetails;
import net.filemaid.web.SeriesInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class WriteMetadataXml
extends ApplyFolder {
    @Override
    protected void applyMovieFolder(Movie movie, Locale locale, File file, Feedback feedback) throws Exception {
        File file2 = new File(file, "movie.nfo");
        if (file2.exists()) {
            return;
        }
        MovieDetails movieDetails = WebServices.TheMovieDB.getMovieInfo(movie, locale, true);
        if (movieDetails == null) {
            return;
        }
        feedback.file(movieDetails, file2);
        XmlUtilities.writeDocument(this.xml(movie, movieDetails, file, feedback), file2);
    }

    @Override
    protected void applySeriesFolder(ApplyFolder.SID sID, Locale locale, File file, Feedback feedback) throws Exception {
        File file2 = new File(file, "tvshow.nfo");
        if (file2.exists()) {
            return;
        }
        SeriesDetails seriesDetails = sID.getSeriesDetails(locale);
        if (seriesDetails == null) {
            return;
        }
        feedback.file(seriesDetails, file2);
        XmlUtilities.writeDocument(this.xml(seriesDetails, file, feedback), file2);
    }

    @Override
    protected void applySeasonFolder(ApplyFolder.SID sID, int n, Locale locale, File file, Feedback feedback) throws Exception {
    }

    private Document xml(SeriesDetails seriesDetails, File file, Feedback feedback) throws Exception {
        Object object;
        Element element = XmlUtilities.root("tvshow");
        Link link = this.db(seriesDetails);
        XmlUtilities.text(element, "id", seriesDetails.getId());
        XmlUtilities.text(element, "title", seriesDetails.getName());
        this.sorttitle(element, seriesDetails.getName(), seriesDetails.getStartDate());
        XmlUtilities.text(element, "year", seriesDetails.getStartDate() == null ? null : Integer.valueOf(seriesDetails.getStartDate().getYear()));
        XmlUtilities.text(element, "premiered", seriesDetails.getStartDate());
        XmlUtilities.text(element, "mpaa", seriesDetails.getCertification());
        XmlUtilities.text(element, "plot", seriesDetails.getOverview());
        XmlUtilities.text(element, "runtime", seriesDetails.getRuntime());
        Element element2 = XmlUtilities.element(element, "ratings");
        Element element3 = XmlUtilities.element(element2, "rating");
        XmlUtilities.attr(element3, "name", link.tag());
        XmlUtilities.attr(element3, "max", "10");
        XmlUtilities.attr(element3, "default", "true");
        XmlUtilities.text(element3, "value", seriesDetails.getRating());
        XmlUtilities.text(element3, "votes", seriesDetails.getRatingCount());
        XmlUtilities.text(element, "status", seriesDetails.getStatus());
        XmlUtilities.text(element, "studio", seriesDetails.getNetwork());
        XmlUtilities.text(element, "episodeguide", seriesDetails.getId());
        HashSet<Integer> hashSet = new HashSet<Integer>();
        for (Episode object22 : ExtendedMetadataMethods.getEpisodes(seriesDetails)) {
            if (object22.getGroup() == null || (object = Integer.valueOf(EpisodeUtilities.isRegularEpisode(object22) ? object22.getSeason() : 0)) == null || !hashSet.add((Integer)object)) continue;
            Element element4 = XmlUtilities.text(element, "namedseason", object22.getGroup());
            XmlUtilities.attr(element4, "number", object);
        }
        seriesDetails.getGenres().forEach(string -> XmlUtilities.text(element, "genre", string));
        seriesDetails.getCountry().forEach(string -> XmlUtilities.text(element, "country", string));
        List<Artwork> list = ExtendedMetadataMethods.getArtwork(seriesDetails);
        if (list != null) {
            list.stream().filter(artwork -> artwork.matches("posters")).findFirst().ifPresent(artwork -> {
                Element element5 = XmlUtilities.text(element, "thumb", artwork.getUrl());
                XmlUtilities.attr(element5, "aspect", "poster");
            });
            list.stream().filter(artwork -> artwork.matches("logos")).findFirst().ifPresent(artwork -> {
                Element element6 = XmlUtilities.text(element, "thumb", artwork.getUrl());
                XmlUtilities.attr(element6, "aspect", "clearlogo");
            });
            list.stream().filter(artwork -> artwork.matches("backdrops")).findFirst().ifPresent(artwork -> {
                Element element7 = XmlUtilities.element(element, "fanart");
                XmlUtilities.text(element7, "thumb", artwork.getUrl());
            });
        }
        seriesDetails.getCertifications().forEach((string, string2) -> this.certification(element, (String)string, (String)string2));
        List<Person> list2 = ExtendedMetadataMethods.getCrew(seriesDetails);
        if (list2 != null) {
            list2.forEach(person -> this.person(element, (Person)person));
        }
        for (File file2 : this.getMediaFiles(file)) {
            try {
                this.fileinfo(element, file2);
            }
            catch (Exception exception) {
                feedback.warning(exception, file2);
            }
        }
        this.db(element, seriesDetails);
        object = XmlUtilities.text(element, "uniqueid", seriesDetails.getId());
        XmlUtilities.attr((Element)object, "type", link.tag());
        XmlUtilities.attr((Element)object, "default", "true");
        return element.getOwnerDocument();
    }

    private Document xml(Movie movie, MovieDetails movieDetails, File file, Feedback feedback) throws Exception {
        Object object;
        List<Movie> list;
        Element element = XmlUtilities.root("movie");
        XmlUtilities.text(element, "id", movieDetails.getId());
        XmlUtilities.text(element, "title", movieDetails.getName());
        XmlUtilities.text(element, "originaltitle", movieDetails.getOriginalName());
        if (movieDetails.getCollection() != null && movieDetails.getReleased() != null) {
            this.sorttitle(element, movieDetails.getCollection(), movieDetails.getReleased(), movieDetails.getName());
        } else {
            this.sorttitle(element, movieDetails.getName(), movieDetails.getReleased());
        }
        XmlUtilities.text(element, "year", movieDetails.getReleased() == null ? null : Integer.valueOf(movieDetails.getReleased().getYear()));
        XmlUtilities.text(element, "premiered", movieDetails.getReleased());
        XmlUtilities.text(element, "mpaa", movieDetails.getCertification());
        XmlUtilities.text(element, "plot", movieDetails.getOverview());
        XmlUtilities.text(element, "tagline", movieDetails.getTagline());
        XmlUtilities.text(element, "runtime", movieDetails.getRuntime());
        XmlUtilities.text(element, "status", movieDetails.getStatus());
        Element element2 = XmlUtilities.element(element, "ratings");
        Element element3 = XmlUtilities.element(element2, "rating");
        XmlUtilities.attr(element3, "name", "tmdb");
        XmlUtilities.attr(element3, "max", "10");
        XmlUtilities.attr(element3, "default", "true");
        XmlUtilities.text(element3, "value", movieDetails.getRating());
        XmlUtilities.text(element3, "votes", movieDetails.getVotes());
        if (movieDetails.getCollection() != null && (list = ExtendedMetadataMethods.getCollection(movie)) != null) {
            object = XmlUtilities.element(element, "set");
            XmlUtilities.attr((Element)object, "id", ((MovieCollection)list).getId());
            XmlUtilities.text((Node)object, "name", ((MovieCollection)list).getName());
            XmlUtilities.text((Node)object, "overview", ((MovieCollection)list).getOverview());
        }
        movieDetails.getGenres().forEach(string -> XmlUtilities.text(element, "genre", string));
        movieDetails.getKeywords().forEach(string -> XmlUtilities.text(element, "tag", string));
        movieDetails.getProductionCountries().forEach(string -> XmlUtilities.text(element, "country", string));
        movieDetails.getProductionCompanies().forEach(string -> XmlUtilities.text(element, "studio", string));
        List<Artwork> artworkList = ExtendedMetadataMethods.getArtwork(movie);
        if (artworkList != null) {
            artworkList.stream().filter(artwork -> artwork.matches("posters")).findFirst().ifPresent(artwork -> {
                Element element4 = XmlUtilities.text(element, "thumb", artwork.getUrl());
                XmlUtilities.attr(element4, "aspect", "poster");
            });
            artworkList.stream().filter(artwork -> artwork.matches("backdrops")).findFirst().ifPresent(artwork -> {
                Element element5 = XmlUtilities.element(element, "fanart");
                XmlUtilities.text(element5, "thumb", artwork.getUrl());
            });
        }
        movieDetails.getCertifications().forEach((string, string2) -> this.certification(element, (String)string, (String)string2));
        movieDetails.getCrew().forEach(person -> this.person(element, (Person)person));
        for (File file2 : this.getMediaFiles(file)) {
            try {
                this.fileinfo(element, file2);
            }
            catch (Exception exception) {
                feedback.warning(exception, file2);
            }
        }
        this.db(element, Link.IMDb, movieDetails.getImdbId());
        this.db(element, Link.TheMovieDB, movieDetails.getId());
        object = XmlUtilities.text(element, "uniqueid", movieDetails.getId());
        XmlUtilities.attr((Element)object, "type", "tmdb");
        XmlUtilities.attr((Element)object, "default", "true");
        return element.getOwnerDocument();
    }

    private Node sorttitle(Node node, Object ... objectArray) {
        String string = Arrays.stream(objectArray).filter(Objects::nonNull).map(Objects::toString).map(ExpressionFormatMethods::sortName).collect(Collectors.joining(" :: "));
        return XmlUtilities.text(node, "sorttitle", string);
    }

    private Node certification(Node node, String string, String string2) {
        Element element = XmlUtilities.element(node, "certification");
        XmlUtilities.text(element, "country", string);
        XmlUtilities.text(element, "rating", string2);
        return element;
    }

    private Node person(Node node, Person person) {
        if (person.isActor()) {
            Element element = XmlUtilities.element(node, "actor");
            XmlUtilities.text(element, "name", person.getName());
            XmlUtilities.text(element, "role", person.getCharacter());
            XmlUtilities.text(element, "order", person.getOrder());
            XmlUtilities.text(element, "thumb", person.getImage());
            return element;
        }
        if (person.isDirector()) {
            return XmlUtilities.text(node, "director", person.getName());
        }
        if (person.isWriter() || person.isWritingDepartment()) {
            return XmlUtilities.text(node, "credits", person.getName());
        }
        return null;
    }

    private Link db(SeriesDetails seriesDetails) {
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, (SeriesInfo)seriesDetails)) {
            return Link.TheMovieDB_TV;
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, (SeriesInfo)seriesDetails)) {
            return Link.TheTVDB;
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.AniDB, (SeriesInfo)seriesDetails)) {
            return Link.AniDB;
        }
        return null;
    }

    private Node db(Node node, SeriesDetails seriesDetails) {
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, (SeriesInfo)seriesDetails)) {
            return this.db(node, Link.TheMovieDB_TV, seriesDetails.getId());
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, (SeriesInfo)seriesDetails)) {
            Element element = XmlUtilities.text(node, Link.TheTVDB.tag(), "https://thetvdb.com/series/" + seriesDetails.getSlug());
            XmlUtilities.attr(element, "id", seriesDetails.getId());
            return element;
        }
        if (EpisodeUtilities.isInstance((Datasource)WebServices.AniDB, (SeriesInfo)seriesDetails)) {
            return this.db(node, Link.AniDB, seriesDetails.getId());
        }
        return null;
    }

    private Node db(Node node, Link link, Integer n) {
        if (n != null) {
            Element element = XmlUtilities.text(node, link.tag(), link.getURL(n));
            XmlUtilities.attr(element, "id", link.getID(n));
            return element;
        }
        return null;
    }

    private Node fileinfo(Node node, File file) throws Exception {
        MediaInfoTable mediaInfoTable = MediaInfoTable.read(file);
        Element element = XmlUtilities.element(node, "fileinfo");
        XmlUtilities.text(element, "name", file.getName());
        XmlUtilities.text(element, "size", file.length());
        Element element2 = XmlUtilities.element(element, "streamdetails");
        mediaInfoTable.forEach((streamKind, list) -> {
            if (streamKind == StreamKind.Video) {
                for (Map map : list) {
                    Element element3 = XmlUtilities.element(element2, "video");
                    XmlUtilities.text(element3, "codec", this.value(map, "Encoded_Library/Name", "CodecID/Hint", "Format"));
                    XmlUtilities.text(element3, "aspect", this.value(map, "DisplayAspectRatio/String"));
                    XmlUtilities.text(element3, "width", this.value(map, "Width"));
                    XmlUtilities.text(element3, "height", this.value(map, "Height"));
                    XmlUtilities.text(element3, "hdrtype", this.value(map, "HDR_Format_Commercial", "HDR_Format"));
                    XmlUtilities.text(element3, "framerate", this.value(map, "FrameRate"));
                    XmlUtilities.text(element3, "bitrate", this.value(map, "BitRate"));
                    XmlUtilities.text(element3, "duration", ExpressionFormatMethods.round(Double.parseDouble(this.value(map, "Duration")) / 60000.0, 4));
                }
            }
            if (streamKind == StreamKind.Audio) {
                for (Map map : list) {
                    Element element3 = XmlUtilities.element(element2, "audio");
                    XmlUtilities.text(element3, "codec", this.value(map, "CodecID/Hint", "Format"));
                    XmlUtilities.text(element3, "language", this.value(map, "Language/String3"));
                    XmlUtilities.text(element3, "channels", this.value(map, "Channel(s)_Original", "Channel(s)"));
                    XmlUtilities.text(element3, "bitrate", this.value(map, "BitRate"));
                }
            }
            if (streamKind == StreamKind.Text) {
                for (Map map : list) {
                    Element element3 = XmlUtilities.element(element2, "subtitle");
                    XmlUtilities.text(element3, "codec", this.value(map, "Format"));
                    XmlUtilities.text(element3, "language", this.value(map, "Language/String3"));
                }
            }
        });
        return element;
    }

    private String value(Map<String, String> map, String ... stringArray) {
        return Arrays.stream(stringArray).map(map::get).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private List<File> getMediaFiles(File file) {
        if (MediaCharacteristicsParser.getDefault().canRead()) {
            return FileUtilities.listFiles(file, MediaCharacteristicsParser.mediainfo::acceptVideoFile, FileUtilities.HUMAN_NAME_ORDER);
        }
        return Collections.emptyList();
    }
}

