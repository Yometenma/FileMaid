package net.filemaid.media;

import java.io.File;
import java.util.Optional;
import java.util.function.Function;
import net.filemaid.Logging;
import net.filemaid.MemoryCache;
import net.filemaid.Resource;
import net.filemaid.Settings;
import net.filemaid.WebServices;
import net.filemaid.media.MetaAttributes;
import net.filemaid.util.FileKey;
import net.filemaid.util.ReadOnlyFile;
import net.filemaid.web.Episode;
import net.filemaid.web.Movie;
import net.filemaid.web.SimpleDate;

public class XattrMetaInfo {
    public static final XattrMetaInfo xattr = new XattrMetaInfo(Settings.useExtendedFileAttributes(), Settings.useCreationDate());
    private final boolean useExtendedFileAttributes;
    private final boolean useCreationDate;
    private final MemoryCache<FileKey, Optional<Object>> xattrMetaInfoCache = MemoryCache.forDays();
    private final MemoryCache<FileKey, Optional<Object>> xattrOriginalNameCache = MemoryCache.forDays();

    public XattrMetaInfo(boolean bl, boolean bl2) {
        this.useExtendedFileAttributes = bl;
        this.useCreationDate = bl2;
    }

    public boolean isMetaInfo(Object object) {
        return object instanceof Episode || object instanceof Movie;
    }

    public long getTimeStamp(Object object) throws Exception {
        SimpleDate simpleDate;
        Movie movie;
        if (object instanceof Episode) {
            Episode episode = (Episode)object;
            if (episode.getAirdate() != null) {
                return episode.getAirdate().getTimeStamp();
            }
        } else if (object instanceof Movie && (movie = (Movie)object).getYear() > 0 && movie.getTmdbId() > 0 && (simpleDate = WebServices.TheMovieDB.getMovieInfo(movie, movie.getLanguage(), false).getReleased()) != null) {
            return simpleDate.getTimeStamp();
        }
        return -1L;
    }

    public Object getMetaInfo(File file) {
        return this.getXattrValue(this.xattrMetaInfoCache, this.key(file), MetaAttributes::getObject);
    }

    public String getOriginalName(File file) {
        return (String)this.getXattrValue(this.xattrOriginalNameCache, this.key(file), MetaAttributes::getOriginalName);
    }

    private Object getXattrValue(MemoryCache<FileKey, Optional<Object>> memoryCache, FileKey fileKey2, Function<MetaAttributes, Object> function) {
        if (fileKey2.getLastModified() > 0L) {
            return memoryCache.get(fileKey2, fileKey -> {
                try {
                    return Optional.ofNullable(function.apply(this.xattr(fileKey.getFile())));
                }
                catch (Exception exception) {
                    Logging.debug.warning(Logging.message("Failed to read xattr", fileKey, exception));
                    return Optional.empty();
                }
            }).orElse(null);
        }
        Optional<Object> optional = memoryCache.getIfPresent(fileKey2);
        return optional == null ? null : optional.orElse(null);
    }

    private File writable(File file) {
        if (!(file = ReadOnlyFile.asRegularFile(file)).canWrite()) {
            if (file.setWritable(true)) {
                Logging.debug.finest(Logging.message("Grant write permissions", file));
            } else {
                Logging.debug.warning(Logging.message("Failed to grant write permissions", file));
            }
        }
        return file;
    }

    private MetaAttributes xattr(File file) throws Exception {
        return new MetaAttributes(file);
    }

    private FileKey key(File file) {
        return this.useExtendedFileAttributes ? new FileKey(file, file.lastModified()) : new FileKey(file, 0L);
    }

    public void setMetaInfo(File file, Object object, String string) {
        if (!this.isMetaInfo(object)) {
            return;
        }
        Resource.Memoized<FileKey> memoized = Resource.lazy(() -> this.key(file));
        Resource.Memoized<MetaAttributes> memoized2 = Resource.lazy(() -> this.xattr(this.writable(file)));
        if (this.useCreationDate) {
            try {
                long l = this.getTimeStamp(object);
                if (l > 0L) {
                    ((MetaAttributes)memoized2.get()).setCreationDate(l);
                }
            }
            catch (Throwable throwable) {
                Logging.debug.warning(Logging.cause("Failed to set creation date", throwable));
            }
        }
        try {
            this.xattrMetaInfoCache.put((FileKey)memoized.get(), Optional.of(object));
            if (this.useExtendedFileAttributes) {
                ((MetaAttributes)memoized2.get()).setObject(object);
            }
            if (string != null && string.length() > 0 && this.getOriginalName(file) == null) {
                this.xattrOriginalNameCache.put((FileKey)memoized.get(), Optional.of(string));
                if (this.useExtendedFileAttributes) {
                    ((MetaAttributes)memoized2.get()).setOriginalName(string);
                }
            }
        }
        catch (Throwable throwable) {
            Logging.debug.warning(Logging.cause("Failed to set xattr", throwable));
        }
    }

    public void clear(File file) {
        FileKey fileKey = this.key(file);
        this.xattrMetaInfoCache.invalidate(fileKey);
        this.xattrOriginalNameCache.invalidate(fileKey);
        if (this.useExtendedFileAttributes) {
            try {
                this.xattr(this.writable(file)).clear();
            }
            catch (Throwable throwable) {
                Logging.debug.warning(Logging.cause("Failed to clear xattr", throwable));
            }
        }
    }
}

