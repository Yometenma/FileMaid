package net.filemaid.cli;

import groovy.lang.Closure;
import groovy.lang.Range;
import groovy.xml.StreamingMarkupBuilder;
import groovy.xml.XmlSlurper;
import groovy.xml.slurpersupport.GPathResult;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.MetaAttributeView;
import net.filemaid.RenameAction;
import net.filemaid.StandardRenameAction;
import net.filemaid.UserFiles;
import net.filemaid.cli.FolderWatchService;
import net.filemaid.format.AssociativeEnumObject;
import net.filemaid.format.ExpressionFormat;
import net.filemaid.format.FileSize;
import net.filemaid.format.MediaBindingBean;
import net.filemaid.hash.VerificationUtilities;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.media.FFProbe;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.media.MediaInfoTable;
import net.filemaid.media.VideoQuality;
import net.filemaid.media.XattrChecksum;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.similarity.NameSimilarityMetric;
import net.filemaid.similarity.Normalization;
import net.filemaid.similarity.SimilarityComparator;
import net.filemaid.util.ByteBufferInputStream;
import net.filemaid.util.ByteBufferOutputStream;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.JsonUtilities;
import net.filemaid.util.XZ;
import net.filemaid.util.XmlUtilities;
import net.filemaid.web.Episode;
import net.filemaid.web.Movie;
import net.filemaid.web.OpenSubtitlesHasher;
import net.filemaid.web.WebRequest;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.imgscalr.Scalr;

public class ScriptShellMethods {
    public static String getAt(File file, int n) {
        List<File> list = FileUtilities.listPath(file);
        File file2 = (File)DefaultGroovyMethods.getAt(list, (int)n);
        return file2 == null ? null : (n == 0 ? file2.getPath() : file2.getName());
    }

    public static File getAt(File file3, Range range) {
        List<File> list = FileUtilities.listPath(file3);
        return DefaultGroovyMethods.getAt(list, (Range)range).stream().reduce(null, (file, file2) -> file == null && file2 == list.get(0) ? file2 : new File((File)file, file2.getName()));
    }

    public static File getAt(File file3, Collection collection) {
        List<File> list = FileUtilities.listPath(file3);
        return DefaultGroovyMethods.getAt(list, (Collection)collection).stream().reduce(null, (file, file2) -> file == null && file2 == list.get(0) ? file2 : new File((File)file, file2.getName()));
    }

    public static File resolve(File file, String string) {
        File file2 = new File(string);
        if (file2.isAbsolute()) {
            return file2;
        }
        return new File(file, file2.getPath());
    }

    public static File resolveSibling(File file, String string) {
        return ScriptShellMethods.resolve(file.getParentFile(), string);
    }

    public static File findSibling(File file2, Closure<?> closure) throws Exception {
        return MediaFileUtilities.findSiblingFiles(file2, file -> DefaultTypeTransformation.castToBoolean((Object)closure.call((Object)file))).stream().findFirst().orElse(null);
    }

    public static List<File> listFiles(File file2, Closure<?> closure) {
        return FileUtilities.getChildren(file2, file -> DefaultTypeTransformation.castToBoolean((Object)closure.call((Object)file)), FileUtilities.HUMAN_NAME_ORDER);
    }

    public static boolean isVideo(File file) {
        return MediaTypes.VIDEO_FILES.accept(file);
    }

    public static boolean isAudio(File file) {
        return MediaTypes.AUDIO_FILES.accept(file);
    }

    public static boolean isSubtitle(File file) {
        return MediaTypes.SUBTITLE_FILES.accept(file);
    }

    public static boolean isVerification(File file) {
        return MediaTypes.VERIFICATION_FILES.accept(file);
    }

    public static boolean isArchive(File file) {
        return MediaTypes.ARCHIVE_FILES.accept(file);
    }

    public static boolean isImage(File file) {
        return MediaTypes.IMAGE_FILES.accept(file);
    }

    public static boolean isDisk(File file) {
        if (MediaFileUtilities.isDiskFolder(file)) {
            return true;
        }
        if (file.isFile() && MediaTypes.ISO.accept(file)) {
            try {
                return MediaFileUtilities.isVideoDiskFile(file);
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("Failed to read disk image", exception));
            }
        }
        return false;
    }

    public static boolean isClutter(File file) {
        return MediaFileUtilities.CLUTTER_TYPES.accept(file) || MediaFileUtilities.EXTRA_FOLDERS.accept(file) || MediaFileUtilities.EXTRA_FILES.accept(file);
    }

    public static boolean isSystem(File file) {
        return MediaFileUtilities.SYSTEM_EXCLUDES.accept(file);
    }

    public static boolean isSymlink(File file) {
        return Files.isSymbolicLink(file.toPath());
    }

    public static Object getAttribute(File file, String string) throws IOException {
        return Files.getAttribute(file.toPath(), string, new LinkOption[0]);
    }

    public static Object getKey(File file) throws IOException {
        return FileUtilities.getFileKey(file.getCanonicalFile());
    }

    public static int getLinkCount(File file) throws IOException {
        return FileUtilities.getLinkCount(file);
    }

    public static boolean isNetworkDrive(File file) {
        return FileUtilities.isNetworkDrive(file);
    }

    public static long getCreationDate(File file) throws IOException {
        return FileUtilities.getCreationDate(file).toEpochMilli();
    }

    public static File getRealPath(File file) {
        return FileUtilities.getRealPath(file);
    }

    public static List<File> getChildren(File file) {
        return FileUtilities.getChildren(file, FileUtilities.NOT_HIDDEN, FileUtilities.HUMAN_NAME_ORDER);
    }

    public static File getDir(File file) {
        return file.getParentFile();
    }

    public static boolean hasFile(File file, Closure<?> closure) {
        return ScriptShellMethods.listFiles(file, closure).size() > 0;
    }

    public static List<File> getFiles(File file) {
        return ScriptShellMethods.getFiles(Collections.singleton(file), null);
    }

    public static List<File> getFiles(File file, Closure<?> closure) {
        return ScriptShellMethods.getFiles(Collections.singleton(file), closure);
    }

    public static List<File> getFiles(Collection<?> collection) {
        return ScriptShellMethods.getFiles(collection, null);
    }

    public static List<File> getFiles(Collection<?> collection, Closure<?> closure) {
        List<File> list = FileUtilities.asFileList(collection.stream().distinct().toArray());
        List list2 = FileUtilities.listFiles(list, FileUtilities.FILES, FileUtilities.HUMAN_NAME_ORDER);
        if (closure != null) {
            list2 = DefaultGroovyMethods.findAll(list2, closure);
        }
        return list2;
    }

    public static List<File> getFolders(File file) {
        return ScriptShellMethods.getFolders(file, null);
    }

    public static List<File> getFolders(File file, Closure<?> closure) {
        return ScriptShellMethods.getFolders(Collections.singletonList(file), closure);
    }

    public static List<File> getFolders(Collection<?> collection) {
        return ScriptShellMethods.getFolders(collection, null);
    }

    public static List<File> getFolders(Collection<?> collection, Closure<?> closure) {
        List<File> list = FileUtilities.asFileList(collection.toArray());
        List list2 = FileUtilities.listFiles(list, FileUtilities.FOLDERS, FileUtilities.HUMAN_NAME_ORDER);
        if (closure != null) {
            list2 = DefaultGroovyMethods.findAll(list2, closure);
        }
        return list2;
    }

    public static List<File> getMediaFolders(Collection<?> collection) throws IOException {
        final ArrayList<File> arrayList = new ArrayList<File>();
        for (File file : FileUtilities.asFileList(collection.toArray())) {
            if (file.isDirectory()) {
                Files.walkFileTree(file.toPath(), EnumSet.of(FileVisitOption.FOLLOW_LINKS), 64, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

                    @Override
                    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                        File file2 = path.toFile();
                        if (file2.isHidden() || !file2.canRead() || ScriptShellMethods.isSystem(file2) || ScriptShellMethods.isClutter(file2)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        if (FileUtilities.getChildren(file2, file -> ScriptShellMethods.isVideo(file) && !ScriptShellMethods.isClutter(file)).size() > 0 || MediaFileUtilities.isDiskFolder(file2)) {
                            arrayList.add(file2);
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
                continue;
            }
            if (file.getParentFile() == null || !ScriptShellMethods.isVideo(file) || ScriptShellMethods.isClutter(file)) continue;
            arrayList.add(file.getParentFile());
        }
        return arrayList.stream().sorted().distinct().collect(Collectors.toList());
    }

    public static void eachMediaFolder(Collection<?> collection, Closure<?> closure) throws IOException {
        DefaultGroovyMethods.each(ScriptShellMethods.getMediaFolders(collection), closure);
    }

    public static String getNameWithoutExtension(File file) {
        return FileUtilities.getNameWithoutExtension(file.getName());
    }

    public static String getNameWithoutExtension(String string) {
        return FileUtilities.getNameWithoutExtension(string);
    }

    public static String getExtension(File file) {
        return FileUtilities.getExtension(file);
    }

    public static String getExtension(String string) {
        return FileUtilities.getExtension(string);
    }

    public static boolean hasExtension(File file, String ... stringArray) {
        return FileUtilities.hasExtension(file, stringArray);
    }

    public static boolean hasExtension(String string, String ... stringArray) {
        return FileUtilities.hasExtension(string, stringArray);
    }

    public static boolean isDerived(File file, File file2) {
        return MediaFileUtilities.isDerived(file, file2) && !file.equals(file2);
    }

    public static String validateFileName(String string) {
        return FileUtilities.validateFileName(string);
    }

    public static File validateFilePath(File file) {
        return FileUtilities.validateFilePath(file);
    }

    public static float getAge(File file) throws IOException {
        return (float)(System.currentTimeMillis() - ScriptShellMethods.getCreationDate(file)) / 8.64E7f;
    }

    public static float getAgeLastModified(File file) throws IOException {
        return (float)(System.currentTimeMillis() - file.lastModified()) / 8.64E7f;
    }

    public static FileSize getSize(File file) throws IOException {
        return new FileSize(file.length());
    }

    public static String getDisplaySize(File file) {
        return FileUtilities.formatSize(file.length());
    }

    public static String getDisplaySize(Number number) {
        return FileUtilities.formatSize(number.longValue());
    }

    public static void createIfNotExists(File file) throws IOException {
        if (!file.isFile()) {
            Files.createDirectories(file.toPath().getParent(), new FileAttribute[0]);
            Files.createFile(file.toPath(), new FileAttribute[0]);
        }
    }

    public static File relativize(File file, File file2) throws IOException {
        return FileUtilities.relativize(file, file2);
    }

    public static Map<File, List<File>> mapByFolder(Collection<?> collection) {
        return FileUtilities.mapByFolder(FileUtilities.asFileList(collection.toArray()));
    }

    public static Map<String, List<File>> mapByExtension(Collection<?> collection) {
        return FileUtilities.mapByExtension(FileUtilities.asFileList(collection.toArray()));
    }

    public static String normalizePunctuation(String string) {
        return Normalization.normalizePunctuation(string);
    }

    public static String stripReleaseInfo(String string, boolean bl) {
        return MediaDetection.stripReleaseInfo(string, bl);
    }

    public static String getCRC32(File file) throws Exception {
        return XattrChecksum.CRC32.computeIfAbsent(file);
    }

    public static String hash(File file, String string) throws Exception {
        switch (string.toLowerCase(Locale.ROOT)) {
            case "moviehash": {
                return OpenSubtitlesHasher.computeHash(file);
            }
            case "crc32": {
                return VerificationUtilities.crc32(file);
            }
            case "md5": {
                return VerificationUtilities.md5(file);
            }
            case "sha256": {
                return VerificationUtilities.sha256(file);
            }
        }
        throw new UnsupportedOperationException(string);
    }

    public static long mismatch(File file, File file2) throws Exception {
        return FileUtilities.mismatch(file, file2);
    }

    public static File move(File file, File file2) throws Exception {
        return ScriptShellMethods.call(StandardRenameAction.MOVE, file, file2);
    }

    public static File copy(File file, File file2) throws Exception {
        return ScriptShellMethods.call(StandardRenameAction.COPY, file, file2);
    }

    public static File duplicate(File file, File file2) throws Exception {
        return ScriptShellMethods.call(StandardRenameAction.DUPLICATE, file, file2);
    }

    public static File call(RenameAction renameAction, File file, File file2) throws Exception {
        if ((file2 = renameAction.resolve(file, file2)).isAbsolute() && file2.isDirectory()) {
            file2 = new File(file2, file.getName());
        }
        if (renameAction.canRename(file, file2)) {
            return renameAction.rename(file, file2);
        }
        return null;
    }

    public static void trash(File file) throws Exception {
        UserFiles.trash(file);
    }

    public static URL toURL(String string, Map<?, ?> map) throws Exception {
        URI uRI = new URI(string);
        String string2 = WebRequest.encodeParameters(map);
        if (string2.isEmpty()) {
            return uRI.toURL();
        }
        if (uRI.getQuery() == null) {
            return uRI.resolve(uRI.getPath() + "?" + string2).toURL();
        }
        return uRI.resolve(uRI.getPath() + "?" + uRI.getQuery() + "&" + string2).toURL();
    }

    public static URL div(URL uRL, String string) throws Exception {
        return uRL.toURI().resolve(string).toURL();
    }

    public static String getText(ByteBuffer byteBuffer) {
        return StandardCharsets.UTF_8.decode(byteBuffer.duplicate()).toString();
    }

    public static ByteBuffer encode(String string, String string2) throws IOException {
        return Charset.forName(string2).encode(string);
    }

    public static ByteBuffer xz(ByteBuffer byteBuffer) throws IOException {
        return XZ.isXZ(byteBuffer) ? byteBuffer : XZ.xz(byteBuffer.duplicate());
    }

    public static ByteBuffer unxz(ByteBuffer byteBuffer) throws IOException {
        return XZ.isXZ(byteBuffer) ? XZ.unxz(byteBuffer.duplicate()) : byteBuffer;
    }

    public static ByteBuffer cache(URL uRL) throws Exception {
        return Cache.getConcurrentCache("url", CacheType.Monthly).url(uRL).transform(ByteBuffer::wrap).get();
    }

    public static BufferedImage getImage(URL uRL) throws Exception {
        return ScriptShellMethods.getImage(ScriptShellMethods.cache(uRL));
    }

    public static BufferedImage getImage(ByteBuffer byteBuffer) {
        try {
            return ImageIO.read(new MemoryCacheImageInputStream(new ByteBufferInputStream(byteBuffer.duplicate())));
        }
        catch (Exception exception) {
            Logging.debug.severe(Logging.cause(exception));
            return null;
        }
    }

    public static BufferedImage scale(BufferedImage bufferedImage, int n, int n2) {
        return Scalr.resize((BufferedImage)bufferedImage, (Scalr.Method)Scalr.Method.ULTRA_QUALITY, (Scalr.Mode)Scalr.Mode.AUTOMATIC, (int)n, (int)n2, (BufferedImageOp[])new BufferedImageOp[0]);
    }

    public static File saveAs(BufferedImage bufferedImage, File file) throws IOException {
        return ImageIO.write((RenderedImage)bufferedImage, ScriptShellMethods.getExtension(file), file) ? file : null;
    }

    public static ByteBuffer encode(BufferedImage bufferedImage, String string) throws IOException {
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
        return ImageIO.write((RenderedImage)bufferedImage, string, byteBufferOutputStream) ? byteBufferOutputStream.getByteBuffer() : null;
    }

    public static String base64(ByteBuffer byteBuffer) throws IOException {
        return ScriptShellMethods.getText(Base64.getEncoder().encode(byteBuffer.duplicate()));
    }

    public static ByteBuffer fetch(URL uRL) throws IOException {
        return WebRequest.fetch(uRL);
    }

    public static ByteBuffer get(URL uRL) throws IOException {
        return WebRequest.fetch(uRL);
    }

    public static ByteBuffer get(URL uRL, Map<String, String> map) throws IOException {
        return WebRequest.fetch(uRL, 0L, null, map, null);
    }

    public static ByteBuffer post(URL uRL, Map<String, ?> map, Map<String, String> map2) throws IOException {
        return WebRequest.post(uRL, map, map2);
    }

    public static ByteBuffer post(URL uRL, String string, Map<String, String> map) throws IOException {
        return WebRequest.post(uRL, string.getBytes(StandardCharsets.UTF_8), "text/plain", map);
    }

    public static ByteBuffer post(URL uRL, byte[] byArray, String string, Map<String, String> map) throws IOException {
        return WebRequest.post(uRL, byArray, string, map);
    }

    public static int head(URL uRL) throws IOException {
        return WebRequest.status("HEAD", uRL, null);
    }

    public static File saveAs(String string, String string2) throws IOException {
        return ScriptShellMethods.saveAs(StandardCharsets.UTF_8.encode(string), new File(string2));
    }

    public static File saveAs(String string, File file) throws IOException {
        return ScriptShellMethods.saveAs(StandardCharsets.UTF_8.encode(string), file);
    }

    public static File saveAs(URL uRL, String string) throws IOException {
        return ScriptShellMethods.saveAs(WebRequest.fetch(uRL), new File(string));
    }

    public static File saveAs(URL uRL, File file) throws IOException {
        return ScriptShellMethods.saveAs(WebRequest.fetch(uRL), file);
    }

    public static File saveAs(ByteBuffer byteBuffer, String string) throws IOException {
        return ScriptShellMethods.saveAs(byteBuffer, new File(string));
    }

    public static File saveAs(ByteBuffer byteBuffer, File file) throws IOException {
        file = file.getCanonicalFile();
        FileUtilities.createFolders(file.getParentFile());
        return FileUtilities.writeFile(byteBuffer, file);
    }

    public static GPathResult getXml(File file) throws Exception {
        return new XmlSlurper().parse(file);
    }

    public static String serialize(GPathResult gPathResult) {
        StreamingMarkupBuilder streamingMarkupBuilder = new StreamingMarkupBuilder();
        streamingMarkupBuilder.setEncoding((Object)"UTF-8");
        return streamingMarkupBuilder.bindNode((Object)gPathResult).toString();
    }

    public static File saveAs(GPathResult gPathResult, File file) throws Exception {
        return XmlUtilities.writeDocument(WebRequest.getDocument(ScriptShellMethods.serialize(gPathResult)), file);
    }

    public static File getStructureRoot(File file) throws Exception {
        return MediaFileUtilities.getStructureRoot(file);
    }

    public static File getStructurePathTail(File file) throws Exception {
        return MediaFileUtilities.getStructurePathTail(file);
    }

    public static FolderWatchService watchFolder(File file2, Closure<?> closure) {
        return ScriptShellMethods.watchFolder(file2, false, 2000L, file -> FileUtilities.NOT_HIDDEN.accept(file) && (FileUtilities.FILES.accept(file) || FileUtilities.FOLDERS.accept(file)), closure);
    }

    public static FolderWatchService watchFolder(File file, boolean bl, long l, FileFilter fileFilter, Closure<?> closure) {
        FolderWatchService folderWatchService = new FolderWatchService(bl, l, fileFilter, arg_0 -> closure.call(arg_0));
        folderWatchService.watchFolder(file);
        return folderWatchService;
    }

    public static float getSimilarity(String string, String string2) {
        return new NameSimilarityMetric().getSimilarity(string, string2);
    }

    public static Collection<?> sortBySimilarity(Collection<?> collection, Object object, Closure<String> closure) {
        return collection.stream().sorted(SimilarityComparator.compareTo(object.toString(), closure == null ? Object::toString : arg_0 -> closure.call(arg_0))).collect(Collectors.toList());
    }

    public static boolean isBetter(File file, File file2) {
        if (MediaTypes.VIDEO_FILES.accept(file) && MediaTypes.VIDEO_FILES.accept(file2)) {
            return VideoQuality.isBetter(file, file2);
        }
        throw new UnsupportedOperationException("Compare [" + file + "] to [" + file2 + "]");
    }

    public static MetaAttributeView getXattr(File file) {
        try {
            return new MetaAttributeView(file);
        }
        catch (Exception exception) {
            Logging.debug.severe(Logging.cause(exception));
            return null;
        }
    }

    public static Object getMetadata(File file) {
        try {
            return XattrMetaInfo.xattr.getMetaInfo(file);
        }
        catch (Exception exception) {
            Logging.debug.severe(Logging.cause(exception));
            return null;
        }
    }

    public static void setMetadata(File file, Object object) {
        try {
            XattrMetaInfo.xattr.setMetaInfo(file, object, null);
        }
        catch (Exception exception) {
            Logging.debug.severe(Logging.cause(exception));
        }
    }

    public static MediaCharacteristics getMediaCharacteristics(File file) {
        return CachedMediaCharacteristics.getMediaCharacteristics(file).orElse(null);
    }

    public static Object getMediaInfo(File file) throws Exception {
        return new AssociativeEnumObject(MediaInfoTable.read(file));
    }

    public static Object ffprobe(File file) throws Exception {
        return new AssociativeEnumObject(FFProbe.read(file));
    }

    public static boolean isEpisode(File file) {
        return MediaDetection.isEpisode(file, true);
    }

    public static boolean isMovie(File file) {
        return MediaDetection.isMovie(file);
    }

    public static Object toJsonString(Object object) {
        return JsonUtilities.json(object, true, false);
    }

    public static String apply(ExpressionFormat expressionFormat, Object object) {
        return expressionFormat.format(new MediaBindingBean(object, null));
    }

    public static String apply(ExpressionFormat expressionFormat, File file) {
        return expressionFormat.format(new MediaBindingBean(file, file));
    }

    public static String call(Movie movie, String string) throws Exception {
        return new ExpressionFormat(string).format(new MediaBindingBean(movie, null));
    }

    public static String call(Episode episode, String string) throws Exception {
        return new ExpressionFormat(string).format(new MediaBindingBean(episode, null));
    }

    public static String call(File file, String string) throws Exception {
        return new ExpressionFormat(string).format(new MediaBindingBean(file, file));
    }

    private ScriptShellMethods() {
        throw new UnsupportedOperationException();
    }
}

