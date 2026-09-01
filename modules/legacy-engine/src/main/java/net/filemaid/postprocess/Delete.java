package net.filemaid.postprocess;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CancellationException;
import net.filemaid.RenameAction;
import net.filemaid.UserFiles;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.postprocess.ApplyHistory;
import net.filemaid.postprocess.Feedback;
import net.filemaid.util.FileUtilities;

public enum Delete implements ApplyHistory
{
    EMPTY_FOLDERS{

        @Override
        public boolean isHidden(File file) {
            return FileUtilities.isThumbnailStore(file) || FileUtilities.isSystemFolder(file) || FileUtilities.isXattrFolder(file);
        }

        @Override
        public void delete(File file) throws Exception {
            FileUtilities.delete(file);
        }

        public String toString() {
            return "Delete empty folders";
        }
    }
    ,
    CLUTTER_FILES{

        @Override
        public boolean isHidden(File file) {
            return EMPTY_FOLDERS.isHidden(file) || file.isHidden() || MediaFileUtilities.CLUTTER_TYPES.accept(file) || MediaFileUtilities.EXTRA_FOLDERS.accept(file) || MediaFileUtilities.EXTRA_FILES.accept(file);
        }

        @Override
        public void delete(File file) throws Exception {
            UserFiles.trash(file);
        }

        public String toString() {
            return "Delete clutter files";
        }
    };


    public abstract boolean isHidden(File var1);

    public abstract void delete(File var1) throws Exception;

    @Override
    public void applyHistory(Map<File, File> map, RenameAction renameAction, Feedback feedback) throws Exception {
        File[] fileArray = map.values().toArray(new File[0]);
        File[] fileArray2 = map.keySet().toArray(new File[0]);
        for (File file : MediaFileUtilities.getRemainingEmptyFolders(fileArray, fileArray2, this::isHidden)) {
            if (feedback.isCancelled()) {
                throw new CancellationException();
            }
            if (!file.exists()) continue;
            feedback.info(this, file);
            this.delete(file);
        }
    }
}

