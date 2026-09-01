package net.filemaid.ui.sfv;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import net.filemaid.CategoryFileFilter;
import net.filemaid.MediaTypes;
import net.filemaid.Settings;
import net.filemaid.hash.HashType;
import net.filemaid.hash.VerificationFileReader;
import net.filemaid.hash.VerificationUtilities;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.ui.sfv.ChecksumCell;
import net.filemaid.ui.sfv.ChecksumComputationService;
import net.filemaid.ui.sfv.ChecksumComputationTask;
import net.filemaid.ui.sfv.ChecksumTable;
import net.filemaid.ui.sfv.ChecksumTableModel;
import net.filemaid.ui.transfer.BackgroundFileTransferablePolicy;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.ExtensionFileFilter;
import net.filemaid.util.FileTree;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ReadOnlyFile;
import net.filemaid.util.ui.SwingUI;

class ChecksumTableTransferablePolicy
extends BackgroundFileTransferablePolicy<ChecksumCell> {
    private final ChecksumTable table;
    private final ChecksumTableModel model;
    private final ChecksumComputationService computationService;
    private final ThreadLocal<ExecutorService> executor = new ThreadLocal();
    private final ThreadLocal<VerificationTracker> verificationTracker = new ThreadLocal();

    public ChecksumTableTransferablePolicy(ChecksumTable checksumTable, ChecksumComputationService checksumComputationService) {
        this.table = checksumTable;
        this.model = checksumTable.getModel();
        this.computationService = checksumComputationService;
    }

    @Override
    protected boolean accept(List<File> list) {
        return true;
    }

    @Override
    protected void clear() {
        this.computationService.reset();
        this.model.clear();
    }

    @Override
    protected void submit(List<File> list, TransferablePolicy.TransferAction transferAction) {
        if (list.size() == 1 && VerificationUtilities.getHashType(list.get(0)) != null) {
            this.model.setHashType(VerificationUtilities.getHashType(list.get(0)));
        }
        super.submit(list, transferAction);
    }

    @Override
    protected void process(List<ChecksumCell> list) {
        this.model.addAll(list);
    }

    @Override
    protected void load(List<File> list, TransferablePolicy.TransferAction transferAction) throws IOException {
        BackgroundFileTransferablePolicy.BackgroundWorker backgroundWorker = this.currentWorker();
        if (Settings.isMacSandbox()) {
            MacAppUtilities.askUnlockFolders(SwingUI.getWindow(this.table), list);
        }
        this.executor.set(this.computationService.newExecutor());
        this.verificationTracker.set(new VerificationTracker(5));
        list = ReadOnlyFile.of(list);
        try {
            if (FileUtilities.containsOnly(list, (FileFilter)MediaTypes.VERIFICATION_FILES)) {
                for (File file : list) {
                    if (!backgroundWorker.accept(file)) continue;
                    this.loadVerificationFile(file, VerificationUtilities.getHashType(file));
                }
                return;
            }
            if (list.size() == 1 && FileUtilities.containsOnly(list, FileUtilities.FOLDERS)) {
                for (File file : list) {
                    for (File file2 : FileUtilities.getChildren(file)) {
                        this.load(file2, null, file, backgroundWorker);
                    }
                }
                return;
            }
            if (FileUtilities.mapByFolder(list).size() == 1) {
                for (File file : list) {
                    this.load(file, null, file.getParentFile(), backgroundWorker);
                }
                return;
            }
            FileTree fileTree = new FileTree();
            list.forEach(fileTree::add);
            for (Map.Entry<Path, List<Path>> entry : fileTree.getRoots().entrySet()) {
                File file = entry.getKey().toFile();
                for (Path path : entry.getValue()) {
                    File file3 = path.toFile().getParentFile();
                    File file4 = new File(file, path.toString());
                    this.load(ReadOnlyFile.of(file4), file3, ReadOnlyFile.of(file), backgroundWorker);
                }
            }
        }
        finally {
            this.executor.get().shutdown();
            this.executor.remove();
            this.verificationTracker.remove();
        }
    }

    protected void loadVerificationFile(File file, HashType hashType) throws IOException {
        try (VerificationFileReader verificationFileReader = hashType.newReader(file);){
            File file2 = file.getParentFile();
            while (verificationFileReader.hasNext()) {
                Map.Entry<File, String> verificationEntry = verificationFileReader.next();
                String string = FileUtilities.normalizePathSeparators(verificationEntry.getKey().getPath());
                String string2 = new String(verificationEntry.getValue());
                ChecksumCell checksumCell = new ChecksumCell(string, file, Collections.singletonMap(hashType, string2));
                ChecksumCell checksumCell2 = this.createComputationCell(string, file2, hashType);
                ChecksumCell[] checksumCellArray = new ChecksumCell[]{checksumCell, checksumCell2};
                this.publish(checksumCellArray);
            }
        }
    }

    protected void load(File file, File file2, File file3, FileFilter fileFilter) throws IOException {
        block4: {
            if (!fileFilter.accept(file) || !FileUtilities.NOT_HIDDEN.accept(file)) break block4;
            File file4 = new File(file2, file.getName());
            if (file.isDirectory()) {
                for (File file5 : FileUtilities.getChildren(file)) {
                    this.load(file5, file4, file3, fileFilter);
                }
            } else {
                String string = FileUtilities.normalizePathSeparators(file4.getPath());
                ChecksumCell[] checksumCellArray = new ChecksumCell[]{this.createComputationCell(string, file3, this.model.getHashType())};
                this.publish(checksumCellArray);
                Map<File, String> map = this.verificationTracker.get().getHashByVerificationFile(file);
                for (Map.Entry<File, String> entry : map.entrySet()) {
                    HashType hashType = this.verificationTracker.get().getVerificationFileType(entry.getKey());
                    ChecksumCell[] checksumCellArray2 = new ChecksumCell[]{new ChecksumCell(string, entry.getKey(), Collections.singletonMap(hashType, entry.getValue()))};
                    this.publish(checksumCellArray2);
                }
            }
        }
    }

    protected ChecksumCell createComputationCell(String string, File file, HashType hashType) {
        ChecksumCell checksumCell = new ChecksumCell(string, file, new ChecksumComputationTask(new File(file, string), hashType));
        this.executor.get().execute(checksumCell.getTask());
        return checksumCell;
    }

    @Override
    public CategoryFileFilter getFileFilter() {
        CategoryFileFilter categoryFileFilter = new CategoryFileFilter("All Files", ExtensionFileFilter.WILDCARD);
        for (HashType hashType : HashType.values()) {
            CategoryFileFilter categoryFileFilter2 = hashType.getFilter();
            categoryFileFilter.add(categoryFileFilter2.getDescription(), categoryFileFilter2);
        }
        return categoryFileFilter;
    }

    private static class VerificationTracker {
        private final Map<File, Integer> seen = new HashMap<File, Integer>();
        private final Map<File, Map<File, String>> cache = new HashMap<File, Map<File, String>>();
        private final Map<File, HashType> types = new HashMap<File, HashType>();
        private final int maxDepth;

        public VerificationTracker(int n) {
            this.maxDepth = n;
        }

        public Map<File, String> getHashByVerificationFile(File file) throws IOException {
            File file2 = file.getParentFile();
            for (int n = 0; file2 != null && n <= this.maxDepth; file2 = file2.getParentFile(), ++n) {
                Integer seenDepth = this.seen.get(file2);
                if (seenDepth != null && seenDepth <= n) {
                    break;
                }
                if (seenDepth == null) {
                    for (File entry : FileUtilities.getChildren(file2, MediaTypes.VERIFICATION_FILES)) {
                        HashType hashType = VerificationUtilities.getHashType(entry);
                        this.cache.put(entry, this.importVerificationFile(entry, hashType, entry.getParentFile()));
                        this.types.put(entry, hashType);
                    }
                }
                this.seen.put(file2, n);
            }
            if (this.cache.isEmpty()) {
                return Collections.emptyMap();
            }
            HashMap<File, String> hashMap = new HashMap<>(2);
            for (Map.Entry<File, Map<File, String>> entry : this.cache.entrySet()) {
                String string = entry.getValue().get(file);
                if (string == null) continue;
                hashMap.put(entry.getKey(), string);
            }
            return hashMap;
        }

        public HashType getVerificationFileType(File file) {
            return this.types.get(file);
        }

        private Map<File, String> importVerificationFile(File file, HashType hashType, File file2) throws IOException {
            HashMap<File, String> hashMap = new HashMap<File, String>();
            try (VerificationFileReader verificationFileReader = hashType.newReader(file);){
                while (verificationFileReader.hasNext()) {
                    Map.Entry<File, String> verificationEntry = verificationFileReader.next();
                    hashMap.put(new File(file2, verificationEntry.getKey().getPath()), new String(verificationEntry.getValue()));
                }
            }
            return hashMap;
        }
    }
}

