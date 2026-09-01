package net.filemaid.web;

import java.io.File;
import java.util.Collections;
import java.util.List;
import net.filemaid.MediaTypes;
import net.filemaid.MemoryCache;
import net.filemaid.util.FileKey;
import net.filemaid.web.AudioTrack;
import net.filemaid.web.MusicLookupService;

public abstract class AbstractMusicLookupService
implements MusicLookupService {
    private final MemoryCache<FileKey, List<AudioTrack>> cache = MemoryCache.forMinutes();

    protected abstract List<AudioTrack> fetchLookupResult(File var1) throws Exception;

    public boolean accept(File file) {
        return MediaTypes.AUDIO_FILES.accept(file) || MediaTypes.VIDEO_FILES.accept(file);
    }

    @Override
    public List<AudioTrack> lookup(File file) throws Exception {
        if (this.accept(file)) {
            return this.cache.get(FileKey.of(file), fileKey -> {
                try {
                    return this.fetchLookupResult(fileKey.getFile());
                }
                catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            });
        }
        return Collections.emptyList();
    }
}

