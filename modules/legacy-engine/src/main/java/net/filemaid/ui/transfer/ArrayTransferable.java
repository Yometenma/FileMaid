package net.filemaid.ui.transfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.lang.reflect.Array;

public class ArrayTransferable<T>
implements Transferable {
    private final T[] array;

    public static DataFlavor flavor(Class<?> clazz) {
        return new DataFlavor(Array.newInstance(clazz, 0).getClass(), "Array");
    }

    public ArrayTransferable(T[] TArray) {
        this.array = TArray;
    }

    public int size() {
        return this.array.length;
    }

    public T[] getArray() {
        return this.array.clone();
    }

    @Override
    public Object getTransferData(DataFlavor dataFlavor) throws UnsupportedFlavorException, IOException {
        if (this.isDataFlavorSupported(dataFlavor)) {
            return this.getArray();
        }
        return null;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{new DataFlavor(this.array.getClass(), "Array")};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
        return this.array.getClass().equals(dataFlavor.getRepresentationClass());
    }
}

