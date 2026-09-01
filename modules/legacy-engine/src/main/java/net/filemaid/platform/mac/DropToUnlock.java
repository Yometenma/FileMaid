package net.filemaid.platform.mac;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.UserData;
import net.filemaid.UserFiles;
import net.filemaid.UserInteraction;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.ui.HeaderPanel;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.transfer.DefaultTransferHandler;
import net.filemaid.ui.transfer.FileTransferable;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.util.ui.notification.SeparatorBorder;
import net.miginfocom.swing.MigLayout;

public class DropToUnlock
extends JList<File> {
    public static final Map<String, String> persistentSecurityScopedBookmarks = UserData.forPackage(DropToUnlock.class).node("SecurityScopedBookmarks").asMap();
    private static final File VOLUMES_FOLDER = new File("/Volumes");
    private final RoundRectangle2D dropArea = new RoundRectangle2D.Double(0.0, 0.0, 0.0, 0.0, 20.0, 20.0);
    private final BasicStroke dashedStroke = new BasicStroke(1.0f, 1, 1, 10.0f, new float[]{5.0f}, 0.0f);

    public static void unlockBySecurityScopedBookmarks(List<File> list) {
        Map<String, String> map = persistentSecurityScopedBookmarks;
        synchronized (map) {
            Set<String> set = persistentSecurityScopedBookmarks.keySet();
            for (File file2 : list) {
                Optional<File> optional = FileUtilities.listPath(file2).stream().filter(file -> set.contains(file.getPath())).findFirst();
                if (!optional.isPresent() || !MacAppUtilities.isLockedFolder(file2)) continue;
                try {
                    MacAppUtilities.NSURL_URLByResolvingBookmarkData_startAccessingSecurityScopedResource(persistentSecurityScopedBookmarks.get(optional.get().getPath()));
                }
                catch (Throwable throwable) {
                    Logging.debug.severe(Logging.cause("NSURL.URLByResolvingBookmarkData.startAccessingSecurityScopedResource", throwable));
                }
            }
        }
    }

    public static void storeSecurityScopedBookmarks(List<File> list) {
        Map<String, String> map = persistentSecurityScopedBookmarks;
        synchronized (map) {
            Set<String> set = persistentSecurityScopedBookmarks.keySet();
            for (File file : list) {
                if (!Collections.disjoint(set, FileUtilities.listPath(file)) || MacAppUtilities.isLockedFolder(file)) continue;
                try {
                    String string = MacAppUtilities.NSURL_bookmarkDataWithOptions(file.getPath());
                    persistentSecurityScopedBookmarks.put(file.getPath(), string);
                }
                catch (Throwable throwable) {
                    Logging.debug.severe(Logging.cause("NSURL.bookmarkDataWithOptions", throwable));
                }
            }
        }
    }

    public static List<File> getParentFolders(Collection<File> collection) {
        return collection.stream().map(file -> file.isDirectory() ? file : file.getParentFile()).sorted().distinct().filter(file -> !file.exists() || MacAppUtilities.isLockedFolder(file)).map(file -> {
            try {
                List<File> object;
                File file2 = file.getCanonicalFile();
                File file3 = MediaFileUtilities.getStructureRoot(file2);
                if (VOLUMES_FOLDER.equals(file3) && (object = FileUtilities.listPath(file2)).size() >= 3) {
                    return FileUtilities.listPath(file2).get(2);
                }
                if (file3 == null || file3.getName().isEmpty() || file3.getParentFile() == null || file3.getParentFile().getName().isEmpty()) {
                    for (File file4 : FileUtilities.listPathTailReverse(file2)) {
                        if (!file4.isDirectory()) continue;
                        return file4;
                    }
                }
                return file3;
            }
            catch (Exception exception) {
                Logging.trace(exception);
                return null;
            }
        }).filter(file -> file != null && !file.getName().isEmpty() && MacAppUtilities.isLockedFolder(file)).sorted().distinct().collect(Collectors.toList());
    }

    public static boolean showUnlockFoldersDialog(Window window, Collection<File> collection) {
        final List<File> list = DropToUnlock.getParentFolders(collection);
        if (list.isEmpty()) {
            return true;
        }
        DropToUnlock.unlockBySecurityScopedBookmarks(list);
        if (list.stream().allMatch(file -> !MacAppUtilities.isLockedFolder(file))) {
            return true;
        }
        FutureTask<Boolean> futureTask = new FutureTask<Boolean>(() -> {
            final JDialog jDialog = new JDialog(window);
            final AtomicBoolean atomicBoolean = new AtomicBoolean(true);
            DropToUnlock dropToUnlock = new DropToUnlock(list){

                @Override
                public void updateLockStatus(File ... fileArray) {
                    super.updateLockStatus(fileArray);
                    if (list.stream().allMatch(file -> !MacAppUtilities.isLockedFolder(file))) {
                        atomicBoolean.set(false);
                        SwingUI.invokeLater(750, () -> jDialog.setVisible(false));
                        SwingUI.invokeLater(1000, () -> Desktop.getDesktop().requestForeground(true));
                    } else {
                        list.stream().filter(file -> MacAppUtilities.isLockedFolder(file)).findFirst().ifPresent(file -> SwingUI.invokeLater(250, () -> UserInteraction.reveal(file)));
                    }
                }
            };
            dropToUnlock.setBorder(BorderFactory.createEmptyBorder(5, 15, 120, 15));
            JComponent jComponent = (JComponent)jDialog.getContentPane();
            jComponent.setLayout((LayoutManager)new MigLayout("insets 0, fill"));
            HeaderPanel headerPanel = new HeaderPanel();
            headerPanel.getTitleLabel().setText("Folder Permissions Required");
            headerPanel.getTitleLabel().setIcon(ResourceManager.getIcon("file.lock"));
            headerPanel.getTitleLabel().setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 64));
            JLabel jLabel = new JLabel("<html>FileBot does not have permission to access the folder above. To allow FileBot access, drag and drop the folder from Finder onto the drop area above</b>. The permissions for this folder (and all the folders it contains) will be remembered and FileBot will not need to ask for it again.</html>");
            jLabel.setBorder(BorderFactory.createCompoundBorder(ThemeSupport.getSeparatorBorder(SeparatorBorder.Position.TOP), BorderFactory.createTitledBorder("About App Sandboxing")));
            jComponent.add((Component)headerPanel, "wmin 150px, hmin 75px, growx, dock north");
            jComponent.add((Component)dropToUnlock, "wmin 150px, hmin 150px, grow");
            jComponent.add((Component)jLabel, "wmin 150px, hmin 75px, growx, aligny center, dock south");
            jDialog.setModal(true);
            jDialog.setModalExclusionType(Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
            jDialog.setSize(new Dimension(540, 500));
            jDialog.setResizable(false);
            jDialog.setLocationByPlatform(true);
            jDialog.setAlwaysOnTop(true);
            SwingUI.invokeLater(500, () -> UserInteraction.revealFiles(list));
            jDialog.setVisible(true);
            return !atomicBoolean.get();
        });
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                futureTask.run();
            } else {
                SwingUtilities.invokeAndWait(futureTask);
            }
            if (((Boolean)futureTask.get()).booleanValue()) {
                DropToUnlock.storeSecurityScopedBookmarks(list);
                return true;
            }
            return false;
        }
        catch (InterruptedException | InvocationTargetException | ExecutionException exception) {
            throw new RuntimeException("Failed to request permissions: " + exception.getMessage(), exception);
        }
    }

    public DropToUnlock(Collection<File> collection) {
        super(collection.toArray(new File[0]));
        this.setLayoutOrientation(2);
        this.setVisibleRowCount(-1);
        this.setCellRenderer(new FolderLockCellRenderer());
        this.setCursor(Cursor.getPredefinedCursor(12));
        this.addMouseListener(SwingUI.mouseClicked(this::onClick));
        this.setTransferHandler(new DefaultTransferHandler(new FolderDropPolicy(), null));
    }

    protected void onClick(MouseEvent mouseEvent) {
        File file;
        int n = this.locationToIndex(mouseEvent.getPoint());
        if (n >= 0 && this.getCellBounds(n, n).contains(mouseEvent.getPoint()) && MacAppUtilities.isLockedFolder(file = (File)this.getModel().getElementAt(n)) && null != UserFiles.showOpenDialogSelectFolder(file, "Grant Permission", mouseEvent)) {
            this.updateLockStatus(file);
        }
    }

    protected void updateLockStatus(File ... fileArray) {
        this.repaint();
        Stream.of(fileArray).filter(file -> MacAppUtilities.isLockedFolder(file)).forEach(file -> {
            try {
                String string = Files.getOwner(file.toPath(), new LinkOption[0]).getName();
                String string2 = PosixFilePermissions.toString(Files.getPosixFilePermissions(file.toPath(), new LinkOption[0]));
                String string3 = file.isDirectory() ? "+d" : "-d";
                String string4 = file.canRead() ? "+r" : "-r";
                String string5 = file.canExecute() ? "+x" : "-x";
                String string6 = Optional.ofNullable(file).map(File::list).map(stringArray -> ((String[])stringArray).length).map(Object::toString).orElse("");
                Logging.log.log(Level.SEVERE, Logging.format("Permission denied: %s (%s %s) [%s %s %s] [%s]", file, string2, string, string3, string4, string5, string6));
            }
            catch (Exception exception) {
                Logging.log.log(Level.SEVERE, exception, Logging.format("Permission denied: %s", file));
            }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D graphics2D = (Graphics2D)graphics;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int n = 300;
        int n2 = 70;
        int n3 = 20;
        graphics2D.setColor(Color.lightGray);
        this.dropArea.setFrameFromCenter(this.getWidth() / 2, this.getHeight() - n2 / 2 - n3 - 10, (this.getWidth() - n) / 2, this.getHeight() - n2 - 2 * n3);
        graphics2D.setStroke(this.dashedStroke);
        graphics2D.draw(this.dropArea);
        graphics2D.setColor(Color.gray);
        graphics2D.setFont(graphics2D.getFont().deriveFont(2, 36.0f));
        graphics2D.drawString("Drop 'em", (int)this.dropArea.getMinX() + 15, (int)this.dropArea.getMinY() + 40);
        graphics2D.drawString("to Unlock 'em", (int)this.dropArea.getMinX() + 45, (int)this.dropArea.getMinY() + 40 + 35);
    }

    protected static class FolderLockCellRenderer
    extends DefaultListCellRenderer {
        protected FolderLockCellRenderer() {
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(100, 100);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> jList, Object object, int n, boolean bl, boolean bl2) {
            File file = (File)object;
            JLabel jLabel = (JLabel)super.getListCellRendererComponent(jList, file.getName(), n, false, false);
            jLabel.setIcon(ResourceManager.getIcon(MacAppUtilities.isLockedFolder(file) ? "folder.locked" : "folder.open"));
            jLabel.setHorizontalTextPosition(0);
            jLabel.setVerticalTextPosition(3);
            jLabel.setToolTipText(file.getAbsolutePath());
            return jLabel;
        }
    }

    protected class FolderDropPolicy
    extends TransferablePolicy {
        protected FolderDropPolicy() {
        }

        @Override
        public boolean accept(Transferable transferable) throws Exception {
            return true;
        }

        @Override
        public void handleTransferable(Transferable transferable, TransferablePolicy.TransferAction transferAction) throws Exception {
            List<File> list = FileTransferable.getFilesFromTransferable(transferable);
            if (list == null || list.isEmpty()) {
                return;
            }
            List<File> list2 = FileUtilities.filter(list, FileUtilities.FOLDERS);
            if (list2.isEmpty()) {
                return;
            }
            DropToUnlock.this.updateLockStatus(list2.toArray(new File[0]));
        }
    }
}

