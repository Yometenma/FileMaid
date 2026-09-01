package net.filemaid.format;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.ApplicationFolder;
import net.filemaid.HistorySpooler;
import net.filemaid.Language;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.MemoryCache;
import net.filemaid.Resource;
import net.filemaid.Settings;
import net.filemaid.WebServices;
import net.filemaid.format.AssociativeEnumObject;
import net.filemaid.format.AssociativeScriptObject;
import net.filemaid.format.AutoUnitDecimal;
import net.filemaid.format.BindingException;
import net.filemaid.format.BitRate;
import net.filemaid.format.ChannelCount;
import net.filemaid.format.Define;
import net.filemaid.format.DynamicBindings;
import net.filemaid.format.ExpressionBindings;
import net.filemaid.format.ExpressionFormatMethods;
import net.filemaid.format.ExtendedMetadataMethods;
import net.filemaid.format.FileSize;
import net.filemaid.format.FrameRate;
import net.filemaid.format.PropertyBindings;
import net.filemaid.format.Resolution;
import net.filemaid.format.StructuredFile;
import net.filemaid.hash.HashType;
import net.filemaid.hash.VerificationUtilities;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.media.ImageMetadata;
import net.filemaid.media.LocalDatasource;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.media.MediaInfoTable;
import net.filemaid.media.MetaAttributes;
import net.filemaid.media.NamingStandard;
import net.filemaid.media.VideoFormat;
import net.filemaid.media.XattrChecksum;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.mediainfo.MediaInfoException;
import net.filemaid.mediainfo.StreamKind;
import net.filemaid.similarity.Normalization;
import net.filemaid.subtitle.SubtitleUtilities;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.StringUtilities;
import net.filemaid.vfs.VFS;
import net.filemaid.web.AnimeLists;
import net.filemaid.web.AudioTrack;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeFormat;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.Link;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieDetails;
import net.filemaid.web.MoviePart;
import net.filemaid.web.SeriesDetails;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.SortOrder;
import net.filemaid.web.XDB;
import net.filemaid.web.XEM;

public class MediaBindingBean {
    private final Object infoObject;
    private final File mediaFile;
    private final Map<File, ?> context;
    private final Resource<SeriesDetails> seriesDetails = Resource.lazy(() -> ExtendedMetadataMethods.getDetails(this.getSeriesInfo()));
    private final Resource<MovieDetails> movieDetails = Resource.lazy(() -> ExtendedMetadataMethods.getInfo(this.getMovie()));
    private final Resource<File> inferredMediaFile = Resource.lazy(() -> this.getInferredMediaFile(this.getMediaFile()));
    private final Resource<MediaInfoTable> mediaInfo = Resource.lazy(() -> {
        try {
            return MediaInfoTable.read(this.inferredMediaFile.get());
        }
        catch (BindingException bindingException) {
            throw bindingException;
        }
        catch (Exception exception) {
            throw new MediaInfoException(exception.getMessage(), exception);
        }
    });
    private final Resource<String[]> mediaTitles = Resource.lazy(() -> {
        String string = this.getVideoCharacteristics().map(MediaCharacteristics::getTitle).orElse(null);
        String[] stringArray = this.getFileNames(this.inferredMediaFile.get());
        if (string != null && !string.isEmpty()) {
            return (String[])Stream.concat(Stream.of(string), Arrays.stream(stringArray)).distinct().toArray(String[]::new);
        }
        return stringArray;
    });
    private static final MemoryCache<Object, List<AssociativeScriptObject>> modelCache = MemoryCache.weak();

    public MediaBindingBean(Object object, File file) {
        this(object, file, Collections.singletonMap(file, object));
    }

    public MediaBindingBean(Object object, File file, Map<File, ?> map) {
        this.infoObject = object;
        this.mediaFile = file;
        this.context = map;
    }

    @Define(value={"object"})
    public Object getInfoObject() {
        return this.infoObject;
    }

    @Define(value={"file"})
    public File getFileObject() {
        return this.mediaFile;
    }

    @Define(value={""})
    public <T> T undefined(String string) {
        throw new BindingException((Object)string, (Object)"undefined", BindingException.Flag.UNDEFINED);
    }

    @Define(value={"n"})
    public String getName() {
        if (this.infoObject instanceof Episode) {
            return this.getEpisode().getSeriesName();
        }
        if (this.infoObject instanceof Movie) {
            return this.getMovie().getName();
        }
        if (this.infoObject instanceof AudioTrack) {
            return this.getAlbumArtist() != null ? this.getAlbumArtist() : this.getArtist();
        }
        if (this.infoObject instanceof File) {
            return FileUtilities.getName((File)this.infoObject);
        }
        return null;
    }

    @Define(value={"y"})
    public Integer getYear() throws Exception {
        if (this.infoObject instanceof Episode) {
            return this.getStartDate().getYear();
        }
        if (this.infoObject instanceof Movie) {
            return this.getMovie().getYear();
        }
        if (this.infoObject instanceof AudioTrack) {
            return this.getReleaseDate().getYear();
        }
        if (this.infoObject instanceof LocalDatasource.PhotoFile) {
            return this.getPhoto().getDateTaken().map(ZonedDateTime::getYear).orElse(null);
        }
        if (this.infoObject instanceof File) {
            return this.getReleaseDate().getYear();
        }
        return null;
    }

    @Define(value={"ny"})
    public String getNameWithYear() {
        String string = this.getName();
        if (NamingStandard.isNameYear(string)) {
            return string;
        }
        try {
            return string + " (" + this.getYear() + ")";
        }
        catch (Exception exception) {
            Logging.debug.finest(Logging.cause(exception));
            return string;
        }
    }

    @Define(value={"s"})
    public Integer getSeasonNumber() throws Exception {
        return this.getEpisode().getSeason();
    }

    @Define(value={"sn"})
    public String getSeasonName() {
        return this.getEpisode().getGroup();
    }

    @Define(value={"e"})
    public Integer getEpisodeNumber() {
        return this.getEpisode().getEpisode();
    }

    @Define(value={"es"})
    public List<Integer> getEpisodeNumbers() {
        return this.getEpisodes().stream().map(episode -> episode.getEpisode() == null ? (episode.getSpecial() == null ? null : episode.getSpecial()) : episode.getEpisode()).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Define(value={"s00"})
    public String getS00() {
        return EpisodeFormat.DEFAULT.formatMultiNumbers(this.getEpisodes(), "%02d", "", "");
    }

    @Define(value={"e00"})
    public String getE00() {
        return EpisodeFormat.DEFAULT.formatMultiNumbers(this.getEpisodes(), "", "%02d", "x");
    }

    @Define(value={"sxe"})
    public String getSxE() throws Exception {
        return EpisodeFormat.DEFAULT.formatSxE(this.getEpisode());
    }

    @Define(value={"s00e00"})
    public String getS00E00() throws Exception {
        return EpisodeFormat.DEFAULT.formatS00E00(this.getEpisode());
    }

    @Define(value={"t"})
    public String getTitle() {
        String string = null;
        if (this.infoObject instanceof Episode) {
            string = this.getEpisode().getTitle();
        } else if (this.infoObject instanceof Movie) {
            string = this.getMovieDetails().getTagline();
        } else if (this.infoObject instanceof AudioTrack) {
            string = this.getMusic().getTrackTitle() != null ? this.getMusic().getTrackTitle() : this.getMusic().getTitle();
        }
        return Normalization.truncateText(Normalization.replaceSlash(string, " - ", " ", ".", ""), 150);
    }

    @Define(value={"d"})
    public SimpleDate getReleaseDate() throws Exception {
        if (this.infoObject instanceof Episode) {
            return this.getEpisode().getAirdate();
        }
        if (this.infoObject instanceof Movie) {
            return this.getMovieDetails().getReleased();
        }
        if (this.infoObject instanceof AudioTrack) {
            return this.getMusic().getAlbumReleaseDate();
        }
        if (this.infoObject instanceof LocalDatasource.PhotoFile) {
            return this.getPhoto().getDateTaken().map(SimpleDate::from).orElse(null);
        }
        if (this.infoObject instanceof File) {
            return SimpleDate.from(this.getFileCreationTime());
        }
        return null;
    }

    @Define(value={"dt"})
    public ZonedDateTime getMediaCreationTime() throws Exception {
        if (MediaTypes.VIDEO_FILES.accept(this.getMediaFile())) {
            return this.getVideoCharacteristics().map(MediaCharacteristics::getCreationTime).map(instant -> instant.atZone(ZoneOffset.UTC)).orElse(null);
        }
        if (MediaTypes.IMAGE_FILES.accept(this.getMediaFile()) || ImageMetadata.SUPPORTED_FILE_TYPES.accept(this.getMediaFile())) {
            return this.getPhoto().getDateTaken().orElse(null);
        }
        return this.getFileCreationTime();
    }

    @Define(value={"ct"})
    public ZonedDateTime getFileCreationTime() throws Exception {
        return FileUtilities.getCreationDate(this.getMediaFile()).atZone(ZoneId.systemDefault());
    }

    @Define(value={"airdate"})
    public SimpleDate getAirdate() {
        return this.getEpisode().getAirdate();
    }

    @Define(value={"age"})
    public Long getAgeInDays() throws Exception {
        long l;
        SimpleDate simpleDate = this.getReleaseDate();
        if (simpleDate != null && (l = ChronoUnit.DAYS.between(simpleDate.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant(), Instant.now())) >= 0L) {
            return l;
        }
        return null;
    }

    @Define(value={"startdate"})
    public SimpleDate getStartDate() throws Exception {
        SimpleDate simpleDate = this.getSeriesInfo().getStartDate();
        if (simpleDate != null) {
            return simpleDate;
        }
        return EpisodeUtilities.fetchEpisodeList(this.getEpisode()).stream().filter(episode -> EpisodeUtilities.isRegularEpisode(episode)).map(Episode::getAirdate).filter(Objects::nonNull).min(SimpleDate::compareTo).get();
    }

    @Define(value={"absolute"})
    public Integer getAbsoluteEpisodeNumber() {
        return this.getEpisode().getAbsolute();
    }

    @Define(value={"special"})
    public Integer getSpecialNumber() {
        return this.getEpisode().getSpecial();
    }

    @Define(value={"series"})
    public SeriesInfo getSeriesInfo() {
        return this.getEpisode().getSeriesInfo();
    }

    @Define(value={"alias"})
    public List<String> getAliasNames() {
        if (this.infoObject instanceof Movie) {
            return Arrays.asList(this.getMovie().getAliasNames());
        }
        if (this.infoObject instanceof Episode) {
            return this.getSeriesInfo().getAliasNames();
        }
        return null;
    }

    @Define(value={"primaryTitle"})
    public String getPrimaryTitle() {
        if (this.infoObject instanceof Movie) {
            return this.getMovieDetails().getOriginalName();
        }
        if (this.infoObject instanceof Episode) {
            String string = EpisodeUtilities.mapRomajiPrimaryTitle(this.getEpisode());
            if (string != null) {
                return ExpressionFormatMethods.replaceTrailingBrackets(string);
            }
            SeriesDetails seriesDetails = this.getSeriesDetails();
            if (seriesDetails.getOriginalName() != null) {
                return seriesDetails.getOriginalName();
            }
            return seriesDetails.getName();
        }
        return null;
    }

    @Define(value={"id"})
    public Object getId() throws Exception {
        if (this.infoObject instanceof Episode) {
            return this.getSeriesInfo().getId();
        }
        if (this.infoObject instanceof Movie) {
            return this.getMovie().getId();
        }
        if (this.infoObject instanceof AudioTrack) {
            return this.getMusic().getMBID();
        }
        return null;
    }

    @Define(value={"tmdbid"})
    public Integer getTmdbId() throws Exception {
        if (this.infoObject instanceof Movie) {
            if (this.getMovie().getTmdbId() > 0) {
                return this.getMovie().getTmdbId();
            }
            if (this.getMovie().getImdbId() > 0) {
                return this.getMovieDetails().getId();
            }
        }
        if (this.infoObject instanceof Episode) {
            if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, this.getSeriesInfo())) {
                return this.getSeriesInfo().getId();
            }
            return EpisodeUtilities.lookupExternalSeries(this.getSeriesInfo()).getExternalId(XDB.TheMovieDB);
        }
        return null;
    }

    @Define(value={"tvdbid"})
    public Integer getTvdbId() throws Exception {
        if (this.infoObject instanceof Episode) {
            if (EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, this.getSeriesInfo())) {
                return this.getSeriesInfo().getId();
            }
            return EpisodeUtilities.lookupExternalSeries(this.getSeriesInfo()).getExternalId(XDB.TheTVDB);
        }
        return null;
    }

    @Define(value={"imdbid"})
    public String getImdbId() throws Exception {
        if (this.infoObject instanceof Movie) {
            if (this.getMovie().getImdbId() > 0) {
                return Link.IMDb.getID(this.getMovie());
            }
            if (this.getMovie().getTmdbId() > 0) {
                return Link.IMDb.getID(this.getMovieDetails());
            }
        }
        if (this.infoObject instanceof Episode) {
            Map<String, String> map = ExtendedMetadataMethods.getExternalIds(this.getSeriesInfo());
            if (map != null) {
                for (String string : map.values()) {
                    Integer n = Link.IMDb.parseID(string);
                    if (n == null) continue;
                    return Link.IMDb.getID(n);
                }
            }
            return Link.IMDb.getID(this.getSeriesDetails().getImdbId());
        }
        return null;
    }

    @Define(value={"vcf"})
    public String getVideoCompressionFormat() {
        return this.getMediaInfo(StreamKind.Video, "Format");
    }

    @Define(value={"vc"})
    public String getVideoCodec() {
        String string = this.getMediaInfo(StreamKind.Video, "Encoded_Library_Name", "Encoded_Library/Name", "CodecID/Hint", "Format");
        return StringUtilities.tokenize(string).findFirst().orElse(null);
    }

    @Define(value={"ac"})
    public String getAudioCodec() {
        String string2 = this.getMediaInfo(StreamKind.Audio, "CodecID/Hint", "Format/String", "Format");
        return StringUtilities.tokenize(string2).findFirst().map(string -> Normalization.normalizePunctuation(string, "", "")).orElse(null);
    }

    @Define(value={"cf"})
    public String getContainerFormat() {
        String string = this.getMediaInfo(StreamKind.General, "Format/Extensions", "Codec/Extensions", "Format");
        return StringUtilities.tokenize(string).map(String::toLowerCase).findFirst().get();
    }

    @Define(value={"vf"})
    public String getVideoFormat() {
        int n = Integer.parseInt(this.getMediaInfo(StreamKind.Video, "Width"));
        int n2 = Integer.parseInt(this.getMediaInfo(StreamKind.Video, "Height"));
        return VideoFormat.DEFAULT_GROUPS.guessFormat(n, n2) + "p";
    }

    @Define(value={"vk"})
    public String getVideoFormatK() {
        int n = Integer.parseInt(this.getMediaInfo(StreamKind.Video, "Width"));
        int n2 = (int)Math.floor((float)n / 800.0f / 4.0f) * 4;
        return n2 >= 4 ? n2 + "K" : null;
    }

    @Define(value={"hpi"})
    public String getExactVideoFormat() {
        String string = this.getMediaInfo(StreamKind.Video, "Height");
        String string2 = this.getMediaInfo(StreamKind.Video, "ScanType");
        String string3 = string2.codePoints().map(Character::toLowerCase).mapToObj(Character::toChars).map(String::valueOf).findFirst().orElse("p");
        return string + string3;
    }

    @Define(value={"af"})
    public ChannelCount getAudioChannels() {
        return this.getMediaInfo().find(StreamKind.Audio, "Channel(s)_Original", "Channel(s)").map(string2 -> StringUtilities.tokenize(string2, RegularExpressions.SLASH).map(string -> StringUtilities.matchInteger(string)).filter(Objects::nonNull).min(Integer::compare).orElse(null)).filter(Objects::nonNull).max(Integer::compare).map(ChannelCount::count).orElse(null);
    }

    @Define(value={"channels"})
    public ChannelCount getAudioChannelLayout() {
        return this.getMediaInfo().find(StreamKind.Audio, "ChannelLayout_Original", "ChannelLayout").map(string2 -> {
            if (string2.equals("Object Based")) {
                return null;
            }
            return StringUtilities.tokenize(string2, RegularExpressions.SPACE).map(string -> !string.contains("LFE") ? "1" : "0.1").map(BigDecimal::new).reduce(BigDecimal::add).orElse(null);
        }).filter(Objects::nonNull).max(BigDecimal::compareTo).map(ChannelCount::layout).orElseGet(() -> Optional.ofNullable(this.getAudioChannels()).map(AutoUnitDecimal::getValue).map(ChannelCount::layout).orElse(null));
    }

    @Define(value={"acf"})
    public String getAudioChannelFormatTag() {
        String string = this.getMediaInfo(StreamKind.Audio, "Format_Commercial");
        ChannelCount channelCount = this.getAudioChannelLayout();
        if (string == null || channelCount == null) {
            return null;
        }
        return string.replaceAll("\\P{Upper}", "") + channelCount;
    }

    @Define(value={"aco"})
    public String getAudioChannelObjects() {
        return this.getMediaInfo().stream(StreamKind.Audio, "Codec_Profile", "Format_Profile", "Format_Commercial").map(string2 -> RegularExpressions.SLASH.splitAsStream((CharSequence)string2).map(String::trim).filter(string -> !string.isEmpty() && !string.contains("Disc")).findFirst().orElse(null)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Define(value={"resolution"})
    public Resolution getVideoResolution() {
        return Stream.of(StreamKind.Video, StreamKind.Image).map(streamKind -> {
            String string = this.getMediaInfo().getString((StreamKind)((Object)streamKind), "Width");
            String string2 = this.getMediaInfo().getString((StreamKind)((Object)streamKind), "Height");
            return Resolution.parse(string, string2);
        }).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Define(value={"bitdepth"})
    public Integer getBitDepth() {
        return Stream.of(StreamKind.Video, StreamKind.Audio, StreamKind.Image).map(streamKind -> StringUtilities.matchInteger(this.getMediaInfo().getString((StreamKind)((Object)streamKind), "BitDepth"))).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Define(value={"hdr"})
    public String getHighDynamicRange() {
        return this.getMediaInfo().find(StreamKind.Video, "HDR_Format", "HDR_Format_Commercial").flatMap(RegularExpressions.SLASH::splitAsStream).filter(string -> string.contains("HDR") || string.contains("Dolby Vision")).findFirst().map(string2 -> {
            if (string2.contains("Dolby Vision")) {
                if (this.getMediaInfo().find(StreamKind.Video, "HDR_Format_Compatibility").flatMap(RegularExpressions.SLASH::splitAsStream).anyMatch(string -> string.contains("HDR10"))) {
                    return "DV+HDR10";
                }
                return "DV";
            }
            return string2;
        }).orElseGet(() -> this.getBitDepth() >= 10 && this.getMediaInfo().stream(StreamKind.Video, "colour_primaries").anyMatch(string -> string.contains("BT.2020")) ? "HDR" : null);
    }

    @Define(value={"dovi"})
    public String getDolbyVision() {
        return this.getMediaInfo().find(StreamKind.Video, "HDR_Format", "HDR_Format_Commercial").flatMap(RegularExpressions.SLASH::splitAsStream).filter(string -> string.contains("Dolby Vision")).findFirst().orElse(null);
    }

    @Define(value={"ar"})
    public String getAspectRatio() {
        String string = this.getMediaInfo(StreamKind.Video, "DisplayAspectRatio/String");
        return ExpressionFormatMethods.colon(string, "\u2236");
    }

    @Define(value={"ws"})
    public String getWidescreen() {
        float f = Float.parseFloat(this.getMediaInfo(StreamKind.Video, "DisplayAspectRatio"));
        return f > 1.37f ? "WS" : null;
    }

    @Define(value={"hd"})
    public String getVideoDefinitionCategory() {
        Resolution resolution = this.getVideoResolution();
        int n = resolution.getWidth();
        int n2 = resolution.getHeight();
        if (n > 2560 || n2 > 1440) {
            return "UHD";
        }
        if (n > 1920 || n2 > 1080) {
            return "QHD";
        }
        if (n > 1280 || n2 > 720) {
            return "FHD";
        }
        if (n > 1024 || n2 > 576) {
            return "HD";
        }
        return "SD";
    }

    @Define(value={"width"})
    public Integer getWidth() {
        return this.getVideoResolution().getWidth();
    }

    @Define(value={"height"})
    public Integer getHeight() {
        return this.getVideoResolution().getHeight();
    }

    @Define(value={"original"})
    public String getOriginalFileName() {
        String string = XattrMetaInfo.xattr.getOriginalName(this.getMediaFile());
        if (string != null) {
            return FileUtilities.getNameWithoutExtension(string);
        }
        File file = HistorySpooler.HISTORY.getOriginalPath(this.getMediaFile());
        if (file != null) {
            return FileUtilities.getName(file);
        }
        return null;
    }

    @Define(value={"xattr"})
    public Object getMetaAttributesObject() throws Exception {
        return XattrMetaInfo.xattr.getMetaInfo(this.getMediaFile());
    }

    @Define(value={"crc32"})
    public String getCRC32() throws Exception {
        Optional<String> optional = Arrays.stream(this.getFileNames(this.inferredMediaFile.get())).map(Normalization::getEmbeddedChecksum).filter(Objects::nonNull).findFirst();
        if (optional.isPresent()) {
            return optional.get();
        }
        String string = VerificationUtilities.getHashFromVerificationFile(this.inferredMediaFile.get(), HashType.SFV, 3);
        if (string != null) {
            return string;
        }
        return XattrChecksum.CRC32.computeIfAbsent(this.inferredMediaFile.get());
    }

    @Define(value={"fn"})
    public String getFileName() {
        return FileUtilities.getName(this.getMediaFile());
    }

    @Define(value={"ext"})
    public String getExtension() {
        return FileUtilities.getExtension(this.getMediaFile());
    }

    @Define(value={"edition"})
    public String getVideoEdition() throws Exception {
        String string = StringUtilities.matchLastOccurrence(this.getFileName(), Pattern.compile("(?<=[{]edition[-])[^{}]+(?=[}])"));
        if (string != null) {
            return string;
        }
        return MediaDetection.releaseInfo.getVideoTags(this.mediaTitles.get()).stream().findFirst().orElse(null);
    }

    @Define(value={"tags"})
    public List<String> getVideoTags() throws Exception {
        Pattern[] patternArray = new Pattern[]{this.getKeywordExcludePattern()};
        String[] stringArray = (String[])Arrays.stream(this.mediaTitles.get()).map(string -> MediaDetection.releaseInfo.clean((String)string, patternArray)).filter(string -> !string.isEmpty()).toArray(String[]::new);
        return MediaDetection.releaseInfo.getVideoTags(stringArray);
    }

    @Define(value={"vs"})
    public String getVideoSource() throws Exception {
        String string = MediaDetection.releaseInfo.getVideoSource(this.mediaTitles.get()).keySet().stream().findFirst().orElse(null);
        if (string != null) {
            return string;
        }
        return MediaDetection.releaseInfo.getVideoSource(this.getMediaInfo(StreamKind.Video, "OriginalSourceMedium")).keySet().stream().findFirst().orElse(null);
    }

    @Define(value={"source"})
    public String getVideoSourceMatch() throws Exception {
        String string = MediaDetection.releaseInfo.getVideoSource(this.mediaTitles.get()).values().stream().findFirst().orElse(null);
        if (string != null) {
            return string;
        }
        return MediaDetection.releaseInfo.getVideoSource(this.getMediaInfo(StreamKind.Video, "OriginalSourceMedium")).values().stream().findFirst().orElse(null);
    }

    @Define(value={"s3d"})
    public String getStereoscopic3D() throws Exception {
        return MediaDetection.releaseInfo.getStereoscopic3D(this.mediaTitles.get());
    }

    @Define(value={"group"})
    public String getReleaseGroup() throws Exception {
        Pattern[] patternArray = new Pattern[]{this.getKeywordExcludePattern(), MediaDetection.releaseInfo.getVideoSourcePattern(), MediaDetection.releaseInfo.getVideoFormatPattern(true), MediaDetection.releaseInfo.getResolutionPattern()};
        String[] stringArray = (String[])Arrays.stream(this.mediaTitles.get()).map(string -> MediaDetection.releaseInfo.clean((String)string, patternArray)).filter(string -> !string.isEmpty()).toArray(String[]::new);
        return MediaDetection.releaseInfo.getReleaseGroup(stringArray);
    }

    @Define(value={"subt"})
    public String getSubtitleTags() throws Exception {
        Language language = this.getLanguageTag();
        String string = MediaDetection.releaseInfo.getSubtitleCategoryTag(this.getFileNames(this.getMediaFile()));
        if (language != null && string != null) {
            return "." + language.getISO3B() + "." + string;
        }
        if (language != null) {
            return "." + language.getISO3B();
        }
        if (string != null) {
            return "." + string;
        }
        return null;
    }

    @Define(value={"lang"})
    public Language getLanguageTag() throws Exception {
        File file = this.getMediaFile();
        if (!MediaTypes.SUBTITLE_FILES.accept(file)) {
            throw new Exception("Subtitle language tag is only defined for subtitle files");
        }
        CharSequence[] charSequenceArray = this.getFileNames(file);
        Locale locale = MediaDetection.releaseInfo.getSubtitleLanguageTag(charSequenceArray);
        if (locale != null) {
            return Language.getLanguage(locale);
        }
        Language language = Arrays.stream(charSequenceArray).map(string -> StringUtilities.afterLast(string.toString(), '.').orElse(string.toString())).map(Language::findLanguage).filter(Objects::nonNull).findFirst().orElse(null);
        if (language != null) {
            return language;
        }
        return SubtitleUtilities.detectSubtitleLanguage(file);
    }

    @Define(value={"language"})
    public Language getOriginalLanguage() {
        if (this.infoObject instanceof Movie) {
            return Language.forName(this.getMovieDetails().getOriginalLanguage());
        }
        if (this.infoObject instanceof Episode) {
            return Language.forName(this.getSeriesDetails().getOriginalLanguage());
        }
        return null;
    }

    @Define(value={"languages"})
    public List<Language> getSpokenLanguages() {
        if (this.infoObject instanceof Movie) {
            List<String> list = this.getMovieDetails().getSpokenLanguages();
            return list.stream().map(Language::findLanguage).filter(Objects::nonNull).collect(Collectors.toList());
        }
        if (this.infoObject instanceof Episode) {
            List<String> list = this.getSeriesInfo().getSpokenLanguages();
            return list.stream().map(Language::findLanguage).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return null;
    }

    @Define(value={"country"})
    public String getOriginCountry() throws Exception {
        if (this.infoObject instanceof Movie) {
            return this.getMovieDetails().getProductionCountries().stream().findFirst().orElse(null);
        }
        if (this.infoObject instanceof Episode) {
            return this.getSeriesDetails().getCountry().stream().findFirst().orElse(null);
        }
        return null;
    }

    @Define(value={"runtime"})
    public Integer getRuntime() throws Exception {
        if (this.infoObject instanceof Movie) {
            return this.getMovieDetails().getRuntime();
        }
        if (this.infoObject instanceof Episode) {
            return this.getEpisode().getRuntime() != null ? this.getEpisode().getRuntime() : this.getSeriesInfo().getRuntime();
        }
        return null;
    }

    @Define(value={"actors"})
    public List<String> getActors() throws Exception {
        if (this.infoObject instanceof Movie) {
            return this.getMovieDetails().getActors();
        }
        if (this.infoObject instanceof Episode) {
            return ExtendedMetadataMethods.getActors(this.getSeriesInfo());
        }
        return null;
    }

    @Define(value={"genres"})
    public List<String> getGenres() {
        if (this.infoObject instanceof Movie) {
            return this.getMovieDetails().getGenres();
        }
        if (this.infoObject instanceof Episode) {
            return this.getSeriesInfo().getGenres();
        }
        if (this.infoObject instanceof AudioTrack) {
            return Stream.of(this.getMusic().getGenre()).filter(Objects::nonNull).flatMap(RegularExpressions.SEMICOLON::splitAsStream).map(String::trim).filter(string -> !string.isEmpty()).collect(Collectors.toList());
        }
        return null;
    }

    @Define(value={"genre"})
    public String getPrimaryGenre() {
        List<String> list = this.getGenres();
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    @Define(value={"director"})
    public String getDirector() throws Exception {
        if (this.infoObject instanceof Movie) {
            return this.getMovieDetails().getDirector();
        }
        if (this.infoObject instanceof Episode) {
            return ExtendedMetadataMethods.getInfo(this.getEpisode()).getDirector();
        }
        return null;
    }

    @Define(value={"certification"})
    public String getCertification() {
        if (this.infoObject instanceof Movie) {
            return this.getMovieDetails().getCertification();
        }
        if (this.infoObject instanceof Episode) {
            return this.getSeriesInfo().getCertification();
        }
        return null;
    }

    @Define(value={"rating"})
    public Number getRating() {
        Double d = null;
        if (this.infoObject instanceof Movie) {
            d = this.getMovieDetails().getRating();
        } else if (this.infoObject instanceof Episode) {
            d = this.getSeriesInfo().getRating();
        }
        return d == null ? (Number)null : (Number)ExpressionFormatMethods.round(d, 1);
    }

    @Define(value={"votes"})
    public Integer getVotes() {
        if (this.infoObject instanceof Movie) {
            return this.getMovieDetails().getVotes();
        }
        if (this.infoObject instanceof Episode) {
            return this.getSeriesInfo().getRatingCount();
        }
        return null;
    }

    @Define(value={"collection"})
    public String getCollection() {
        if (this.infoObject instanceof Movie) {
            return this.getMovieDetails().getCollection();
        }
        return null;
    }

    @Define(value={"ci"})
    public Integer getCollectionIndex() throws Exception {
        return ExtendedMetadataMethods.getCollection(this.getMovie()).indexOf(this.getMovie()) + 1;
    }

    @Define(value={"cy"})
    public List<Integer> getCollectionYears() throws Exception {
        List<Integer> list = ExtendedMetadataMethods.getCollection(this.getMovie()).stream().map(Movie::getYear).sorted().distinct().collect(Collectors.toList());
        if (list.size() > 1) {
            return Arrays.asList(list.get(0), list.get(list.size() - 1));
        }
        return list;
    }

    @Define(value={"info"})
    public AssociativeScriptObject getMetaInfo() throws Exception {
        if (this.infoObject instanceof Movie) {
            return this.createPropertyBindings(this.getMovieDetails());
        }
        if (this.infoObject instanceof Episode) {
            return this.createPropertyBindings(this.getSeriesDetails());
        }
        return null;
    }

    @Define(value={"omdb"})
    public AssociativeScriptObject getOmdbApiInfo() throws Exception {
        Integer n = Link.IMDb.parseID(this.getImdbId());
        if (n != null) {
            return this.createPropertyBindings(WebServices.OMDb.getMovieInfo(Movie.IMDB(n)));
        }
        return null;
    }

    @Define(value={"order"})
    public DynamicBindings getSortOrderObject() {
        return new DynamicBindings(SortOrder::names, string -> {
            if (this.infoObject instanceof Episode) {
                SortOrder sortOrder = SortOrder.forName(string);
                Episode episode = EpisodeUtilities.reorderEpisode(this.getEpisode(), sortOrder);
                return this.createBindings(episode, null);
            }
            return this.undefined((String)string);
        });
    }

    @Define(value={"localize"})
    public DynamicBindings getLocalizedInfoObject() {
        return new DynamicBindings(Language::availableLanguages, string -> {
            Locale locale;
            Language language = Language.findLanguage(string);
            Locale locale2 = locale = language != null ? language.getLocale() : Locale.forLanguageTag(string);
            if (locale.getLanguage().length() == 2 && locale.getCountry().length() == 2) {
                if (this.infoObject instanceof Movie) {
                    Movie movie = WebServices.TheMovieDB.getMovieDescriptor(this.getMovie(), locale);
                    return this.createBindings(movie, null);
                }
                if (this.infoObject instanceof Episode) {
                    Episode episode = EpisodeUtilities.fetchEpisode(this.getEpisode(), null, locale);
                    return this.createBindings(episode, null);
                }
            }
            return this.undefined((String)string);
        });
    }

    @Define(value={"db"})
    public DynamicBindings getDatabaseMapper() {
        SeriesInfo seriesInfo = this.getSeriesInfo();
        if (EpisodeUtilities.isInstance((Datasource)WebServices.TheMovieDB_TV, seriesInfo) || EpisodeUtilities.isInstance((Datasource)WebServices.AniDB, seriesInfo) || EpisodeUtilities.isInstance((Datasource)WebServices.TheTVDB, seriesInfo)) {
            return new DynamicBindings(AnimeLists.DB::names, string -> {
                AnimeLists.DB dB;
                Episode episode = this.getEpisode();
                AnimeLists.DB dB2 = AnimeLists.DB.get(episode);
                if (dB2 == (dB = AnimeLists.DB.get(string))) {
                    return this.createBindings(episode, null);
                }
                if (dB2 == AnimeLists.DB.TheMovieDB && dB == AnimeLists.DB.TheTVDB) {
                    return this.createBindings(XDB.map(XDB.TheMovieDB, episode, XDB.TheTVDB, WebServices.TheTVDB), null);
                }
                if (dB2 == AnimeLists.DB.TheTVDB && dB == AnimeLists.DB.TheMovieDB) {
                    return this.createBindings(XDB.map(XDB.TheTVDB, episode, XDB.TheMovieDB, WebServices.TheMovieDB_TV), null);
                }
                Episode episode2 = WebServices.AnimeList.map(episode, dB2, dB).orElse(null);
                if (episode2 == null) {
                    throw new Exception("AniDB mapping not found");
                }
                return this.createBindings(EpisodeUtilities.hydrateEpisode(episode2, seriesInfo.getLanguage()), null);
            });
        }
        return null;
    }

    @Define(value={"historic"})
    public AssociativeScriptObject getHistoricBindings() throws Exception {
        File file = HistorySpooler.HISTORY.getOriginalPath(this.getMediaFile());
        return file == null ? null : this.createBindings(null, file);
    }

    @Define(value={"az"})
    public String getSortInitial() {
        try {
            return ExpressionFormatMethods.sortInitial(this.getCollection());
        }
        catch (Exception exception) {
            return ExpressionFormatMethods.sortInitial(this.getName());
        }
    }

    @Define(value={"decade"})
    public Integer getDecade() throws Exception {
        return this.getYear() / 10 * 10;
    }

    @Define(value={"anime"})
    public boolean isAnime() throws Exception {
        if (this.infoObject instanceof Episode) {
            SeriesInfo seriesInfo = this.getSeriesInfo();
            if ("Anime".equals(seriesInfo.getType()) || EpisodeUtilities.isInstance((Datasource)WebServices.AniDB, seriesInfo)) {
                return true;
            }
            if (seriesInfo.getGenres().contains("Anime") || this.seriesDetails.get().getKeywords().contains("anime")) {
                return true;
            }
            return WebServices.AnimeList.find(AnimeLists.DB.get(seriesInfo), seriesInfo.getId()).findAny().isPresent();
        }
        if (this.infoObject instanceof Movie) {
            MovieDetails movieDetails = this.getMovieDetails();
            return movieDetails.getKeywords().contains("anime") || movieDetails.getGenres().contains("Animation") && movieDetails.getSpokenLanguages().contains("ja") && movieDetails.getProductionCountries().contains("JP");
        }
        return false;
    }

    @Define(value={"regular"})
    public boolean isRegular() {
        return EpisodeUtilities.isRegularEpisode(this.getEpisode());
    }

    @Define(value={"sy"})
    public List<Integer> getSeasonYears() throws Exception {
        return EpisodeUtilities.fetchEpisodeList(this.getEpisode()).stream().filter(episode -> EpisodeUtilities.isRegularEpisode(episode) && episode.getAirdate() != null && Objects.equals(episode.getSeason(), this.getEpisode().getSeason())).map(episode -> episode.getAirdate().getYear()).sorted().distinct().collect(Collectors.toList());
    }

    @Define(value={"sc"})
    public Integer getSeasonCount() throws Exception {
        return EpisodeUtilities.fetchEpisodeList(this.getEpisode()).stream().filter(episode -> EpisodeUtilities.isRegularEpisode(episode) && episode.getSeason() != null).map(Episode::getSeason).max(Integer::compare).get();
    }

    @Define(value={"mediaTitle"})
    public String getMediaTitle() {
        return this.getVideoCharacteristics().map(MediaCharacteristics::getTitle).orElse(null);
    }

    @Define(value={"mediaTags"})
    public Object getMediaTags() {
        return this.getVideoCharacteristics().map(MediaCharacteristics::getMediaTags).orElse(null);
    }

    @Define(value={"audioLanguages"})
    public List<Language> getAudioLanguageList() {
        return this.getMediaInfo().stream(StreamKind.Audio, "Language").filter(Objects::nonNull).distinct().map(Language::findLanguage).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Define(value={"textLanguages"})
    public List<Language> getTextLanguageList() {
        return this.getMediaInfo().stream(StreamKind.Text, "Language").filter(Objects::nonNull).distinct().map(Language::findLanguage).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Define(value={"chapters"})
    public Map<String, String> getChapters() {
        return this.getMediaInfo().list(StreamKind.Menu).stream().map(map -> {
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            map.forEach((string, string2) -> {
                if (string.contains(":") && string.contains(".")) {
                    linkedHashMap.putIfAbsent(string, string2);
                }
            });
            return linkedHashMap;
        }).filter(map -> !map.isEmpty()).findFirst().orElse(null);
    }

    @Define(value={"bitrate"})
    public BitRate getOverallBitRate() {
        return BitRate.parse(this.getMediaInfo(StreamKind.General, "OverallBitRate"));
    }

    @Define(value={"vbr"})
    public BitRate getVideoBitRate() {
        return BitRate.parse(this.getMediaInfo(StreamKind.Video, "BitRate"));
    }

    @Define(value={"abr"})
    public BitRate getAudioBitRate() {
        return BitRate.parse(this.getMediaInfo(StreamKind.Audio, "BitRate"));
    }

    @Define(value={"kbps"})
    public BitRate getKiloBytesPerSecond() {
        return this.getOverallBitRate().getKbps();
    }

    @Define(value={"mbps"})
    public BitRate getMegaBytesPerSecond() {
        return this.getOverallBitRate().getMbps();
    }

    @Define(value={"fps"})
    public FrameRate getFrameRate() {
        return FrameRate.parse(this.getMediaInfo(StreamKind.Video, "FrameRate"));
    }

    @Define(value={"khz"})
    public String getSamplingRate() {
        return this.getMediaInfo(StreamKind.Audio, "SamplingRate/String");
    }

    @Define(value={"duration"})
    public Duration getDuration() {
        return Stream.of(StreamKind.Video, StreamKind.Audio, StreamKind.General).map(streamKind -> this.getMediaInfo().getString((StreamKind)((Object)streamKind), "Duration")).filter(Objects::nonNull).map(Double::parseDouble).map(Math::round).map(Duration::ofMillis).findFirst().orElse(null);
    }

    @Define(value={"seconds"})
    public long getSeconds() {
        return Math.round((double)this.getDuration().toMillis() / 1000.0);
    }

    @Define(value={"minutes"})
    public long getMinutes() {
        return Math.round((double)this.getDuration().toMillis() / 60000.0);
    }

    @Define(value={"hours"})
    public String getHours() {
        return String.format(Locale.ROOT, "%d\u2236%02d", this.getMinutes() / 60L, this.getMinutes() % 60L);
    }

    @Define(value={"media"})
    public AssociativeScriptObject getGeneralProperties() {
        return this.createMediaInfoBindings(StreamKind.General).findFirst().orElse(null);
    }

    @Define(value={"video"})
    public List<AssociativeScriptObject> getVideoPropertiesList() {
        return this.createMediaInfoBindings(StreamKind.Video).collect(Collectors.toList());
    }

    @Define(value={"audio"})
    public List<AssociativeScriptObject> getAudioPropertiesList() {
        return this.createMediaInfoBindings(StreamKind.Audio).collect(Collectors.toList());
    }

    @Define(value={"text"})
    public List<AssociativeScriptObject> getTextPropertiesList() {
        return this.createMediaInfoBindings(StreamKind.Text).collect(Collectors.toList());
    }

    @Define(value={"image"})
    public AssociativeScriptObject getImageProperties() {
        return this.createMediaInfoBindings(StreamKind.Image).findFirst().orElse(null);
    }

    @Define(value={"menu"})
    public AssociativeScriptObject getMenuProperties() {
        return this.createMediaInfoBindings(StreamKind.Menu).findFirst().orElse(null);
    }

    @Define(value={"exif"})
    public AssociativeScriptObject getImageMetadata() throws Exception {
        return new AssociativeScriptObject(this.getPhoto().snapshot(), this::undefined);
    }

    @Define(value={"camera"})
    public AssociativeEnumObject getCamera() throws Exception {
        return this.getPhoto().getCameraModel().map(AssociativeEnumObject::new).orElse(null);
    }

    @Define(value={"location"})
    public AssociativeEnumObject getLocation() throws Exception {
        return this.getPhoto().getLocationTaken(WebServices.GoogleMaps).map(AssociativeEnumObject::new).orElse(null);
    }

    @Define(value={"medium"})
    public Integer getMedium() {
        Integer n = this.getMusic().getMedium();
        Integer n2 = this.getMusic().getMediumCount();
        if (n != null && n2 != null && n2 > 1) {
            return n;
        }
        return null;
    }

    @Define(value={"artist"})
    public String getArtist() {
        return this.getMusic().getArtist();
    }

    @Define(value={"albumArtist"})
    public String getAlbumArtist() {
        return this.getMusic().getAlbumArtist();
    }

    @Define(value={"album"})
    public String getAlbum() {
        return this.getMusic().getAlbum();
    }

    @Define(value={"episode"})
    public Episode getEpisode() {
        return this.asType(this.infoObject, Episode.class);
    }

    @Define(value={"episodes"})
    public List<Episode> getEpisodes() {
        return EpisodeUtilities.streamMultiEpisode(this.getEpisode()).collect(Collectors.toList());
    }

    @Define(value={"movie"})
    public Movie getMovie() {
        return this.asType(this.infoObject, Movie.class);
    }

    @Define(value={"music"})
    public AudioTrack getMusic() {
        return this.asType(this.infoObject, AudioTrack.class);
    }

    @Define(value={"photo"})
    public ImageMetadata getPhoto() throws Exception {
        if (this.infoObject instanceof LocalDatasource.PhotoFile) {
            return ((LocalDatasource.PhotoFile)this.infoObject).getMetadata();
        }
        return new ImageMetadata(this.getMediaFile());
    }

    @Define(value={"pi"})
    public Number getPartIndex() throws Exception {
        if (this.infoObject instanceof AudioTrack) {
            return this.getMusic().getTrack();
        }
        if (this.infoObject instanceof Movie) {
            return this.infoObject instanceof MoviePart ? Integer.valueOf(((MoviePart)this.infoObject).getPartIndex()) : null;
        }
        List<File> list = this.getDuplicateGroup(this.inferredMediaFile.get());
        return list.size() > 1 ? this.identityIndexOf(list, this.inferredMediaFile.get()) : null;
    }

    @Deprecated
    @Define(value={"pn"})
    public Number getPartCountNumber() throws Exception {
        Logging.debug.severe("[DEPRECATED] {pn} is deprecated. Please use {pc} instead.");
        return this.getPartCount();
    }

    @Define(value={"pc"})
    public Number getPartCount() throws Exception {
        if (this.infoObject instanceof AudioTrack) {
            return this.getMusic().getTrackCount();
        }
        if (this.infoObject instanceof Movie) {
            return this.infoObject instanceof MoviePart ? Integer.valueOf(((MoviePart)this.infoObject).getPartCount()) : null;
        }
        List<File> list = this.getDuplicateGroup(this.inferredMediaFile.get());
        return list.size() > 1 ? Integer.valueOf(list.size()) : null;
    }

    @Define(value={"type"})
    public String getInfoObjectType() {
        return this.infoObject.getClass().getSimpleName();
    }

    @Define(value={"mediaFile"})
    public File getInferredMediaFile() throws Exception {
        return this.inferredMediaFile.get();
    }

    @Define(value={"mediaFileName"})
    public String getInferredMediaFileName() throws Exception {
        return FileUtilities.getName(this.inferredMediaFile.get());
    }

    @Define(value={"relativeFile"})
    public File getRelativeFilePath() {
        File file = this.getMediaFile();
        try {
            return MediaFileUtilities.getStructurePathTail(this.getMediaFile());
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Failed to detect relative library path", file, exception));
            return FileUtilities.getRelativePathTail(file, 2);
        }
    }

    @Define(value={"f"})
    public File getMediaFile() {
        if (this.mediaFile == null) {
            throw new BindingException((Object)"file", (Object)"undefined", BindingException.Flag.SAMPLE_FILE_NOT_SET);
        }
        return this.mediaFile;
    }

    @Define(value={"folder"})
    public File getMediaParentFolder() {
        return this.getMediaFile().getParentFile();
    }

    @Define(value={"files"})
    public List<File> files() throws Exception {
        File file = this.inferredMediaFile.get();
        if (file.isDirectory()) {
            return FileUtilities.listFiles(file, FileUtilities.FILES, FileUtilities.HUMAN_NAME_ORDER);
        }
        if (VFS.hasIndex(file) && file.isFile()) {
            return VFS.getIndex(file).stream().map(object -> new File(file, object.toString())).collect(Collectors.toList());
        }
        return FileUtilities.getChildren(file.getParentFile(), file2 -> MediaFileUtilities.isDerived(file2, file), FileUtilities.HUMAN_NAME_ORDER);
    }

    @Define(value={"bytes"})
    public FileSize getFileSize() throws Exception {
        if (this.getMediaFile().isDirectory()) {
            long l = FileUtilities.listFiles(this.getMediaFile(), FileUtilities.FILES).stream().mapToLong(File::length).sum();
            return new FileSize(l);
        }
        long l = this.inferredMediaFile.get().length();
        return new FileSize(l);
    }

    @Define(value={"megabytes"})
    public FileSize getFileSizeInMegaBytes() throws Exception {
        return this.getFileSize().getMB();
    }

    @Define(value={"gigabytes"})
    public FileSize getFileSizeInGigaBytes() throws Exception {
        return this.getFileSize().getGB();
    }

    @Define(value={"today"})
    public SimpleDate getToday() {
        return SimpleDate.now();
    }

    @Define(value={"root"})
    public File getMediaRootFolder() throws Exception {
        return MediaFileUtilities.getStructureRoot(this.getMediaFile());
    }

    @Define(value={"drive"})
    public File getMountPoint() throws Exception {
        return FileUtilities.getMountPoint(this.getMediaFile());
    }

    @Define(value={"home"})
    public File getUserHome() {
        return ApplicationFolder.UserHome.getDirectory();
    }

    @Define(value={"output"})
    public File getUserDefinedOutputFolder() throws IOException {
        return new File(Settings.getApplicationArguments().output).getCanonicalFile();
    }

    @Define(value={"defines"})
    public Map<String, String> getUserDefinedArguments() throws IOException {
        return Collections.unmodifiableMap(Settings.getApplicationArguments().defines);
    }

    @Define(value={"label"})
    public String getUserDefinedLabel() throws IOException {
        return this.getUserDefinedArguments().entrySet().stream().filter(entry -> ((String)entry.getKey()).endsWith("label") && entry.getValue() != null && ((String)entry.getValue()).length() > 0).map(entry -> (String)entry.getValue()).findFirst().orElse(null);
    }

    @Define(value={"i"})
    public Number getModelIndex() {
        return this.identityIndexOf(this.context.values(), this.getInfoObject());
    }

    @Define(value={"di"})
    public Number getDuplicateIndex() {
        List list = this.getDuplicateContext(MediaBindingBean::getInfoObject, MediaBindingBean::getExtension).map(MediaBindingBean::getInfoObject).collect(Collectors.toList());
        return this.identityIndexOf(list, this.getInfoObject());
    }

    @Define(value={"dc"})
    public Number getDuplicateCount() {
        return this.getDuplicateContext(MediaBindingBean::getInfoObject, MediaBindingBean::getExtension).count();
    }

    @Define(value={"plex"})
    public StructuredFile getPlexStandardPath() throws Exception {
        return this.getStandardPath(NamingStandard.Plex);
    }

    @Define(value={"kodi"})
    public StructuredFile getKodiStandardPath() throws Exception {
        return this.getStandardPath(NamingStandard.Kodi);
    }

    @Define(value={"emby"})
    public StructuredFile getEmbyStandardPath() throws Exception {
        return this.getStandardPath(NamingStandard.Emby);
    }

    @Define(value={"jellyfin"})
    public StructuredFile getJellyfinStandardPath() throws Exception {
        return this.getStandardPath(NamingStandard.Jellyfin);
    }

    @Define(value={"self"})
    public AssociativeScriptObject getSelf() {
        return this.createNullableBindings(this.infoObject, this.mediaFile, this.context);
    }

    @Define(value={"model"})
    public List<AssociativeScriptObject> getModel() {
        return modelCache.get(this.context, object -> this.context.entrySet().stream().map(entry -> this.createNullableBindings(entry.getValue(), (File)entry.getKey(), this.context)).collect(Collectors.toList()));
    }

    @Define(value={"episodelist"})
    public List<AssociativeScriptObject> getEpisodeList() throws Exception {
        List<Episode> list = EpisodeUtilities.fetchEpisodeList(this.getEpisode());
        SeriesInfo seriesInfo = list.get(0).getSeriesInfo();
        return modelCache.get(seriesInfo, object -> {
            try {
                return list.stream().map(episode -> this.createNullableBindings(episode, null, Collections.emptyMap())).collect(Collectors.toList());
            }
            catch (Exception exception) {
                throw new BindingException((Object)"episodelist", (Object)Logging.cause("Failed to retrieve episode list", object, exception), exception, new BindingException.Flag[0]);
            }
        });
    }

    @Define(value={"json"})
    public String getInfoObjectDump() {
        return MetaAttributes.toJson(this.infoObject, false);
    }

    @Define(value={"XEM"})
    public DynamicBindings getXrossEntityMapper() {
        return new DynamicBindings(XEM::names, string -> {
            if (this.infoObject instanceof Episode) {
                XEM xEM;
                Episode episode = this.getEpisode();
                XEM xEM2 = XEM.forName(episode.getSeriesInfo().getDatabase());
                return xEM2 == (xEM = XEM.forName(string)) ? episode : xEM2.map(episode, xEM);
            }
            return this.undefined((String)string);
        });
    }

    @Define(value={"AnimeList"})
    public DynamicBindings getAnimeLists() {
        return new DynamicBindings(AnimeLists.DB::names, string -> {
            if (this.infoObject instanceof Episode) {
                AnimeLists.DB dB;
                Episode episode = this.getEpisode();
                AnimeLists.DB dB2 = AnimeLists.DB.get(episode);
                return dB2 == (dB = AnimeLists.DB.get(string)) ? episode : WebServices.AnimeList.map(episode, dB2, dB).orElse(null);
            }
            return this.undefined((String)string);
        });
    }

    public StructuredFile getStandardPath(NamingStandard namingStandard) throws Exception {
        StructuredFile structuredFile = StructuredFile.of(this.infoObject, namingStandard);
        if (structuredFile != null) {
            try {
                structuredFile = structuredFile.suffix(this.getSubtitleTags());
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return structuredFile;
    }

    public SeriesDetails getSeriesDetails() {
        try {
            return this.seriesDetails.get();
        }
        catch (Exception exception) {
            throw new BindingException((Object)"details", (Object)Logging.cause("Failed to retrieve series details", exception), exception, new BindingException.Flag[0]);
        }
    }

    public MovieDetails getMovieDetails() {
        try {
            return this.movieDetails.get();
        }
        catch (Exception exception) {
            throw new BindingException((Object)"details", (Object)Logging.cause("Failed to retrieve movie details", exception), exception, new BindingException.Flag[0]);
        }
    }

    private File getInferredMediaFile(File file) {
        if (MediaTypes.VIDEO_FILES.accept(file)) {
            return file;
        }
        if (this.infoObject instanceof AudioTrack || this.infoObject instanceof LocalDatasource.PhotoFile) {
            return file;
        }
        return MediaFileUtilities.findPrimaryFile(file, MediaTypes.VIDEO_FILES).orElseGet(() -> {
            ArrayList<File> arrayList = new ArrayList<File>();
            if (this.infoObject instanceof Episode || this.infoObject instanceof Movie) {
                this.context.forEach((file2, object) -> {
                    if (this.infoObject.equals(object) && MediaTypes.VIDEO_FILES.accept(file2)) {
                        arrayList.add(file2);
                    }
                });
            }
            if (arrayList.isEmpty() && (MediaTypes.TEXT_FILES.accept(file) || MediaTypes.IMAGE_FILES.accept(file))) {
                try {
                    arrayList.addAll(MediaFileUtilities.findSiblingFiles(file, MediaTypes.VIDEO_FILES));
                }
                catch (Exception exception) {
                    Logging.debug.warning(Logging.cause("Failed to find sibling files", exception));
                }
            }
            return MediaFileUtilities.findPrimaryFile(file, arrayList).orElse(file);
        });
    }

    public MediaInfoTable getMediaInfo() {
        try {
            return this.mediaInfo.get();
        }
        catch (BindingException bindingException) {
            throw bindingException;
        }
        catch (Exception exception) {
            throw new BindingException((Object)"media", (Object)Logging.cause("Failed to read media info", exception), exception, new BindingException.Flag[0]);
        }
    }

    private String getMediaInfo(StreamKind streamKind, String ... stringArray) {
        String string2 = this.getMediaInfo().stream(streamKind, stringArray).filter(string -> string.length() < 150).findFirst().orElse(null);
        return string2 != null ? string2 : (String)this.undefined(streamKind.name() + Arrays.asList(stringArray));
    }

    private Stream<MediaBindingBean> getDuplicateContext(Function<MediaBindingBean, Object> ... functionArray) {
        return this.context.entrySet().stream().filter(entry -> entry.getKey() != null && entry.getValue() != null).map(entry -> new MediaBindingBean(entry.getValue(), (File)entry.getKey())).filter(mediaBindingBean -> Arrays.stream(functionArray).allMatch(function -> Objects.equals(function.apply(this), function.apply(mediaBindingBean))));
    }

    private List<File> getDuplicateGroup(File file) throws Exception {
        List<File> list = this.getDuplicateContext(MediaBindingBean::getInfoObject).map(MediaBindingBean::getFileObject).collect(Collectors.toList());
        if (list.size() > 1) {
            for (List<File> list2 : MediaFileUtilities.groupByMediaCharacteristics(list)) {
                if (this.identityIndexOf(list2, file) == null) continue;
                return list2;
            }
        }
        return Collections.emptyList();
    }

    private Integer identityIndexOf(Collection<?> collection, Object object) {
        int n = 0;
        for (Object obj : collection) {
            ++n;
            if (obj != object) continue;
            return n;
        }
        return null;
    }

    private AssociativeScriptObject createBindings(Object object, File file) {
        return new AssociativeScriptObject(new ExpressionBindings(new MediaBindingBean(object, file)), this::undefined);
    }

    private AssociativeScriptObject createNullableBindings(Object object, File file, Map<File, ?> map) {
        return new AssociativeScriptObject(new ExpressionBindings(new MediaBindingBean(object, file, map)), string -> null);
    }

    private AssociativeScriptObject createPropertyBindings(Object object) {
        return new AssociativeScriptObject(new PropertyBindings(object), this::undefined);
    }

    private Stream<AssociativeScriptObject> createMediaInfoBindings(StreamKind streamKind) {
        return this.getMediaInfo().list(streamKind).stream().map(map -> new AssociativeScriptObject((Map<?, ?>)map, string -> null));
    }

    private Optional<MediaCharacteristics> getVideoCharacteristics() {
        try {
            switch (CachedMediaCharacteristics.getMediaCharacteristicsParser()) {
                case mediainfo: {
                    return Optional.of(this.getMediaInfo());
                }
                case none: {
                    return Optional.empty();
                }
            }
            return CachedMediaCharacteristics.getMediaCharacteristics(this.inferredMediaFile.get());
        }
        catch (Exception exception) {
            Logging.debug.finest(Logging.cause("Failed to read video characteristics", exception));
            return Optional.empty();
        }
    }

    private String[] getFileNames(File file) {
        ArrayList<String> arrayList = new ArrayList<String>();
        String string = XattrMetaInfo.xattr.getOriginalName(file);
        if (string != null) {
            arrayList.add(FileUtilities.getNameWithoutExtension(string));
        }
        arrayList.add(FileUtilities.getNameWithoutExtension(file.getName()));
        File file2 = file.getParentFile();
        if (file2 != null && file2.getParent() != null) {
            arrayList.add(file2.getName());
        }
        return arrayList.toArray(new String[0]);
    }

    private Pattern getKeywordExcludePattern() {
        ArrayList<Object> arrayList = new ArrayList<Object>();
        if (this.infoObject instanceof Episode || this.infoObject instanceof Movie) {
            arrayList.add(this.getName());
            arrayList.addAll(this.getAliasNames());
            if (this.infoObject instanceof Episode) {
                for (Episode episode : this.getEpisodes()) {
                    arrayList.add(episode.getTitle());
                }
            } else if (this.infoObject instanceof Movie) {
                arrayList.add(this.getMovie().getYear());
            }
        }
        String string2 = arrayList.stream().filter(Objects::nonNull).map(Objects::toString).map(string -> Normalization.normalizePunctuation(string, " ", "\\P{Alnum}+")).filter(string -> !string.isEmpty()).collect(Collectors.joining("|", "\\b(?:", ")\\b"));
        return Pattern.compile(string2, 258);
    }

    private <T> T asType(Object object, Class<T> clazz) {
        if (clazz.isInstance(object)) {
            return clazz.cast(object);
        }
        throw new ClassCastException(clazz.getSimpleName() + " type bindings are not available for " + object.getClass().getSimpleName() + " type objects");
    }

    public String toString() {
        return String.format(Locale.ROOT, "%s \u21d4 %s", this.infoObject, this.mediaFile == null ? null : this.mediaFile.getName());
    }
}

