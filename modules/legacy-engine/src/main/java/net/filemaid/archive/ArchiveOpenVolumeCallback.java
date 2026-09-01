package net.filemaid.archive;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.IArchiveOpenVolumeCallback;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

class ArchiveOpenVolumeCallback
implements IArchiveOpenVolumeCallback,
IArchiveOpenCallback,
Closeable {
    private Map<String, RandomAccessFile> openedRandomAccessFileList = new HashMap<String, RandomAccessFile>();
    private String name;

    ArchiveOpenVolumeCallback() {
    }

    public Object getProperty(PropID propID) throws SevenZipException {
        switch (propID) {
            case NAME: {
                return this.name;
            }
        }
        return null;
    }

    public IInStream getStream(String string) throws SevenZipException {
        try {
            RandomAccessFile randomAccessFile = this.openedRandomAccessFileList.get(string);
            if (randomAccessFile != null) {
                randomAccessFile.seek(0L);
                this.name = string;
                return new RandomAccessFileInStream(randomAccessFile);
            }
            randomAccessFile = new RandomAccessFile(string, "r");
            this.openedRandomAccessFileList.put(string, randomAccessFile);
            this.name = string;
            return new RandomAccessFileInStream(randomAccessFile);
        }
        catch (FileNotFoundException fileNotFoundException) {
            return null;
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void close() throws IOException {
        for (RandomAccessFile randomAccessFile : this.openedRandomAccessFileList.values()) {
            randomAccessFile.close();
        }
    }

    public void setCompleted(Long l, Long l2) throws SevenZipException {
    }

    public void setTotal(Long l, Long l2) throws SevenZipException {
    }
}

