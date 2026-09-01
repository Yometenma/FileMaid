package net.filemaid.postprocess;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import net.filemaid.MediaTypes;
import net.filemaid.WebServices;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.postprocess.ApplyMetadata;
import net.filemaid.postprocess.Feedback;
import net.filemaid.subtitle.SubtitleFormat;
import net.filemaid.subtitle.SubtitleUtilities;
import net.filemaid.util.FileUtilities;
import net.filemaid.vfs.MemoryFile;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.Movie;
import net.filemaid.web.SubtitleDescriptor;
import net.filemaid.web.SubtitleLookupService;
import net.filemaid.web.SubtitleProvider;

public class FetchSubtitles
implements ApplyMetadata {
    private final SubtitleFormat format;
    private final Charset encoding;

    public FetchSubtitles(SubtitleFormat subtitleFormat, Charset charset) {
        this.format = subtitleFormat;
        this.encoding = charset;
    }

    @Override
    public boolean accept(File file, Object object) {
        return MediaTypes.VIDEO_FILES.accept(file);
    }

    @Override
    public void apply(File file, File file2, Movie movie, Feedback feedback) throws Exception {
        this.lookup(file2, movie.getLanguage(), true, feedback);
    }

    @Override
    public void apply(File file, File file2, Episode episode, Feedback feedback) throws Exception {
        this.lookup(file2, episode.getSeriesInfo().getLanguage(), true, feedback);
    }

    public void lookup(File file, Locale locale, boolean bl, Feedback feedback) throws Exception {
        SubtitleDescriptor subtitleDescriptor;
        File file2 = new File(file.getParentFile(), SubtitleUtilities.formatSubtitle(FileUtilities.getName(file), locale.getLanguage(), this.format.getFilter().extension()));
        if (file2.exists()) {
            return;
        }
        String string = CachedMediaCharacteristics.getMediaCharacteristics(file, MediaCharacteristics::getSubtitleLanguage).orElse("");
        if (string.contains(locale.getLanguage())) {
            feedback.info("Embedded subtitle languages: " + string, file);
            return;
        }
        for (SubtitleLookupService datasource : WebServices.getSubtitleLookupServices(locale)) {
            if (feedback.isCancelled()) {
                throw new CancellationException();
            }
            if (datasource.requireLogin()) {
                feedback.warning(datasource.getName() + " does not support anonymous login", file2);
                return;
            }
            feedback.info("Looking up subtitles by hash", file);
            subtitleDescriptor = SubtitleUtilities.lookupSubtitlesByHash(datasource, Collections.singleton(file), locale, false, bl).values().stream().flatMap(Collection::stream).findFirst().orElse(null);
            if (!this.fetch(subtitleDescriptor, file2, feedback)) continue;
            return;
        }
        for (SubtitleProvider datasource : WebServices.getSubtitleProviders(locale)) {
            if (feedback.isCancelled()) {
                throw new CancellationException();
            }
            if (datasource.requireLogin()) {
                feedback.warning(datasource.getName() + " does not support anonymous login", file2);
                return;
            }
            feedback.info("Looking up subtitles by name", file);
            subtitleDescriptor = SubtitleUtilities.findSubtitlesByName((SubtitleProvider)datasource, Collections.singleton(file), locale, null, false, bl).values().stream().flatMap(Collection::stream).findFirst().orElse(null);
            if (!this.fetch(subtitleDescriptor, file2, feedback)) continue;
            return;
        }
        feedback.warning("Failed to find subtitles", file);
    }

    public boolean fetch(SubtitleDescriptor subtitleDescriptor, File file, Feedback feedback) throws Exception {
        if (feedback.isCancelled()) {
            throw new CancellationException();
        }
        if (subtitleDescriptor == null) {
            return false;
        }
        feedback.file(subtitleDescriptor, file);
        MemoryFile memoryFile = SubtitleUtilities.fetchSubtitle(subtitleDescriptor);
        ByteBuffer byteBuffer = SubtitleUtilities.exportSubtitles(memoryFile, this.format, this.encoding);
        FileUtilities.writeFile(byteBuffer, file);
        return true;
    }
}

