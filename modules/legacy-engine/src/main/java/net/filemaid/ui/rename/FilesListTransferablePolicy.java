package net.filemaid.ui.rename;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.CategoryFileFilter;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.ui.transfer.BackgroundFileTransferablePolicy;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.ExtensionFileFilter;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ReadOnlyFile;

class FilesListTransferablePolicy
extends BackgroundFileTransferablePolicy<File> {
    private final List<File> model;

    public FilesListTransferablePolicy(List<File> list) {
        this.model = list;
    }

    @Override
    protected boolean accept(List<File> list) {
        return true;
    }

    @Override
    protected void clear() {
        this.model.clear();
    }

    @Override
    protected void load(List<File> collection, TransferablePolicy.TransferAction transferAction) {
        if (transferAction == TransferablePolicy.TransferAction.LINK) {
            File[] fileArray = (File[])ReadOnlyFile.of(collection).stream().filter(File::exists).toArray(File[]::new);
            this.publish(fileArray);
            return;
        }
        if (FileUtilities.containsOnly(collection, (FileFilter)MediaTypes.LIST_FILES)) {
            List<File> expanded = this.expandTextFiles(collection).collect(Collectors.toList());
            if (!expanded.isEmpty()) {
                collection = expanded;
            }
        }
        Collection<File> collection2 = new LinkedHashSet<File>(64);
        for (File file : this.walkFileTree(collection, FileUtilities.NOT_HIDDEN, 64)) {
            this.walk(file, collection2, 64);
        }
        this.publish(collection2.toArray(new File[0]));
    }

    private void walk(File file, Collection<File> collection, int n) {
        if (n < 0) {
            return;
        }
        if (file.isFile() || MediaFileUtilities.isDiskFolder(file)) {
            collection.add(file);
            return;
        }
        if (FileUtilities.FOLDERS.accept(file)) {
            for (File file2 : FileUtilities.getChildren(file, FileUtilities.NOT_HIDDEN, FileUtilities.HUMAN_NAME_ORDER)) {
                this.walk(file2, collection, n - 1);
            }
        }
    }

    private Stream<File> expandTextFiles(List<File> list) {
        return list.stream().flatMap(file -> {
            try {
                return FileUtilities.readLines(file).stream().map(File::new).filter(File::isAbsolute).filter(File::exists);
            }
            catch (Exception exception) {
                Logging.trace("Failed to read file paths from text file: " + file, exception);
                return Stream.empty();
            }
        });
    }

    @Override
    public CategoryFileFilter getFileFilter() {
        return new CategoryFileFilter("All Files", ExtensionFileFilter.WILDCARD);
    }

    @Override
    protected void process(List<File> list) {
        this.model.addAll(list);
    }
}

