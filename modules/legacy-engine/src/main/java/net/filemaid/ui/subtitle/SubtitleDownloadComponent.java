package net.filemaid.ui.subtitle;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import java.awt.Component;
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EventObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;
import net.filemaid.CategoryFileFilter;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.ResourceManager;
import net.filemaid.UserData;
import net.filemaid.UserFiles;
import net.filemaid.UserInteraction;
import net.filemaid.subtitle.SubtitleFormat;
import net.filemaid.subtitle.SubtitleUtilities;
import net.filemaid.ui.subtitle.MemoryFileListExportHandler;
import net.filemaid.ui.subtitle.SubtitlePackage;
import net.filemaid.ui.subtitle.SubtitlePackageCellRenderer;
import net.filemaid.ui.subtitle.SubtitlePackageFeatureLink;
import net.filemaid.ui.subtitle.SubtitleViewer;
import net.filemaid.ui.transfer.DefaultTransferHandler;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.ListView;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.vfs.MemoryFile;
import net.miginfocom.swing.MigLayout;

class SubtitleDownloadComponent
extends JComponent {
    private EventList<SubtitlePackage> packages = new BasicEventList();
    private EventList<MemoryFile> files = new BasicEventList();
    private SubtitlePackageCellRenderer renderer = new SubtitlePackageCellRenderer();
    private JTextField filterEditor = new JTextField();

    public SubtitleDownloadComponent() {
        JList<SubtitlePackage> jList = new JList<SubtitlePackage>(this.createPackageListModel());
        jList.setCellRenderer(this.renderer);
        jList.setPrototypeCellValue(SubtitlePackageFeatureLink.EXACT_SEARCH);
        DefaultEventSelectionModel defaultEventSelectionModel = new DefaultEventSelectionModel(this.packages);
        defaultEventSelectionModel.setSelectionMode(103);
        jList.setSelectionModel((ListSelectionModel)defaultEventSelectionModel);
        jList.addMouseListener(SwingUI.mouseDoubleClicked(mouseEvent -> this.handle(jList.getSelectedValuesList(), (EventObject)mouseEvent)));
        jList.addMouseListener(SwingUI.mousePopupTriggerClicked(mouseEvent -> {
            List<SubtitlePackage> list;
            int n = jList.locationToIndex(mouseEvent.getPoint());
            if (n >= 0 && !jList.isSelectedIndex(n)) {
                jList.setSelectedIndex(n);
            }
            if ((list = jList.getSelectedValuesList()).isEmpty()) {
                return;
            }
            SubtitlePackageFeatureLink subtitlePackageFeatureLink = this.getFeatureLink(list);
            if (subtitlePackageFeatureLink != null) {
                if (list.removeIf(subtitlePackage -> !subtitlePackage.isDownload()) && !((SubtitlePackage)jList.getModel().getElementAt(n)).isDownload()) {
                    jList.clearSelection();
                    jList.setSelectedValue(subtitlePackageFeatureLink, false);
                    subtitlePackageFeatureLink.handle((EventObject)mouseEvent);
                    return;
                }
                for (int i = 0; i < jList.getModel().getSize(); ++i) {
                    if (((SubtitlePackage)jList.getModel().getElementAt(i)).isDownload()) continue;
                    jList.removeSelectionInterval(i, i);
                }
            }
            JPopupMenu jPopupMenu = SwingUI.newPopupMenu("Subtitles");
            JMenuItem jMenuItem = jPopupMenu.add(SwingUI.newAction("Download", ResourceManager.getIcon("package.fetch"), actionEvent -> this.handle(list, (EventObject)actionEvent)));
            jMenuItem.setEnabled(this.isPending(list));
            jPopupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        }));
        ListView listView = new ListView(this.createFileListModel()){

            @Override
            protected String convertValueToText(Object object) {
                MemoryFile memoryFile = (MemoryFile)object;
                return memoryFile.getName();
            }

            @Override
            protected Icon convertValueToIcon(Object object) {
                return MediaTypes.SUBTITLE_FILES.accept(object.toString()) ? ResourceManager.getIcon("file.subtitle") : ResourceManager.getIcon("file.generic");
            }
        };
        DefaultEventSelectionModel defaultEventSelectionModel2 = new DefaultEventSelectionModel(this.files);
        defaultEventSelectionModel2.setSelectionMode(103);
        listView.setSelectionModel((ListSelectionModel)defaultEventSelectionModel2);
        MemoryFileListExportHandler memoryFileListExportHandler = new MemoryFileListExportHandler();
        listView.setTransferHandler(new DefaultTransferHandler(null, memoryFileListExportHandler, memoryFileListExportHandler));
        listView.setDragEnabled(true);
        listView.addMouseListener(SwingUI.mouseDoubleClicked(mouseEvent -> this.open(listView.getSelectedValuesList().toArray())));
        listView.addMouseListener(SwingUI.mousePopupTriggerClicked(mouseEvent -> {
            Object[] objectArray;
            int n = listView.locationToIndex(mouseEvent.getPoint());
            if (n >= 0 && !listView.isSelectedIndex(n)) {
                listView.setSelectedIndex(n);
            }
            if ((objectArray = listView.getSelectedValuesList().toArray()).length > 0) {
                JPopupMenu jPopupMenu = SwingUI.newPopupMenu("Subtitles");
                jPopupMenu.add(SwingUI.newAction("Preview", ResourceManager.getIcon("action.find"), actionEvent -> this.open(objectArray)));
                jPopupMenu.add(SwingUI.newAction("Save As...", ResourceManager.getIcon("action.save"), actionEvent -> this.save(objectArray, (EventObject)mouseEvent)));
                jPopupMenu.add(SwingUI.newAction("Export...", ResourceManager.getIcon("action.export"), actionEvent -> this.export(objectArray, (EventObject)mouseEvent)));
                jPopupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            }
        }));
        JButton jButton = SwingUI.createImageButton(SwingUI.newAction("Clear Filter", ResourceManager.getIcon("edit.clear"), actionEvent -> this.filterEditor.setText("")));
        jButton.setOpaque(false);
        this.setLayout((LayoutManager)new MigLayout("nogrid, fill, novisualpadding", "[fill]", "[pref!][fill]"));
        this.add((Component)new JLabel("Filter:"), "gap indent:push");
        this.add((Component)this.filterEditor, "wmin 120px, gap rel");
        this.add((Component)jButton, "w pref!, h pref!");
        this.add((Component)new JScrollPane(jList), "newline, hmin 80px");
        JScrollPane jScrollPane = new JScrollPane(listView);
        jScrollPane.setViewportBorder(new LineBorder(listView.getBackground()));
        this.add((Component)jScrollPane, "newline, hmin max(80px, 30%)");
        SwingUI.installAction(jList, 10, SwingUI.newAction("Fetch", actionEvent -> this.handle(jList.getSelectedValuesList(), (EventObject)actionEvent)));
        SwingUI.installAction((JComponent)listView, 10, SwingUI.newAction("Open", actionEvent -> this.open(listView.getSelectedValuesList().toArray())));
    }

    protected ListModel<SubtitlePackage> createPackageListModel() {
        EventList<SubtitlePackage> filterList = this.getPackageModel();
        TextFilterator<SubtitlePackage> textFilterator = (list, subtitlePackage) -> {
            if (subtitlePackage.isDownload()) {
                list.add(subtitlePackage.getName());
                if (subtitlePackage.getLanguage() != null) {
                    list.add(subtitlePackage.getLanguage().getName());
                }
            }
        };
        filterList = new FilterList<>(filterList, (MatcherEditor)new TextComponentMatcherEditor((JTextComponent)this.filterEditor, textFilterator));
        filterList = new ObservableElementList<>(filterList, GlazedLists.beanConnector(SubtitlePackage.class));
        return new DefaultEventListModel<>(filterList);
    }

    protected ListModel<MemoryFile> createFileListModel() {
        EventList<MemoryFile> sortedList = this.getFileModel();
        sortedList = new SortedList<>(sortedList, Comparator.comparing(MemoryFile::getName, String.CASE_INSENSITIVE_ORDER));
        return new DefaultEventListModel<>(sortedList);
    }

    public EventList<SubtitlePackage> getPackageModel() {
        return this.packages;
    }

    public EventList<MemoryFile> getFileModel() {
        return this.files;
    }

    public void setLanguageVisible(boolean bl) {
        this.renderer.getLanguageLabel().setVisible(bl);
    }

    private void handle(List<SubtitlePackage> list, EventObject eventObject) {
        SubtitlePackageFeatureLink subtitlePackageFeatureLink = this.getFeatureLink(list);
        if (subtitlePackageFeatureLink != null) {
            subtitlePackageFeatureLink.handle(eventObject);
            return;
        }
        for (SubtitlePackage subtitlePackage : list) {
            this.fetch(subtitlePackage);
        }
    }

    private void fetch(final SubtitlePackage subtitlePackage) {
        if (!subtitlePackage.isDownload() || subtitlePackage.getDownload().isStarted()) {
            return;
        }
        subtitlePackage.addPropertyChangeListener(new PropertyChangeListener(){

            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                if (propertyChangeEvent.getNewValue() == SubtitlePackage.Download.Phase.DONE) {
                    try {
                        SubtitleDownloadComponent.this.files.addAll((Collection)subtitlePackage.getDownload().get());
                    }
                    catch (CancellationException cancellationException) {
                        Logging.trace(cancellationException);
                    }
                    catch (Exception exception) {
                        Logging.log.log(Level.WARNING, exception, Logging.cause(exception));
                        subtitlePackage.reset();
                    }
                    subtitlePackage.removePropertyChangeListener(this);
                }
            }
        });
        subtitlePackage.getDownload().start();
    }

    private void open(Object[] objectArray) {
        SwingUI.withWaitCursor((Object)this, () -> {
            for (Object object : objectArray) {
                MemoryFile memoryFile = (MemoryFile)object;
                if (!MediaTypes.SUBTITLE_FILES.accept(memoryFile.getName())) continue;
                SubtitleViewer subtitleViewer = new SubtitleViewer(memoryFile.getName());
                subtitleViewer.getTitleLabel().setText("Subtitle Viewer");
                subtitleViewer.getInfoLabel().setText(memoryFile.getName());
                subtitleViewer.setData(SubtitleUtilities.decodeSubtitles(memoryFile));
                if (objectArray.length == 1) {
                    UserData.forPackage(SubtitleDownloadComponent.class).node("viewer").restoreWindowBounds(subtitleViewer, window -> SwingUI.getOffsetLocation(window));
                }
                SwingUI.showLater(subtitleViewer);
            }
        });
    }

    private void save(Object[] objectArray, EventObject eventObject) {
        SwingUI.withWaitCursor((Object)this, () -> {
            LinkedHashMap<File, MemoryFile> linkedHashMap = new LinkedHashMap<File, MemoryFile>();
            if (objectArray.length == 1) {
                MemoryFile memoryFile = (MemoryFile)objectArray[0];
                File file = new File(FileUtilities.validateFileName(memoryFile.getName()));
                File file2 = UserFiles.showSaveDialogSelectFile(file, new CategoryFileFilter("Subtitles", MediaTypes.SUBTITLE_FILES), "Save Subtitles", eventObject);
                if (file2 != null) {
                    linkedHashMap.put(file2, memoryFile);
                }
            } else {
                File file = UserFiles.showOpenDialogSelectFolder(null, "Save Subtitles", eventObject);
                if (file != null) {
                    for (Object object : objectArray) {
                        MemoryFile memoryFile = (MemoryFile)object;
                        File file3 = new File(file, FileUtilities.validateFileName(memoryFile.getName()));
                        linkedHashMap.put(file3, memoryFile);
                    }
                }
            }
            if (linkedHashMap.isEmpty()) {
                return;
            }
            SwingUI.onSwingWorker(() -> {
                ArrayList<File> arrayList = new ArrayList<File>();
                for (Map.Entry entry : linkedHashMap.entrySet()) {
                    arrayList.add(FileUtilities.writeFile(((MemoryFile)entry.getValue()).getData(), (File)entry.getKey()));
                }
                return arrayList;
            }, UserInteraction::revealFiles, exception -> Logging.log.warning(Logging.cause(exception)));
        });
    }

    private void export(Object[] objectArray, EventObject eventObject) {
        SwingUI.withWaitCursor((Object)this, () -> {
            File file = UserFiles.showOpenDialogSelectFolder(null, "Export Subtitles (SubRip / UTF-8)", eventObject);
            if (file == null) {
                return;
            }
            SwingUI.onSwingWorker(() -> {
                ArrayList<File> arrayList = new ArrayList<File>();
                for (Object object : objectArray) {
                    MemoryFile memoryFile = (MemoryFile)object;
                    File file2 = new File(file, FileUtilities.validateFileName(FileUtilities.getNameWithoutExtension(memoryFile.getName()) + ".srt"));
                    arrayList.add(FileUtilities.writeFile(SubtitleUtilities.exportSubtitles(memoryFile, SubtitleFormat.SubRip, StandardCharsets.UTF_8), file2));
                }
                return arrayList;
            }, UserInteraction::revealFiles, exception -> Logging.log.warning(Logging.cause(exception)));
        });
    }

    private boolean isPending(List<SubtitlePackage> list) {
        for (SubtitlePackage subtitlePackage : list) {
            if (!subtitlePackage.isDownload() || subtitlePackage.getDownload().isStarted()) continue;
            return true;
        }
        return false;
    }

    private SubtitlePackageFeatureLink getFeatureLink(List<SubtitlePackage> list) {
        for (SubtitlePackage subtitlePackage : list) {
            if (!(subtitlePackage instanceof SubtitlePackageFeatureLink)) continue;
            return (SubtitlePackageFeatureLink)subtitlePackage;
        }
        return null;
    }
}

