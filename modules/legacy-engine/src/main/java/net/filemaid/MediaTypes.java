package net.filemaid;

import java.util.Arrays;
import net.filemaid.util.ExtensionFileFilter;

public interface MediaTypes {
    public static final ExtensionFileFilter MP4 = MediaTypes.extension("mp4", "m4v", "3gp");
    public static final ExtensionFileFilter MKV = MediaTypes.extension("mkv", "mk3d");
    public static final ExtensionFileFilter ISO = MediaTypes.extension("iso");
    public static final ExtensionFileFilter SFV = MediaTypes.extension("sfv");
    public static final ExtensionFileFilter MD5 = MediaTypes.extension("md5");
    public static final ExtensionFileFilter SHA1 = MediaTypes.extension("sha1", "sha");
    public static final ExtensionFileFilter SHA256 = MediaTypes.extension("sha256");
    public static final ExtensionFileFilter SHA3 = MediaTypes.extension("sha3");
    public static final ExtensionFileFilter SRT = MediaTypes.extension("srt");
    public static final ExtensionFileFilter SUB = MediaTypes.extension("sub");
    public static final ExtensionFileFilter SSA = MediaTypes.extension("ssa", "ass");
    public static final ExtensionFileFilter SAMI = MediaTypes.extension("smi", "sami");
    public static final ExtensionFileFilter XML = MediaTypes.extension("xml");
    public static final ExtensionFileFilter ZIP = MediaTypes.extension("zip");
    public static final ExtensionFileFilter NFO_FILES = MediaTypes.extension("nfo", "url");
    public static final ExtensionFileFilter LIST_FILES = MediaTypes.extension("txt");
    public static final ExtensionFileFilter TORRENT_FILES = MediaTypes.extension("torrent");
    public static final ExtensionFileFilter AUDIO_FILES = MediaTypes.extension("mp3", "m4a", "m4b", "mka", "flac", "opus", "wma", "ogg", "ogm", "oga", "opus", "wav", "aiff", "alac", "ac3", "dts");
    public static final ExtensionFileFilter VIDEO_FILES = MediaTypes.extension(MKV, MP4, ISO, MediaTypes.extension("mov", "avi", "mpg", "mpeg", "vob", "ts", "tp", "m2ts", "m4s", "ogg", "ogm", "divx", "asf", "wmv", "wtv", "dvr-ms", "rec", "webm", "flv", "rm", "rmvb", "rmp4", "tivo", "nuv", "3DSBS", "3DTAB", "strm", "strmlnk", "streamlnk"));
    public static final ExtensionFileFilter SUBTITLE_FILES = MediaTypes.extension(SRT, SUB, SSA, SAMI, MediaTypes.extension("mpl", "sup", "vtt", "ttml", "vobsub", "sub", "idx"));
    public static final ExtensionFileFilter IMAGE_FILES = MediaTypes.extension("png", "jpg", "jpeg", "jxl", "avif", "webp", "heic", "heif", "dng", "tbn");
    public static final ExtensionFileFilter ARCHIVE_FILES = MediaTypes.extension(ISO, ZIP, MediaTypes.extension("rar", "rar5", "7z", "tar", "gzip", "gz", "bzip2", "bz2", "xz"));
    public static final ExtensionFileFilter VERIFICATION_FILES = MediaTypes.extension(SFV, MD5, SHA1, SHA256, SHA3);
    public static final ExtensionFileFilter TEXT_FILES = MediaTypes.extension(SUBTITLE_FILES, NFO_FILES, LIST_FILES, VERIFICATION_FILES, XML);
    public static final ExtensionFileFilter LICENSE_FILES = MediaTypes.extension("psm", "txt");

    public static ExtensionFileFilter extension(String ... stringArray) {
        return new ExtensionFileFilter(stringArray);
    }

    public static ExtensionFileFilter extension(ExtensionFileFilter ... extensionFileFilterArray) {
        return new ExtensionFileFilter((String[])Arrays.stream(extensionFileFilterArray).flatMap(ExtensionFileFilter::extensions).toArray(String[]::new));
    }
}

