package net.filemaid.subtitle;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.filemaid.subtitle.SubtitleUtilities;
import net.filemaid.util.FileUtilities;
import net.filemaid.web.SubtitleDescriptor;

public enum SubtitleNaming {
    ORIGINAL{

        @Override
        public String format(File file, SubtitleDescriptor subtitleDescriptor, String string) {
            return subtitleDescriptor.toFile().getName();
        }

        public String toString() {
            return "Keep Original";
        }
    }
    ,
    MATCH_VIDEO{

        @Override
        public String format(File file, SubtitleDescriptor subtitleDescriptor, String string) {
            return SubtitleUtilities.formatSubtitle(FileUtilities.getName(file), null, string);
        }

        public String toString() {
            return "Match Video";
        }
    }
    ,
    MATCH_VIDEO_ADD_LANGUAGE_TAG{

        @Override
        public String format(File file, SubtitleDescriptor subtitleDescriptor, String string) {
            return SubtitleUtilities.formatSubtitle(FileUtilities.getName(file), subtitleDescriptor.getLanguageName(), string);
        }

        public String toString() {
            return "Match Video and Language";
        }
    };


    public abstract String format(File var1, SubtitleDescriptor var2, String var3);

    public static List<String> names() {
        return Arrays.stream(SubtitleNaming.values()).map(Enum::name).collect(Collectors.toList());
    }

    public static SubtitleNaming forName(String string) {
        for (SubtitleNaming subtitleNaming : SubtitleNaming.values()) {
            if (!subtitleNaming.name().equalsIgnoreCase(string)) continue;
            return subtitleNaming;
        }
        throw new IllegalArgumentException(string + " not in " + SubtitleNaming.names());
    }
}

