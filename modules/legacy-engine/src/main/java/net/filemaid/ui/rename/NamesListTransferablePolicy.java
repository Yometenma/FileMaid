package net.filemaid.ui.rename;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.ListSelectionModel;
import net.filemaid.CategoryFileFilter;
import net.filemaid.ui.transfer.ArrayTransferable;
import net.filemaid.ui.transfer.BackgroundFileTransferablePolicy;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.ExtensionFileFilter;
import net.filemaid.util.FileUtilities;
import net.filemaid.vfs.VFS;
import net.filemaid.web.Episode;
import net.filemaid.web.Movie;

class NamesListTransferablePolicy
extends BackgroundFileTransferablePolicy<Object> {
    private static final DataFlavor episodeArrayFlavor = ArrayTransferable.flavor(Episode.class);
    private static final DataFlavor movieArrayFlavor = ArrayTransferable.flavor(Movie.class);
    private final List<Object> model;
    private final ListSelectionModel cursor;
    private boolean directory;
    private boolean vfs;

    public NamesListTransferablePolicy(List<Object> list, ListSelectionModel listSelectionModel) {
        this.model = list;
        this.cursor = listSelectionModel;
        this.directory = true;
        this.vfs = true;
    }

    protected NamesListTransferablePolicy(NamesListTransferablePolicy namesListTransferablePolicy, boolean bl, boolean bl2) {
        super(namesListTransferablePolicy);
        this.model = namesListTransferablePolicy.model;
        this.cursor = namesListTransferablePolicy.cursor;
        this.directory = bl;
        this.vfs = bl2;
    }

    public NamesListTransferablePolicy withDirectoryMode(boolean bl) {
        return new NamesListTransferablePolicy(this, bl, !bl);
    }

    @Override
    protected void clear() {
        this.model.clear();
    }

    @Override
    public boolean accept(Transferable transferable) throws Exception {
        return super.accept(transferable) || transferable.isDataFlavorSupported(episodeArrayFlavor) || transferable.isDataFlavorSupported(movieArrayFlavor) || transferable.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @Override
    protected boolean accept(List<File> list) {
        return true;
    }

    @Override
    public boolean importData(Transferable transferable, TransferablePolicy.TransferAction transferAction) throws Exception {
        if (transferable.isDataFlavorSupported(episodeArrayFlavor)) {
            Episode[] episodeArray = (Episode[])transferable.getTransferData(episodeArrayFlavor);
            return this.paste(Arrays.asList(episodeArray), transferAction);
        }
        if (transferable.isDataFlavorSupported(movieArrayFlavor)) {
            Movie[] movieArray = (Movie[])transferable.getTransferData(movieArrayFlavor);
            return this.paste(Arrays.asList(movieArray), transferAction);
        }
        if (super.importData(transferable, transferAction)) {
            return true;
        }
        if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            String string = transferable.getTransferData(DataFlavor.stringFlavor).toString();
            return this.paste(VFS.listTextFile(string), transferAction);
        }
        return false;
    }

    protected boolean paste(List<?> list, TransferablePolicy.TransferAction transferAction) {
        int n;
        if ((transferAction == TransferablePolicy.TransferAction.INSERT || transferAction == TransferablePolicy.TransferAction.LINK) && (n = this.cursor.getMaxSelectionIndex()) >= 0) {
            this.model.addAll(n + 1, list);
            this.cursor.setSelectionInterval(n + 1, n + list.size());
            return true;
        }
        if (transferAction == TransferablePolicy.TransferAction.PUT) {
            this.clear();
        }
        this.model.addAll(list);
        return true;
    }

    @Override
    protected void load(List<File> list, TransferablePolicy.TransferAction transferAction) throws IOException {
        if (this.vfs && FileUtilities.containsOnly(list, VFS::hasIndex)) {
            ArrayList arrayList = new ArrayList();
            for (File file : list) {
                if (!this.currentWorker().accept(file)) continue;
                arrayList.addAll(VFS.getIndex(file));
            }
            this.publish(arrayList.toArray());
        } else {
            List<File> list2 = FileUtilities.listFiles(this.walkFileTree(list, FileUtilities.NOT_HIDDEN, 64), FileUtilities.FILES, FileUtilities.HUMAN_NAME_ORDER, FileUtilities.FOLDERS, 64);
            this.publish(list2.toArray());
        }
    }

    @Override
    public CategoryFileFilter getFileFilter() {
        if (this.vfs && !this.directory) {
            return VFS.getFileFilter();
        }
        return new CategoryFileFilter("All Files", ExtensionFileFilter.WILDCARD);
    }

    @Override
    protected void process(List<Object> list) {
        this.model.addAll(list);
    }
}

