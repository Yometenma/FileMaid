package net.filemaid;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import net.filemaid.History;
import net.filemaid.Logging;
import net.filemaid.util.ByteBufferInputStream;
import net.filemaid.util.ByteBufferOutputStream;

public final class HistorySpooler {
    public static final HistorySpooler HISTORY = new HistorySpooler();
    private final History sessionHistory = new History();
    private History persistentHistory = new History();
    private File persistentHistoryFile = null;
    private long persistentHistoryLastModified = 0L;
    private Map<File, File> sessionReverseRenameMap = null;
    private Map<File, File> persistentReverseRenameMap = null;

    public synchronized void setPersistentHistoryFile(File file) {
        this.persistentHistoryFile = file;
    }

    private synchronized History getPersistentHistory() {
        long l;
        if (this.persistentHistoryFile != null && (l = this.persistentHistoryFile.lastModified()) > this.persistentHistoryLastModified) {
            try {
                ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(this.persistentHistoryFile.length());
                try (FileChannel fileChannel = FileChannel.open(this.persistentHistoryFile.toPath(), StandardOpenOption.READ);
                     FileLock fileLock = fileChannel.lock(0L, Long.MAX_VALUE, true);){
                    byteBufferOutputStream.transferFully(fileChannel);
                }
                this.persistentHistory = History.importHistory(new ByteBufferInputStream(byteBufferOutputStream.getByteBuffer()));
                this.persistentHistoryLastModified = l;
                this.persistentReverseRenameMap = null;
            }
            catch (FileLockInterruptionException fileLockInterruptionException) {
                throw new CancellationException("Interrupted while waiting to acquire a read lock: " + this.persistentHistoryFile);
            }
            catch (Exception exception) {
                Logging.log.severe(Logging.cause("Failed to read history file", this.persistentHistoryFile, exception));
            }
        }
        return this.persistentHistory;
    }

    private synchronized Map<File, File> getSessionReverseRenameMap() {
        if (this.sessionReverseRenameMap == null) {
            this.sessionReverseRenameMap = this.sessionHistory.getReverseRenameMap();
        }
        return this.sessionReverseRenameMap;
    }

    private synchronized Map<File, File> getPersistentReverseRenameMap() {
        if (this.persistentReverseRenameMap == null) {
            this.persistentReverseRenameMap = this.getPersistentHistory().getReverseRenameMap();
        }
        return this.persistentReverseRenameMap;
    }

    public File getOriginalPath(File file) {
        File file2 = this.getSessionReverseRenameMap().get(file);
        if (file2 != null) {
            return file2;
        }
        return this.getPersistentReverseRenameMap().get(file);
    }

    public synchronized History getSessionHistory() {
        return new History(this.sessionHistory.sequences);
    }

    public synchronized History getCompleteHistory() {
        History history = this.getSessionHistory();
        history.add(this.getPersistentHistory());
        return history;
    }

    private void seekForward(ByteBuffer byteBuffer, int n, char c, int n2) {
        for (int i = n; i < n + n2; ++i) {
            if (byteBuffer.get(i) != c) continue;
            byteBuffer.position(i + 1);
            return;
        }
        throw new NoSuchElementException("<history> not found");
    }

    private void seekBackward(FileChannel fileChannel, long l, char c, int n) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(n);
        fileChannel.read(byteBuffer, l - (long)n);
        for (int i = n - 1; i >= 0; --i) {
            if (byteBuffer.get(i) != c) continue;
            fileChannel.position(l - (long)n + (long)i + 1L);
            return;
        }
        throw new NoSuchElementException("</history> not found");
    }

    public synchronized void commit() {
        if (this.sessionHistory.sequences().isEmpty() || this.persistentHistoryFile == null) {
            return;
        }
        try {
            boolean bl = this.persistentHistoryFile.exists();
            ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
            History.exportHistory(this.sessionHistory, bl, byteBufferOutputStream);
            ByteBuffer byteBuffer = byteBufferOutputStream.getByteBuffer();
            try (FileChannel fileChannel = FileChannel.open(this.persistentHistoryFile.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
                 FileLock fileLock = fileChannel.lock(0L, Long.MAX_VALUE, false);){
                if (bl) {
                    this.seekForward(byteBuffer, 8, '\n', 8);
                    this.seekBackward(fileChannel, fileChannel.size() - 8L, '\n', 8);
                }
                fileChannel.write(byteBuffer);
                fileChannel.truncate(fileChannel.position());
                fileChannel.force(false);
            }
            this.sessionHistory.clear();
            this.sessionReverseRenameMap = null;
            this.persistentHistory.clear();
            this.persistentHistoryLastModified = 0L;
            this.persistentReverseRenameMap = null;
        }
        catch (Exception exception) {
            Logging.log.severe(Logging.cause("Failed to write history file", this.persistentHistoryFile, exception));
        }
    }

    public synchronized void append(Map<File, File> map) {
        ArrayList<History.Element> arrayList = new ArrayList<History.Element>();
        map.forEach((file, file2) -> {
            if (file != null && file2 != null) {
                File file3 = file.getParentFile();
                String string = file.getName();
                String string2 = file3.equals(file2.getParentFile()) ? file2.getName() : file2.getPath();
                arrayList.add(new History.Element(string, string2, file3));
            }
        });
        if (arrayList.size() > 0) {
            this.sessionHistory.add(arrayList);
            this.sessionReverseRenameMap = null;
        }
    }

    public synchronized void append(History history) {
        this.sessionHistory.merge(history);
        this.sessionReverseRenameMap = null;
    }

    public synchronized int getSessionCount() {
        return this.sessionHistory.size();
    }

    public synchronized int getTotalCount() {
        return this.getSessionCount() + this.getPersistentHistory().size();
    }

    public void commitOnExit() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::commit, "HistorySpoolerShutdownHook"));
    }
}

