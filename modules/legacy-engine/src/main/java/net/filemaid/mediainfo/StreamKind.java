package net.filemaid.mediainfo;

public enum StreamKind {
    General,
    Video,
    Audio,
    Text,
    Other,
    Image,
    Menu,
    EXIF;


    public static StreamKind get(String string) {
        for (StreamKind streamKind : StreamKind.values()) {
            if (!string.startsWith(streamKind.name())) continue;
            return streamKind;
        }
        return null;
    }
}

