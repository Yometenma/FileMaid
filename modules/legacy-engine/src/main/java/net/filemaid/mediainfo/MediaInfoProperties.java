package net.filemaid.mediainfo;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.mediainfo.StreamKind;
import net.filemaid.util.DateTimeUtilities;
import net.filemaid.web.Episode;
import net.filemaid.web.Link;
import net.filemaid.web.Movie;
import net.filemaid.web.SimpleDate;

public interface MediaInfoProperties
extends MediaCharacteristics {
    public String get(StreamKind var1, int var2, String var3);

    public Map<String, String> map(StreamKind var1, int var2);

    public int streamCount(StreamKind var1);

    @Override
    default public String getFileName() {
        return this.getString(StreamKind.General, "FileName");
    }

    @Override
    default public Long getFileSize() {
        return this.get(StreamKind.General, "FileSize", Long::parseLong);
    }

    @Override
    default public String getVideoCodec() {
        return this.getString(StreamKind.General, "Video_Codec_List");
    }

    @Override
    default public String getVideoProfile() {
        return this.getString(StreamKind.Video, "Format_Profile");
    }

    @Override
    default public String getAudioCodec() {
        return this.getString(StreamKind.General, "Audio_Codec_List");
    }

    @Override
    default public String getAudioLanguage() {
        return this.getString(StreamKind.General, "Audio_Language_List");
    }

    @Override
    default public String getSubtitleCodec() {
        return this.getString(StreamKind.General, "Text_Codec_List");
    }

    @Override
    default public String getSubtitleLanguage() {
        return this.getString(StreamKind.General, "Text_Language_List");
    }

    @Override
    default public Duration getDuration() {
        return this.get(StreamKind.General, "Duration", (String string) -> Duration.ofMillis(Math.round(Double.parseDouble(string))));
    }

    @Override
    default public Integer getWidth() {
        return this.getInteger(StreamKind.Video, "Width");
    }

    @Override
    default public Integer getHeight() {
        return this.getInteger(StreamKind.Video, "Height");
    }

    @Override
    default public Double getBitRate() {
        return this.getDouble(StreamKind.General, "OverallBitRate");
    }

    @Override
    default public Double getFrameRate() {
        return this.getDouble(StreamKind.Video, "FrameRate");
    }

    @Override
    default public String getTitle() {
        return Stream.of(StreamKind.General, StreamKind.Video).flatMap(streamKind -> this.stream((StreamKind)((Object)streamKind), "Title", "Movie")).filter(string -> !string.contains("#")).findFirst().orElse(null);
    }

    @Override
    default public Instant getCreationTime() {
        if (this.stream(StreamKind.General, "Encoded_Application").anyMatch("no_variable_data"::equals)) {
            return null;
        }
        return this.get(StreamKind.General, "Encoded_Date", (String string) -> DateTimeUtilities.matchDateTime(string, ZoneOffset.UTC));
    }

    @Override
    default public Object getMediaTags() {
        String string2;
        String string3 = this.getString(StreamKind.General, "Collection");
        Integer n = this.getInteger(StreamKind.General, "Season");
        Integer n2 = this.getInteger(StreamKind.General, "Part");
        String string4 = this.getString(StreamKind.General, "Title");
        Instant instant = this.get(StreamKind.General, "Recorded_Date", (String string) -> DateTimeUtilities.matchDateTime(string, ZoneOffset.UTC));
        Integer n3 = this.get(StreamKind.General, "Duration", (String string) -> (int)Duration.ofMillis(Math.round(Double.parseDouble(string))).toMinutes());
        if (string3 != null && n2 != null) {
            return new Episode(string3, n, n2, string4, null, null, SimpleDate.from(instant), n3, null, null, null);
        }
        Integer n4 = this.get(StreamKind.General, "IMDB", Link.IMDb::parseID);
        Integer n5 = this.get(StreamKind.General, "TMDB", Link.TheMovieDB::parseID);
        Movie movie = Movie.matchNameYear(string4);
        String string5 = movie != null ? (string2 = movie.getName()) : (string2 = string4 != null ? string4 : null);
        Integer n6 = movie != null ? Integer.valueOf(movie.getYear()) : (instant != null ? Integer.valueOf(instant.atOffset(ZoneOffset.UTC).getYear()) : null);
        return Movie.ID(n5, n4, string2, n6);
    }

    default public String getString(StreamKind streamKind, String string) {
        return this.get(streamKind, string, Object::toString);
    }

    default public Integer getInteger(StreamKind streamKind, String string) {
        return this.get(streamKind, string, Integer::parseInt);
    }

    default public Double getDouble(StreamKind streamKind, String string) {
        return this.get(streamKind, string, Double::parseDouble);
    }

    default public <T> T get(StreamKind streamKind, String string, Function<String, T> function) {
        return this.stream(streamKind, string).findFirst().map(function).orElse(null);
    }

    default public Stream<String> stream(StreamKind streamKind, String ... stringArray) {
        return IntStream.range(0, this.streamCount(streamKind)).mapToObj(n -> Stream.of(stringArray).map(string -> this.get(streamKind, n, (String)string)).filter(string -> string != null && !string.isEmpty()).findFirst().orElse(null)).filter(Objects::nonNull);
    }

    default public Stream<String> find(StreamKind streamKind, String ... stringArray) {
        int n = this.streamCount(streamKind);
        return Stream.of(stringArray).flatMap(string2 -> IntStream.range(0, n).mapToObj(i -> this.get(streamKind, i, (String)string2)).filter(string -> string != null && !string.isEmpty()));
    }

    default public List<Map<String, String>> list(StreamKind streamKind) {
        int n = this.streamCount(streamKind);
        ArrayList<Map<String, String>> arrayList = new ArrayList<Map<String, String>>(n);
        for (int i = 0; i < n; ++i) {
            arrayList.add(this.map(streamKind, i));
        }
        return arrayList;
    }
}

