package net.filemaid;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.RenameAction;
import net.filemaid.Settings;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.Delete;
import net.filemaid.postprocess.Feedback;
import net.filemaid.postprocess.FetchArtwork;
import net.filemaid.postprocess.FetchFolderCover;
import net.filemaid.postprocess.FetchSubtitles;
import net.filemaid.postprocess.FetchThumbnails;
import net.filemaid.postprocess.ImportCompanionFiles;
import net.filemaid.postprocess.Refresh;
import net.filemaid.postprocess.RevealFiles;
import net.filemaid.postprocess.SetDate;
import net.filemaid.postprocess.SetFinderTags;
import net.filemaid.postprocess.SetLastModified;
import net.filemaid.postprocess.SetPosixPermissions;
import net.filemaid.postprocess.TranscodeSubtitles;
import net.filemaid.postprocess.WriteInternetShortcut;
import net.filemaid.postprocess.WriteMetadataXml;
import net.filemaid.postprocess.WriteTags;
import net.filemaid.postprocess.XattrFolder;
import net.filemaid.similarity.Match;
import net.filemaid.subtitle.SubtitleFormat;

public enum StandardPostProcessAction implements Apply
{
    ARTWORK(new FetchArtwork()),
    COVER(new FetchFolderCover("folder.jpg")),
    NFO(new WriteMetadataXml()),
    URL(new WriteInternetShortcut()),
    METADATA(new XattrFolder(".xattr")),
    IMPORT(new ImportCompanionFiles()),
    SRT(new TranscodeSubtitles(SubtitleFormat.SubRip, StandardCharsets.UTF_8)),
    SUBTITLES(new FetchSubtitles(SubtitleFormat.SubRip, StandardCharsets.UTF_8)),
    FINDER(new SetFinderTags()),
    TAGS(new WriteTags()),
    DATE(new SetDate()),
    CHMOD(new SetPosixPermissions(SetPosixPermissions.DEFAULT_PERMISSIONS)),
    TOUCH(new SetLastModified(ChronoUnit.MINUTES)),
    THUMBNAIL(FetchThumbnails.platform(Settings.DEPLOYMENT)),
    PRUNE(Delete.EMPTY_FOLDERS),
    CLEAN(Delete.CLUTTER_FILES),
    REVEAL(new RevealFiles()),
    REFRESH(Refresh.platform(Settings.DEPLOYMENT));

    private final Apply action;

    private StandardPostProcessAction(Apply apply) {
        this.action = apply;
    }

    @Override
    public void apply(Map<File, Match<File, ?>> map, RenameAction renameAction, Feedback feedback) throws Exception {
        this.action.apply(map, renameAction, feedback);
    }

    public String getLabel() {
        switch (this) {
            case ARTWORK: {
                return "Fetch artwork";
            }
            case COVER: {
                return "Fetch cover images";
            }
            case NFO: {
                return "Export NFO files";
            }
            case URL: {
                return "Export URL files";
            }
            case METADATA: {
                return "Export .xattr folders";
            }
            case IMPORT: {
                return "Import companion files";
            }
            case SRT: {
                return "Transcode subtitle files";
            }
            case SUBTITLES: {
                return "Fetch subtitle files";
            }
            case FINDER: {
                return "Set Finder tags";
            }
            case TAGS: {
                return "Set media tags";
            }
            case DATE: {
                return "Set creation date";
            }
            case CHMOD: {
                return "Set permissions";
            }
            case TOUCH: {
                return "Update time stamp";
            }
            case THUMBNAIL: {
                return "Fetch thumbnails";
            }
            case PRUNE: {
                return "Prune empty folders";
            }
            case CLEAN: {
                return "Delete clutter files";
            }
            case REVEAL: {
                return "Reveal files";
            }
            case REFRESH: {
                return "Refresh file services";
            }
        }
        return null;
    }

    public String getDescription() {
        switch (this) {
            case ARTWORK: {
                return "Fetch artwork for movie / series / season folders";
            }
            case COVER: {
                return "Fetch folder.jpg files for movie / series folders";
            }
            case NFO: {
                return "Generate *.nfo files for movie / series folders";
            }
            case URL: {
                return "Generate *.url files for movie / series folders";
            }
            case METADATA: {
                return "Export xattr metadata into hidden .xattr folders";
            }
            case IMPORT: {
                return "Move along companion files (artwork, nfo, extras, trailers, etc) from the original folder to the destination folder";
            }
            case SRT: {
                return "Transcode subtitle files to SRT format / UTF-8 encoding";
            }
            case SUBTITLES: {
                return "Fetch missing subtitle files in the preferred language";
            }
            case FINDER: {
                return "Set Finder tags";
            }
            case TAGS: {
                return "Write embedded media tags";
            }
            case DATE: {
                return "Set Last-Modified and Creation-Date to the episode airdate / movie release date";
            }
            case CHMOD: {
                return "Set Posix file permissions";
            }
            case TOUCH: {
                return "Set Last-Modified to the current date and time";
            }
            case THUMBNAIL: {
                return "Fetch thumbnails for movie / episode files";
            }
            case REVEAL: {
                return Settings.isWindowsApp() ? "Reveal files in File Explorer" : (Settings.isMacApp() ? "Reveal files in Finder" : "Reveal files in File Manager");
            }
        }
        return null;
    }

    public boolean isPlatformSupported() {
        switch (this) {
            case ARTWORK: 
            case COVER: 
            case NFO: 
            case URL: 
            case METADATA: 
            case IMPORT: 
            case SRT: 
            case SUBTITLES: 
            case DATE: 
            case TOUCH: 
            case THUMBNAIL: 
            case REVEAL: {
                return true;
            }
            case FINDER: {
                return Settings.isMacApp();
            }
            case TAGS: {
                return Settings.isLinuxApp() || WriteTags.Command.executables().anyMatch(File::isAbsolute);
            }
            case CHMOD: {
                return Settings.isLinuxApp();
            }
            case REFRESH: {
                return Settings.isNAS();
            }
        }
        return false;
    }

    public static Set<StandardPostProcessAction> getMetadataActions() {
        return Stream.of(StandardPostProcessAction.values()).filter(StandardPostProcessAction::isPlatformSupported).collect(Collectors.toCollection(() -> EnumSet.noneOf(StandardPostProcessAction.class)));
    }

    public static List<String> names() {
        return Arrays.stream(StandardPostProcessAction.values()).map(Enum::toString).map(String::toLowerCase).collect(Collectors.toList());
    }

    public static StandardPostProcessAction forName(String string) {
        for (StandardPostProcessAction standardPostProcessAction : StandardPostProcessAction.values()) {
            if (!standardPostProcessAction.toString().equalsIgnoreCase(string)) continue;
            return standardPostProcessAction;
        }
        throw new IllegalArgumentException(string + " not in " + StandardPostProcessAction.names());
    }
}

