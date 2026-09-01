package net.filemaid.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.filemaid.MediaTypes;
import net.filemaid.util.ExtensionFileFilter;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.SystemProperty;

public class ReadOnlyFile
extends File {
    private ReadOnlyFile parentFile;
    private String name;
    private final boolean absolute;
    private BasicFileAttributes stats;
    private File[] children;
    private Boolean hidden;
    private Long totalSpace;
    private Long usableSpace;
    public static final boolean HEURISTICS = SystemProperty.get("net.filemaid.files.heuristics", Boolean::parseBoolean, false);
    private static final ExtensionFileFilter FILE_TYPES = MediaTypes.extension(MediaTypes.VIDEO_FILES, MediaTypes.AUDIO_FILES, MediaTypes.TEXT_FILES, MediaTypes.IMAGE_FILES);
    private String fileType;
    public static final File[] NO_FILES = new File[0];
    public static final BasicFileAttributes UNDEFINED_FILE_ATTRIBUTES = new BasicFileAttributes(){

        @Override
        public boolean isRegularFile() {
            return false;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean isSymbolicLink() {
            return false;
        }

        @Override
        public boolean isOther() {
            return false;
        }

        @Override
        public Object fileKey() {
            return null;
        }

        @Override
        public long size() {
            return 0L;
        }

        @Override
        public FileTime lastModifiedTime() {
            return FileTime.fromMillis(0L);
        }

        @Override
        public FileTime lastAccessTime() {
            return FileTime.fromMillis(0L);
        }

        @Override
        public FileTime creationTime() {
            return FileTime.fromMillis(0L);
        }
    };

    protected ReadOnlyFile(File file) {
        super(file.getAbsolutePath());
        this.absolute = true;
    }

    protected ReadOnlyFile(String string, boolean bl) {
        super(string);
        this.absolute = bl;
    }

    protected ReadOnlyFile(ReadOnlyFile readOnlyFile, String string, BasicFileAttributes basicFileAttributes) {
        super(readOnlyFile, string);
        this.parentFile = readOnlyFile;
        this.absolute = readOnlyFile.absolute;
        this.name = string;
        this.stats = basicFileAttributes;
    }

    public BasicFileAttributes stats() {
        if (this.stats == null) {
            try {
                this.stats = this.absolute ? Files.getFileAttributeView(this.toPath(), BasicFileAttributeView.class, new LinkOption[0]).readAttributes() : UNDEFINED_FILE_ATTRIBUTES;
            }
            catch (Exception exception) {
                this.stats = UNDEFINED_FILE_ATTRIBUTES;
            }
        }
        return this.stats;
    }

    private String getFileType() {
        if (this.fileType == null) {
            String string = FileUtilities.getExtension(this.getName());
            this.fileType = FILE_TYPES.acceptExtension(string) ? string : "";
        }
        return this.fileType;
    }

    @Override
    public boolean isDirectory() {
        if (HEURISTICS && !this.getFileType().isEmpty()) {
            return false;
        }
        return this.stats().isDirectory();
    }

    @Override
    public boolean isFile() {
        if (HEURISTICS && !this.getFileType().isEmpty()) {
            return true;
        }
        return this.stats().isRegularFile();
    }

    @Override
    public long length() {
        return this.stats().size();
    }

    @Override
    public long lastModified() {
        return this.stats().lastModifiedTime().toMillis();
    }

    @Override
    public boolean exists() {
        return this.stats() != UNDEFINED_FILE_ATTRIBUTES;
    }

    @Override
    public boolean canRead() {
        return this.exists();
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public boolean canExecute() {
        return false;
    }

    @Override
    public String getName() {
        if (this.name == null) {
            this.name = super.getName();
        }
        return this.name;
    }

    @Override
    public synchronized File getParentFile() {
        if (this.parentFile == null) {
            String string = super.getParent();
            if (string == null) {
                return null;
            }
            this.parentFile = new Prefix(string, this.absolute, this);
        }
        return this.parentFile;
    }

    @Override
    public String getParent() {
        File file = this.getParentFile();
        if (file == null) {
            return null;
        }
        return file.getPath();
    }

    @Override
    public boolean isAbsolute() {
        return this.absolute;
    }

    @Override
    public File getAbsoluteFile() {
        if (this.absolute) {
            return this;
        }
        throw new UnsupportedOperationException(this + " is a relative path");
    }

    @Override
    public String getAbsolutePath() {
        if (this.absolute) {
            return this.getPath();
        }
        throw new UnsupportedOperationException(this + " is a relative path");
    }

    @Override
    public File getCanonicalFile() {
        return this.getAbsoluteFile();
    }

    @Override
    public String getCanonicalPath() {
        return this.getAbsolutePath();
    }

    protected synchronized File[] children() {
        if (this.children == null) {
            if (FileUtilities.UNIX) {
                String[] stringArray = super.list();
                this.children = stringArray != null ? (File[])Arrays.stream(stringArray).map(string -> this.child((String)string, null)).toArray(File[]::new) : NO_FILES;
            } else {
                try {
                    ArrayList arrayList = new ArrayList(64);
                    Files.find(this.toPath(), 1, (path, basicFileAttributes) -> {
                        arrayList.add(this.child(path.getFileName().toString(), (BasicFileAttributes)basicFileAttributes));
                        return false;
                    }, FileVisitOption.FOLLOW_LINKS).count();
                    this.children = (File[])arrayList.stream().skip(1L).sorted(FileUtilities.HUMAN_NAME_ORDER).toArray(File[]::new);
                }
                catch (Exception exception) {
                    this.children = NO_FILES;
                }
            }
        }
        return this.children;
    }

    protected ReadOnlyFile child(String string, BasicFileAttributes basicFileAttributes) {
        return new ReadOnlyFile(this, string, basicFileAttributes);
    }

    @Override
    public File[] listFiles() {
        return (File[])this.children().clone();
    }

    @Override
    public File[] listFiles(FileFilter fileFilter) {
        return (File[])Arrays.stream(this.children()).filter(fileFilter::accept).toArray(File[]::new);
    }

    @Override
    public File[] listFiles(FilenameFilter filenameFilter) {
        return (File[])Arrays.stream(this.children()).filter(file -> filenameFilter.accept(this, file.getName())).toArray(File[]::new);
    }

    @Override
    public String[] list() {
        return (String[])Arrays.stream(this.children()).map(File::getName).toArray(String[]::new);
    }

    @Override
    public boolean isHidden() {
        if (this.hidden == null) {
            this.hidden = FileUtilities.UNIX ? Boolean.valueOf(this.getName().startsWith(".")) : (FileUtilities.isUNC(this) || !this.getFileType().isEmpty() ? Boolean.valueOf(false) : Boolean.valueOf(super.isHidden()));
        }
        return this.hidden;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof File) {
            File file = (File)object;
            return this.getAbsolutePath().equals(file.getAbsolutePath());
        }
        return false;
    }

    @Override
    public int compareTo(File file) {
        return this.getAbsolutePath().compareTo(file.getAbsolutePath());
    }

    @Override
    public long getTotalSpace() {
        if (this.totalSpace == null) {
            this.totalSpace = super.getTotalSpace();
        }
        return this.totalSpace;
    }

    @Override
    public long getUsableSpace() {
        if (this.usableSpace == null) {
            this.usableSpace = super.getUsableSpace();
        }
        return this.usableSpace;
    }

    @Override
    public long getFreeSpace() {
        return this.getUsableSpace();
    }

    @Override
    public boolean createNewFile() {
        return false;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public void deleteOnExit() {
        throw new UnsupportedOperationException(this + " is a relative path");
    }

    @Override
    public boolean mkdir() {
        return false;
    }

    @Override
    public boolean mkdirs() {
        return false;
    }

    @Override
    public boolean renameTo(File file) {
        return false;
    }

    @Override
    public boolean setLastModified(long l) {
        return false;
    }

    @Override
    public boolean setReadOnly() {
        return false;
    }

    @Override
    public boolean setWritable(boolean bl, boolean bl2) {
        return false;
    }

    @Override
    public boolean setWritable(boolean bl) {
        return false;
    }

    @Override
    public boolean setReadable(boolean bl, boolean bl2) {
        return false;
    }

    @Override
    public boolean setReadable(boolean bl) {
        return false;
    }

    @Override
    public boolean setExecutable(boolean bl, boolean bl2) {
        return false;
    }

    @Override
    public boolean setExecutable(boolean bl) {
        return false;
    }

    public static ReadOnlyFile of(File file) {
        if (file == null) {
            return null;
        }
        if (file instanceof ReadOnlyFile) {
            return (ReadOnlyFile)file;
        }
        return new ReadOnlyFile(file);
    }

    public static List<File> of(Iterable<File> iterable) {
        ArrayList<File> arrayList = new ArrayList<File>();
        FileUtilities.mapByFolder(iterable).forEach((file, list2) -> arrayList.addAll(Prefix.parent(file, list2)));
        return arrayList;
    }

    public static List<File> find(File file, FileFilter fileFilter, Function<File, FileVisitResult> function) {
        return FileUtilities.listFiles(Collections.singleton(ReadOnlyFile.of(file)), ReadOnlyFile.cancellable(fileFilter, function), FileUtilities.HUMAN_NAME_ORDER, ReadOnlyFile.cancellable(FileUtilities.FOLDERS, function), 64);
    }

    private static FileFilter cancellable(FileFilter fileFilter, Function<File, FileVisitResult> function) {
        return file -> {
            switch ((FileVisitResult)((Object)((Object)function.apply(file)))) {
                case CONTINUE: {
                    return fileFilter.accept(file);
                }
                case TERMINATE: {
                    throw new CancellationException("Background file filter has been cancelled");
                }
            }
            return false;
        };
    }

    public static File asRegularFile(File file) {
        if (file == null) {
            return null;
        }
        if (file instanceof ReadOnlyFile) {
            return new File(((ReadOnlyFile)file).getPath());
        }
        return file;
    }

    public static class Prefix
    extends ReadOnlyFile {
        private transient Map<String, ReadOnlyFile> link;

        private Prefix(String string, boolean bl, List<ReadOnlyFile> list) {
            super(string, bl);
            this.link = list.stream().collect(Collectors.toMap(readOnlyFile -> readOnlyFile.getName(), readOnlyFile -> readOnlyFile));
        }

        private Prefix(String string, boolean bl, ReadOnlyFile readOnlyFile) {
            super(string, bl);
            this.link = Collections.singletonMap(readOnlyFile.getName(), readOnlyFile);
        }

        @Override
        protected ReadOnlyFile child(String string, BasicFileAttributes basicFileAttributes) {
            ReadOnlyFile readOnlyFile = this.link.get(string);
            if (readOnlyFile != null) {
                return readOnlyFile;
            }
            return super.child(string, basicFileAttributes);
        }

        protected static List<ReadOnlyFile> parent(File file, List<File> list) {
            List<ReadOnlyFile> list2 = list.stream().map(ReadOnlyFile::of).collect(Collectors.toList());
            Prefix prefix = new Prefix(file.getAbsolutePath(), true, list2);
            list2.forEach(readOnlyFile -> {
                readOnlyFile.parentFile = prefix;
            });
            return list2;
        }
    }
}

