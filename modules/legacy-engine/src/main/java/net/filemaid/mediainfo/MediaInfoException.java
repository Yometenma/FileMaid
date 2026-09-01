package net.filemaid.mediainfo;

import com.sun.jna.Platform;

public class MediaInfoException
extends RuntimeException {
    public MediaInfoException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public MediaInfoException(LinkageError linkageError) {
        this(MediaInfoException.getLinkageErrorMessage(linkageError), linkageError);
    }

    private static String getLinkageErrorMessage(LinkageError linkageError) {
        String string = Platform.isWindows() ? "MediaInfo.dll" : (Platform.isMac() ? "libmediainfo.dylib" : "libmediainfo.so");
        String string2 = System.getProperty("os.arch");
        String string3 = Platform.is64Bit() ? "64-bit" : "32-bit";
        return String.format("Unable to load %s (%s) native library %s: %s", string2, string3, string, linkageError.getMessage());
    }
}

