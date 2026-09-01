package net.filemaid.ui.rename;

import java.io.File;
import java.util.Map;
import net.filemaid.similarity.Match;
import net.filemaid.ui.rename.MatchFormatter;
import net.filemaid.ui.rename.StringMatch;
import net.filemaid.util.FileUtilities;
import net.filemaid.vfs.FileInfo;
import net.filemaid.web.AudioTrack;
import net.filemaid.web.AudioTrackFormat;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeFormat;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieFormat;

enum MatchFormatterType implements MatchFormatter
{
    INPUT{

        @Override
        public boolean canFormat(Match<?, ?> match) {
            return match instanceof StringMatch;
        }

        @Override
        public String preview(Match<?, ?> match) {
            return this.format(match, true, null);
        }

        @Override
        public String format(Match<?, ?> match, boolean bl, Map<?, ?> map) {
            StringMatch stringMatch = (StringMatch)match;
            return MatchFormatterType.asFilePath(stringMatch.getStringValue());
        }
    }
    ,
    EPISODE{

        @Override
        public boolean canFormat(Match<?, ?> match) {
            return match.getValue() instanceof Episode;
        }

        @Override
        public String preview(Match<?, ?> match) {
            return MatchFormatterType.asFileName(EpisodeFormat.DEFAULT.format(match.getValue()));
        }
    }
    ,
    MOVIE{

        @Override
        public boolean canFormat(Match<?, ?> match) {
            return match.getValue() instanceof Movie;
        }

        @Override
        public String preview(Match<?, ?> match) {
            return MatchFormatterType.asFileName(MovieFormat.DEFAULT.format(match.getValue()));
        }
    }
    ,
    MUSIC{

        @Override
        public boolean canFormat(Match<?, ?> match) {
            return match.getValue() instanceof AudioTrack;
        }

        @Override
        public String preview(Match<?, ?> match) {
            return MatchFormatterType.asFileName(AudioTrackFormat.DEFAULT.format(match.getValue()));
        }
    }
    ,
    FILE{

        @Override
        public boolean canFormat(Match<?, ?> match) {
            return match.getValue() instanceof File;
        }

        @Override
        public String preview(Match<?, ?> match) {
            return this.format(match, true, null);
        }

        @Override
        public String format(Match<?, ?> match, boolean bl, Map<?, ?> map) {
            File file = (File)match.getValue();
            return MatchFormatterType.asFileName(bl ? file.getName() : FileUtilities.getName(file));
        }
    }
    ,
    FILE_VFS{

        @Override
        public boolean canFormat(Match<?, ?> match) {
            return match.getValue() instanceof FileInfo;
        }

        @Override
        public String preview(Match<?, ?> match) {
            return this.format(match, true, null);
        }

        @Override
        public String format(Match<?, ?> match, boolean bl, Map<?, ?> map) {
            FileInfo fileInfo = (FileInfo)match.getValue();
            return MatchFormatterType.asFileName(bl ? fileInfo.toFile().getName() : fileInfo.getName());
        }
    }
    ,
    STRING{

        @Override
        public boolean canFormat(Match<?, ?> match) {
            return match.getValue() instanceof String;
        }

        @Override
        public String preview(Match<?, ?> match) {
            return this.format(match, true, null);
        }

        @Override
        public String format(Match<?, ?> match, boolean bl, Map<?, ?> map) {
            String string = (String)match.getValue();
            return MatchFormatterType.asFilePath(bl ? string : FileUtilities.getNameWithoutExtension(string));
        }
    }
    ,
    OBJECT{

        @Override
        public boolean canFormat(Match<?, ?> match) {
            return true;
        }

        @Override
        public String preview(Match<?, ?> match) {
            return MatchFormatterType.asFilePath(match.getValue().toString());
        }
    };


    @Override
    public String format(Match<?, ?> match, boolean bl, Map<?, ?> map) {
        String string;
        String object = this.preview(match);
        if (bl && match.getCandidate() instanceof File && (string = FileUtilities.getExtension((File)match.getCandidate())) != null) {
            object = (String)object + "." + string;
        }
        return object;
    }

    private static String asFilePath(String string) {
        return FileUtilities.normalizePathSeparators(string).trim();
    }

    private static String asFileName(String string) {
        return FileUtilities.stripPathSeparators(string).trim();
    }
}

