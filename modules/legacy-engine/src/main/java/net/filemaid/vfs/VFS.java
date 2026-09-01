package net.filemaid.vfs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.filemaid.CategoryFileFilter;
import net.filemaid.MediaTypes;
import net.filemaid.archive.Archive;
import net.filemaid.hash.HashType;
import net.filemaid.hash.VerificationFileReader;
import net.filemaid.hash.VerificationUtilities;
import net.filemaid.util.ExtensionFileFilter;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.vfs.FileInfo;
import net.filemaid.vfs.Torrent;

public class VFS {
    public static CategoryFileFilter getFileFilter() {
        CategoryFileFilter categoryFileFilter = new CategoryFileFilter("List Files", new ExtensionFileFilter[0]);
        categoryFileFilter.add("List", MediaTypes.LIST_FILES);
        categoryFileFilter.add("Verification", MediaTypes.VERIFICATION_FILES);
        categoryFileFilter.add("Archive", MediaTypes.ARCHIVE_FILES);
        categoryFileFilter.add("Torrent", MediaTypes.TORRENT_FILES);
        return categoryFileFilter;
    }

    public static boolean hasIndex(File file) {
        return MediaTypes.LIST_FILES.accept(file) || MediaTypes.VERIFICATION_FILES.accept(file) || MediaTypes.ARCHIVE_FILES.accept(file) || MediaTypes.TORRENT_FILES.accept(file);
    }

    public static List<?> getIndex(File file) throws IOException {
        if (MediaTypes.LIST_FILES.accept(file)) {
            return VFS.listTextFile(file);
        }
        if (MediaTypes.VERIFICATION_FILES.accept(file)) {
            return VFS.listVerificationFile(file);
        }
        if (MediaTypes.TORRENT_FILES.accept(file)) {
            return VFS.listTorrentFile(file);
        }
        if (MediaTypes.ARCHIVE_FILES.accept(file)) {
            return VFS.listArchiveFile(file);
        }
        return Collections.emptyList();
    }

    public static List<String> listTextFile(String string) {
        List<String> list = RegularExpressions.NEWLINE.splitAsStream(string).map(string2 -> RegularExpressions.TAB.splitAsStream((CharSequence)string2).map(part -> part.trim()).filter(part -> !part.isEmpty()).findFirst().orElse(null)).filter(Objects::nonNull).collect(Collectors.toList());
        return list;
    }

    public static List<String> listTextFile(File file) throws IOException {
        return VFS.listTextFile(FileUtilities.readTextFile(file));
    }

    public static List<String> listVerificationFile(File file) throws IOException {
        HashType hashType = VerificationUtilities.getHashType(file);
        if (hashType == null) {
            return Collections.emptyList();
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        try (VerificationFileReader verificationFileReader = hashType.newReader(file);){
            while (verificationFileReader.hasNext()) {
                arrayList.add(((File)verificationFileReader.next().getKey()).getName());
            }
        }
        return arrayList;
    }

    public static List<FileInfo> listTorrentFile(File file) throws IOException {
        Torrent torrent = new Torrent(file);
        return torrent.getFiles();
    }

    public static List<FileInfo> listArchiveFile(File file) throws IOException {
        try (Archive archive = Archive.open(file);){
            List<FileInfo> list = archive.listFiles();
            return list;
        }
    }
}

