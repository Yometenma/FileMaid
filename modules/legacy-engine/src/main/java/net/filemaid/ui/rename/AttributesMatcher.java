package net.filemaid.ui.rename;

import java.awt.Component;
import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.filemaid.InvalidInputException;
import net.filemaid.MediaTypes;
import net.filemaid.WebServices;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.media.LocalDatasource;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.similarity.Match;
import net.filemaid.ui.rename.AutoCompleteMatcher;
import net.filemaid.ui.rename.AutoDetectionMode;
import net.filemaid.ui.rename.AutoSelectionMode;
import net.filemaid.ui.rename.MatchMode;
import net.filemaid.util.FileUtilities;
import net.filemaid.web.SortOrder;

class AttributesMatcher
implements AutoCompleteMatcher {
    private static final FileFilter XATTR_MEDIA_FILES = FileUtilities.filter(MediaTypes.VIDEO_FILES, MediaTypes.SUBTITLE_FILES, MediaTypes.IMAGE_FILES, MediaTypes.AUDIO_FILES);

    AttributesMatcher() {
    }

    @Override
    public List<Match<File, ?>> match(Collection<File> collection, MatchMode matchMode, SortOrder sortOrder, Locale locale, AutoDetectionMode autoDetectionMode, Set<AutoSelectionMode> set, Component component) throws Exception {
        List<Match<File, ?>> list = collection.stream().map(file -> {
            Object object = this.match((File)file);
            if (object != null) {
                return Match.of(file, object);
            }
            if (autoDetectionMode == AutoDetectionMode.Input) {
                return Match.of(file, file);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (list.isEmpty()) {
            if (FileUtilities.containsOnly(collection, (FileFilter)MediaTypes.AUDIO_FILES)) {
                throw new InvalidInputException("ID3 audio metadata not found. Please <Load> audio files that have ID3 audio metadata.");
            }
            if (FileUtilities.containsOnly(collection, (FileFilter)MediaTypes.IMAGE_FILES)) {
                throw new InvalidInputException("EXIF image metadata not found. Please <Load> image files that have EXIF image metadata.");
            }
            throw new InvalidInputException("XATTR metadata not found. Please <Load> files that have XATTR metadata.");
        }
        return list;
    }

    public Object match(File file) {
        Object object;
        if ((XATTR_MEDIA_FILES.accept(file) || file.isDirectory()) && (object = LocalDatasource.XATTR.match(file)) != null) {
            return object;
        }
        if (MediaTypes.IMAGE_FILES.accept(file) && (object = LocalDatasource.EXIF.match(file)) != null) {
            return object;
        }
        if (MediaTypes.AUDIO_FILES.accept(file) && (object = WebServices.MediaInfoID3.getAudioTrack(file)) != null) {
            return object;
        }
        return CachedMediaCharacteristics.getMediaCharacteristics(file, MediaCharacteristics::getMediaTags).orElse(null);
    }
}

