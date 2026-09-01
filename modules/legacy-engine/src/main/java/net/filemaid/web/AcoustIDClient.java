package net.filemaid.web;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.Execute;
import net.filemaid.InvalidResponseException;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.util.JsonUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.web.AbstractMusicLookupService;
import net.filemaid.web.AudioTrack;
import net.filemaid.web.FloodLimit;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.WebRequest;

public class AcoustIDClient
extends AbstractMusicLookupService {
    private static final FloodLimit REQUEST_LIMIT = new FloodLimit(3, 1L, TimeUnit.SECONDS);
    private String apikey;

    public AcoustIDClient(String string) {
        this.apikey = string;
    }

    @Override
    public String getIdentifier() {
        return "AcoustID";
    }

    @Override
    public Icon getIcon() {
        return ResourceManager.getIcon("search.acoustid");
    }

    public Cache getCache() {
        return Cache.getCache(this.getName(), CacheType.Monthly);
    }

    @Override
    public List<AudioTrack> fetchLookupResult(File file) throws Exception {
        Map<ChromaprintField, String> map = this.fpcalc(file);
        if (map.containsKey((Object)ChromaprintField.DURATION) && map.containsKey((Object)ChromaprintField.FINGERPRINT)) {
            String string;
            int n = Integer.parseInt(map.get((Object)ChromaprintField.DURATION));
            String string2 = map.get((Object)ChromaprintField.FINGERPRINT);
            if (n >= 10 && (string = this.lookup(n, string2)) != null && !string.isEmpty()) {
                return this.parseResult(string, n).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    public String lookup(int n, String string) throws Exception {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        linkedHashMap.put("duration", String.valueOf(n));
        linkedHashMap.put("fingerprint", string);
        FloodLimit floodLimit = REQUEST_LIMIT;
        synchronized (floodLimit) {
            return (String)this.getCache().computeIfAbsent(((Object)linkedHashMap).toString(), element -> {
                REQUEST_LIMIT.acquirePermit();
                URL uRL = WebRequest.newURL("http://api.acoustid.org/v2/lookup?client=" + this.apikey + "&meta=recordings+releases+releasegroups+tracks+compress");
                HashMap<String, String> hashMap = new HashMap<String, String>();
                hashMap.put("Content-Encoding", "gzip");
                hashMap.put("Accept-Encoding", "gzip");
                ByteBuffer byteBuffer = WebRequest.post(uRL, linkedHashMap, hashMap);
                Logging.debug.finest(WebRequest.log(byteBuffer));
                return StandardCharsets.UTF_8.decode(byteBuffer).toString();
            });
        }
    }

    public Stream<AudioTrack> parseResult(String string, int n) throws Exception {
        Object object2 = JsonUtilities.readJson(string);
        String string2 = JsonUtilities.getString(object2, "status");
        if (!"ok".equals(string2)) {
            throw new InvalidResponseException(this.getName() + " responded with error: " + string2);
        }
        return Arrays.stream(JsonUtilities.getArray(object2, "results")).flatMap(object -> JsonUtilities.streamJsonObjects(object, "recordings").sorted((map, map2) -> {
            Double d = JsonUtilities.getDouble(map, "duration");
            Double d2 = JsonUtilities.getDouble(map2, "duration");
            return Double.compare(d == null ? Double.NaN : Math.abs(d - (double)n), d2 == null ? Double.NaN : Math.abs(d2 - (double)n));
        }).flatMap(map -> {
            try {
                Map<Object, Object> map3 = JsonUtilities.getFirstMap(map, "releasegroups");
                String artist = JsonUtilities.getString(JsonUtilities.getFirstMap(map, "artists"), "name");
                String title = JsonUtilities.getString(map, "title");
                if (artist == null || title == null || map3.isEmpty()) {
                    return null;
                }
                AudioTrack audioTrack = new AudioTrack(artist, title, null, this.getIdentifier());
                audioTrack.mbid = JsonUtilities.getString(map, "id");
                Object[] objectArray = JsonUtilities.getArray(map3, "releases");
                String string3 = JsonUtilities.getString(map3, "type");
                if (objectArray.length == 0 || !"Album".equals(string3)) {
                    return Stream.of(audioTrack);
                }
                return JsonUtilities.streamJsonObjects(objectArray).map(map2 -> {
                    Map<Object, Object> dateMap;
                    AudioTrack audioTrack2 = audioTrack.clone();
                    try {
                        dateMap = JsonUtilities.getMap(map2, "date");
                        audioTrack2.albumReleaseDate = new SimpleDate(JsonUtilities.getInteger(dateMap, "year"), JsonUtilities.getInteger(dateMap, "month"), JsonUtilities.getInteger(dateMap, "day"));
                    }
                    catch (Exception exception) {
                        audioTrack2.albumReleaseDate = null;
                    }
                    if (audioTrack2.albumReleaseDate == null || audioTrack2.albumReleaseDate.getTimeStamp() >= (audioTrack.albumReleaseDate == null ? Long.MAX_VALUE : audioTrack.albumReleaseDate.getTimeStamp())) {
                        return null;
                    }
                    dateMap = JsonUtilities.getFirstMap(map2, "mediums");
                    audioTrack2.mediumIndex = JsonUtilities.getInteger(dateMap, "position");
                    audioTrack2.mediumCount = JsonUtilities.getInteger(map2, "medium_count");
                    Map<Object, Object> map4 = JsonUtilities.getFirstMap(dateMap, "tracks");
                    audioTrack2.trackIndex = JsonUtilities.getInteger(map4, "position");
                    audioTrack2.trackCount = JsonUtilities.getInteger(dateMap, "track_count");
                    try {
                        audioTrack2.album = JsonUtilities.getString(map2, "title");
                    }
                    catch (Exception exception) {
                        audioTrack2.album = JsonUtilities.getString((Object)dateMap, "title");
                    }
                    try {
                        audioTrack2.albumArtist = JsonUtilities.getString(JsonUtilities.getFirstMap(dateMap, "artists"), "name");
                    }
                    catch (Exception exception) {
                        audioTrack2.albumArtist = null;
                    }
                    audioTrack2.trackTitle = JsonUtilities.getString(map4, "title");
                    if (!(audioTrack2.album == null || "Various Artists".equalsIgnoreCase(audioTrack2.albumArtist) || audioTrack2.album != null && audioTrack2.album.contains("Greatest Hits"))) {
                        return audioTrack2;
                    }
                    return null;
                });
            }
            catch (Exception exception) {
                Logging.debug.log(Level.WARNING, exception, exception::toString);
                return null;
            }
        })).filter(Objects::nonNull).sorted(new MostFieldsNotNull()).distinct();
    }

    public String getChromaprintCommand() {
        return System.getProperty("net.filemaid.AcoustID.fpcalc", "fpcalc");
    }

    public String version() throws IOException {
        return Execute.execute(this.getChromaprintCommand(), "-version").toString().trim();
    }

    public Map<ChromaprintField, String> fpcalc(File file) {
        EnumMap<ChromaprintField, String> enumMap = new EnumMap<ChromaprintField, String>(ChromaprintField.class);
        try {
            CharSequence charSequence = Execute.execute(this.getChromaprintCommand(), file.getCanonicalPath());
            try (Scanner scanner = new Scanner(charSequence.toString());){
                while (scanner.hasNextLine()) {
                    String[] stringArray = RegularExpressions.EQUALS.split(scanner.nextLine(), 2);
                    if (stringArray.length != 2) continue;
                    enumMap.put(ChromaprintField.valueOf(stringArray[0]), stringArray[1]);
                }
            }
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(this.getChromaprintCommand(), exception));
        }
        return enumMap;
    }

    public static enum ChromaprintField {
        FILE,
        FINGERPRINT,
        DURATION;

    }

    private static class MostFieldsNotNull
    implements Comparator<Object> {
        private MostFieldsNotNull() {
        }

        @Override
        public int compare(Object object, Object object2) {
            return Integer.compare(this.count(object2), this.count(object));
        }

        public int count(Object object) {
            int n = 0;
            try {
                for (Field field : object.getClass().getDeclaredFields()) {
                    if (field.get(object) == null) continue;
                    ++n;
                }
            }
            catch (Exception exception) {
                Logging.debug.log(Level.WARNING, exception.getMessage(), exception);
            }
            return n;
        }
    }
}

