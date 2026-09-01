package net.filemaid.archive;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.Execute;
import net.filemaid.archive.ArchiveExtractor;
import net.filemaid.util.ExtensionFileFilter;
import net.filemaid.util.RegularExpressions;
import net.filemaid.vfs.FileInfo;
import net.filemaid.vfs.SimpleFileInfo;

public class ShellExecutables
implements ArchiveExtractor {
    private Command command;
    private File archive;
    public static final FileFilter RAR_FILES = new ExtensionFileFilter("rar", "r00");

    public ShellExecutables(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getPath());
        }
        this.archive = file.getCanonicalFile();
        this.command = this.getCommand(this.archive);
    }

    @Override
    public List<FileInfo> listFiles() throws IOException {
        return this.command.listFiles(this.archive);
    }

    @Override
    public void extract(File file) throws IOException {
        this.command.extract(this.archive, file.getCanonicalFile());
    }

    @Override
    public void extract(File file, FileFilter fileFilter) throws IOException {
        this.command.extract(this.archive, file.getCanonicalFile(), fileFilter);
    }

    protected Command getCommand(File file) {
        return RAR_FILES.accept(file) && Command.unrar.exists() ? Command.unrar : Command.p7zip;
    }

    public static enum Command {
        p7zip{
            private static final String PREFIX_PATH = "Path = ";
            private static final String PREFIX_TYPE_FILE = "Folder = -";
            private static final String PREFIX_SIZE = "Size = ";

            @Override
            public String getCommand() {
                return System.getProperty("net.filemaid.archive.7z", "7z");
            }

            @Override
            public List<FileInfo> listFiles(File file) throws IOException {
                CharSequence charSequence = Execute.execute(this.getCommand(), "l", "-sccUTF-8", "-slt", "-y", "--", file.getPath());
                ArrayList<FileInfo> arrayList = new ArrayList<FileInfo>();
                String[] stringArray = RegularExpressions.NEWLINE.split(charSequence);
                for (int i = 0; i < stringArray.length - 2; ++i) {
                    if (!stringArray[i].startsWith(PREFIX_PATH) || !stringArray[i + 1].startsWith(PREFIX_TYPE_FILE) || !stringArray[i + 2].startsWith(PREFIX_SIZE)) continue;
                    String string = stringArray[i].substring(PREFIX_PATH.length());
                    String string2 = stringArray[i + 2].substring(PREFIX_SIZE.length());
                    arrayList.add(new SimpleFileInfo(string, Long.parseLong(string2)));
                }
                return arrayList;
            }

            @Override
            public void extract(File file, File file2) throws IOException {
                Execute.execute(this.getCommand(), "x", "-y", "-o" + file2.getPath(), "--", file.getPath());
            }

            @Override
            public void extract(File file, File file2, FileFilter fileFilter) throws IOException {
                Stream<String> stream = Stream.of("x", "-y", "-o" + file2.getPath(), "--", file.getPath());
                Stream<String> stream2 = this.listFiles(file).stream().filter(fileInfo -> fileFilter.accept(fileInfo.toFile())).map(FileInfo::getPath);
                Execute.execute(this.getCommand(), (String[])Stream.concat(stream, stream2).toArray(String[]::new));
            }

            @Override
            public String version() throws IOException {
                CharSequence charSequence = Execute.execute(this.getCommand(), "i");
                return RegularExpressions.NEWLINE.splitAsStream(charSequence).map(String::trim).filter(string -> string.startsWith("p7zip") || string.startsWith("7-Zip")).findFirst().orElse(null);
            }
        }
        ,
        unrar{

            @Override
            public String getCommand() {
                return System.getProperty("net.filemaid.archive.unrar", "unrar");
            }

            @Override
            public List<FileInfo> listFiles(File file) throws IOException {
                CharSequence charSequence = Execute.execute(this.getCommand(), "l", "-y", "--", file.getPath());
                return RegularExpressions.NEWLINE.splitAsStream(charSequence).map(String::trim).map(string -> RegularExpressions.SPACE.split((CharSequence)string, 5)).filter(stringArray -> ((String[])stringArray).length == 5 && stringArray[4].length() > 0 && RegularExpressions.NON_DIGIT.matcher(stringArray[0]).matches() && RegularExpressions.DIGIT.matcher(stringArray[1]).matches()).map(stringArray -> new SimpleFileInfo(stringArray[4], Long.parseLong(stringArray[1]))).collect(Collectors.toList());
            }

            @Override
            public void extract(File file, File file2) throws IOException {
                Execute.execute(this.getCommand(), "x", "-y", "--", file.getPath(), file2.getPath());
            }

            @Override
            public void extract(File file, File file2, FileFilter fileFilter) throws IOException {
                Stream<String> stream2 = Stream.of("x", "-y", "--", file.getPath());
                Stream<String> stream3 = this.listFiles(file).stream().filter(fileInfo -> fileFilter.accept(fileInfo.toFile())).map(FileInfo::getPath);
                Stream<String> stream4 = Stream.of(file2.getPath());
                Execute.execute(this.getCommand(), (String[])Stream.of(stream2, stream3, stream4).flatMap(stream -> stream).toArray(String[]::new));
            }

            @Override
            public String version() throws IOException {
                CharSequence charSequence = Execute.execute(this.getCommand(), "-V");
                return Pattern.compile("\\n+|\\s\\s+").splitAsStream(charSequence).map(String::trim).filter(string -> !string.isEmpty()).findFirst().orElse(null);
            }
        };


        public abstract String getCommand();

        public abstract List<FileInfo> listFiles(File var1) throws IOException;

        public abstract void extract(File var1, File var2) throws IOException;

        public abstract void extract(File var1, File var2, FileFilter var3) throws IOException;

        public abstract String version() throws IOException;

        public boolean exists() {
            File file = new File(this.getCommand());
            return file.isAbsolute() && file.canExecute();
        }

        public String toString() {
            File file = new File(this.getCommand());
            return file.getName();
        }
    }
}

