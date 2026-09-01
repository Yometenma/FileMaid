package net.filemaid;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.filemaid.ApplicationFolder;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.Logging;
import net.filemaid.Settings;
import net.filemaid.WebServices;
import net.filemaid.format.ExpressionEngine;
import net.filemaid.util.FileUtilities;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.pool.Size;
import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.SizeOfEngineFactory;

public class DiskStore {
    private Directory directory;
    private CacheManager manager;
    private final Map<String, Cache> caches = new HashMap<String, Cache>();

    public DiskStore() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "DiskStoreShutdownHook"));
    }

    public synchronized void acquire() throws IOException {
        if (this.manager != null) {
            throw new IllegalStateException(Status.STATUS_ALIVE.toString());
        }
        this.directory = this.acquireDiskStore();
        this.manager = CacheManager.newInstance((Configuration)new Configuration().diskStore(new DiskStoreConfiguration().path(this.directory.toString())));
    }

    private String getCacheName(String string, CacheType cacheType, boolean bl) {
        if (bl) {
            return string.toLowerCase(Locale.ROOT) + "_" + cacheType.ordinal() + "c";
        }
        return string.toLowerCase(Locale.ROOT) + "_" + cacheType.ordinal();
    }

    public synchronized Cache getCache(String string2, CacheType cacheType, boolean bl) {
        if (this.manager == null) {
            throw new IllegalStateException(Status.STATUS_UNINITIALISED.toString());
        }
        return this.caches.computeIfAbsent(this.getCacheName(string2, cacheType, bl), string -> {
            if (!this.manager.cacheExists(string)) {
                this.manager.addCache(new net.sf.ehcache.Cache(cacheType.getConfiguration((String)string)));
            }
            Cache cache = new Cache(this.manager.getCache(string), cacheType);
            if (bl) {
                return cache.concurrent();
            }
            return cache;
        });
    }

    public synchronized void clear(Predicate<String> predicate) {
        if (this.manager == null) {
            return;
        }
        long l = Stream.of(this.manager.getCacheNames()).filter(predicate).sorted().map(arg_0 -> ((CacheManager)this.manager).getCache(arg_0)).filter(cache -> {
            if (cache.getSize() > 0) {
                try {
                    cache.removeAll();
                    return true;
                }
                catch (Exception exception) {
                    Logging.debug.warning(Logging.cause(cache.getName(), "clear", exception));
                }
            }
            return false;
        }).count();
        Logging.log.info((String)(l == 0L ? "There are no active caches." : (l == 1L ? "1 active cache has been cleared." : l + " active caches have been cleared.")));
    }

    public synchronized void flush() {
        if (this.manager == null) {
            return;
        }
        for (String string : this.manager.getCacheNames()) {
            try {
                this.manager.getCache(string).flush();
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause(string, "flush", exception));
            }
        }
        WebServices.requestPool().execute(ExpressionEngine::getExpressionEngine);
    }

    public synchronized void shutdown() {
        if (this.manager == null) {
            return;
        }
        if (this.manager.getStatus().equals(Status.STATUS_ALIVE)) {
            try {
                this.manager.shutdown();
                this.manager = null;
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause(this.directory, "shutdown", exception));
            }
            Logging.debug.finest(Logging.message(this.directory, "shutdown", this.caches.keySet()));
            this.caches.clear();
            try {
                this.directory.release();
                this.directory = null;
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause(this.directory, "release", exception));
            }
        }
    }

    private Directory acquireDiskStore() throws IOException {
        for (int i = 0; i < 10; ++i) {
            File file = ApplicationFolder.Cache.resolve(Integer.toString(i));
            FileUtilities.createFolders(file);
            if (!file.canRead()) {
                throw new AccessDeniedException("Cache directory is not readable: " + file);
            }
            if (!file.canWrite()) {
                throw new AccessDeniedException("Cache directory is not writable: " + file);
            }
            File file2 = new File(file, ".lock");
            boolean bl = !file2.exists();
            FileChannel fileChannel = FileChannel.open(file2.toPath(), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
            FileLock fileLock = fileChannel.tryLock();
            if (fileLock != null) {
                Logging.debug.config(Logging.message("Using persistent disk cache", file));
                FileUtilities.checkDiskSpace(file);
                int n = Settings.getApplicationRevisionNumber();
                int n2 = 0;
                if (fileChannel.size() > 0L) {
                    try {
                        n2 = new Scanner((ReadableByteChannel)fileChannel, "UTF-8").nextInt();
                    }
                    catch (Exception exception) {
                        Logging.trace(exception);
                    }
                }
                if (n2 != n && n > 0 && !bl) {
                    Logging.debug.warning(Logging.format("Current application revision (r%s) does not match cache revision (r%s)", n, n2));
                    bl = true;
                    this.clearDiskStore(file);
                }
                if (bl) {
                    Logging.debug.warning(Logging.message("Initialize new disk cache", file));
                    fileChannel.position(0L);
                    fileChannel.write(StandardCharsets.UTF_8.encode(Integer.toString(n)));
                    fileChannel.truncate(fileChannel.position());
                    fileChannel.force(true);
                }
                return new Directory(file, fileLock, fileChannel);
            }
            fileChannel.close();
        }
        throw new IOException("Unable to acquire cache lock: " + ApplicationFolder.Cache.getDirectory());
    }

    private void clearDiskStore(File file2) {
        FileUtilities.getChildren(file2, FileUtilities.FILES).stream().filter(file -> !file.getName().startsWith(".")).forEach(file -> {
            try {
                FileUtilities.delete(file);
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause(file, "delete", exception));
            }
        });
    }

    static {
        net.sf.ehcache.pool.SizeOfEngineLoader.INSTANCE.load(SizeOfEngineLoader.class, false);
    }

    private static class Directory {
        public final File path;
        public final FileLock lock;
        public final FileChannel lockFile;

        public Directory(File file, FileLock fileLock, FileChannel fileChannel) {
            this.path = file;
            this.lock = fileLock;
            this.lockFile = fileChannel;
        }

        public void release() throws IOException {
            this.lock.release();
            this.lockFile.close();
        }

        public String toString() {
            return this.path.getPath();
        }
    }

    public static class SizeOfEngineLoader
    implements SizeOfEngine,
    SizeOfEngineFactory {
        public SizeOfEngine createSizeOfEngine(int n, boolean bl, boolean bl2) {
            Logging.trace("ehcache", new UnsupportedOperationException("createSizeOfEngine"));
            return this;
        }

        public Size sizeOf(Object object, Object object2, Object object3) {
            Logging.trace("ehcache", new UnsupportedOperationException("sizeOf"));
            return new Size(0L, false);
        }

        public SizeOfEngine copyWith(int n, boolean bl) {
            Logging.trace("ehcache", new UnsupportedOperationException("copyWith"));
            return this;
        }
    }
}

