package net.filemaid.ui.rename;

import java.io.File;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.Action;
import javax.swing.Icon;
import net.filemaid.StandardRenameAction;
import net.filemaid.util.ui.GlassProgressMonitor;

class StandardRenameWorker
implements GlassProgressMonitor.ProgressWorker<Map<File, File>> {
    private final Map<File, File> renameMap;
    private final Map<File, File> renameLog;
    private final StandardRenameAction action;
    private final Action owner;

    public StandardRenameWorker(Map<File, File> map, Map<File, File> map2, StandardRenameAction standardRenameAction, Action action) {
        this.renameMap = map;
        this.renameLog = map2;
        this.action = standardRenameAction;
        this.owner = action;
    }

    @Override
    public String getName() {
        return this.action.getDisplayVerb() + " " + this.renameMap.size() + " " + (this.renameMap.size() == 1 ? "file" : "files");
    }

    @Override
    public Icon getIcon() {
        return (Icon)this.owner.getValue("SmallIcon");
    }

    @Override
    public String getDescription() {
        return "Preparing...";
    }

    @Override
    public boolean isIndeterminate() {
        return false;
    }

    @Override
    public Map<File, File> call(Consumer<String> consumer, Consumer<String> consumer2, BiConsumer<Integer, Integer> biConsumer, Supplier<Boolean> supplier) throws Exception {
        for (Map.Entry<File, File> entry : this.renameMap.entrySet()) {
            if (supplier.get().booleanValue()) break;
            biConsumer.accept(this.renameLog.size(), this.renameMap.size());
            consumer2.accept(entry.getKey().getName());
            File file = entry.getKey();
            File file2 = this.action.resolve(entry.getKey(), entry.getValue());
            if (this.action.canRename(file, file2)) {
                file2 = this.action.rename(file, file2);
            }
            this.renameLog.put(file, file2);
        }
        return this.renameLog;
    }
}

