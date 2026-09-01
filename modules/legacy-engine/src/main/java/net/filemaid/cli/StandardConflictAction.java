package net.filemaid.cli;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.filemaid.cli.ConflictAction;
import net.filemaid.media.VideoQuality;
import net.filemaid.util.FileUtilities;

public enum StandardConflictAction implements ConflictAction
{
    SKIP{

        @Override
        public File conflict(File file, File file2) throws Exception {
            return null;
        }
    }
    ,
    REPLACE{

        @Override
        public File conflict(File file, File file2) throws Exception {
            return file2;
        }
    }
    ,
    AUTO{

        @Override
        public File conflict(File file, File file2) throws Exception {
            return VideoQuality.isBetter(file, file2) ? file2 : null;
        }
    }
    ,
    INDEX{

        @Override
        public File conflict(File file2, File file3) throws Exception {
            File file4 = file3.getParentFile();
            String string = FileUtilities.getName(file3);
            String string2 = FileUtilities.getExtension(file3);
            return IntStream.range(1, 100).mapToObj(n -> new File(file4, string + "." + n + "." + string2)).filter(file -> !file.exists()).findFirst().orElse(null);
        }
    }
    ,
    FAIL{

        @Override
        public File conflict(File file, File file2) throws Exception {
            throw new Exception("Cannot process [" + file + "] because [" + file2 + "] already exists");
        }
    };


    public static List<String> names() {
        return Arrays.stream(StandardConflictAction.values()).map(Enum::name).collect(Collectors.toList());
    }

    public static StandardConflictAction forName(String string) {
        for (StandardConflictAction standardConflictAction : StandardConflictAction.values()) {
            if (!standardConflictAction.name().equalsIgnoreCase(string)) continue;
            return standardConflictAction;
        }
        if ("override".equalsIgnoreCase(string) || "overwrite".equalsIgnoreCase(string)) {
            return REPLACE;
        }
        throw new IllegalArgumentException(string + " not in " + StandardConflictAction.names());
    }
}

