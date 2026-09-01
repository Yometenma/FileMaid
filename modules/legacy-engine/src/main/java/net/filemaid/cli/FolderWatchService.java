package net.filemaid.cli;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.filemaid.Logging;
import net.filemaid.util.DefaultThreadFactory;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.Timer;

public class FolderWatchService
implements Closeable {
    private final ExecutorService processor = Executors.newSingleThreadExecutor();
    private final ExecutorService watchers = Executors.newCachedThreadPool(new DefaultThreadFactory("FolderWatcher"));
    private final List<File> queue = new ArrayList<File>();
    private final Timer timer = new Timer(this::commit);
    private final boolean recursive;
    private final long delay;
    private final FileFilter filter;
    private final Consumer<List<File>> handler;

    public FolderWatchService(boolean bl, long l, FileFilter fileFilter, Consumer<List<File>> consumer) {
        this.recursive = bl;
        this.delay = l;
        this.filter = fileFilter;
        this.handler = consumer;
    }

    public synchronized void resetCommitTimer() {
        this.timer.set(this.delay, TimeUnit.MILLISECONDS, false);
    }

    private void enqueue(List<File> list) {
        this.resetCommitTimer();
        List<File> list2 = this.queue;
        synchronized (list2) {
            this.queue.addAll(list);
        }
    }

    private void process(List<File> list) {
        try {
            this.handler.accept(list);
        }
        catch (Throwable throwable) {
            Logging.debug.severe(Logging.cause("Failed to process changes", list, throwable));
        }
    }

    public synchronized void commit() {
        ArrayList<File> arrayList = new ArrayList<>();
        List<File> list = this.queue;
        synchronized (list) {
            this.queue.stream().filter(this.filter::accept).sorted().distinct().forEach(arrayList::add);
            this.queue.clear();
        }
        if (arrayList.isEmpty()) {
            return;
        }
        if (this.recursive) {
            arrayList.stream().filter(File::isDirectory).forEach(this::watchFolder);
        }
        this.processor.submit(() -> this.process(arrayList));
    }

    public synchronized void watchFolder(File file) {
        if (this.recursive) {
            for (File file2 : FileUtilities.getChildren(file, this.filter)) {
                if (!file2.isDirectory()) continue;
                this.watchFolder(file2);
            }
        }
        this.watchers.submit(new FolderWatcher(file, this.filter, this::enqueue));
    }

    @Override
    public synchronized void close() {
        this.timer.cancel();
        this.processor.shutdownNow();
        this.watchers.shutdownNow();
    }

    private static class FolderWatcher
    implements Runnable {
        private final Path node;
        private final FileFilter filter;
        private final Consumer<List<File>> handler;

        public FolderWatcher(File file, FileFilter fileFilter, Consumer<List<File>> consumer) {
            this.node = file.toPath();
            this.filter = fileFilter;
            this.handler = consumer;
        }

        @Override
        public void run() {
            Logging.debug.config(Logging.message("Start watching folder", this.node));
            try (WatchService watchService = this.node.getFileSystem().newWatchService();){
                this.node.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
                while (true) {
                    boolean bl;
                    WatchKey watchKey;
                    List list;
                    if (!(list = (watchKey = watchService.take()).pollEvents().stream().filter(watchEvent -> watchEvent.kind() != StandardWatchEventKinds.OVERFLOW).map(watchEvent -> this.node.resolve((Path)watchEvent.context()).toFile()).filter(file -> this.filter.accept((File)file)).collect(Collectors.toList())).isEmpty()) {
                        this.handler.accept(list);
                    }
                    if (bl = watchKey.reset()) continue;
                    Logging.debug.severe(Logging.message("Stop watching folder", this.node));
                    return;
                }
            }
            catch (Throwable throwable) {
                Logging.debug.severe(Logging.cause("Failed to watch folder", this.node, throwable));
                return;
            }
        }
    }
}

