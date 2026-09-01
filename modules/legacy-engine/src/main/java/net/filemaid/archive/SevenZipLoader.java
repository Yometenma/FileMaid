package net.filemaid.archive;

import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.SevenZipNativeInitializationException;

public class SevenZipLoader {
    private static boolean nativeLibrariesLoaded = false;

    private static synchronized void requireNativeLibraries() throws SevenZipNativeInitializationException {
        if (nativeLibrariesLoaded) {
            return;
        }
        try {
            System.loadLibrary("7-Zip-JBinding");
            SevenZip.initLoadedLibraries();
            nativeLibrariesLoaded = true;
        }
        catch (Throwable throwable) {
            throw new SevenZipNativeInitializationException("Failed to load 7z-JBinding: " + throwable.getMessage(), throwable);
        }
    }

    public static String getNativeVersion() throws SevenZipNativeInitializationException {
        SevenZipLoader.requireNativeLibraries();
        return SevenZip.getSevenZipVersion().version;
    }

    public static IInArchive open(IInStream iInStream, IArchiveOpenCallback iArchiveOpenCallback) throws SevenZipException, SevenZipNativeInitializationException {
        SevenZipLoader.requireNativeLibraries();
        return iArchiveOpenCallback == null ? SevenZip.openInArchive(null, (IInStream)iInStream) : SevenZip.openInArchive(null, (IInStream)iInStream, (IArchiveOpenCallback)iArchiveOpenCallback);
    }
}

