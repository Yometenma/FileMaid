package net.filemaid.ui.rename;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.LicenseError;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.Settings;
import net.filemaid.StandardRenameAction;
import net.filemaid.UserInteraction;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.postprocess.Apply;
import net.filemaid.similarity.Match;
import net.filemaid.ui.rename.ConflictDialog;
import net.filemaid.ui.rename.NativeRenameWorker;
import net.filemaid.ui.rename.PostProcessWorker;
import net.filemaid.ui.rename.RenameModel;
import net.filemaid.ui.rename.StandardRenameWorker;
import net.filemaid.ui.rename.ValidateDialog;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.GlassProgressMonitor;
import net.filemaid.util.ui.SwingUI;

class RenameAction
extends AbstractAction {
    public static final String RENAME_ACTION = "RENAME_ACTION";
    public static final String APPLY_ACTIONS = "APPLY_ACTIONS";
    private final RenameModel model;

    public RenameAction(RenameModel renameModel) {
        this.model = renameModel;
    }

    public void configure(StandardRenameAction standardRenameAction) {
        if (standardRenameAction == StandardRenameAction.MOVE) {
            this.putValue(RENAME_ACTION, StandardRenameAction.MOVE);
            this.putValue("Name", "Rename");
            this.putValue("SmallIcon", ResourceManager.getIcon("action.rename"));
        } else {
            this.putValue(RENAME_ACTION, standardRenameAction);
            this.putValue("Name", standardRenameAction.getDisplayName());
            this.putValue("SmallIcon", ResourceManager.getIcon("action." + standardRenameAction.name().toLowerCase(Locale.ROOT)));
        }
    }

    public void configurePostProcess(Apply ... applyArray) {
        this.putValue(APPLY_ACTIONS, applyArray.clone());
    }

    public Icon getIcon() {
        return (Icon)this.getValue("SmallIcon");
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (this.model.size() == 0 && SwingUI.isShiftOrAltDown(actionEvent) && Settings.LICENSE.isFile()) {
            UserInteraction.showLicensePopup(actionEvent);
            return;
        }
        if (this.model.candidates().isEmpty() || this.model.values().isEmpty()) {
            Logging.log.info("No match to rename. Please <Load> files and <Fetch Data> first.");
            return;
        }
        Window window = SwingUI.getWindow(actionEvent.getSource());
        SwingUI.disableSuddenTermination(window, () -> {
            Object object;
            StandardRenameAction standardRenameAction = (StandardRenameAction)this.getValue(RENAME_ACTION);
            Apply[] applyArray = (Apply[])this.getValue(APPLY_ACTIONS);
            List<Match<File, Object>> list = this.model.getMatchModel();
            LinkedHashMap<File, File> linkedHashMap = new LinkedHashMap<File, File>();
            Map<File, File> map = null;
            try {
                Settings.LICENSE.check();
                Cache.DISK_STORE.flush();
                map = this.validate(this.model.getRenameMap(), window);
                if (map == null || map.isEmpty()) {
                    return;
                }
                if (Settings.useNativeShell() && NativeRenameWorker.isSupported(standardRenameAction)) {
                    object = new NativeRenameWorker(map, linkedHashMap, standardRenameAction, window);
                    ((NativeRenameWorker)object).run();
                } else {
                    object = new StandardRenameWorker(map, linkedHashMap, standardRenameAction, this);
                    GlassProgressMonitor.runTask((GlassProgressMonitor.ProgressWorker<?>)object, window);
                }
            }
            catch (LicenseError licenseError) {
                if (Settings.LICENSE.isFile() && !licenseError.isNetworkError()) {
                    UserInteraction.showLicensePopup("License Required", licenseError.getMessage(), actionEvent);
                } else {
                    Logging.log.severe(licenseError::getMessage);
                }
            }
            catch (CancellationException cancellationException) {
                Logging.trace(cancellationException);
            }
            catch (InvalidPathException | ExecutionException exception) {
                Logging.log.severe(Logging.cause(standardRenameAction, exception));
            }
            catch (IllegalStateException illegalStateException) {
                Logging.log.warning(illegalStateException::getMessage);
            }
            catch (Throwable throwable) {
                Logging.log.log(Level.SEVERE, throwable, Logging.cause(standardRenameAction, throwable));
            }
            if (linkedHashMap.isEmpty()) {
                return;
            }
            Logging.log.info(standardRenameAction.getDisplayStatus(linkedHashMap.size()));
            linkedHashMap.forEach((file, file2) -> this.model.removeMatch(this.model.files().indexOf(file)));
            object = new PostProcessWorker(linkedHashMap, list, standardRenameAction, applyArray);
            File[] fileArray = GlassProgressMonitor.runTask((GlassProgressMonitor.ProgressWorker<File[]>)object, window);
            if (fileArray.length > 0) {
                Logging.log.info(Logging.format("%,d %s added.", fileArray.length, fileArray.length == 1 ? "file" : "files"));
            }
        });
    }

    private Map<File, File> validate(Map<File, File> map, Window window) throws Exception {
        if (Settings.isUnixFS() || ValidateDialog.validate((Component)window, map)) {
            if (Settings.isMacSandbox()) {
                MacAppUtilities.askUnlockFolders(window, map.entrySet().stream().flatMap(entry -> Stream.of((File)entry.getKey(), FileUtilities.resolveSibling((File)entry.getKey(), (File)entry.getValue()))).collect(Collectors.toList()));
            }
            if (ConflictDialog.check(window, map)) {
                return map;
            }
        }
        return Collections.emptyMap();
    }
}

