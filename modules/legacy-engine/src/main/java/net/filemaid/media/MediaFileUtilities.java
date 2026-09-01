package net.filemaid.media;

import java.io.File;
import java.io.FileFilter;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import net.filemaid.ApplicationFolder;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.archive.Archive;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.media.ClutterFileFilter;
import net.filemaid.media.MediaDetection;
import net.filemaid.similarity.SimilarityComparator;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.SystemProperty;
import net.filemaid.vfs.FileInfo;

public class MediaFileUtilities {
    public static final long CLUTTER_MAX_SIZE = (long)SystemProperty.get("net.filemaid.media.clutter.size", Integer::parseInt, 200).intValue() * 1000000L;
    public static final Duration CLUTTER_MAX_LENGTH = SystemProperty.get("net.filemaid.media.clutter.length", Duration::parse, Duration.ofMinutes(20L));
    public static final FileFilter DISK_FOLDER_ENTRY = FileUtilities.newRegexMatchFilter(MediaDetection.releaseInfo.getDiskFolderEntryPattern());
    public static final FileFilter DISK_FOLDERS = file -> !FileUtilities.getChildren(file, DISK_FOLDER_ENTRY).isEmpty();
    public static final FileFilter EXTRA_FILES_INLINE = FileUtilities.newRegexFindFilter(MediaDetection.releaseInfo.getClutterFilePattern());
    public static final FileFilter EXTRA_FOLDERS = FileUtilities.newRegexMatchFilter(MediaDetection.releaseInfo.getClutterFolderPattern());
    public static final FileFilter EXTRA_FOLDER_ENTRY = file -> FileUtilities.listPathTailReverse(file.getParentFile(), 2).stream().anyMatch(EXTRA_FOLDERS::accept);
    public static final FileFilter CLUTTER_TYPES = FileUtilities.newRegexFindFilter(MediaDetection.releaseInfo.getClutterTypesPattern());
    public static final FileFilter CLUTTER_EXCLUDES = new ClutterFileFilter(MediaDetection.releaseInfo.getClutterExcludesPattern(), CLUTTER_MAX_SIZE, CLUTTER_MAX_LENGTH);
    public static final FileFilter EXTRA_FILES = file -> EXTRA_FOLDER_ENTRY.accept(file) || EXTRA_FILES_INLINE.accept(file) || CLUTTER_EXCLUDES.accept(file);
    public static final FileFilter SYSTEM_EXCLUDES = FileUtilities.newRegexMatchFilter(MediaDetection.releaseInfo.getSystemFilesPattern());
    public static final Comparator<File> FILE_SIZE_ORDER = Comparator.comparingLong(File::length);
    public static final Comparator<File> FILE_SIZE_DESCENDING_ORDER = FILE_SIZE_ORDER.reversed();

    public static boolean isDiskFolder(File file) {
        return file.isDirectory() && DISK_FOLDERS.accept(file);
    }

    public static boolean isVideoDiskFile(File file) throws Exception {
        if (file.isFile() && file.length() > 1000000L) {
            try (Archive archive = Archive.open(file);){
                for (FileInfo fileInfo : archive.listFiles()) {
                    for (File file2 : FileUtilities.listPath(fileInfo.toFile())) {
                        if (!DISK_FOLDER_ENTRY.accept(file2)) continue;
                        boolean bl = true;
                        return bl;
                    }
                }
            }
        }
        return false;
    }

    public static Locale guessLanguageFromSuffix(File file) {
        return MediaDetection.releaseInfo.getSubtitleLanguageTag(FileUtilities.getName(file));
    }

    public static boolean isDerived(File file, File file2) {
        if (Archive.isArchivePart(file)) {
            return false;
        }
        String string = FileUtilities.getName(file);
        String string2 = FileUtilities.getName(file2);
        if (string.length() >= string2.length() && string.toLowerCase().startsWith(string2.toLowerCase())) {
            return string.length() == string2.length() || !Character.isLetterOrDigit(string.charAt(string2.length()));
        }
        return false;
    }

    public static boolean isVolumeRoot(File file) {
        return file == null || file.getName() == null || file.getName().isEmpty() || MediaDetection.releaseInfo.getVolumeRoots().contains(file) || FileUtilities.isNetworkShareRoot(file);
    }

    public static boolean isStructureRoot(String string) throws Exception {
        return string == null || string.isEmpty() || MediaDetection.releaseInfo.getStructureRootPattern().matcher(string).matches();
    }

    public static boolean isStructureRoot(File file) throws Exception {
        return MediaFileUtilities.isVolumeRoot(file) || MediaFileUtilities.isStructureRoot(file.getName()) || ApplicationFolder.UserHome.getDirectory().equals(file.getParentFile());
    }

    public static Collection<File> walkStructureTree(Collection<File> collection) {
        LinkedHashSet<File> linkedHashSet = new LinkedHashSet<File>();
        for (File file2 : collection) {
            linkedHashSet.addAll(FileUtilities.listFiles(file2, FileUtilities.NOT_HIDDEN));
        }
        Collection<File> files = collection;
        while (files.size() > 0) {
            files = files.stream().map(File::getParentFile).distinct().filter(file -> {
                try {
                    return !MediaFileUtilities.isStructureRoot(file);
                }
                catch (Exception exception) {
                    Logging.debug.warning(Logging.cause("Ignore parent folder", file, exception));
                    return false;
                }
            }).collect(Collectors.toList());
            linkedHashSet.addAll(files);
        }
        return linkedHashSet;
    }

    public static File getStructureRoot(File file) throws Exception {
        boolean bl = false;
        for (File file2 : FileUtilities.listPathTailReverse(file)) {
            if (!bl && !MediaFileUtilities.isStructureRoot(file2)) continue;
            if (file2.isDirectory()) {
                return file2;
            }
            bl = true;
        }
        return null;
    }

    public static Deque<String> listStructurePathTail(File file) throws Exception {
        ArrayDeque<String> arrayDeque = new ArrayDeque<String>();
        for (File file2 : FileUtilities.listPathTailReverse(file)) {
            if (MediaFileUtilities.isStructureRoot(file2)) break;
            arrayDeque.addFirst(file2.getName());
        }
        return arrayDeque;
    }

    public static File getStructurePathTail(File file) throws Exception {
        File file2 = null;
        for (String string : MediaFileUtilities.listStructurePathTail(file)) {
            file2 = file2 == null ? new File(string) : new File(file2, string);
        }
        return file2;
    }

    public static File guessMediaFolder(File file) {
        for (File file2 : FileUtilities.listPathTailReverse(file.getParentFile(), 2)) {
            if (MediaDetection.stripReleaseInfo(file2.getName()).length() <= 0) continue;
            return file2;
        }
        return null;
    }

    public static Map<String, List<File>> mapByMediaExtension(Iterable<File> iterable) {
        LinkedHashMap<String, List<File>> linkedHashMap = new LinkedHashMap<String, List<File>>();
        for (File file : iterable) {
            Locale locale;
            Object object = FileUtilities.getExtension(file);
            if (object != null && MediaTypes.SUBTITLE_FILES.accept(file) && (locale = MediaDetection.releaseInfo.getSubtitleLanguageTag(FileUtilities.getName(file))) != null) {
                object = locale.getLanguage() + "." + (String)object;
            }
            if (object != null) {
                object = ((String)object).toLowerCase(Locale.ROOT);
            }
            linkedHashMap.computeIfAbsent((String)object, string -> new ArrayList()).add(file);
        }
        return linkedHashMap;
    }

    public static List<List<File>> groupByMediaCharacteristics(Collection<File> collection) {
        ArrayList<List<File>> arrayList = new ArrayList<List<File>>();
        if (collection.size() < 2) {
            arrayList.add(new ArrayList<File>(collection));
            return arrayList;
        }
        FileUtilities.mapByExtension(collection).forEach((string, list3) -> {
            if (list3.size() < 2) {
                arrayList.add((List<File>)list3);
                return;
            }
            list3.stream().collect(Collectors.groupingBy(file -> CachedMediaCharacteristics.<Object>getMediaCharacteristics(file, mediaCharacteristics -> {
                Instant instant = mediaCharacteristics.getCreationTime();
                Integer n = instant == null ? null : Integer.valueOf(instant.atOffset(ZoneOffset.UTC).getYear());
                Integer n2 = mediaCharacteristics.getWidth();
                Integer n3 = mediaCharacteristics.getHeight();
                String codec = mediaCharacteristics.getVideoCodec();
                String string2 = mediaCharacteristics.getVideoProfile();
                String string3 = mediaCharacteristics.getAudioCodec();
                String string4 = mediaCharacteristics.getAudioLanguage();
                String string5 = mediaCharacteristics.getSubtitleLanguage();
                return Arrays.asList(n, n2, n3, codec, string2, string3, string4, string5);
            }).orElseGet(() -> {
                if (MediaTypes.VIDEO_FILES.accept((File)file)) {
                    File file2 = MediaFileUtilities.guessMediaFolder(file);
                    return file2 != null ? file2 : file.getParentFile();
                }
                return file;
            }), LinkedHashMap::new, Collectors.toList())).forEach((object, list2) -> arrayList.add((List<File>)list2));
        });
        return arrayList;
    }

    public static List<File> findSiblingFiles(File file, FileFilter fileFilter) throws Exception {
        for (File file2 : FileUtilities.listPathTailReverse(file.getParentFile())) {
            List<File> list = FileUtilities.getChildren(file2, MediaTypes.VIDEO_FILES, FileUtilities.HUMAN_NAME_ORDER);
            if (list.isEmpty() && !MediaFileUtilities.isStructureRoot(file2)) continue;
            return list;
        }
        return Collections.emptyList();
    }

    public static Optional<File> findPrimaryFile(File file2, FileFilter fileFilter) {
        if (file2.isDirectory()) {
            return FileUtilities.listFiles(file2, fileFilter).stream().max(FILE_SIZE_ORDER);
        }
        if (fileFilter.accept(file2)) {
            return Optional.empty();
        }
        ArrayList<File> arrayList = new ArrayList<File>();
        String string = MediaFileUtilities.normalizeFileName(file2);
        if (!string.isEmpty()) {
            for (File file3 : FileUtilities.getChildren(file2.getParentFile(), file -> fileFilter.accept(file) && FileUtilities.NOT_HIDDEN.accept(file), FileUtilities.HUMAN_NAME_ORDER)) {
                String string2 = MediaFileUtilities.normalizeFileName(file3);
                if (string2.isEmpty() || !string.startsWith(string2)) continue;
                arrayList.add(file3);
            }
        }
        return MediaFileUtilities.findPrimaryFile(file2, arrayList);
    }

    public static Optional<File> findPrimaryFile(File file, Collection<File> collection) {
        return collection.stream().min(SimilarityComparator.compareTo(file.getPath(), File::getPath).thenComparing(FILE_SIZE_DESCENDING_ORDER));
    }

    private static String normalizeFileName(File file) {
        return MediaDetection.stripReleaseInfo(FileUtilities.getName(file)).toLowerCase();
    }

    public static SortedSet<File> getRemainingEmptyFolders(File[] fileArray, File[] fileArray2, FileFilter fileFilter) throws Exception {
        TreeSet<File> treeSet = new TreeSet<File>();
        for (int i = 0; i < fileArray.length; ++i) {
            File[] fileArray3;
            if (fileArray[i] == null || !fileArray[i].isAbsolute()) {
                throw new IllegalArgumentException("Expected absolute source path: " + fileArray[i]);
            }
            File file2 = fileArray[i].getParentFile();
            if (treeSet.contains(file2) || FileUtilities.sameParentFolder(FileUtilities.resolveSibling(fileArray[i], fileArray2[i]), fileArray[i])) continue;
            int n = MediaFileUtilities.listStructurePathTail(fileArray2[i]).size();
            for (int j = 0; j < n && !MediaFileUtilities.isStructureRoot(file2) && (fileArray3 = file2.listFiles()) != null && Arrays.stream(fileArray3).allMatch(file -> treeSet.contains(file) || fileFilter.accept((File)file) || file.isDirectory() && FileUtilities.containsOnly(file, fileFilter)); ++j) {
                Arrays.stream(fileArray3).forEach(treeSet::add);
                treeSet.add(file2);
                file2 = file2.getParentFile();
            }
        }
        return treeSet;
    }
}

