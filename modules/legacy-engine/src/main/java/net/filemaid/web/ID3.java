package net.filemaid.web;

import java.io.File;
import java.util.Collections;
import java.util.List;
import javax.swing.Icon;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.media.MediaInfoTable;
import net.filemaid.mediainfo.MediaInfoProperties;
import net.filemaid.mediainfo.StreamKind;
import net.filemaid.util.StringUtilities;
import net.filemaid.web.AbstractMusicLookupService;
import net.filemaid.web.AudioTrack;
import net.filemaid.web.SimpleDate;

public class ID3
extends AbstractMusicLookupService {
    @Override
    public String getIdentifier() {
        return "ID3";
    }

    @Override
    public String getName() {
        return "ID3 Tags";
    }

    @Override
    public Icon getIcon() {
        return ResourceManager.getIcon("search.mediainfo");
    }

    @Override
    public List<AudioTrack> fetchLookupResult(File file) {
        AudioTrack audioTrack = this.getAudioTrack(file);
        if (audioTrack != null) {
            return Collections.singletonList(audioTrack);
        }
        return Collections.emptyList();
    }

    public AudioTrack getAudioTrack(File file) {
        try {
            return this.getAudioTrack(MediaInfoTable.read(file));
        }
        catch (Throwable throwable) {
            Logging.debug.warning(Logging.cause(this.getName(), file, throwable));
            return null;
        }
    }

    public AudioTrack getAudioTrack(MediaInfoProperties mediaInfoProperties) {
        Integer n;
        String string = this.getString(mediaInfoProperties, "ARTISTS", "Performer", "Composer");
        String string2 = this.getString(mediaInfoProperties, "Title", "Track");
        if (string == null || string2 == null) {
            return null;
        }
        String string3 = this.getString(mediaInfoProperties, "Album");
        String string4 = this.getString(mediaInfoProperties, "Album/Performer", "Album/Composer");
        String string5 = this.getString(mediaInfoProperties, "Track");
        String string6 = this.getString(mediaInfoProperties, "Genre");
        Integer n2 = this.getInteger(mediaInfoProperties, "Part/Position", "Part");
        Integer n3 = this.getInteger(mediaInfoProperties, "Part/Position_Total");
        Integer n4 = this.getInteger(mediaInfoProperties, "Track/Position");
        Integer n5 = this.getInteger(mediaInfoProperties, "Track/Position_Total");
        String string7 = this.getString(mediaInfoProperties, "Acoustid Id");
        String string8 = this.getString(mediaInfoProperties, "Recorded_Date");
        SimpleDate simpleDate = SimpleDate.parse(string8);
        if (simpleDate == null && (n = StringUtilities.matchInteger(string8)) != null) {
            simpleDate = new SimpleDate(n);
        }
        return new AudioTrack(string, string2, string3, string4, string5, string6, simpleDate, n2, n3, n4, n5, string7, this.getIdentifier());
    }

    private String getString(MediaInfoProperties mediaInfoProperties, String ... stringArray) {
        return mediaInfoProperties.stream(StreamKind.General, stringArray).findFirst().orElse(null);
    }

    private Integer getInteger(MediaInfoProperties mediaInfoProperties, String ... stringArray) {
        return StringUtilities.matchInteger(this.getString(mediaInfoProperties, stringArray));
    }
}

