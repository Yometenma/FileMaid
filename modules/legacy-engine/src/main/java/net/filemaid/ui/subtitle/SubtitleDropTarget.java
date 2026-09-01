package net.filemaid.ui.subtitle;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import net.filemaid.CategoryFileFilter;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.ResourceManager;
import net.filemaid.Settings;
import net.filemaid.UserFiles;
import net.filemaid.WebServices;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.subtitle.SubtitleAutoMatchDialog;
import net.filemaid.ui.subtitle.upload.SubtitleUploadDialog;
import net.filemaid.ui.transfer.FileTransferable;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.Datasource;
import net.filemaid.web.SubtitleLookupService;
import net.filemaid.web.SubtitleProvider;

abstract class SubtitleDropTarget
extends JButton {
    private Color lineColor = ThemeSupport.getColor(0xD7D7D7);
    private final DropTargetAdapter dropHandler = new DropTargetAdapter(){

        @Override
        public void dragEnter(DropTargetDragEvent dropTargetDragEvent) {
            DropAction dropAction = DropAction.Accept;
            try {
                List<File> list = FileTransferable.getFilesFromTransferable(dropTargetDragEvent.getTransferable());
                if (list.size() > 0) {
                    dropAction = SubtitleDropTarget.this.getDropAction(list);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
            SubtitleDropTarget.this.setDropAction(dropAction);
            if (dropAction != DropAction.Cancel) {
                dropTargetDragEvent.acceptDrag(0x40000000);
            } else {
                dropTargetDragEvent.rejectDrag();
            }
        }

        @Override
        public void dragExit(DropTargetEvent dropTargetEvent) {
            SubtitleDropTarget.this.setDropAction(DropAction.Accept);
        }

        @Override
        public void drop(DropTargetDropEvent dropTargetDropEvent) {
            dropTargetDropEvent.acceptDrop(0x40000000);
            boolean bl = false;
            try {
                List<File> list = FileTransferable.getFilesFromTransferable(dropTargetDropEvent.getTransferable());
                boolean bl2 = bl = SubtitleDropTarget.this.getDropAction(list) != DropAction.Cancel;
                if (bl) {
                    SwingUtilities.invokeLater(() -> SubtitleDropTarget.this.handleDrop(list));
                }
            }
            catch (Exception exception) {
                Logging.log.log(Level.WARNING, exception.getMessage(), exception);
            }
            dropTargetDropEvent.dropComplete(bl);
            this.dragExit(dropTargetDropEvent);
        }
    };

    public SubtitleDropTarget() {
        this.setHorizontalAlignment(0);
        this.setHorizontalTextPosition(0);
        this.setVerticalAlignment(0);
        this.setVerticalTextPosition(1);
        this.setIconTextGap(0);
        this.setHideActionText(true);
        this.setContentAreaFilled(false);
        this.setFocusPainted(false);
        this.setBorderPainted(false);
        this.setBorder(BorderFactory.createEmptyBorder());
        this.setBackground(ThemeSupport.getPanelBackground());
        this.setDropAction(DropAction.Accept);
        this.setCursor(Cursor.getPredefinedCursor(12));
        this.addActionListener(actionEvent -> {
            List<File> list = UserFiles.showLoadDialogSelectFiles(true, true, null, this.getFileFilter(), "Select Video Files", actionEvent);
            if (list.size() > 0 && this.getDropAction(list) != DropAction.Cancel) {
                this.handleDrop(list);
            }
        });
        new DropTarget(this, this.dropHandler);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D)graphics;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Ellipse2D.Float float_ = new Ellipse2D.Float(0.0f, 0.0f, this.getWidth() - 1, this.getHeight() - 1);
        graphics2D.setColor(this.getBackground());
        graphics2D.fill(float_);
        graphics2D.setColor(this.lineColor);
        graphics2D.draw(float_);
        super.paintComponent(graphics2D);
    }

    protected void setDropAction(DropAction dropAction) {
        this.setIcon(this.getIcon(dropAction));
    }

    protected abstract WebServices.OpenSubtitlesClient getSubtitleService();

    protected abstract CategoryFileFilter getFileFilter();

    protected abstract void handleDrop(List<File> var1);

    protected abstract DropAction getDropAction(List<File> var1);

    protected abstract Icon getIcon(DropAction var1);

    public static enum DropAction {
        Accept,
        Cancel;

    }

    public static abstract class Upload
    extends SubtitleDropTarget {
        @Override
        public CategoryFileFilter getFileFilter() {
            return null;
        }

        @Override
        protected DropAction getDropAction(List<File> list) {
            return FileUtilities.filter(list, MediaTypes.SUBTITLE_FILES).size() > 0 || FileUtilities.filter(list, FileUtilities.FOLDERS).size() > 0 ? DropAction.Accept : DropAction.Cancel;
        }

        @Override
        protected void handleDrop(List<File> list) {
            this.setCursor(Cursor.getPredefinedCursor(3));
            try {
                if (Settings.isMacSandbox()) {
                    MacAppUtilities.askUnlockFolders(SwingUI.getWindow(this), list);
                }
                List<File> list2 = FileUtilities.listFiles(list, FileUtilities.FILES, FileUtilities.HUMAN_NAME_ORDER);
                List<File> list3 = FileUtilities.filter(list2, MediaTypes.VIDEO_FILES);
                List<File> list4 = FileUtilities.filter(list2, MediaTypes.SUBTITLE_FILES);
                LinkedHashMap<File, File> linkedHashMap = new LinkedHashMap<File, File>();
                for (File file : list4) {
                    File file2 = this.getVideoForSubtitle(file, FileUtilities.filter(list3, FileUtilities.newParentFilter(file.getParentFile())));
                    linkedHashMap.put(file, file2);
                }
                if (linkedHashMap.size() > 0) {
                    SwingUtilities.invokeLater(() -> this.handleUpload(linkedHashMap));
                }
            }
            finally {
                this.setCursor(Cursor.getPredefinedCursor(12));
            }
        }

        protected void handleUpload(Map<File, File> map) {
            SubtitleUploadDialog subtitleUploadDialog = new SubtitleUploadDialog(this.getSubtitleService(), SwingUI.getWindow(this));
            subtitleUploadDialog.setIconImage(SwingUI.getImage(this.getIcon(DropAction.Accept)));
            subtitleUploadDialog.setDefaultCloseOperation(2);
            subtitleUploadDialog.setSize(950, 575);
            subtitleUploadDialog.setLocation(SwingUI.getOffsetLocation(subtitleUploadDialog));
            subtitleUploadDialog.setUploadPlan(map);
            subtitleUploadDialog.startChecking();
            subtitleUploadDialog.setVisible(true);
        }

        protected File getVideoForSubtitle(File file, List<File> list) {
            return this.findMatch(file, list, FileUtilities::getName).orElseGet(() -> this.findMatch(file, FileUtilities.getChildren(file.getParentFile(), MediaTypes.VIDEO_FILES), FileUtilities::getName).orElse(null));
        }

        private Optional<File> findMatch(File file, List<File> list, Function<File, String> function) {
            String string = function.apply(file).toLowerCase();
            for (File file2 : list) {
                if (string.length() <= 0 || !string.startsWith(function.apply(file2).toLowerCase())) continue;
                return Optional.of(file2);
            }
            return Optional.empty();
        }

        @Override
        protected Icon getIcon(DropAction dropAction) {
            if (dropAction == DropAction.Accept) {
                return ResourceManager.getIcon("subtitle.exact.upload");
            }
            return ResourceManager.getIcon("message.error");
        }
    }

    public static abstract class Download
    extends SubtitleDropTarget {
        public abstract SubtitleLookupService[] getVideoHashSubtitleServices();

        public abstract SubtitleProvider[] getSubtitleProviders();

        public abstract Locale getQueryLanguage();

        @Override
        public CategoryFileFilter getFileFilter() {
            return new CategoryFileFilter("Video", MediaTypes.VIDEO_FILES);
        }

        @Override
        protected DropAction getDropAction(List<File> list) {
            return FileUtilities.filter(list, MediaTypes.VIDEO_FILES, FileUtilities.FOLDERS).size() > 0 ? DropAction.Accept : DropAction.Cancel;
        }

        @Override
        protected void handleDrop(List<File> list) {
            if (this.getQueryLanguage() == null) {
                Logging.log.info("Please select your preferred subtitle language.");
                return;
            }
            List<File> list2 = FileUtilities.listFiles(list, (FileFilter)MediaTypes.VIDEO_FILES, FileUtilities.HUMAN_NAME_ORDER);
            if (list2.size() > 0) {
                this.handleDownload(list2);
            }
        }

        protected boolean handleDownload(Collection<File> collection) {
            SubtitleAutoMatchDialog subtitleAutoMatchDialog = new SubtitleAutoMatchDialog(SwingUI.getWindow(this));
            subtitleAutoMatchDialog.setVideoFiles(collection.toArray(new File[0]));
            for (SubtitleLookupService datasource : this.getVideoHashSubtitleServices()) {
                subtitleAutoMatchDialog.addSubtitleService(datasource);
            }
            for (SubtitleProvider datasource : this.getSubtitleProviders()) {
                subtitleAutoMatchDialog.addSubtitleService(datasource);
            }
            subtitleAutoMatchDialog.startQuery(this.getQueryLanguage());
            subtitleAutoMatchDialog.setIconImage(SwingUI.getImage(this.getIcon(DropAction.Accept)));
            subtitleAutoMatchDialog.setDefaultCloseOperation(2);
            subtitleAutoMatchDialog.setSize(1050, 600);
            subtitleAutoMatchDialog.setLocationRelativeTo(subtitleAutoMatchDialog.getOwner());
            subtitleAutoMatchDialog.setVisible(true);
            return true;
        }

        @Override
        protected Icon getIcon(DropAction dropAction) {
            if (dropAction == DropAction.Accept) {
                return ResourceManager.getIcon("subtitle.exact.download");
            }
            return ResourceManager.getIcon("message.error");
        }
    }
}

