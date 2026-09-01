package net.filemaid.ui.subtitle;

import java.awt.event.MouseEvent;
import java.io.File;
import java.util.EventObject;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import net.filemaid.CategoryFileFilter;
import net.filemaid.MediaTypes;
import net.filemaid.ResourceManager;
import net.filemaid.UserFiles;
import net.filemaid.ui.Mode;
import net.filemaid.ui.TargetTransferable;
import net.filemaid.ui.subtitle.SubtitlePackage;
import net.filemaid.ui.transfer.FileTransferable;
import net.filemaid.util.ui.SwingEventBus;
import net.filemaid.util.ui.SwingUI;

abstract class SubtitlePackageFeatureLink
extends SubtitlePackage {
    public static final SubtitlePackageFeatureLink EXACT_SEARCH = new SubtitlePackageFeatureLink(){

        @Override
        public String getName() {
            return "Find more subtitles via exact search...";
        }

        @Override
        public Icon getIcon() {
            return ResourceManager.getIcon("subtitle.exact.download");
        }

        @Override
        public void handle(EventObject eventObject) {
            if (eventObject instanceof MouseEvent) {
                this.handle((MouseEvent)eventObject);
                return;
            }
            this.selectFiles(false, eventObject);
        }

        private void handle(MouseEvent mouseEvent) {
            if (mouseEvent.isPopupTrigger()) {
                JPopupMenu jPopupMenu = SwingUI.newPopupMenu("Exact Search");
                jPopupMenu.add(SwingUI.newAction("Select video folder", ResourceManager.getIcon("tree.closed"), actionEvent -> SwingUI.invokeLater(50, () -> this.selectFiles(true, mouseEvent))));
                jPopupMenu.add(SwingUI.newAction("Select video files", ResourceManager.getIcon("file.generic"), actionEvent -> SwingUI.invokeLater(50, () -> this.selectFiles(false, mouseEvent))));
                jPopupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                return;
            }
            this.selectFiles(false, mouseEvent);
        }

        private void selectFiles(boolean bl, EventObject eventObject) {
            SwingUI.withWaitCursor((Object)eventObject, () -> {
                List<File> list = UserFiles.showLoadDialogSelectFiles(bl, true, null, bl ? null : new CategoryFileFilter("Video", MediaTypes.VIDEO_FILES), "Select video files", eventObject);
                if (list.size() > 0) {
                    SwingEventBus.getInstance().post(new TargetTransferable(Mode.Subtitles, new FileTransferable(list)));
                }
            });
        }
    };

    SubtitlePackageFeatureLink() {
    }

    @Override
    public abstract String getName();

    public abstract Icon getIcon();

    public abstract void handle(EventObject var1);
}

