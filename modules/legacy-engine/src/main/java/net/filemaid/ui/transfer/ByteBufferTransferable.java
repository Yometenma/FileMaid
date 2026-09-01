package net.filemaid.ui.transfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import net.filemaid.ui.transfer.FileTransferable;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.TemporaryFolder;

public class ByteBufferTransferable
implements Transferable {
    protected final Map<String, ByteBuffer> vfs;
    private FileTransferable transferable;

    public ByteBufferTransferable(Map<String, ByteBuffer> map) {
        this.vfs = map;
    }

    @Override
    public Object getTransferData(DataFlavor dataFlavor) throws UnsupportedFlavorException {
        if (FileTransferable.isFileListFlavor(dataFlavor)) {
            try {
                if (this.transferable == null) {
                    this.transferable = this.createFileTransferable();
                }
                return this.transferable.getTransferData(dataFlavor);
            }
            catch (IOException iOException) {
                throw new UncheckedIOException(iOException);
            }
        }
        throw new UnsupportedFlavorException(dataFlavor);
    }

    protected FileTransferable createFileTransferable() throws IOException {
        ArrayList<File> arrayList = new ArrayList<File>();
        for (Map.Entry<String, ByteBuffer> entry : this.vfs.entrySet()) {
            String string = entry.getKey();
            ByteBuffer byteBuffer = entry.getValue().duplicate();
            arrayList.add(this.createTemporaryFile(string, byteBuffer));
        }
        return new FileTransferable(arrayList);
    }

    protected File createTemporaryFile(String string, ByteBuffer byteBuffer) throws IOException {
        String string2 = FileUtilities.validateFileName(string);
        File file = TemporaryFolder.getFolder("dnd").createFile(string2);
        FileUtilities.writeFile(byteBuffer, file);
        return file;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.javaFileListFlavor, FileTransferable.uriListFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
        return FileTransferable.isFileListFlavor(dataFlavor);
    }
}

