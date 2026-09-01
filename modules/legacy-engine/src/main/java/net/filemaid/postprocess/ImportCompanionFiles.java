package net.filemaid.postprocess;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.HistorySpooler;
import net.filemaid.RenameAction;
import net.filemaid.StandardRenameAction;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.postprocess.ApplyHistory;
import net.filemaid.postprocess.Feedback;
import net.filemaid.util.FileUtilities;

public class ImportCompanionFiles
implements ApplyHistory {
    private boolean isCompanionFile(File file) {
        if (FileUtilities.isThumbnailStore(file)) {
            return false;
        }
        if (FileUtilities.isXattrFolder(file)) {
            return true;
        }
        return MediaFileUtilities.CLUTTER_TYPES.accept(file) || MediaFileUtilities.EXTRA_FOLDERS.accept(file) || MediaFileUtilities.EXTRA_FILES.accept(file);
    }

    private List<File> getCompanionFiles(File file) {
        return FileUtilities.listFiles(FileUtilities.getChildren(file, this::isCompanionFile), FileUtilities.FILES, FileUtilities.HUMAN_NAME_ORDER);
    }

    @Override
    public void applyHistory(Map<File, File> map, RenameAction renameAction, Feedback feedback) throws Exception {
        LinkedHashMap<File, Set> linkedHashMap = new LinkedHashMap<File, Set>();
        map.forEach((file2, file3) -> {
            File file4 = file3.getParentFile();
            File file5 = file2.getParentFile();
            linkedHashMap.computeIfAbsent(file4, file -> new LinkedHashSet()).add(file5);
        });
        LinkedHashMap<File, Set> linkedHashMap2 = new LinkedHashMap<File, Set>();
        Set<File> set3 = this.getExcludeList(map);
        linkedHashMap.forEach((file2, set2) -> {
            List<File> list = this.getCompanionFiles((File)file2);
            if (list.isEmpty()) {
                return;
            }
            if (set2.size() > 1) {
                feedback.warning("Skip due to ambiguous target folder: " + set2, file2);
            }
            for (File file3 : list) {
                if (set3.contains(file3)) continue;
                File file4 = FileUtilities.relativize(file2, file3);
                File file5 = new File((File)set2.iterator().next(), file4.getPath());
                linkedHashMap2.computeIfAbsent(file3, file -> new LinkedHashSet()).add(file5);
            }
        });
        TreeMap<File, File> treeMap = new TreeMap<File, File>();
        linkedHashMap2.forEach((file, set) -> {
            if (set.size() > 1) {
                feedback.warning("Skip due to ambiguous target file: " + set, file);
            }
            treeMap.put(file, (File)set.iterator().next());
        });
        int n = 0;
        int n2 = treeMap.size();
        LinkedHashMap<File, File> linkedHashMap3 = new LinkedHashMap<File, File>();
        try {
            for (Map.Entry entry : treeMap.entrySet()) {
                if (feedback.isCancelled()) {
                    throw new CancellationException();
                }
                feedback.progress(n++, n2);
                File file4 = (File)entry.getKey();
                File file5 = (File)entry.getValue();
                try {
                    file5 = renameAction.resolve(file4, file5);
                    if (renameAction.canRename(file4, file5) && !file5.exists()) {
                        feedback.file(this.message(renameAction, file4), file5);
                        file5 = renameAction.rename(file4, file5);
                        linkedHashMap3.put(file4, file5);
                        continue;
                    }
                    feedback.warning("Destination file already exists: " + file5, file4);
                }
                catch (Exception exception) {
                    feedback.warning(exception, file4);
                }
            }
        }
        finally {
            HistorySpooler.HISTORY.append(linkedHashMap3);
        }
    }

    private String message(RenameAction renameAction, File file) {
        if (renameAction instanceof StandardRenameAction) {
            return ((StandardRenameAction)renameAction).getDisplayVerb() + " " + file.getName();
        }
        return renameAction + " " + file.getName();
    }

    private Set<File> getExcludeList(Map<File, File> map) {
        return map.entrySet().stream().flatMap(entry -> Stream.of((File)entry.getKey(), (File)entry.getValue())).flatMap(file -> file.isDirectory() ? FileUtilities.listFiles(file, FileUtilities.FILES).stream() : Stream.of(file)).collect(Collectors.toSet());
    }
}

