package net.filemaid.ui.rename;

import java.io.File;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.HistorySpooler;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.ResourceManager;
import net.filemaid.StandardPostProcessAction;
import net.filemaid.StandardRenameAction;
import net.filemaid.media.MediaInfoTable;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.FeedbackSpooler;
import net.filemaid.postprocess.Script;
import net.filemaid.similarity.Match;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.GlassProgressMonitor;

class PostProcessWorker
implements GlassProgressMonitor.ProgressWorker<File[]> {
    private final Map<File, File> renameLog;
    private final List<Match<File, Object>> matches;
    private final StandardRenameAction operation;
    private final Apply[] apply;

    public PostProcessWorker(Map<File, File> map, List<Match<File, Object>> list, StandardRenameAction standardRenameAction, Apply[] applyArray) {
        this.renameLog = map;
        this.matches = list;
        this.operation = standardRenameAction;
        this.apply = applyArray;
    }

    private Map<File, Match<File, ?>> getDestinationMap() {
        LinkedHashMap linkedHashMap = new LinkedHashMap(this.matches.size());
        for (Match<File, Object> match : this.matches) {
            File file = match.getValue();
            File file2 = this.renameLog.get(file);
            if (file2 == null) continue;
            linkedHashMap.put(FileUtilities.resolveSibling(file, file2), match);
        }
        return linkedHashMap;
    }

    @Override
    public String getName() {
        return "Processing...";
    }

    @Override
    public Icon getIcon() {
        return ResourceManager.getIcon("script.palette");
    }

    @Override
    public String getDescription() {
        return "Preparing...";
    }

    @Override
    public boolean isIndeterminate() {
        return true;
    }

    @Override
    public File[] call(Consumer<String> consumer, Consumer<String> consumer2, BiConsumer<Integer, Integer> biConsumer, Supplier<Boolean> supplier) throws Exception {
        HistorySpooler.HISTORY.append(this.renameLog);
        HistorySpooler.HISTORY.commit();
        Map<File, Match<File, ?>> map = this.getDestinationMap();
        consumer.accept("Add extended attributes");
        map.forEach((file, match) -> {
            consumer2.accept(file.getName());
            File file2 = (File)match.getValue();
            long l = file2.isFile() ? file2.lastModified() : file.lastModified();
            XattrMetaInfo.xattr.setMetaInfo((File)file, match.getCandidate(), file2.getName());
            file.setLastModified(l);
            if (MediaTypes.VIDEO_FILES.accept(file2)) {
                MediaInfoTable.copy(file2, file);
            }
        });
        File[] fileArray = this.applyPostProcess(map, consumer, consumer2, biConsumer, supplier);
        Cache.DISK_STORE.flush();
        HistorySpooler.HISTORY.commit();
        if (this.operation == StandardRenameAction.MOVE) {
            for (StandardPostProcessAction standardPostProcessAction : EnumSet.of(StandardPostProcessAction.PRUNE)) {
                consumer.accept(this.getLabel(standardPostProcessAction));
                consumer2.accept(this.getDescription());
                biConsumer.accept(-1, -1);
                standardPostProcessAction.apply(map, this.operation, new FeedbackSpooler(consumer2, biConsumer, supplier, FeedbackSpooler.Record::info));
            }
        }
        return fileArray;
    }

    public String getLabel(Apply apply) {
        if (apply instanceof StandardPostProcessAction) {
            return ((StandardPostProcessAction)apply).getLabel();
        }
        if (apply instanceof Script) {
            return ((Script)apply).getName();
        }
        return null;
    }

    public File[] applyPostProcess(Map<File, Match<File, ?>> map, Consumer<String> consumer, Consumer<String> consumer2, BiConsumer<Integer, Integer> biConsumer, Supplier<Boolean> supplier) throws Exception {
        if (this.apply == null || this.apply.length == 0) {
            return new File[0];
        }
        FeedbackSpooler feedbackSpooler = new FeedbackSpooler(consumer2, biConsumer, supplier);
        for (Apply apply : this.apply) {
            consumer.accept(this.getLabel(apply));
            consumer2.accept(this.getDescription());
            biConsumer.accept(-1, -1);
            try {
                apply.apply(map, this.operation, feedbackSpooler);
            }
            catch (Exception exception) {
                Logging.trace(apply, exception);
            }
        }
        feedbackSpooler.warnings().forEach(Logging.debug::warning);
        return (File[])feedbackSpooler.files().filter(File::isFile).toArray(File[]::new);
    }
}

