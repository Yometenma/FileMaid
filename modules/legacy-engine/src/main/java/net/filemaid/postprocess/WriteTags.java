package net.filemaid.postprocess;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.Execute;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.format.ExtendedMetadataMethods;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.media.MediaInfoTable;
import net.filemaid.media.MetaAttributes;
import net.filemaid.mediainfo.StreamKind;
import net.filemaid.postprocess.ApplyMetadata;
import net.filemaid.postprocess.Feedback;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.TemporaryFolder;
import net.filemaid.util.XmlUtilities;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeDetails;
import net.filemaid.web.Link;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieDetails;
import net.filemaid.web.MoviePart;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SeriesDetails;
import net.filemaid.web.SimpleDate;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class WriteTags
implements ApplyMetadata {
    @Override
    public boolean accept(File file, Object object) {
        return Command.forContentType(file) != null;
    }

    @Override
    public void apply(File file, File file2, Movie movie, Feedback feedback) throws Exception {
        Command.forContentType(file2).tag(file2, movie, feedback);
    }

    @Override
    public void apply(File file, File file2, Episode episode, Feedback feedback) throws Exception {
        Command.forContentType(file2).tag(file2, episode, feedback);
    }

    public static enum Command {
        mkvpropedit{

            @Override
            public FileFilter getFileFilter() {
                return MediaTypes.MKV;
            }

            @Override
            public String version() throws Exception {
                return Execute.execute(this.getCommand(), Arrays.asList("--version"), null, null, true).toString();
            }

            @Override
            public void tag(File file3, Object object, Feedback feedback) throws Exception {
                LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
                linkedHashMap.put("--edit", "info");
                linkedHashMap.put("--set", "title=" + FileUtilities.getName(file3));
                this.prepareTags(object).ifPresent(file -> linkedHashMap.put("--tags", "global:" + file));
                this.prepareCoverImage(object).ifPresent(file2 -> {
                    linkedHashMap.put("--attachment-name", "cover.png");
                    linkedHashMap.put("--attachment-mime-type", "image/png");
                    if (this.hasAttachment(file3, "cover.png")) {
                        linkedHashMap.put("--replace-attachment", "name:cover.png:" + file2);
                    } else {
                        linkedHashMap.put("--add-attachment", file2);
                    }
                });
                feedback.info(this.getCommand() + " " + linkedHashMap, file3);
                Execute.system(this.getCommand(), this.arguments("--verbose", file3, linkedHashMap));
            }

            private boolean hasAttachment(File file, String string) {
                try {
                    String string2 = MediaInfoTable.read(file).getString(StreamKind.General, "Attachments");
                    if (string2 != null) {
                        return RegularExpressions.SLASH.splitAsStream(string2).map(String::trim).anyMatch(string::equals);
                    }
                }
                catch (Exception exception) {
                    Logging.debug.warning(Logging.cause("Failed to check attachments", file, exception));
                }
                return false;
            }

            private Element tag(Node node, int n) {
                Element element = XmlUtilities.element(node, "Tag");
                Element element2 = XmlUtilities.element(element, "Targets");
                XmlUtilities.text(element2, "TargetTypeValue", n);
                return element;
            }

            private Element simple(Node node, String string, String string2) {
                if (string2 == null || string2.isEmpty()) {
                    return null;
                }
                Element element = XmlUtilities.element(node, "Simple");
                XmlUtilities.text(element, "Name", string);
                XmlUtilities.text(element, "String", string2);
                return element;
            }

            private Element simple(Node node, String string, Object object) {
                return object == null ? null : this.simple(node, string, this.argument(object));
            }

            private <T> Element simple(Node node, String string, T t, Function<T, String> function) {
                return t == null ? null : this.simple(node, string, function.apply(t));
            }

            private Optional<File> prepareTags(Object object) {
                try {
                    Object object3;
                    Element element;
                    Serializable serializable;
                    Element element2 = XmlUtilities.root("Tags");
                    if (object instanceof Movie) {
                        serializable = (Movie)object;
                        element = this.tag(element2, 50);
                        this.simple((Node)element, "CONTENT_TYPE", "Movie");
                        this.simple((Node)element, "TITLE", ((SearchResult)serializable).getName());
                        object3 = ExtendedMetadataMethods.getInfo((Movie)serializable);
                        if (object3 != null) {
                            this.simple((Node)element, "DATE_RELEASED", ((MovieDetails)object3).getReleased());
                            this.simple((Node)element, "DIRECTOR", ((MovieDetails)object3).getDirector());
                            this.simple((Node)element, "GENRE", ((MovieDetails)object3).getGenres());
                            this.simple((Node)element, "KEYWORDS", ((MovieDetails)object3).getCollection());
                            this.simple((Node)element, "SUMMARY", ((MovieDetails)object3).getTagline());
                            this.simple((Node)element, "SYNOPSIS", ((MovieDetails)object3).getOverview());
                        } else {
                            this.simple((Node)element, "DATE_RELEASED", ((Movie)serializable).getYear());
                        }
                        if (serializable instanceof MoviePart) {
                            MoviePart moviePart = (MoviePart)serializable;
                            this.simple((Node)element, "PART_NUMBER", moviePart.getPartIndex());
                            this.simple((Node)element, "TOTAL_PARTS", moviePart.getPartCount());
                        }
                        this.simple(element, "TMDB", Link.TheMovieDB.getID((Movie)serializable), "movie/"::concat);
                        this.simple((Node)element, "IMDB", Link.IMDb.getID((Movie)serializable));
                        this.simple((Node)element, "XATTR", MetaAttributes.toJson(object, false));
                    }
                    if (object instanceof Episode) {
                        serializable = (Episode)object;
                        element = this.tag(element2, 70);
                        this.simple((Node)element, "CONTENT_TYPE", "TV Show");
                        this.simple((Node)element, "TITLE", ((Episode)serializable).getSeriesName());
                        this.simple((Node)element, "TVDB", Link.TheTVDB.getID(((Episode)serializable).getSeriesInfo()));
                        this.simple(element, "TMDB", Link.TheMovieDB.getID(((Episode)serializable).getSeriesInfo()), "tv/"::concat);
                        Element element3 = this.tag(element2, 60);
                        this.simple(element3, "PART_NUMBER", ((Episode)serializable).getSeason());
                        Element element4 = this.tag(element2, 50);
                        this.simple(element4, "PART_NUMBER", ((Episode)serializable).getEpisode());
                        this.simple(element4, "TITLE", ((Episode)serializable).getTitle());
                        this.simple(element4, "DATE_RELEASED", ((Episode)serializable).getAirdate());
                        this.simple(element4, "GENRE", ((Episode)serializable).getSeriesInfo().getGenres());
                        this.simple(element4, "PUBLISHER", ((Episode)serializable).getSeriesInfo().getNetwork());
                        EpisodeDetails episodeDetails = ExtendedMetadataMethods.getInfo((Episode)serializable);
                        if (episodeDetails != null) {
                            this.simple(element4, "DIRECTOR", episodeDetails.getDirector());
                            this.simple(element4, "SYNOPSIS", episodeDetails.getOverview());
                        }
                        this.simple(element4, "XATTR", MetaAttributes.toJson(object, false));
                    }
                    File file = TemporaryFolder.getFolder(this.name()).createFile("tags", ".xml");
                    XmlUtilities.writeDocument(element2.getOwnerDocument(), file);
                    return Optional.of(file);
                }
                catch (Exception exception) {
                    Logging.debug.warning(Logging.cause("Failed to write tags", object, exception));
                    return Optional.empty();
                }
            }
        }
        ,
        AtomicParsley{

            @Override
            public FileFilter getFileFilter() {
                return MediaTypes.MP4;
            }

            @Override
            public String version() throws Exception {
                return Execute.execute(this.getCommand(), Arrays.asList("-v"), null, null, true).toString();
            }

            @Override
            public void tag(File file2, Object object, Feedback feedback) throws Exception {
                Serializable serializable;
                Serializable serializable2;
                Serializable serializable3;
                ArrayList<String> arrayList = new ArrayList<String>();
                arrayList.add("--overWrite");
                LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
                linkedHashMap.put("--title", FileUtilities.getName(file2));
                if (object instanceof Episode) {
                    serializable3 = (Episode)object;
                    linkedHashMap.put("--stik", "TV Show");
                    serializable2 = ((Episode)serializable3).getAirdate();
                    if (serializable2 != null) {
                        linkedHashMap.put("--year", ((SimpleDate)serializable2).toInstant());
                    }
                    linkedHashMap.put("--TVShowName", ((Episode)serializable3).getSeriesName());
                    linkedHashMap.put("--TVEpisodeNum", ((Episode)serializable3).getEpisode());
                    linkedHashMap.put("--TVSeasonNum", ((Episode)serializable3).getSeason());
                    linkedHashMap.put("--description", ((Episode)serializable3).getTitle());
                    linkedHashMap.put("--genre", ((Episode)serializable3).getSeriesInfo().getGenres());
                    linkedHashMap.put("--TVNetwork", ((Episode)serializable3).getSeriesInfo().getNetwork());
                    serializable = ExtendedMetadataMethods.getInfo((Episode)serializable3);
                    if (serializable != null) {
                        linkedHashMap.put("--artist", ((EpisodeDetails)serializable).getDirector());
                        linkedHashMap.put("--longdesc", ((EpisodeDetails)serializable).getOverview());
                    }
                }
                if (object instanceof Movie) {
                    serializable3 = (Movie)object;
                    linkedHashMap.put("--stik", "Movie");
                    linkedHashMap.put("--year", ((Movie)serializable3).getYear());
                    serializable2 = ExtendedMetadataMethods.getInfo((Movie)serializable3);
                    if (serializable2 != null) {
                        serializable = ((MovieDetails)serializable2).getReleased();
                        if (serializable != null) {
                            linkedHashMap.put("--year", ((SimpleDate)serializable).toInstant());
                        }
                        linkedHashMap.put("--artist", ((MovieDetails)serializable2).getDirector());
                        linkedHashMap.put("--grouping", ((MovieDetails)serializable2).getCollection());
                        linkedHashMap.put("--genre", ((MovieDetails)serializable2).getGenres());
                        linkedHashMap.put("--description", ((MovieDetails)serializable2).getTagline());
                        linkedHashMap.put("--longdesc", ((MovieDetails)serializable2).getOverview());
                    }
                    if (object instanceof MoviePart) {
                        serializable = (MoviePart)object;
                        linkedHashMap.put("--disk", ((MoviePart)serializable).getPartIndex() + "/" + ((MoviePart)serializable).getPartCount());
                    }
                }
                CachedMediaCharacteristics.applyMediaCharacteristics(file2, mediaCharacteristics -> linkedHashMap.put("--hdvideo", mediaCharacteristics.getHeight() >= 1000));
                this.prepareCoverImage(object).ifPresent(file -> {
                    linkedHashMap.put("--artwork", file);
                    arrayList.add("--artwork");
                    arrayList.add("REMOVE_ALL");
                });
                feedback.info(this.getCommand() + " " + arrayList + " " + linkedHashMap, file2);
                Execute.system(this.getCommand(), this.arguments(file2, arrayList, linkedHashMap));
            }
        };


        public abstract FileFilter getFileFilter();

        public abstract String version() throws Exception;

        public abstract void tag(File var1, Object var2, Feedback var3) throws Exception;

        protected URL getCoverImage(Object object) throws Exception {
            Serializable serializable;
            Serializable serializable2;
            if (object instanceof Movie && (serializable2 = ExtendedMetadataMethods.getInfo((Movie)(serializable = (Movie)object))) != null) {
                return ((MovieDetails)serializable2).getPoster();
            }
            if (object instanceof Episode && (serializable2 = ExtendedMetadataMethods.getDetails(((Episode)(serializable = (Episode)object)).getSeriesInfo())) != null) {
                return ((SeriesDetails)serializable2).getPoster();
            }
            return null;
        }

        protected Optional<File> prepareCoverImage(Object object) {
            try {
                byte[] byArray;
                BufferedImage bufferedImage;
                URL uRL = this.getCoverImage(object);
                if (uRL != null && (bufferedImage = ImageIO.read(new MemoryCacheImageInputStream(new ByteArrayInputStream(byArray = Cache.getConcurrentCache("url", CacheType.Monthly).url(uRL).get())))) != null) {
                    File file = TemporaryFolder.getFolder(this.name()).createFile("cover", ".png");
                    ImageIO.write((RenderedImage)bufferedImage, "png", file);
                    return Optional.of(file);
                }
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("Failed to fetch cover image", object, exception));
            }
            return Optional.empty();
        }

        public String getCommand() {
            return System.getProperty("net.filemaid.postprocess." + this.name(), this.name());
        }

        protected String[] arguments(Object ... objectArray) {
            return (String[])Stream.of(objectArray).flatMap(object -> {
                if (object instanceof Map) {
                    return ((Map<?, ?>)object).entrySet().stream().flatMap(entry -> entry.getValue() == null ? Stream.empty() : Stream.of(entry.getKey(), entry.getValue()));
                }
                if (object instanceof List) {
                    return ((List)object).stream();
                }
                return Stream.of(object);
            }).map(this::argument).toArray(String[]::new);
        }

        protected String argument(Object object) {
            if (object instanceof Collection) {
                return ((Collection<?>)object).stream().filter(Objects::nonNull).map(Objects::toString).filter(string -> !string.isEmpty()).collect(Collectors.joining(";"));
            }
            return object.toString();
        }

        public static Command forContentType(File file) {
            for (Command command : Command.values()) {
                if (!command.getFileFilter().accept(file)) continue;
                return command;
            }
            return null;
        }

        public static Stream<File> executables() {
            return Stream.of(Command.values()).map(Command::getCommand).map(File::new);
        }
    }
}

