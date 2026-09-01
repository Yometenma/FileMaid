package net.filemaid.mediainfo;

import com.sun.jna.Memory;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import net.filemaid.mediainfo.MediaInfoException;
import net.filemaid.mediainfo.MediaInfoLibrary;
import net.filemaid.mediainfo.MediaInfoProperties;
import net.filemaid.mediainfo.StreamKind;

public class MediaInfo
implements MediaInfoProperties {
    private final Pointer handle;

    public MediaInfo() {
        try {
            this.handle = MediaInfoLibrary.INSTANCE.New();
            this.option("Language", "raw");
            this.option("Complete", "1");
        }
        catch (LinkageError linkageError) {
            throw new MediaInfoException(linkageError);
        }
    }

    public synchronized int open(File file) {
        return MediaInfoLibrary.INSTANCE.Open(this.handle, new WString(file.getPath()));
    }

    public synchronized long read(File file, int n) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);){
            long l;
            long l2 = fileChannel.size();
            Memory memory = new Memory((long)n);
            ByteBuffer byteBuffer = memory.getByteBuffer(0L, memory.size());
            long l3 = 0L;
            MediaInfoLibrary.INSTANCE.Open_Buffer_Init(this.handle, l2, 0L);
            int n2 = -1;
            while ((n2 = fileChannel.read(byteBuffer)) >= 0) {
                l3 += (long)n2;
                if ((MediaInfoLibrary.INSTANCE.Open_Buffer_Continue(this.handle, (Pointer)memory, n2) & 8) == 8) break;
                l = MediaInfoLibrary.INSTANCE.Open_Buffer_Continue_GoTo_Get(this.handle);
                if (l >= 0L) {
                    fileChannel.position(l);
                    MediaInfoLibrary.INSTANCE.Open_Buffer_Init(this.handle, l2, l);
                }
                byteBuffer.rewind();
            }
            MediaInfoLibrary.INSTANCE.Open_Buffer_Finalize(this.handle);
            l = l3;
            return l;
        }
    }

    public synchronized String option(String string) {
        return this.option(string, "");
    }

    public synchronized String option(String string, String string2) {
        return MediaInfoLibrary.INSTANCE.Option(this.handle, new WString(string), new WString(string2)).toString();
    }

    @Override
    public synchronized String get(StreamKind streamKind, int n, String string) {
        return MediaInfoLibrary.INSTANCE.Get(this.handle, streamKind.ordinal(), n, new WString(string), InfoKind.Text.ordinal(), InfoKind.Name.ordinal()).toString();
    }

    public synchronized String get(StreamKind streamKind, int n, int n2, InfoKind infoKind) {
        return MediaInfoLibrary.INSTANCE.GetI(this.handle, streamKind.ordinal(), n, n2, infoKind.ordinal()).toString();
    }

    @Override
    public synchronized int streamCount(StreamKind streamKind) {
        if (Platform.isWindows()) {
            String string = this.get(streamKind, 0, "StreamCount");
            return string.isEmpty() ? 0 : Integer.parseInt(string);
        }
        return MediaInfoLibrary.INSTANCE.Count_Get(this.handle, streamKind.ordinal(), -1);
    }

    public synchronized int parameterCount(StreamKind streamKind, int n) {
        return MediaInfoLibrary.INSTANCE.Count_Get(this.handle, streamKind.ordinal(), n);
    }

    @Override
    public synchronized Map<String, String> map(StreamKind streamKind, int n) {
        int n2 = this.parameterCount(streamKind, n);
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>(n2);
        for (int i = 0; i < n2; ++i) {
            String string = this.get(streamKind, n, i, InfoKind.Text);
            if (string.isEmpty()) continue;
            String string2 = this.get(streamKind, n, i, InfoKind.Name);
            linkedHashMap.put(string2, string);
        }
        return linkedHashMap;
    }

    public synchronized String raw() throws IOException {
        return MediaInfoLibrary.INSTANCE.Inform(this.handle).toString();
    }

    @Override
    public synchronized void close() {
        MediaInfoLibrary.INSTANCE.Close(this.handle);
        MediaInfoLibrary.INSTANCE.Delete(this.handle);
    }

    public static String version() {
        return MediaInfo.staticOption("Info_Version");
    }

    public static String parameters() {
        return MediaInfo.staticOption("Info_Parameters");
    }

    public static String codecs() {
        return MediaInfo.staticOption("Info_Codecs");
    }

    public static String capacities() {
        return MediaInfo.staticOption("Info_Capacities");
    }

    public static String staticOption(String string) {
        return MediaInfo.staticOption(string, "");
    }

    public static String staticOption(String string, String string2) {
        return MediaInfoLibrary.INSTANCE.Option(null, new WString(string), new WString(string2)).toString();
    }

    public static enum InfoKind {
        Name,
        Text,
        Measure,
        Options,
        Name_Text,
        Measure_Text,
        Info,
        HowTo,
        Domain;

    }
}

