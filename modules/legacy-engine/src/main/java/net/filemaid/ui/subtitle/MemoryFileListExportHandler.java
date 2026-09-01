package net.filemaid.ui.subtitle;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JList;
import net.filemaid.ui.transfer.ByteBufferTransferable;
import net.filemaid.ui.transfer.ClipboardHandler;
import net.filemaid.ui.transfer.TransferableExportHandler;
import net.filemaid.vfs.MemoryFile;

class MemoryFileListExportHandler
implements TransferableExportHandler,
ClipboardHandler {
    MemoryFileListExportHandler() {
    }

    public boolean canExport(JComponent jComponent) {
        JList jList = (JList)jComponent;
        return !jList.isSelectionEmpty();
    }

    public List<MemoryFile> export(JComponent jComponent) {
        JList<MemoryFile> jList = (JList<MemoryFile>)jComponent;
        return jList.getSelectedValuesList().stream().map(MemoryFile.class::cast).collect(Collectors.toList());
    }

    @Override
    public int getSourceActions(JComponent jComponent) {
        return this.canExport(jComponent) ? 3 : 0;
    }

    @Override
    public Transferable createTransferable(JComponent jComponent) {
        LinkedHashMap<String, ByteBuffer> linkedHashMap = new LinkedHashMap<String, ByteBuffer>();
        for (MemoryFile memoryFile : this.export(jComponent)) {
            linkedHashMap.put(memoryFile.getName(), memoryFile.getData());
        }
        return new ByteBufferTransferable(linkedHashMap);
    }

    @Override
    public void exportToClipboard(JComponent jComponent, Clipboard clipboard, int n) {
        clipboard.setContents(this.createTransferable(jComponent), null);
    }

    @Override
    public void exportDone(JComponent jComponent, Transferable transferable, int n) {
    }
}

