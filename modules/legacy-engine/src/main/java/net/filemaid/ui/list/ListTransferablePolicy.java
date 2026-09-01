package net.filemaid.ui.list;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import net.filemaid.CategoryFileFilter;
import net.filemaid.ui.list.ListMode;
import net.filemaid.ui.transfer.ArrayTransferable;
import net.filemaid.ui.transfer.BackgroundFileTransferablePolicy;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.ExtensionFileFilter;
import net.filemaid.util.FileUtilities;
import net.filemaid.vfs.VFS;
import net.filemaid.web.Episode;

class ListTransferablePolicy
extends BackgroundFileTransferablePolicy<Object> {
    private static final DataFlavor episodeArrayFlavor = ArrayTransferable.flavor(Episode.class);
    private Consumer<String> title;
    private Consumer<ListMode> mode;
    private Consumer<List<?>> model;

    public ListTransferablePolicy(Consumer<String> consumer, Consumer<ListMode> consumer2, Consumer<List<?>> consumer3) {
        this.title = consumer;
        this.mode = consumer2;
        this.model = consumer3;
    }

    @Override
    public boolean accept(Transferable transferable) throws Exception {
        return super.accept(transferable) || transferable.isDataFlavorSupported(episodeArrayFlavor);
    }

    @Override
    protected boolean accept(List<File> list) {
        return true;
    }

    @Override
    public boolean importData(Transferable transferable, TransferablePolicy.TransferAction transferAction) throws Exception {
        Episode[] episodeArray;
        if (transferable.isDataFlavorSupported(episodeArrayFlavor) && (episodeArray = (Episode[])transferable.getTransferData(episodeArrayFlavor)).length > 0) {
            this.reset();
            this.model.accept(Collections.emptyList());
            this.mode.accept(ListMode.Episode);
            this.title.accept(episodeArray[0].getSeriesName());
            this.model.accept(Arrays.asList(episodeArray));
            return true;
        }
        return super.importData(transferable, TransferablePolicy.TransferAction.LINK);
    }

    @Override
    protected void clear() {
        this.model.accept(Collections.emptyList());
        this.mode.accept(ListMode.File);
    }

    @Override
    protected void load(List<File> list, TransferablePolicy.TransferAction transferAction) throws IOException {
        if (FileUtilities.containsOnly(list, VFS::hasIndex)) {
            this.publish(ListMode.File, list.get(0).getName());
            ArrayList arrayList = new ArrayList();
            for (File file : list) {
                if (!this.currentWorker().accept(file)) continue;
                arrayList.addAll(VFS.getIndex(file));
            }
            this.publish(arrayList.toArray());
        } else {
            this.publish(ListMode.File, FileUtilities.getFolderName(list.size() == 1 && list.get(0).isDirectory() ? list.get(0) : list.get(0).getParentFile()));
            List<File> list2 = FileUtilities.listFiles(this.walkFileTree(list, FileUtilities.NOT_HIDDEN, 64), FileUtilities.FILES, FileUtilities.HUMAN_NAME_ORDER, FileUtilities.FOLDERS, 64);
            this.publish(list2.toArray());
        }
    }

    public void publish(ListMode listMode, String string) {
        SwingUtilities.invokeLater(() -> {
            this.mode.accept(listMode);
            this.title.accept(string);
        });
    }

    @Override
    public CategoryFileFilter getFileFilter() {
        CategoryFileFilter categoryFileFilter = new CategoryFileFilter("All Files", ExtensionFileFilter.WILDCARD);
        VFS.getFileFilter().each(categoryFileFilter::add);
        return categoryFileFilter;
    }

    @Override
    protected void process(List<Object> list) {
        this.model.accept(list);
    }
}

