package net.filemaid.ui.filter;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Window;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import net.filemaid.Cache;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.ResourceManager;
import net.filemaid.StandardPostProcessAction;
import net.filemaid.StandardRenameAction;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.FeedbackSpooler;
import net.filemaid.postprocess.Script;
import net.filemaid.similarity.Match;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.filter.Tool;
import net.filemaid.ui.rename.PostProcessConfigurationDialog;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.ActionPopup;
import net.filemaid.util.ui.GlassProgressMonitor;
import net.filemaid.util.ui.LoadingOverlayPane;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.Episode;
import net.filemaid.web.Movie;
import net.filemaid.web.SeriesInfo;
import net.miginfocom.swing.MigLayout;

class AttributeTool
extends Tool<TableModel> {
    private JTable table = new JTable(new FileAttributesTableModel());

    public AttributeTool() {
        super("Attributes");
        this.table.setAutoCreateRowSorter(true);
        this.table.setAutoCreateColumnsFromModel(true);
        this.table.setFillsViewportHeight(true);
        this.table.setAutoResizeMode(2);
        this.table.setSelectionMode(2);
        this.table.setBackground(ThemeSupport.getPanelBackground());
        this.table.setGridColor(ThemeSupport.getColor(0xEEEEEE));
        this.table.setRowHeight(25);
        JScrollPane jScrollPane = new JScrollPane(this.table);
        jScrollPane.setBorder(ThemeSupport.getHorizontalRule());
        this.setLayout((LayoutManager)new MigLayout("insets 0, nogrid, fill", "align center", "[fill]0px[pref!]"));
        this.add((Component)new LoadingOverlayPane(jScrollPane, this, "25px", "30px"), "grow");
        JComponent jComponent = SwingUI.newPanel((LayoutManager)new MigLayout("insets 10px, nogrid, novisualpadding, fill", "align center"));
        jComponent.add(this.createApplyButton());
        this.add((Component)jComponent, "dock south");
    }

    protected JButton createApplyButton() {
        return SwingUI.newButton("Apply", ResourceManager.getIcon("script.palette"), actionEvent3 -> {
            JButton jButton = (JButton)actionEvent3.getSource();
            List<Script> list = PostProcessConfigurationDialog.getUserScripts();
            ActionPopup actionPopup = new ActionPopup(jButton.getText(), jButton.getIcon());
            actionPopup.addGroup((Action[])StandardPostProcessAction.getMetadataActions().stream().map(standardPostProcessAction -> this.createApplyAction(standardPostProcessAction.getLabel(), (Apply)standardPostProcessAction)).toArray(Action[]::new));
            actionPopup.addGroup((Action[])list.stream().map(script -> this.createApplyAction(script.getName(), (Apply)script)).toArray(Action[]::new));
            Action action = SwingUI.newAction("New Script", ResourceManager.getIcon("script.add"), actionEvent -> PostProcessConfigurationDialog.showUserScriptEditor(PostProcessConfigurationDialog.BLANK_SCRIPT, SwingUI.getWindow(this)));
            if (list.size() > 0) {
                actionPopup.addGroup(SwingUI.newAction("Edit Script", ResourceManager.getIcon("script.edit"), actionEvent2 -> {
                    ActionPopup editPopup = new ActionPopup("Edit Script", ResourceManager.getIcon("script.edit"));
                    editPopup.addGroup((Action[])list.stream().map(script -> SwingUI.newAction(script.getName(), ResourceManager.getIcon("script.edit"), actionEvent -> PostProcessConfigurationDialog.showUserScriptEditor(script, SwingUI.getWindow(this)))).toArray(Action[]::new));
                    editPopup.addGroup(action);
                    SwingUI.showDropDown((JPopupMenu)editPopup, actionEvent3);
                }));
            } else {
                actionPopup.addGroup(action);
            }
            SwingUI.showDropDown((JPopupMenu)actionPopup, actionEvent3);
        });
    }

    protected Action createApplyAction(String string, Apply apply) {
        return SwingUI.newAction(string, ResourceManager.getIcon("script.go"), actionEvent -> {
            Window window = SwingUI.getWindow(this);
            SwingUI.withWaitCursor((Object)window, () -> {
                ApplyWorker applyWorker = new ApplyWorker(string, apply, (FileAttributesTableModel)this.table.getModel());
                File[] fileArray = GlassProgressMonitor.runTask(applyWorker, window);
                if (fileArray.length > 0) {
                    Logging.log.info(Logging.format("%,d %s added.", fileArray.length, fileArray.length == 1 ? "file" : "files"));
                }
            });
        });
    }

    @Override
    protected TableModel createModelInBackground(List<File> list) {
        FileAttributesTableModel fileAttributesTableModel = new FileAttributesTableModel();
        if (list.isEmpty()) {
            return fileAttributesTableModel;
        }
        List<File> list2 = FileUtilities.listFiles(list, FileUtilities.filter(MediaTypes.VIDEO_FILES, MediaTypes.SUBTITLE_FILES), FileUtilities.HUMAN_NAME_ORDER);
        for (File file : list2) {
            Serializable serializable;
            Object object = XattrMetaInfo.xattr.getMetaInfo(file);
            String string = XattrMetaInfo.xattr.getOriginalName(file);
            if (object instanceof Episode) {
                serializable = ((Episode)object).getSeriesInfo();
                if (serializable != null) {
                    fileAttributesTableModel.addRow(((SeriesInfo)serializable).getDatabase() + "::" + ((SeriesInfo)serializable).getId(), object, string, file);
                }
            } else if (object instanceof Movie) {
                serializable = (Movie)object;
                if (((Movie)serializable).getTmdbId() > 0) {
                    fileAttributesTableModel.addRow("TheMovieDB::" + ((Movie)serializable).getTmdbId(), object, string, file);
                } else if (((Movie)serializable).getImdbId() > 0) {
                    fileAttributesTableModel.addRow("OMDb::" + ((Movie)serializable).getImdbId(), object, string, file);
                }
            }
            if (!Thread.interrupted()) continue;
            throw new CancellationException();
        }
        return fileAttributesTableModel;
    }

    @Override
    protected void setModel(TableModel tableModel) {
        this.table.setModel(tableModel);
    }

    private static class FileAttributesTableModel
    extends AbstractTableModel {
        private final List<Object[]> rows = new ArrayList<Object[]>();

        private FileAttributesTableModel() {
        }

        public boolean addRow(Object ... objectArray) {
            if (objectArray.length != this.getColumnCount()) {
                return false;
            }
            return this.rows.add(objectArray);
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int n) {
            switch (n) {
                case 0: {
                    return "Meta ID";
                }
                case 1: {
                    return "Meta Attributes";
                }
                case 2: {
                    return "Original Name";
                }
                case 3: {
                    return "File Path";
                }
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return this.rows.size();
        }

        @Override
        public Object getValueAt(int n, int n2) {
            return this.rows.get(n)[n2];
        }

        public Object getMetaAttributes(int n) {
            return this.getValueAt(n, 1);
        }

        public String getOriginalName(int n) {
            return (String)this.getValueAt(n, 2);
        }

        public File getFilePath(int n) {
            return (File)this.getValueAt(n, 3);
        }
    }

    private static class ApplyWorker
    implements GlassProgressMonitor.ProgressWorker<File[]> {
        private String name;
        private Apply action;
        private FileAttributesTableModel model;

        public ApplyWorker(String string, Apply apply, FileAttributesTableModel fileAttributesTableModel) {
            this.name = string;
            this.action = apply;
            this.model = fileAttributesTableModel;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Icon getIcon() {
            return ResourceManager.getIcon("script.palette");
        }

        @Override
        public String getDescription() {
            return "Processing...";
        }

        @Override
        public boolean isIndeterminate() {
            return false;
        }

        @Override
        public File[] call(Consumer<String> consumer, Consumer<String> consumer2, BiConsumer<Integer, Integer> biConsumer, Supplier<Boolean> supplier) throws Exception {
            FeedbackSpooler feedbackSpooler = new FeedbackSpooler(consumer2, biConsumer, supplier);
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            for (int i = 0; i < this.model.getRowCount(); ++i) {
                Object object = this.model.getMetaAttributes(i);
                File file = this.model.getFilePath(i);
                File file2 = Optional.of(this.model.getOriginalName(i)).map(File::new).orElse(null);
                linkedHashMap.put(file, Match.of(file2, object));
            }
            try {
                this.action.apply(linkedHashMap, StandardRenameAction.DUPLICATE, feedbackSpooler);
            }
            catch (Exception exception) {
                Logging.trace(this.action, exception);
            }
            Cache.DISK_STORE.flush();
            feedbackSpooler.warnings().forEach(Logging.debug::warning);
            return (File[])feedbackSpooler.files().filter(File::isFile).toArray(File[]::new);
        }
    }
}

