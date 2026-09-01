package net.filemaid.ui.transfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CompositeTranserable
implements Transferable {
    private final Transferable[] transferables;
    private final DataFlavor[] flavors;

    public CompositeTranserable(List<Transferable> list) {
        this.transferables = (Transferable[])list.stream().toArray(Transferable[]::new);
        this.flavors = (DataFlavor[])list.stream().flatMap(transferable -> Arrays.stream(transferable.getTransferDataFlavors())).toArray(DataFlavor[]::new);
    }

    public CompositeTranserable(Transferable ... transferableArray) {
        this.transferables = transferableArray;
        this.flavors = (DataFlavor[])Arrays.stream(transferableArray).flatMap(transferable -> Arrays.stream(transferable.getTransferDataFlavors())).toArray(DataFlavor[]::new);
    }

    @Override
    public Object getTransferData(DataFlavor dataFlavor) throws UnsupportedFlavorException, IOException {
        Transferable transferable = this.getTransferable(dataFlavor);
        if (transferable == null) {
            return null;
        }
        return transferable.getTransferData(dataFlavor);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return this.flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
        return this.getTransferable(dataFlavor) != null;
    }

    protected Transferable getTransferable(DataFlavor dataFlavor) {
        for (Transferable transferable : this.transferables) {
            if (!transferable.isDataFlavorSupported(dataFlavor)) continue;
            return transferable;
        }
        return null;
    }
}

