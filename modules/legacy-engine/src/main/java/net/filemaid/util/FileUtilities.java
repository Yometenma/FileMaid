package net.filemaid.util;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.ApplicationFolder;
import net.filemaid.Logging;
import net.filemaid.platform.windows.WinFile;
import net.filemaid.util.AlphanumComparator;
import net.filemaid.util.BOM;
import net.filemaid.util.ByteBufferInputStream;
import net.filemaid.util.FileNameComparator;
import net.filemaid.util.RegularExpressions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public final class FileUtilities {
    public static final Pattern EXTENSION = Pattern.compile("(?<=.[.])[\\p{Alnum}-]+$");
    public static final String UNC_PREFIX = "\\\\";
    public static final boolean UNIX = File.separatorChar == '/';
    public static final int FILE_WALK_MAX_DEPTH = 64;
    public static final Pattern ILLEGAL_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|]|\\R|\\p{Cntrl}|^\\s+|\\s+$|^[.]+(?=[^.])|(?<=[^.])[.]+$|(?<=.{247}).+(?=[.][^.]+$)");
    public static final int BUFFER_SIZE = 65536;
    public static final int LARGE_BUFFER_SIZE = 0x400000;
    public static final long ONE_KILOBYTE = 1000L;
    public static final long ONE_MEGABYTE = 1000000L;
    public static final long ONE_GIGABYTE = 1000000000L;
    public static final long ONE_TERABYTE = 1000000000000L;
    public static final Pattern SYSTEM_FOLDERS = Pattern.compile("System Volume Information|[.]xattr|@eaDir|[.]@__thumb|lost[+]found|#recycle|@Recycle|@Recently-Snapshot");
    public static final Pattern XATTR_FOLDERS = Pattern.compile("[.]xattr|@eaDir");
    public static final Pattern THUMBNAIL_STORE_FILES = Pattern.compile("Thumbs[.]db|[.]DS_Store");
    public static final FileFilter FOLDERS = file -> file.isDirectory() && !FileUtilities.isSystemFolder(file);
    public static final FileFilter FILES = File::isFile;
    public static final FileFilter NOT_HIDDEN = file -> !file.isHidden() && !FileUtilities.isThumbnailStore(file);
    public static final Comparator<File> CASE_INSENSITIVE_PATH_ORDER = Comparator.comparing(File::getPath, String.CASE_INSENSITIVE_ORDER);
    public static final Comparator<File> HUMAN_NAME_ORDER = new FileNameComparator(AlphanumComparator.getInstance());

    public static File move(File file, File file2) throws IOException {
        if (FileUtilities.sameFile(file, file2)) {
            if (!file.getName().equals(file2.getName())) {
                try {
                    return Files.move(file.toPath(), file2.toPath(), StandardCopyOption.ATOMIC_MOVE).toFile();
                }
                catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
                    Logging.debug.warning(Logging.cause(StandardCopyOption.ATOMIC_MOVE, atomicMoveNotSupportedException));
                }
            }
            return file2;
        }
        if (file.isDirectory()) {
            FileUtils.moveDirectory((File)file, (File)file2);
            return file2;
        }
        return Files.move(file.toPath(), file2.toPath(), StandardCopyOption.REPLACE_EXISTING).toFile();
    }

    public static File rename(File file, String string) throws IOException {
        File file2 = new File(file.getParentFile(), string);
        if (file2.exists() && !FileUtilities.sameFile(file, file2)) {
            throw new FileAlreadyExistsException(string);
        }
        return Files.move(file.toPath(), file2.toPath(), StandardCopyOption.ATOMIC_MOVE).toFile();
    }

    public static File copy(File file, File file2) throws IOException {
        if (file.isDirectory()) {
            FileUtils.copyDirectory((File)file, (File)file2);
            return file2;
        }
        return Files.copy(file.toPath(), file2.toPath(), StandardCopyOption.REPLACE_EXISTING).toFile();
    }

    public static File resolveSibling(File file, File file2) {
        if (file2.isAbsolute()) {
            return file2;
        }
        return new File(file.getParentFile(), file2.getPath());
    }

    public static File relativize(File file, File file2) {
        try {
            return file.toPath().relativize(file2.toPath()).toFile();
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Unable to relativize file path", file2, exception));
            return file2;
        }
    }

    public static File symlink(File file, File file2) throws IOException {
        return Files.createSymbolicLink(file2.toPath(), file.toPath(), new FileAttribute[0]).toFile();
    }

    public static File hardlink(File file3, File file4) throws IOException {
        return FileUtilities.mirror(file3, file4, (file, file2) -> Files.createLink(file2.toPath(), file.toPath()).toFile());
    }

    public static File mirror(File file, File file2, final FileOperation fileOperation) throws IOException {
        if (file.isDirectory()) {
            final Path path = file.toPath();
            final Path path2 = file2.toPath();
            Files.walkFileTree(path, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 64, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

                @Override
                public FileVisitResult preVisitDirectory(Path path3, BasicFileAttributes basicFileAttributes) throws IOException {
                    Files.createDirectories(path2.resolve(path.relativize(path3)), new FileAttribute[0]);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path3, BasicFileAttributes basicFileAttributes) throws IOException {
                    fileOperation.apply(path3.toFile(), path2.resolve(path.relativize(path3)).toFile());
                    return FileVisitResult.CONTINUE;
                }
            });
            return path2.toFile();
        }
        return fileOperation.apply(file, file2);
    }

    public static boolean existsNoFollowLinks(File file) {
        return Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS);
    }

    public static boolean isWritable(File file) {
        return Files.isWritable(file.toPath());
    }

    public static void delete(File file) throws IOException {
        if (file.isDirectory()) {
            Files.walkFileTree(file.toPath(), (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException iOException) throws IOException {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.delete(file.toPath());
        }
    }

    public static void createFolders(File file) throws IOException {
        if (Files.notExists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.createDirectories(file.toPath(), new FileAttribute[0]);
            }
            catch (Exception exception) {
                throw new IOException("Cannot create parent folder: " + file + ": " + Logging.cause(exception));
            }
        }
    }

    public static File createFile(File file) throws IOException {
        if (file.isFile()) {
            return file;
        }
        FileUtilities.createFolders(file.getParentFile());
        return Files.createFile(file.toPath(), new FileAttribute[0]).toFile();
    }

    public static Instant getCreationDate(File file) throws IOException {
        BasicFileAttributes basicFileAttributes = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class, new LinkOption[0]).readAttributes();
        Instant instant = basicFileAttributes.creationTime().toInstant();
        if (instant.isAfter(Instant.EPOCH)) {
            return instant;
        }
        return basicFileAttributes.lastModifiedTime().toInstant();
    }

    public static Object getFileKey(File file) {
        try {
            if (UNIX) {
                return Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class, new LinkOption[0]).readAttributes().fileKey();
            }
            if (!FileUtilities.isUNC(file) && file.isFile()) {
                return WinFile.getFileKey(file);
            }
        }
        catch (Exception exception) {
            Logging.debug.finest(Logging.cause(file, exception));
        }
        return null;
    }

    public static int getLinkCount(File file) throws IOException {
        try {
            if (UNIX) {
                return (Integer)Files.getAttribute(file.toPath(), "unix:nlink", new LinkOption[0]);
            }
            if (!FileUtilities.isUNC(file) && file.isFile()) {
                return WinFile.getLinkCount(file);
            }
        }
        catch (Exception exception) {
            Logging.debug.finest(Logging.cause(file, exception));
        }
        return 0;
    }

    public static long getDiskSpace(File file) {
        for (File file2 = file; file2 != null; file2 = file2.getParentFile()) {
            long l = file2.getUsableSpace();
            if (l <= 0L) continue;
            return l;
        }
        return 0L;
    }

    public static File getMountPoint(File file) throws IOException {
        if (UNIX) {
            Path path = file.getCanonicalFile().toPath();
            FileStore fileStore = Files.getFileStore(path);
            UserPrincipal userPrincipal = Files.getOwner(path, new LinkOption[0]);
            Path path2 = ApplicationFolder.UserHome.getDirectory().toPath();
            while (path != null) {
                Path path3 = path.getParent();
                if (path2.equals(path3)) {
                    return path2.toFile();
                }
                if (path3.endsWith("gvfs") || !fileStore.equals(Files.getFileStore(path3)) || !userPrincipal.equals(Files.getOwner(path3, new LinkOption[0]))) {
                    if (Files.isDirectory(path, new LinkOption[0])) {
                        return path.toFile();
                    }
                    return path3.toFile();
                }
                path = path3;
            }
        } else if (FileUtilities.isUNC(file)) {
            List<File> list = FileUtilities.listPath(file);
            if (list.size() > 0) {
                return list.get(0);
            }
        } else {
            List<File> list = FileUtilities.listPath(file.getCanonicalFile());
            if (list.size() > 0) {
                File file2 = ApplicationFolder.UserHome.getDirectory();
                if (list.contains(file2)) {
                    return file2;
                }
                return list.get(0);
            }
        }
        throw new IOException("No mount point: " + file);
    }

    public static File getRealPath(File file) {
        try {
            if (Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                return file.toPath().toRealPath(new LinkOption[0]).toFile();
            }
            return file.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS).toFile();
        }
        catch (Exception exception) {
            Logging.debug.finest(Logging.cause(file, exception));
            return file.getAbsoluteFile();
        }
    }

    public static FileStore getFileStore(File file) {
        try {
            for (Path path = file.toPath(); path != null; path = path.getParent()) {
                if (!Files.exists(path, new LinkOption[0])) continue;
                return Files.getFileStore(path);
            }
        }
        catch (Exception exception) {
            Logging.debug.finest(Logging.cause(file, exception));
        }
        return null;
    }

    public static byte[] readFile(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    public static String readTextFile(File file) throws IOException {
        long l = file.length();
        if (l > 1000000000L) {
            throw new IOException("Plain text file is too large: " + file + " (" + FileUtilities.formatSize(l) + ")");
        }
        byte[] byArray = FileUtilities.readFile(file);
        BOM bOM = BOM.detect(byArray);
        if (bOM != null) {
            return new String(byArray, bOM.size(), byArray.length - bOM.size(), bOM.getCharset());
        }
        return new String(byArray, StandardCharsets.UTF_8);
    }

    public static List<String> readLines(File file) throws IOException {
        return Arrays.asList(RegularExpressions.NEWLINE.split(FileUtilities.readTextFile(file)));
    }

    public static File writeFile(ByteBuffer byteBuffer, File file) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);){
            fileChannel.write(byteBuffer);
        }
        return file;
    }

    public static File writeFile(byte[] byArray, File file) throws IOException {
        return Files.write(file.toPath(), byArray, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE).toFile();
    }

    public static Reader newTextFileReader(InputStream inputStream, boolean bl, Charset charset) throws IOException {
        byte[] byArray = new byte[4];
        inputStream.mark(byArray.length);
        inputStream.read(byArray);
        inputStream.reset();
        BOM bOM = BOM.detect(byArray);
        if (bOM != null) {
            inputStream.skip(bOM.size());
            return new InputStreamReader(inputStream, bOM.getCharset());
        }
        if (bl) {
            CharsetDetector charsetDetector = new CharsetDetector();
            charsetDetector.setDeclaredEncoding(charset.name());
            charsetDetector.setText(inputStream);
            CharsetMatch[] charsetMatchArray = charsetDetector.detectAll();
            if (charsetMatchArray != null && charsetMatchArray.length > 0) {
                CharsetMatch charsetMatch2 = charsetMatchArray[0];
                CharsetMatch charsetMatch3 = charsetMatch2.getConfidence() >= 50 ? charsetMatch2 : Stream.of(charsetMatchArray).filter(charsetMatch -> charsetMatch.getConfidence() >= 15 && charsetMatch.getName().equalsIgnoreCase(charset.name())).findFirst().orElse(charsetMatch2);
                Reader reader = charsetMatch3.getReader();
                if (reader != null) {
                    return reader;
                }
                switch (charsetMatch3.getName()) {
                    case "ISO-8859-8-I": {
                        return new InputStreamReader(inputStream, Charset.forName("ISO-8859-8"));
                    }
                }
                Logging.debug.warning(Logging.message("Unsupported charset", charsetMatch3.getName()));
            }
        }
        return new InputStreamReader(inputStream, charset);
    }

    public static Reader newTextFileReader(File file, boolean bl, Charset charset) throws IOException {
        return FileUtilities.newTextFileReader(new BufferedInputStream(new FileInputStream(file), 65536), bl, charset);
    }

    public static String decodeTextContent(ByteBuffer byteBuffer, boolean bl, Charset charset) throws IOException {
        try (Reader reader = FileUtilities.newTextFileReader(new ByteBufferInputStream(byteBuffer), bl, charset);){
            String string = IOUtils.toString((Reader)reader);
            return string;
        }
    }

    public static boolean sameFile(File file, File file2) {
        try {
            return Files.isSameFile(file.toPath(), file2.toPath());
        }
        catch (NoSuchFileException noSuchFileException) {
            return false;
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(exception));
            return file.equals(file2);
        }
    }

    public static boolean sameFileStore(File file, File file2) {
        FileStore fileStore;
        FileStore fileStore2 = FileUtilities.getFileStore(file);
        if (fileStore2 != null && (fileStore = FileUtilities.getFileStore(file2)) != null) {
            return fileStore2.equals(fileStore);
        }
        return false;
    }

    public static boolean equalsFileContent(File file, File file2) {
        if (file.length() != file2.length()) {
            return false;
        }
        if (file.isFile() && file2.isFile()) {
            try {
                return FileUtilities.mismatch(file, file2) < 0L;
            }
            catch (Exception exception) {
                Logging.trace(exception);
            }
        }
        return false;
    }

    public static long mismatch(File file, File file2) throws Exception {
        if (Files.isSameFile(file.toPath(), file2.toPath())) {
            return -1L;
        }
        byte[] byArray = new byte[0x400000];
        byte[] byArray2 = new byte[0x400000];
        try (InputStream inputStream = Files.newInputStream(file.toPath(), new OpenOption[0]);
             InputStream inputStream2 = Files.newInputStream(file2.toPath(), new OpenOption[0]);){
            long l = 0L;
            while (true) {
                int n;
                int n2;
                int n3;
                if ((n3 = Arrays.mismatch(byArray, 0, n2 = inputStream.readNBytes(byArray, 0, 0x400000), byArray2, 0, n = inputStream2.readNBytes(byArray2, 0, 0x400000))) > -1) {
                    long l2 = l + (long)n3;
                    return l2;
                }
                if (n2 < 0x400000) {
                    long l3 = -1L;
                    return l3;
                }
                l += (long)n2;
            }
        }
    }

    public static boolean equalsLastModified(File file, File file2, int n) {
        return file.lastModified() / (long)n == file2.lastModified() / (long)n;
    }

    public static Integer getUID(File file) throws IOException {
        if (UNIX) {
            return (Integer)Files.getAttribute(file.toPath(), "unix:uid", new LinkOption[0]);
        }
        return null;
    }

    public static Integer getGID(File file) throws IOException {
        if (UNIX) {
            return (Integer)Files.getAttribute(file.toPath(), "unix:gid", new LinkOption[0]);
        }
        return null;
    }

    public static String getPermissions(File file) throws IOException {
        if (UNIX) {
            return PosixFilePermissions.toString(Files.getPosixFilePermissions(file.toPath(), new LinkOption[0]));
        }
        return null;
    }

    public static boolean isUNC(File file) {
        return !UNIX && file.getPath().startsWith(UNC_PREFIX);
    }

    public static boolean isNetworkShareRoot(File file) {
        return !UNIX && FileUtilities.isUNC(file) && (file.getParentFile() == null || file.getParentFile().getParentFile() == null || file.getParentFile().getParentFile().getParentFile() == null);
    }

    public static boolean isNetworkDrive(File file) {
        if (UNIX) {
            return Stream.of("smbfs", "afpfs", "cifs", "nfs", "sshfs", "shfs", "rclone", "ceph").anyMatch(FileUtilities.getFileStore(file).type()::contains);
        }
        return FileUtilities.isUNC(file) || WinFile.getDriveType(file.getParent() == null ? file : FileUtilities.listPath(file).get(0)) == 4;
    }

    public static String getExtension(File file) {
        if (file.isAbsolute() && file.isDirectory()) {
            return null;
        }
        return FileUtilities.getExtension(file.getName());
    }

    public static String getExtension(String string) {
        Matcher matcher;
        int n = string.lastIndexOf(46);
        if (n > 0 && (matcher = EXTENSION.matcher(string)).find(n)) {
            return matcher.group();
        }
        return null;
    }

    public static boolean hasExtension(File file, String ... stringArray) {
        return FileUtilities.hasExtension(file.getName(), stringArray) && (!file.isAbsolute() || !file.isDirectory());
    }

    public static boolean hasExtension(String string, String ... stringArray) {
        int n = string.length();
        for (String string2 : stringArray) {
            String string3;
            if (n - string2.length() < 2 || string.charAt(n - string2.length() - 1) != '.' || !(string3 = string.substring(n - string2.length(), n)).equalsIgnoreCase(string2)) continue;
            return true;
        }
        return false;
    }

    public static String getNameWithoutExtension(String string) {
        Matcher matcher;
        if (string == null) {
            return null;
        }
        int n = string.lastIndexOf(46);
        if (n > 0 && (matcher = EXTENSION.matcher(string)).find(n)) {
            return string.substring(0, matcher.start() - 1);
        }
        return string;
    }

    public static String getName(File file) {
        if (file == null) {
            return null;
        }
        if (file.isAbsolute() && file.isDirectory()) {
            return FileUtilities.getFolderName(file);
        }
        return FileUtilities.getNameWithoutExtension(file.getName());
    }

    public static String getFolderName(File file) {
        if (!UNIX && UNC_PREFIX.equals(file.getParent())) {
            return file.toString();
        }
        String string = file.getName();
        if (string.length() > 0) {
            return string;
        }
        return FileUtilities.replacePathSeparators(file.toString(), "");
    }

    public static boolean sameParentFolder(File file, File file2) {
        File file3 = file2.getParentFile();
        for (File file4 = file.getParentFile(); file4 != null; file4 = file4.getParentFile()) {
            if (!file4.equals(file3)) continue;
            return true;
        }
        return false;
    }

    public static boolean containsOnly(Collection<File> collection, FileFilter fileFilter) {
        if (collection == null || collection.isEmpty()) {
            return false;
        }
        for (File file : collection) {
            if (fileFilter.accept(file)) continue;
            return false;
        }
        return true;
    }

    public static boolean containsOnly(File file2, FileFilter fileFilter) {
        if (file2.isDirectory() && !file2.isHidden()) {
            return FileUtilities.getChildren(file2).stream().allMatch(file -> FileUtilities.containsOnly(file, fileFilter));
        }
        return fileFilter.accept(file2);
    }

    public static boolean lastModifiedWithin(File file, Duration duration) {
        return System.currentTimeMillis() - file.lastModified() < duration.toMillis();
    }

    public static Duration getFileAge(File file) {
        return Duration.ofMillis(System.currentTimeMillis() - file.lastModified());
    }

    public static List<File> filter(Iterable<File> iterable, FileFilter ... fileFilterArray) {
        ArrayList<File> arrayList = new ArrayList<File>();
        block0: for (File file : iterable) {
            for (FileFilter fileFilter : fileFilterArray) {
                if (!fileFilter.accept(file)) continue;
                arrayList.add(file);
                continue block0;
            }
        }
        return arrayList;
    }

    public static FileFilter not(FileFilter fileFilter) {
        return file -> !fileFilter.accept(file);
    }

    public static FileFilter filter(FileFilter ... fileFilterArray) {
        return file -> Arrays.stream(fileFilterArray).filter(Objects::nonNull).anyMatch(fileFilter -> fileFilter.accept(file));
    }

    public static List<File> listPath(File file) {
        ArrayDeque<File> arrayDeque = new ArrayDeque<File>();
        for (File file2 = file; file2 != null; file2 = file2.getParentFile()) {
            arrayDeque.addFirst(file2);
        }
        if (FileUtilities.isUNC(file) && arrayDeque.size() > 2) {
            arrayDeque.removeFirst();
            arrayDeque.removeFirst();
        }
        return new ArrayList<File>(arrayDeque);
    }

    public static List<File> listPathTail(File file, int n) {
        ArrayDeque<File> arrayDeque = new ArrayDeque<File>(n);
        File file2 = file;
        for (int i = 0; file2 != null && i < n; ++i, file2 = file2.getParentFile()) {
            arrayDeque.addFirst(file2);
        }
        if (FileUtilities.isUNC(file) && (((File)arrayDeque.peekFirst()).getParentFile() == null || ((File)arrayDeque.peekFirst()).getParentFile().getParentFile() == null)) {
            arrayDeque.removeFirst();
            if (((File)arrayDeque.peekFirst()).getParentFile() == null) {
                arrayDeque.removeFirst();
            }
        }
        return new ArrayList<File>(arrayDeque);
    }

    public static List<File> listPathTailReverse(File file) {
        return FileUtilities.listPathTailReverse(file, 64);
    }

    public static List<File> listPathTailReverse(File file, int n) {
        ArrayList<File> arrayList = new ArrayList<File>(n);
        File file2 = file;
        for (int i = 0; file2 != null && i < n; ++i, file2 = file2.getParentFile()) {
            arrayList.add(file2);
        }
        if (FileUtilities.isUNC(file) && (arrayList.get(arrayList.size() - 1).getParentFile() == null || arrayList.get(arrayList.size() - 1).getParentFile().getParentFile() == null)) {
            arrayList.remove(arrayList.size() - 1);
            if (arrayList.get(arrayList.size() - 1).getParentFile() == null) {
                arrayList.remove(arrayList.size() - 1);
            }
        }
        return arrayList;
    }

    public static File getRelativePathTail(File file3, int n) {
        return FileUtilities.listPathTail(file3, n).stream().reduce(null, (file, file2) -> file == null && file2.getName().isEmpty() ? null : new File((File)file, file2.getName()));
    }

    public static List<File> getFileSystemRoots() {
        File[] fileArray = File.listRoots();
        if (fileArray == null) {
            fileArray = new File[]{};
        }
        return Arrays.asList(fileArray);
    }

    public static List<File> getChildren(File file) {
        return FileUtilities.getChildren(file, null, null);
    }

    public static List<File> getChildren(File file, FileFilter fileFilter) {
        return FileUtilities.getChildren(file, fileFilter, null);
    }

    public static List<File> getChildren(File file, FileFilter fileFilter, Comparator<File> comparator) {
        File[] fileArray;
        File[] fileArray2 = fileArray = fileFilter == null ? file.listFiles() : file.listFiles(fileFilter);
        if (fileArray == null) {
            Logging.help(Logging.message("Permission denied", file));
            return Collections.emptyList();
        }
        if (fileArray.length == 0) {
            return Collections.emptyList();
        }
        if (comparator != null) {
            Arrays.sort(fileArray, comparator);
        }
        return Arrays.asList(fileArray);
    }

    public static List<File> listFiles(File file, FileFilter fileFilter) {
        return FileUtilities.listFiles(Collections.singleton(file), fileFilter, null, File::isDirectory, 64);
    }

    public static List<File> listFiles(File file, FileFilter fileFilter, Comparator<File> comparator) {
        return FileUtilities.listFiles(Collections.singleton(file), fileFilter, comparator, File::isDirectory, 64);
    }

    public static List<File> listFiles(Collection<File> collection, FileFilter fileFilter, Comparator<File> comparator) {
        return FileUtilities.listFiles(collection, fileFilter, comparator, File::isDirectory, 64);
    }

    public static List<File> listFiles(Collection<File> collection, FileFilter fileFilter, Comparator<File> comparator, FileFilter fileFilter2, int n) {
        ArrayList<File> arrayList = new ArrayList<File>(64);
        for (File file : collection) {
            if (fileFilter2.accept(file)) {
                FileUtilities.listFiles(file, arrayList, fileFilter, comparator, fileFilter2, n - 1);
            }
            if (!fileFilter.accept(file)) continue;
            arrayList.add(file);
        }
        return arrayList;
    }

    private static void listFiles(File file, List<File> list, FileFilter fileFilter, Comparator<File> comparator, FileFilter fileFilter2, int n) {
        if (n < 0) {
            return;
        }
        List<File> list2 = FileUtilities.getChildren(file, NOT_HIDDEN, comparator);
        ArrayList arrayList = new ArrayList(list2.size());
        for (File file2 : list2) {
            if (fileFilter2.accept(file2)) {
                FileUtilities.listFiles(file2, list, fileFilter, comparator, fileFilter2, n - 1);
            }
            if (!fileFilter.accept(file2)) continue;
            list.add(file2);
        }
        list.addAll(arrayList);
    }

    public static Map<File, List<File>> mapByFolder(Iterable<File> iterable) {
        LinkedHashMap<File, List<File>> linkedHashMap = new LinkedHashMap<File, List<File>>();
        for (File file2 : iterable) {
            File file3 = file2.getParentFile();
            if (file3 == null) continue;
            linkedHashMap.computeIfAbsent(file3, file -> new ArrayList()).add(file2);
        }
        return linkedHashMap;
    }

    public static Map<String, List<File>> mapByExtension(Iterable<File> iterable) {
        LinkedHashMap<String, List<File>> linkedHashMap = new LinkedHashMap<String, List<File>>();
        for (File file : iterable) {
            ArrayList<File> arrayList;
            String string = FileUtilities.getExtension(file);
            if (string != null) {
                string = string.toLowerCase(Locale.ROOT);
            }
            if ((arrayList = (ArrayList<File>)linkedHashMap.get(string)) == null) {
                arrayList = new ArrayList<File>();
                linkedHashMap.put(string, arrayList);
            }
            arrayList.add(file);
        }
        return linkedHashMap;
    }

    public static boolean isInvalidFileName(String string) {
        return ILLEGAL_CHARACTERS.matcher(string).find();
    }

    public static String validateFileName(String string) {
        while (ILLEGAL_CHARACTERS.matcher(string).find()) {
            string = ILLEGAL_CHARACTERS.matcher(string).replaceAll("").trim();
        }
        return string;
    }

    public static boolean isInvalidFilePathComponent(File file) {
        if (FileUtilities.isInvalidFileName(file.getName())) {
            try {
                return !Files.exists(file.toPath(), new LinkOption[0]);
            }
            catch (InvalidPathException invalidPathException) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInvalidFilePath(File file2) {
        return FileUtilities.listPath(file2).stream().anyMatch(file -> FileUtilities.isInvalidFilePathComponent(file));
    }

    public static File validateFilePath(File file) {
        File file2 = null;
        boolean bl = false;
        for (File file3 : FileUtilities.listPath(file)) {
            if (bl || FileUtilities.isInvalidFilePathComponent(file3)) {
                bl = true;
            }
            if (bl) {
                String string = FileUtilities.validateFileName(file3.getName());
                if (string.isEmpty()) continue;
                file2 = new File(file2, string);
                continue;
            }
            file2 = file3;
        }
        return file2;
    }

    public static String normalizePathSeparators(String string) {
        if (!UNIX && string.startsWith(UNC_PREFIX)) {
            String[] stringArray = RegularExpressions.SLASH.split(string, 3);
            if (stringArray.length < 3) {
                return string;
            }
            return UNC_PREFIX + stringArray[1] + UNC_PREFIX.charAt(0) + FileUtilities.replacePathSeparators(stringArray[2], "/");
        }
        return FileUtilities.replacePathSeparators(string, "/");
    }

    public static String stripPathSeparators(CharSequence charSequence) {
        String string = FileUtilities.replacePathSeparators(charSequence, " ");
        return RegularExpressions.COLON.matcher(string).replaceAll(" ");
    }

    public static String replacePathSeparators(CharSequence charSequence, String string) {
        return RegularExpressions.SLASH.matcher(charSequence).replaceAll(string);
    }

    public static String abbreviatePath(File file) {
        String string;
        Object object = file.getPath();
        if (((String)object).startsWith(string = ApplicationFolder.UserHome.getDirectory().getPath()) && ((String)object).startsWith(File.separator, string.length())) {
            object = "~" + ((String)object).substring(string.length());
        }
        return FileUtilities.normalizePathSeparators((String)object);
    }

    public static List<File> asFileList(Object ... objectArray) {
        return Arrays.stream(objectArray).flatMap(object -> {
            if (object == null) {
                return Stream.empty();
            }
            if (object instanceof Collection) {
                return FileUtilities.asFileList(((Collection)object).toArray()).stream();
            }
            if (object instanceof File) {
                return Stream.of((File)object);
            }
            return Stream.of(new File(object.toString()));
        }).collect(Collectors.toList());
    }

    public static String formatSize(long l) {
        if (l >= 5000000000000L) {
            return String.format("%,d TB", l / 1000000000000L);
        }
        if (l >= 1000000000000L) {
            return String.format("%.1f TB", (double)l / 1.0E12);
        }
        if (l >= 5000000000L) {
            return String.format("%,d GB", l / 1000000000L);
        }
        if (l >= 1000000000L) {
            return String.format("%.1f GB", (double)l / 1.0E9);
        }
        if (l >= 5000000L) {
            return String.format("%,d MB", l / 1000000L);
        }
        if (l >= 1000000L) {
            return String.format("%.1f MB", (double)l / 1000000.0);
        }
        if (l >= 1000L) {
            return String.format("%,d KB", l / 1000L);
        }
        return String.format("%,d bytes", l);
    }

    public static void checkDiskSpace(File file) throws IOException {
        long l = FileUtilities.getDiskSpace(file);
        if (l < 5000000000L) {
            String string = FileUtilities.formatSize(l);
            FileStore fileStore = FileUtilities.getFileStore(file);
            String string2 = fileStore != null ? fileStore.toString() : file.getParentFile().getPath();
            Logging.log.warning(Logging.format("Low Disk Space (%s) on %s", string, string2));
            if (l < 500000000L) {
                throw new IOException(String.format(Locale.ROOT, "Low Disk Space (%s): A minimum of 500 MB of free disk space is required: %s", string, file));
            }
        }
    }

    public static boolean isSystemFolder(File file) {
        return SYSTEM_FOLDERS.matcher(file.getName()).matches();
    }

    public static boolean isXattrFolder(File file) {
        return XATTR_FOLDERS.matcher(file.getName()).matches();
    }

    public static boolean isThumbnailStore(File file) {
        return THUMBNAIL_STORE_FILES.matcher(file.getName()).matches();
    }

    public static FileFilter newParentFilter(File file2) {
        String string = file2.getPath() + File.separator;
        return file -> file.getPath().startsWith(string);
    }

    public static FileFilter newRegexMatchFilter(Pattern pattern) {
        return file -> pattern.matcher(file.getName()).matches();
    }

    public static FileFilter newRegexFindFilter(Pattern pattern) {
        return file -> pattern.matcher(file.getName()).find();
    }

    private FileUtilities() {
        throw new UnsupportedOperationException();
    }

    @FunctionalInterface
    public static interface FileOperation {
        public File apply(File var1, File var2) throws IOException;
    }
}

