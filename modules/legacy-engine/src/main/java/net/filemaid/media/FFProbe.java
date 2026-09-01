package net.filemaid.media;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.Execute;
import net.filemaid.media.CachedFileAttribute;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.util.DateTimeUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.web.Episode;
import net.filemaid.web.Link;
import net.filemaid.web.Movie;
import net.filemaid.web.SimpleDate;

public class FFProbe
extends LinkedHashMap<String, Object>
implements MediaCharacteristics {
    private static final String ffprobe = System.getProperty("net.filemaid.media.ffprobe", "ffprobe");
    private static final CachedFileAttribute cache = CachedFileAttribute.cache("ffprobe", null, fileKey -> FFProbe.minify(FFProbe.ffprobe(fileKey.getFile())));

    @Override
    public String getFileName() {
        return this.getFormat("filename", string -> new File((String)string).getName());
    }

    @Override
    public Long getFileSize() {
        return this.getFormat("size", Long::parseLong);
    }

    @Override
    public String getVideoCodec() {
        return this.getString("video", "codec_name");
    }

    @Override
    public String getVideoProfile() {
        return this.getString("video", "profile");
    }

    @Override
    public String getAudioCodec() {
        return this.getString("audio", "codec_name");
    }

    @Override
    public String getAudioLanguage() {
        return this.getString("audio", "tags", "language");
    }

    @Override
    public String getSubtitleCodec() {
        return this.getString("subtitle", "codec_name");
    }

    @Override
    public String getSubtitleLanguage() {
        return this.getString("subtitle", "tags", "language");
    }

    @Override
    public Duration getDuration() {
        return this.getFormat("duration", string -> Duration.ofMillis(Math.round(Double.parseDouble(string) * 1000.0)));
    }

    @Override
    public Integer getWidth() {
        return this.getInteger("video", "width");
    }

    @Override
    public Integer getHeight() {
        return this.getInteger("video", "height");
    }

    @Override
    public Double getBitRate() {
        return this.getFormat("bit_rate", Double::parseDouble);
    }

    @Override
    public Double getFrameRate() {
        return this.find("video", "avg_frame_rate").map(string -> RegularExpressions.SLASH.splitAsStream((CharSequence)string).mapToDouble(Double::parseDouble).reduce((d, d2) -> d / d2)).filter(OptionalDouble::isPresent).map(OptionalDouble::getAsDouble).orElse(null);
    }

    @Override
    public String getTitle() {
        return this.getTag("title").orElse(null);
    }

    @Override
    public Instant getCreationTime() {
        return this.getTag("creation_time").map(string -> DateTimeUtilities.matchDateTime(string, ZoneOffset.UTC)).orElse(null);
    }

    @Override
    public Object getMediaTags() {
        String string2;
        String string3 = this.getTag("show").orElse(null);
        Integer n = this.getTag("season_number").map(Integer::parseInt).orElse(null);
        Integer n2 = this.getTag("episode_sort").map(Integer::parseInt).orElse(null);
        String string4 = this.getTag("title").orElse(null);
        Instant instant = this.getTag("date").map(string -> DateTimeUtilities.matchDateTime(string, ZoneOffset.UTC)).orElse(null);
        Integer n3 = this.getFormat("duration", string -> (int)Duration.ofMillis(Math.round(Double.parseDouble(string) * 1000.0)).toMinutes());
        if (string3 != null && n2 != null) {
            return new Episode(string3, n, n2, string4, null, null, SimpleDate.from(instant), n3, null, null, null);
        }
        Integer n4 = this.getTag("IMDB").map(Link.IMDb::parseID).orElse(null);
        Integer n5 = this.getTag("TMDB").map(Link.TheMovieDB::parseID).orElse(null);
        Movie movie = this.getTag("Title").map(Movie::matchNameYear).orElse(null);
        String string5 = movie != null ? (string2 = movie.getName()) : (string2 = string4 != null ? string4 : null);
        Integer n6 = movie != null ? Integer.valueOf(movie.getYear()) : (instant != null ? Integer.valueOf(instant.atOffset(ZoneOffset.UTC).getYear()) : null);
        return Movie.ID(n5, n4, string2, n6);
    }

    public Map<String, Object> getFormat() {
        return (Map)this.get("format");
    }

    public <T> T getFormat(String string2, Function<String, T> function) {
        return Optional.ofNullable(this.getFormat()).map(map -> map.get(string2)).map(Object::toString).filter(string -> !string.isEmpty()).map(function).orElse(null);
    }

    public Optional<String> getTag(String string2) {
        Stream<Object> stream = Stream.of(this.getFormat()).filter(Objects::nonNull).map(map -> map.get("tags"));
        Stream<Object> stream2 = this.stream("video", "tags");
        return Stream.concat(stream2, stream).filter(Map.class::isInstance).map(Map.class::cast).map(map -> map.get(string2)).filter(Objects::nonNull).map(Object::toString).filter(string -> !string.isEmpty()).findFirst();
    }

    public List<Map<String, Object>> getStreams() {
        return Arrays.stream((Object[])this.get("streams")).map(m -> (Map<String, Object>)m).collect(Collectors.toList());
    }

    protected String getString(String string, String string2) {
        return this.stream(string, string2).map(Objects::toString).collect(Collectors.joining(" / "));
    }

    protected String getString(String string, String string2, String string3) {
        return this.stream(string, string2).map(object -> ((Map)object).get(string3)).map(Objects::toString).collect(Collectors.joining(" / "));
    }

    protected Stream<Object> stream(String string, String string2) {
        return this.getStreams().stream().filter(map -> string.equals(map.get("codec_type"))).map(map -> map.get(string2)).filter(Objects::nonNull);
    }

    protected Integer getInteger(String string, String string2) {
        return this.find(string, string2).map(Integer::parseInt).orElse(null);
    }

    protected Optional<String> find(String string2, String string3) {
        return this.stream(string2, string3).map(Object::toString).filter(string -> !string.isEmpty()).findFirst();
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public String toString() {
        return JsonWriter.objectToJson((Object)this);
    }

    public void parse(String string) {
        this.putAll((Map)JsonReader.jsonToJava((String)string, Collections.singletonMap("USE_MAPS", true)));
    }

    public static FFProbe read(File file) throws Exception {
        FFProbe fFProbe = new FFProbe();
        fFProbe.parse(cache.get(file));
        return fFProbe;
    }

    public static String ffprobe(File file) throws IOException {
        return Execute.execute(ffprobe, Arrays.asList("-show_streams", "-show_format", "-print_format", "json", "-v", "error", file.getAbsolutePath()), file.getParentFile(), null, false).toString();
    }

    public static String version() throws IOException {
        return Execute.execute(ffprobe, "-show_program_version", "-hide_banner").toString().trim();
    }

    public static String minify(String string) {
        return RegularExpressions.NEWLINE.splitAsStream(string).map(String::trim).collect(Collectors.joining());
    }
}

