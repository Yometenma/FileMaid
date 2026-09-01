package net.filemaid.media;

import java.io.File;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.CachedResource;
import net.filemaid.Logging;
import net.filemaid.MetaAttributeView;
import net.filemaid.Settings;
import net.filemaid.util.CompressorXattrView;
import net.filemaid.util.FileKey;
import net.filemaid.util.SystemProperty;
import net.filemaid.util.XattrView;

public abstract class CachedFileAttribute
implements CachedResource.Transform<FileKey, String> {
    protected final CachedResource.Transform<FileKey, String> read;
    private static final boolean XATTR_CACHE = SystemProperty.get("net.filemaid.xattr.cache", Boolean::parseBoolean, Settings.useExtendedFileAttributes());

    public CachedFileAttribute(CachedResource.Transform<FileKey, String> transform) {
        this.read = transform;
    }

    public String get(File file) throws Exception {
        return (String)this.transform(FileKey.of(file.getAbsoluteFile()));
    }

    public boolean copy(File file, File file2) {
        return false;
    }

    public static CachedFileAttribute cache(String string, String string2, CachedResource.Transform<FileKey, String> transform) {
        Cache cache = Cache.getCache(string, CacheType.Monthly);
        if (string2 == null || !XATTR_CACHE) {
            return new LocalCache(cache, transform);
        }
        return new LocalCache(cache, new XattrCache(string2, true, transform));
    }

    public static class LocalCache
    extends CachedFileAttribute {
        private final Cache cache;

        public LocalCache(Cache cache, CachedResource.Transform<FileKey, String> transform) {
            super(transform);
            this.cache = cache;
        }

        private String key(FileKey fileKey) {
            return fileKey.getFile() + "@" + fileKey.getLastModified();
        }

        @Override
        public String transform(FileKey fileKey) throws Exception {
            return (String)this.cache.computeIfAbsent(this.key(fileKey), element -> this.read.transform(fileKey));
        }

        @Override
        public boolean copy(File file, File file2) {
            Object object;
            FileKey fileKey = FileKey.of(file.getAbsoluteFile());
            FileKey fileKey2 = FileKey.of(file2.getAbsoluteFile());
            if (fileKey2.getLastModified() <= 0L) {
                return false;
            }
            if (fileKey.getLastModified() <= 0L) {
                fileKey = new FileKey(fileKey.getFile(), fileKey2.getLastModified());
            }
            if ((object = this.cache.get(this.key(fileKey))) != null) {
                this.cache.put(this.key(fileKey2), object);
                return true;
            }
            return false;
        }
    }

    public static class XattrCache
    extends CachedFileAttribute {
        private final String xattrKey;
        private final String xattrKeyLastModified;
        private final boolean xz;

        public XattrCache(String string, boolean bl, CachedResource.Transform<FileKey, String> transform) {
            super(transform);
            this.xattrKey = string;
            this.xattrKeyLastModified = string + ".mtime";
            this.xz = bl;
        }

        @Override
        public String transform(FileKey fileKey) throws Exception {
            String string;
            String string2;
            MetaAttributeView metaAttributeView = this.xattr(fileKey.getFile());
            String string3 = Long.toString(fileKey.getLastModified());
            if (metaAttributeView != null && string3.equals(string2 = metaAttributeView.get(this.xattrKeyLastModified)) && (string = metaAttributeView.get(this.xattrKey)) != null) {
                return string;
            }
            string2 = (String)this.read.transform(fileKey);
            if (metaAttributeView != null) {
                Logging.debug.finest(Logging.format("Write [xattr:%s] %s", this.xattrKey, string2));
                metaAttributeView.put(this.xattrKey, string2);
                Logging.debug.finest(Logging.format("Write [xattr:%s] %s", this.xattrKeyLastModified, string3));
                metaAttributeView.put(this.xattrKeyLastModified, string3);
            }
            return string2;
        }

        private MetaAttributeView xattr(File file) {
            try {
                XattrView xattrView = MetaAttributeView.getXattrView(file);
                if (this.xz) {
                    xattrView = CompressorXattrView.compress(xattrView, false);
                }
                return new MetaAttributeView(xattrView);
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause(this.xattrKey, file, exception));
                return null;
            }
        }
    }
}

