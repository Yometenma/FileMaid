package net.filemaid.ui.transfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import net.filemaid.ui.transfer.ByteBufferTransferable;
import net.filemaid.ui.transfer.FileTransferable;

public class TextFileTransferable
extends ByteBufferTransferable {
    private final String text;

    public TextFileTransferable(String string, String string2) {
        this(string, string2, StandardCharsets.UTF_8);
    }

    public TextFileTransferable(final String string, final String string2, final Charset charset) {
        super((Map<String, ByteBuffer>)new AbstractMap<String, ByteBuffer>(){

            @Override
            public Set<Map.Entry<String, ByteBuffer>> entrySet() {
                return Collections.singletonMap(string, charset.encode(string2)).entrySet();
            }
        });
        this.text = string2;
    }

    @Override
    public Object getTransferData(DataFlavor dataFlavor) throws UnsupportedFlavorException {
        if (super.isDataFlavorSupported(dataFlavor)) {
            return super.getTransferData(dataFlavor);
        }
        if (dataFlavor.isFlavorTextType()) {
            return this.text;
        }
        throw new UnsupportedFlavorException(dataFlavor);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.javaFileListFlavor, FileTransferable.uriListFlavor, DataFlavor.stringFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
        return super.isDataFlavorSupported(dataFlavor) || dataFlavor.isFlavorTextType();
    }
}

