package net.filemaid.media;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.filemaid.Logging;
import net.filemaid.media.CachedFileAttribute;
import net.filemaid.media.ImageMetadata;
import net.filemaid.media.MediaInfoTool;
import net.filemaid.mediainfo.MediaInfoProperties;
import net.filemaid.mediainfo.StreamKind;
import net.filemaid.util.RegularExpressions;

public class MediaInfoTable
extends EnumMap<StreamKind, List<Map<String, String>>>
implements MediaInfoProperties {
    private static final Pattern DELIMITER = Pattern.compile("[ ]+[:][ ]+|\t");
    private static final CachedFileAttribute cache = CachedFileAttribute.cache("mediainfo", "net.filemaid.mediainfo", fileKey -> MediaInfoTable.minify(MediaInfoTable.raw(fileKey.getFile())));

    private MediaInfoTable() {
        super(StreamKind.class);
    }

    @Override
    public String get(StreamKind streamKind, int n, String string) {
        return (String)((Map)((List)this.get((Object)streamKind)).get(n)).get(string);
    }

    @Override
    public Map<String, String> map(StreamKind streamKind, int n) {
        return (Map)((List)this.get((Object)streamKind)).get(n);
    }

    @Override
    public List<Map<String, String>> list(StreamKind streamKind2) {
        return this.computeIfAbsent(streamKind2, streamKind -> new ArrayList(1));
    }

    @Override
    public int streamCount(StreamKind streamKind) {
        return this.containsKey((Object)streamKind) ? ((List)this.get((Object)streamKind)).size() : 0;
    }

    @Override
    public void close() throws Exception {
    }

    public void parse(CharSequence charSequence) {
        LinkedHashMap<String, String> linkedHashMap = null;
        for (String string : RegularExpressions.NEWLINE.split(charSequence)) {
            if (string.isEmpty()) continue;
            String[] stringArray = DELIMITER.split(string, 2);
            if (stringArray.length == 2 && linkedHashMap != null) {
                linkedHashMap.put(stringArray[0].trim(), stringArray[1].trim());
                continue;
            }
            StreamKind streamKind = StreamKind.get(string);
            if (streamKind != null) {
                linkedHashMap = new LinkedHashMap<String, String>(64);
                this.list(streamKind).add(linkedHashMap);
                continue;
            }
            Logging.debug.warning(Logging.message("MediaInfo", "Invalid line", string));
        }
        if (linkedHashMap == null) {
            Logging.debug.warning(Logging.message("MediaInfo", "Invalid table", charSequence));
        }
    }

    public void readImageMetadata(File file) throws Exception {
        this.list(StreamKind.EXIF).add(new ImageMetadata(file).snapshot());
    }

    public static MediaInfoTable read(File file) throws Exception {
        MediaInfoTable mediaInfoTable = new MediaInfoTable();
        mediaInfoTable.parse(cache.get(file));
        if (mediaInfoTable.isEmpty()) {
            throw new IllegalStateException("Empty mediainfo table");
        }
        try {
            if (ImageMetadata.SUPPORTED_FILE_TYPES.accept(file)) {
                mediaInfoTable.readImageMetadata(file);
            }
        }
        catch (Throwable throwable) {
            Logging.debug.warning(Logging.cause("Failed to read image metadata", file, throwable));
        }
        return mediaInfoTable;
    }

    public static boolean copy(File file, File file2) {
        return cache.copy(file, file2);
    }

    public static String raw(File file) throws IOException {
        if (file.isFile() && file.length() > 0L) {
            Logging.debug.finest(Logging.message("MediaInfo", "Read media file", file));
            String string = MediaInfoTool.INSTANCE.raw(file);
            if (string.length() > 0) {
                if (DELIMITER.matcher(string).find()) {
                    return string;
                }
                Logging.debug.warning(Logging.message("MediaInfo", "Invalid output", string));
            }
        }
        throw new IOException("Failed to read media file: " + file);
    }

    public static String minify(String string2) {
        return RegularExpressions.NEWLINE.splitAsStream(string2).map(string -> DELIMITER.matcher((CharSequence)string).replaceFirst("\t").trim()).filter(string -> string.length() > 0).collect(Collectors.joining("\n"));
    }
}

