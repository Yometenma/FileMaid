package net.filemaid.ui.transfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.filemaid.Logging;
import net.filemaid.Settings;
import net.filemaid.platform.posix.GVFS;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.SystemProperty;

public class FileTransferable
implements Transferable {
    public static final boolean forceSortOrder = SystemProperty.get("net.filemaid.dnd.sort", Boolean::parseBoolean, false);
    public static final DataFlavor uriListFlavor = FileTransferable.createUriListFlavor();
    private final File[] files;

    private static DataFlavor createUriListFlavor() {
        try {
            return new DataFlavor("text/uri-list; class=java.nio.CharBuffer");
        }
        catch (ClassNotFoundException classNotFoundException) {
            throw new RuntimeException(classNotFoundException);
        }
    }

    public static boolean isFileListFlavor(DataFlavor dataFlavor) {
        return dataFlavor.isFlavorJavaFileListType() || dataFlavor.equals(uriListFlavor);
    }

    public static boolean hasFileListFlavor(Transferable transferable) {
        return transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) || transferable.isDataFlavorSupported(uriListFlavor);
    }

    public FileTransferable(File ... fileArray) {
        this.files = fileArray;
    }

    public FileTransferable(Collection<File> collection) {
        this.files = collection.toArray(new File[0]);
    }

    @Override
    public Object getTransferData(DataFlavor dataFlavor) throws UnsupportedFlavorException {
        if (dataFlavor.isFlavorJavaFileListType()) {
            return Arrays.asList(this.files);
        }
        if (dataFlavor.equals(uriListFlavor)) {
            return CharBuffer.wrap(this.getUriList());
        }
        throw new UnsupportedFlavorException(dataFlavor);
    }

    private String getUriList() {
        StringBuilder stringBuilder = new StringBuilder(80 * this.files.length);
        for (File file : this.files) {
            stringBuilder.append("file://").append(file.toURI().getRawPath());
            stringBuilder.append("\r\n");
        }
        return stringBuilder.toString();
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.javaFileListFlavor, uriListFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
        return FileTransferable.isFileListFlavor(dataFlavor);
    }

    public static List<File> getFilesFromTransferable(Transferable transferable) throws IOException, UnsupportedFlavorException {
        if (Settings.useGVFS() && transferable.isDataFlavorSupported(uriListFlavor)) {
            CharBuffer charBuffer = FileTransferable.getTransferDataFromTransferable(transferable, uriListFlavor, CharBuffer.class);
            if (charBuffer == null) {
                return Collections.emptyList();
            }
            return RegularExpressions.NEWLINE.splitAsStream(charBuffer).map(string -> {
                if (string.startsWith("#") || string.isEmpty()) {
                    return null;
                }
                try {
                    File file = GVFS.getDefaultVFS().getPathForURI((String)string);
                    if (file == null || !file.exists()) {
                        throw new FileNotFoundException(file.getPath());
                    }
                    return file;
                }
                catch (Throwable throwable) {
                    Logging.debug.warning(Logging.format("GVFS: %s => %s", string, throwable));
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }
        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            List<File> list = FileTransferable.getTransferDataFromTransferable(transferable, DataFlavor.javaFileListFlavor, List.class);
            if (list == null) {
                return Collections.emptyList();
            }
            if (forceSortOrder) {
                return list.stream().sorted(FileUtilities.HUMAN_NAME_ORDER).collect(Collectors.toList());
            }
            return list;
        }
        throw new UnsupportedFlavorException(DataFlavor.javaFileListFlavor);
    }

    private static <T> T getTransferDataFromTransferable(Transferable transferable, DataFlavor dataFlavor, Class<T> clazz) throws IOException, UnsupportedFlavorException, InvalidDnDOperationException {
        try {
            Object object = transferable.getTransferData(dataFlavor);
            if (object != null) {
                return clazz.cast(object);
            }
        }
        catch (IOException iOException) {
            throw new InvalidDnDOperationException(iOException.toString());
        }
        return null;
    }
}

