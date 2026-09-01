package net.filemaid.postprocess;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.Deployment;
import net.filemaid.Execute;
import net.filemaid.RenameAction;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.Feedback;
import net.filemaid.similarity.Match;

public enum Refresh implements Apply
{
    SYNO{

        @Override
        public boolean hasMediaLibrary() {
            return new File("/usr/syno/bin/synoindex").canExecute();
        }

        @Override
        public void refreshMediaLibrary(Iterable<String> iterable, Feedback feedback) throws Exception {
            for (String string : iterable) {
                feedback.info("Refresh File Services", string);
                try {
                    Execute.system("/usr/syno/bin/synoindex", "-R", string);
                }
                catch (Exception exception) {
                    feedback.trace(exception, string);
                }
            }
        }
    }
    ,
    QNAP{

        @Override
        public boolean hasMediaLibrary() {
            return new File("/usr/local/medialibrary").isDirectory();
        }

        @Override
        public void refreshMediaLibrary(Iterable<String> iterable, Feedback feedback) throws Exception {
            Pattern pattern = Pattern.compile("(/share/\\w+/)(.+)");
            for (String string : iterable) {
                Matcher matcher = pattern.matcher(string);
                if (matcher.matches()) {
                    feedback.info("Refresh Media Library", string);
                    try {
                        Execute.system("/opt/filebot/bin/refresh-media-library.sh", matcher.group(1), matcher.group(2) + "/");
                    }
                    catch (Exception exception) {
                        feedback.trace(exception, string);
                    }
                    continue;
                }
                feedback.warning("Unexpected mount path", string);
            }
        }
    };


    public abstract boolean hasMediaLibrary();

    public abstract void refreshMediaLibrary(Iterable<String> var1, Feedback var2) throws Exception;

    @Override
    public void apply(Map<File, Match<File, ?>> map, RenameAction renameAction, Feedback feedback) throws Exception {
        if (this.hasMediaLibrary()) {
            this.refreshMediaLibrary(this.getDirectorySet(map, feedback), feedback);
        }
    }

    private Iterable<String> getDirectorySet(Map<File, Match<File, ?>> map, Feedback feedback) throws Exception {
        Stream<File> stream = map.keySet().stream().filter(file -> file.isAbsolute() && file.exists());
        Stream<File> stream2 = map.values().stream().map(Match::getValue).filter(Objects::nonNull).filter(file -> file.isAbsolute() && !file.exists());
        return Stream.concat(stream, stream2).map(File::getParentFile).distinct().map(file -> {
            try {
                return file.getCanonicalPath();
            }
            catch (Exception exception) {
                feedback.warning(exception, file);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static Apply platform(Deployment deployment) {
        switch (deployment) {
            case SPK: {
                return SYNO;
            }
            case QPKG: {
                return QNAP;
            }
        }
        return (map, renameAction, feedback) -> feedback.trace("Platform not supported", (Object)deployment);
    }
}

