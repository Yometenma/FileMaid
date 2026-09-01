package net.filemaid.archive;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.MediaTypes;
import net.filemaid.archive.ArchiveExtractor;
import net.filemaid.archive.SevenZipNativeBindings;
import net.filemaid.archive.ShellExecutables;
import net.filemaid.util.StringUtilities;
import net.filemaid.util.SystemProperty;
import net.filemaid.vfs.FileInfo;
import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.SevenZipNativeInitializationException;

public class Archive
implements Closeable {
    private final ArchiveExtractor extractor;
    private static final Pattern VOLUMED_ARCHIVE = Pattern.compile("[.][0-9]{3}$");
    private static final Pattern MULTI_PART_ARCHIVE = Pattern.compile("[.]part[0-9]+[.]rar|[.]r[0-9]+$|[.][0-9]+$", 2);

    public static Extractor getExtractor() {
        return SystemProperty.get("net.filemaid.archive.extractor", Extractor::valueOf, Extractor.SevenZipNativeBindings);
    }

    public static Archive open(File file) throws IOException {
        return new Archive(Archive.getExtractor().newInstance(file));
    }

    public Archive(ArchiveExtractor archiveExtractor) {
        this.extractor = archiveExtractor;
    }

    public List<FileInfo> listFiles() throws IOException {
        return this.extractor.listFiles();
    }

    public void extract(File file) throws IOException {
        this.extractor.extract(file);
    }

    public void extract(File file, FileFilter fileFilter) throws IOException {
        this.extractor.extract(file, fileFilter);
    }

    @Override
    public void close() throws IOException {
        if (this.extractor instanceof Closeable) {
            ((Closeable)((Object)this.extractor)).close();
        }
    }

    public static boolean isVolumedArchive(File file) {
        return VOLUMED_ARCHIVE.matcher(file.getName()).find();
    }

    public static boolean isArchive(File file) {
        if (MediaTypes.ARCHIVE_FILES.accept(file) || Archive.isVolumedArchive(file)) {
            Matcher matcher = MULTI_PART_ARCHIVE.matcher(file.getName());
            if (matcher.find()) {
                int n = StringUtilities.matchInteger(matcher.group());
                return n == 1;
            }
            return true;
        }
        return false;
    }

    public static boolean isArchivePart(File file) {
        return MediaTypes.ARCHIVE_FILES.accept(file) || MULTI_PART_ARCHIVE.matcher(file.getName()).find();
    }

    public static enum Extractor {
        SevenZipNativeBindings{

            @Override
            public ArchiveExtractor newInstance(File file) throws IOException {
                try {
                    return new SevenZipNativeBindings(file);
                }
                catch (SevenZipNativeInitializationException sevenZipNativeInitializationException) {
                    throw new RuntimeException(sevenZipNativeInitializationException);
                }
            }
        }
        ,
        ShellExecutables{

            @Override
            public ArchiveExtractor newInstance(File file) throws IOException {
                return new ShellExecutables(file);
            }
        };


        public abstract ArchiveExtractor newInstance(File var1) throws IOException;

        public String[] getSupportedTypes() {
            return (String[])Arrays.stream(ArchiveFormat.values()).map(ArchiveFormat::getMethodName).toArray(String[]::new);
        }
    }
}

