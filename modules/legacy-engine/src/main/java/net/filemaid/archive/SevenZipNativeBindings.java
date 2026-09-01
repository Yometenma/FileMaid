package net.filemaid.archive;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.filemaid.archive.Archive;
import net.filemaid.archive.ArchiveExtractor;
import net.filemaid.archive.ArchiveOpenVolumeCallback;
import net.filemaid.archive.ExtractCallback;
import net.filemaid.archive.ExtractOutProvider;
import net.filemaid.archive.FileMapper;
import net.filemaid.archive.SevenZipLoader;
import net.filemaid.vfs.FileInfo;
import net.filemaid.vfs.SimpleFileInfo;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.IArchiveOpenVolumeCallback;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipNativeInitializationException;
import net.sf.sevenzipjbinding.impl.VolumedArchiveInStream;

public class SevenZipNativeBindings
implements ArchiveExtractor,
Closeable {
    private IInArchive inArchive;
    private ArchiveOpenVolumeCallback openVolume;

    public SevenZipNativeBindings(File file) throws IOException, SevenZipNativeInitializationException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        this.openVolume = new ArchiveOpenVolumeCallback();
        this.inArchive = !Archive.isVolumedArchive(file) ? SevenZipLoader.open(this.openVolume.getStream(file.getAbsolutePath()), this.openVolume) : SevenZipLoader.open((IInStream)new VolumedArchiveInStream(file.getAbsolutePath(), (IArchiveOpenVolumeCallback)this.openVolume), null);
    }

    public int itemCount() throws IOException {
        return this.inArchive.getNumberOfItems();
    }

    public Map<PropID, Object> getItem(int n) throws IOException {
        EnumMap<PropID, Object> enumMap = new EnumMap<PropID, Object>(PropID.class);
        for (PropID propID : PropID.values()) {
            Object object = this.inArchive.getProperty(n, propID);
            if (object == null) continue;
            enumMap.put(propID, object);
        }
        return enumMap;
    }

    @Override
    public List<FileInfo> listFiles() throws IOException {
        ArrayList<FileInfo> arrayList = new ArrayList<FileInfo>();
        for (int i = 0; i < this.inArchive.getNumberOfItems(); ++i) {
            boolean bl = (Boolean)this.inArchive.getProperty(i, PropID.IS_FOLDER);
            if (bl) continue;
            String string = (String)this.inArchive.getProperty(i, PropID.PATH);
            Long l = (Long)this.inArchive.getProperty(i, PropID.SIZE);
            if (string == null) continue;
            arrayList.add(new SimpleFileInfo(string, l != null ? l : -1L));
        }
        return arrayList;
    }

    @Override
    public void extract(File file) throws IOException {
        this.extract(new FileMapper(file));
    }

    @Override
    public void extract(File file, FileFilter fileFilter) throws IOException {
        this.extract(new FileMapper(file), fileFilter);
    }

    public void extract(ExtractOutProvider extractOutProvider) throws IOException {
        this.inArchive.extract(null, false, (IArchiveExtractCallback)new ExtractCallback(this.inArchive, extractOutProvider));
    }

    public void extract(ExtractOutProvider extractOutProvider, FileFilter fileFilter) throws IOException {
        int n;
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (int i = 0; i < this.inArchive.getNumberOfItems(); ++i) {
            String string;
            boolean isFolder = ((Boolean)this.inArchive.getProperty(i, PropID.IS_FOLDER)).booleanValue();
            if (isFolder || (string = (String)this.inArchive.getProperty(i, PropID.PATH)) == null || !fileFilter.accept(new File(string))) continue;
            arrayList.add(i);
        }
        int[] nArray = new int[arrayList.size()];
        for (n = 0; n < nArray.length; ++n) {
            nArray[n] = (Integer)arrayList.get(n);
        }
        this.inArchive.extract(nArray, false, (IArchiveExtractCallback)new ExtractCallback(this.inArchive, extractOutProvider));
    }

    @Override
    public void close() throws IOException {
        try {
            this.inArchive.close();
        }
        finally {
            this.openVolume.close();
        }
    }
}

