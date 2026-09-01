package net.filemaid.ui.subtitle;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.Settings;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.subtitle.SubtitleFormat;
import net.filemaid.subtitle.SubtitleMetrics;
import net.filemaid.subtitle.SubtitleNaming;
import net.filemaid.subtitle.SubtitleUtilities;
import net.filemaid.ui.BaseDialog;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.subtitle.SimpleComboBox;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.AbstractBean;
import net.filemaid.util.ui.EmptySelectionModel;
import net.filemaid.util.ui.GlassOptionPane;
import net.filemaid.util.ui.HorizontalRule;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.vfs.MemoryFile;
import net.filemaid.web.SubtitleDescriptor;
import net.filemaid.web.SubtitleLookupService;
import net.filemaid.web.SubtitleProvider;
import net.miginfocom.swing.MigLayout;

class SubtitleAutoMatchDialog
extends BaseDialog {
    private static final Color hashMatchColor = ThemeSupport.getColor(16448210);
    private static final Color nameMatchColor = ThemeSupport.getColor(16772045);
    private final JPanel hashMatcherServicePanel = this.createServicePanel(hashMatchColor);
    private final JPanel nameMatcherServicePanel = this.createServicePanel(nameMatchColor);
    private final List<SubtitleServiceBean> services = new ArrayList<SubtitleServiceBean>();
    private final JTable subtitleMappingTable = this.createTable();
    private final JComboBox<SubtitleNaming> preferredSubtitleNaming = new JComboBox<SubtitleNaming>(SubtitleNaming.values());
    private final JComboBox<SubtitleCoding> preferredSubtitleCoding = new JComboBox<SubtitleCoding>(SubtitleCoding.values());
    private ExecutorService queryService;
    private ExecutorService downloadService;
    private final Action downloadAction = SwingUI.newAction("Download", ResourceManager.getIcon("dialog.continue"), actionEvent -> {
        Object object2;
        if (this.subtitleMappingTable.getCellEditor() != null) {
            this.subtitleMappingTable.getCellEditor().stopCellEditing();
        }
        if (this.downloadService != null && !this.downloadService.isTerminated()) {
            return;
        }
        SubtitleMappingTableModel subtitleMappingTableModel = (SubtitleMappingTableModel)this.subtitleMappingTable.getModel();
        if (Settings.isMacSandbox()) {
            MacAppUtilities.askUnlockFolders(SwingUI.getWindow(actionEvent.getSource()), subtitleMappingTableModel.getVideoFiles());
        }
        ArrayList<Object> arrayList = new ArrayList<Object>();
        for (Object subtitleMapping : subtitleMappingTableModel) {
            object2 = ((SubtitleMapping)subtitleMapping).getSelectedOption();
            if (object2 == null || !((SubtitleDescriptorBean)object2).isReady()) continue;
            arrayList.add(new DownloadTask(((SubtitleMapping)subtitleMapping).getVideoFile(), (SubtitleDescriptorBean)object2, (SubtitleNaming)((Object)((Object)this.preferredSubtitleNaming.getSelectedItem())), (SubtitleCoding)((Object)((Object)this.preferredSubtitleCoding.getSelectedItem())), ((SubtitleMapping)subtitleMapping)::setSubtitleFile));
        }
        ArrayList<Object> arrayList2 = new ArrayList<>();
        ArrayList<String> fileNames = new ArrayList<>();
        for (Object downloadTask : arrayList) {
            File file;
            if (((DownloadTask)downloadTask).getSubtitleBean().getType() == null || !(file = ((DownloadTask)downloadTask).getDestination()).exists()) continue;
            arrayList2.add(downloadTask);
            fileNames.add(file.getName());
        }
        if (arrayList2.size() > 0) {
            object2 = new JScrollPane(new JList<Object>(fileNames.toArray()));
            GlassOptionPane glassOptionPane = new GlassOptionPane();
            glassOptionPane.initConfigurationDialog("Replace existing subtitle files?", ResourceManager.getIcon("status.warning"), (JComponent)object2, null);
            glassOptionPane.confirm.putValue("Name", "Replace");
            glassOptionPane.cancel.putValue("Name", "Skip");
            glassOptionPane.open(this);
            if (!glassOptionPane.isConfirmed()) {
                arrayList.removeAll(arrayList2);
            }
        }
        if (arrayList.size() > 0) {
            this.downloadService = Executors.newSingleThreadExecutor();
            for (Object task : arrayList) {
                ((DownloadTask)task).getSubtitleBean().setState(SwingWorker.StateValue.PENDING);
                this.downloadService.execute((Runnable)task);
            }
            this.downloadService.shutdown();
        }
    });
    private final Action finishAction = SwingUI.newAction("Close", ResourceManager.getIcon("dialog.cancel"), actionEvent -> {
        if (this.queryService != null) {
            this.queryService.shutdownNow();
        }
        if (this.downloadService != null) {
            this.downloadService.shutdownNow();
        }
        this.setVisible(false);
        this.dispose();
    });

    public SubtitleAutoMatchDialog(Window window) {
        super(window, "Download Subtitles");
        this.preferredSubtitleNaming.setSelectedItem((Object)SubtitleNaming.MATCH_VIDEO_ADD_LANGUAGE_TAG);
        this.preferredSubtitleCoding.setSelectedItem((Object)SubtitleCoding.ORIGINAL);
        if (!ThemeSupport.getTheme().isDark()) {
            this.preferredSubtitleNaming.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Subtitle Naming"), this.preferredSubtitleNaming.getBorder()));
            this.preferredSubtitleCoding.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Subtitle Format"), this.preferredSubtitleCoding.getBorder()));
        }
        JComponent jComponent = (JComponent)this.getContentPane();
        jComponent.setLayout((LayoutManager)new MigLayout("insets 12px 15px 7px 15px, fill, nogrid", "", "[fill][pref!]"));
        jComponent.add((Component)new JScrollPane(this.subtitleMappingTable), "grow, wrap");
        jComponent.add(this.hashMatcherServicePanel);
        jComponent.add((Component)this.nameMatcherServicePanel, "gap rel");
        JComponent jComponent2 = SwingUI.newPanel((LayoutManager)new MigLayout("nogrid"));
        jComponent2.add(this.preferredSubtitleNaming, "gap right 20px");
        jComponent2.add(this.preferredSubtitleCoding, "gap right 30px");
        jComponent2.add((Component)SwingUI.newButton(this.downloadAction), "tag ok");
        jComponent2.add((Component)SwingUI.newButton(this.finishAction), "tag cancel");
        jComponent.add((Component)jComponent2, "gap indent:push");
    }

    protected JPanel createServicePanel(Color color) {
        JPanel jPanel = new JPanel((LayoutManager)new MigLayout("hidemode 3"));
        jPanel.setBorder(ThemeSupport.getRoundBorder());
        jPanel.setOpaque(false);
        jPanel.setBackground(color);
        jPanel.setVisible(false);
        return jPanel;
    }

    protected JTable createTable() {
        JTable jTable = new JTable(new SubtitleMappingTableModel(new File[0]));
        jTable.setDefaultRenderer(SubtitleMapping.class, new SubtitleMappingOptionRenderer());
        jTable.setRowHeight(24);
        jTable.setIntercellSpacing(new Dimension(5, 5));
        jTable.setBackground(ThemeSupport.getPanelBackground());
        jTable.setAutoCreateRowSorter(true);
        jTable.setFillsViewportHeight(true);
        SimpleComboBox simpleComboBox = new SimpleComboBox(ResourceManager.getIcon("action.select"));
        simpleComboBox.setRenderer(new SubtitleOptionRenderer(true));
        jTable.setSelectionModel(new EmptySelectionModel());
        simpleComboBox.setFocusable(false);
        jTable.setDefaultEditor(SubtitleMapping.class, new DefaultCellEditor(simpleComboBox){

            @Override
            public Component getTableCellEditorComponent(JTable jTable, Object object, boolean bl, int n, int n2) {
                JComboBox jComboBox = (JComboBox)super.getTableCellEditorComponent(jTable, object, bl, n, n2);
                SubtitleMapping subtitleMapping = (SubtitleMapping)object;
                DefaultComboBoxModel<SubtitleDescriptorBean> defaultComboBoxModel = new DefaultComboBoxModel<SubtitleDescriptorBean>(subtitleMapping.getOptions());
                defaultComboBoxModel.addElement(null);
                jComboBox.setModel(defaultComboBoxModel);
                jComboBox.setSelectedItem(subtitleMapping.getSelectedOption());
                return jComboBox;
            }
        });
        return jTable;
    }

    public void setVideoFiles(File[] fileArray) {
        this.subtitleMappingTable.setModel(new SubtitleMappingTableModel(fileArray));
    }

    public void addSubtitleService(SubtitleLookupService subtitleLookupService) {
        this.addSubtitleService(new VideoHashSubtitleServiceBean(subtitleLookupService), this.hashMatcherServicePanel);
    }

    public void addSubtitleService(SubtitleProvider subtitleProvider) {
        this.addSubtitleService(new SubtitleProviderBean(subtitleProvider), this.nameMatcherServicePanel);
    }

    protected void addSubtitleService(SubtitleServiceBean subtitleServiceBean, JPanel jPanel) {
        JLabel jLabel = new JLabel(subtitleServiceBean.getDescription(), ResourceManager.getIcon("database.go"), 0);
        jLabel.setBorder(BorderFactory.createEmptyBorder());
        jLabel.setVisible(false);
        subtitleServiceBean.addPropertyChangeListener(propertyChangeEvent -> {
            if (subtitleServiceBean.getState() == SwingWorker.StateValue.STARTED) {
                jLabel.setIcon(ResourceManager.getIcon("database.go"));
            } else {
                jLabel.setIcon(ResourceManager.getIcon(subtitleServiceBean.getError() == null ? "database.ok" : "database.error"));
            }
            jLabel.setVisible(true);
            jLabel.setToolTipText(String.format("%s: %s", subtitleServiceBean.getName(), subtitleServiceBean.getError() == null ? subtitleServiceBean.getState().toString().toLowerCase() : subtitleServiceBean.getError().getMessage()));
            jPanel.setVisible(true);
            jPanel.getParent().revalidate();
        });
        this.services.add(subtitleServiceBean);
        jPanel.add(jLabel);
    }

    public void startQuery(Locale locale) {
        final SubtitleMappingTableModel subtitleMappingTableModel = (SubtitleMappingTableModel)this.subtitleMappingTable.getModel();
        QueryTask queryTask = new QueryTask(this.services, subtitleMappingTableModel.getVideoFiles(), locale, this){

            @Override
            protected void process(List<Map<File, List<SubtitleDescriptorBean>>> list) {
                for (Map<File, List<SubtitleDescriptorBean>> map : list) {
                    for (SubtitleMapping subtitleMapping : subtitleMappingTableModel) {
                        List<SubtitleDescriptorBean> list2 = map.get(subtitleMapping.getVideoFile());
                        if (list2 == null || list2.size() <= 0) continue;
                        subtitleMapping.addOptions(list2);
                    }
                    if (map.size() <= 0) continue;
                    subtitleMappingTableModel.setOptionColumnVisible(true);
                }
            }

            @Override
            protected void done() {
                SwingUtilities.invokeLater(subtitleMappingTableModel::fireTableStructureChanged);
            }
        };
        this.queryService = Executors.newSingleThreadExecutor();
        this.queryService.submit(queryTask);
    }

    protected static enum SubtitleCoding {
        ORIGINAL{

            @Override
            public ByteBuffer transcode(MemoryFile memoryFile) {
                return memoryFile.getData();
            }

            public String toString() {
                return "Keep Original";
            }
        }
        ,
        SRT{

            @Override
            public ByteBuffer transcode(MemoryFile memoryFile) throws Exception {
                return SubtitleUtilities.exportSubtitles(memoryFile, SubtitleFormat.SubRip, StandardCharsets.UTF_8);
            }

            public String toString() {
                return "SubRip / UTF-8";
            }
        };


        public abstract ByteBuffer transcode(MemoryFile var1) throws Exception;
    }

    protected static class SubtitleMappingTableModel
    extends AbstractTableModel
    implements Iterable<SubtitleMapping> {
        private final SubtitleMapping[] data;
        private boolean optionColumnVisible = false;

        public SubtitleMappingTableModel(File ... fileArray) {
            this.data = new SubtitleMapping[fileArray.length];
            for (int i = 0; i < fileArray.length; ++i) {
                this.data[i] = new SubtitleMapping(fileArray[i]);
                this.data[i].addPropertyChangeListener(new SubtitleMappingListener(i));
            }
        }

        public List<File> getVideoFiles() {
            return new AbstractList<File>(){

                @Override
                public File get(int n) {
                    return data[n].getVideoFile();
                }

                @Override
                public int size() {
                    return data.length;
                }
            };
        }

        @Override
        public Iterator<SubtitleMapping> iterator() {
            return Arrays.asList(this.data).iterator();
        }

        public void setOptionColumnVisible(boolean bl) {
            if (this.optionColumnVisible == bl) {
                return;
            }
            this.optionColumnVisible = bl;
            this.fireTableStructureChanged();
        }

        @Override
        public int getColumnCount() {
            return this.optionColumnVisible ? 2 : 1;
        }

        @Override
        public String getColumnName(int n) {
            switch (n) {
                case 0: {
                    return "Video";
                }
                case 1: {
                    return "Subtitle";
                }
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return this.data.length;
        }

        @Override
        public Object getValueAt(int n, int n2) {
            switch (n2) {
                case 0: {
                    return this.data[n].getVideoFile().getName();
                }
                case 1: {
                    return this.data[n];
                }
            }
            return null;
        }

        @Override
        public void setValueAt(Object object, int n, int n2) {
            this.data[n].setSelectedOption((SubtitleDescriptorBean)object);
        }

        @Override
        public boolean isCellEditable(int n, int n2) {
            return n2 == 1 && this.data[n].isEditable();
        }

        @Override
        public Class<?> getColumnClass(int n) {
            switch (n) {
                case 0: {
                    return String.class;
                }
                case 1: {
                    return SubtitleMapping.class;
                }
            }
            return null;
        }

        private class SubtitleMappingListener
        implements PropertyChangeListener {
            private final int index;

            public SubtitleMappingListener(int n) {
                this.index = n;
            }

            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                SubtitleMappingTableModel.this.fireTableRowsUpdated(this.index, this.index);
            }
        }
    }

    protected static class SubtitleMapping
    extends AbstractBean {
        private File videoFile;
        private File subtitleFile;
        private SubtitleDescriptorBean selectedOption;
        private List<SubtitleDescriptorBean> options = new ArrayList<SubtitleDescriptorBean>();
        private final PropertyChangeListener selectedOptionListener = new PropertyChangeListener(){

            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                SubtitleMapping.this.firePropertyChange("selectedOption", null, selectedOption);
            }
        };

        public SubtitleMapping(File file) {
            this.videoFile = file;
        }

        public File getVideoFile() {
            return this.videoFile;
        }

        public File getSubtitleFile() {
            return this.subtitleFile;
        }

        public void setSubtitleFile(File file) {
            this.subtitleFile = file;
            this.firePropertyChange("subtitleFile", null, this.subtitleFile);
        }

        public boolean isEditable() {
            return this.subtitleFile == null && this.options.size() > 0 && (this.selectedOption == null || this.selectedOption.getState() == null || this.selectedOption.getError() != null);
        }

        public SubtitleDescriptorBean getSelectedOption() {
            return this.selectedOption;
        }

        public void setSelectedOption(SubtitleDescriptorBean subtitleDescriptorBean) {
            if (this.selectedOption != null) {
                this.selectedOption.removePropertyChangeListener(this.selectedOptionListener);
            }
            this.selectedOption = subtitleDescriptorBean;
            if (this.selectedOption != null) {
                this.selectedOption.addPropertyChangeListener(this.selectedOptionListener);
            }
            this.firePropertyChange("selectedOption", null, this.selectedOption);
        }

        public SubtitleDescriptorBean[] getOptions() {
            return this.options.toArray(new SubtitleDescriptorBean[0]);
        }

        public void addOptions(List<SubtitleDescriptorBean> list) {
            this.options.addAll(list);
            if (this.selectedOption == null && list.size() > 0) {
                this.setSelectedOption(list.get(0));
            }
        }
    }

    protected static class SubtitleMappingOptionRenderer
    extends DefaultTableCellRenderer {
        private final JComboBox optionComboBox = new SimpleComboBox(ResourceManager.getIcon("action.select"));

        public SubtitleMappingOptionRenderer() {
            this.optionComboBox.setBackground(ThemeSupport.getPanelBackground());
            this.optionComboBox.setRenderer(new SubtitleOptionRenderer(false));
        }

        @Override
        public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
            SubtitleDescriptorBean subtitleDescriptorBean;
            SubtitleMapping subtitleMapping = (SubtitleMapping)object;
            SubtitleDescriptorBean subtitleDescriptorBean2 = subtitleDescriptorBean = subtitleMapping != null ? subtitleMapping.getSelectedOption() : null;
            if (subtitleMapping != null && subtitleMapping.isEditable() && subtitleDescriptorBean != null) {
                this.optionComboBox.setModel(new DefaultComboBoxModel<Object>(new Object[]{subtitleDescriptorBean}));
                return this.optionComboBox;
            }
            super.getTableCellRendererComponent(jTable, object, bl, bl2, n, n2);
            this.setForeground(jTable.getForeground());
            if (subtitleDescriptorBean == null) {
                if (subtitleMapping != null && subtitleMapping.getOptions().length == 0) {
                    this.setText("No subtitles found");
                    this.setIcon(null);
                    this.setForeground(ThemeSupport.getPassiveColor());
                } else {
                    this.setText("No subtitles selected");
                    this.setIcon(null);
                    this.setForeground(ThemeSupport.getPassiveColor());
                }
            } else if (subtitleDescriptorBean.getState() == SwingWorker.StateValue.PENDING) {
                this.setText(subtitleDescriptorBean.getText());
                this.setIcon(ResourceManager.getIcon("worker.pending"));
            } else if (subtitleDescriptorBean.getState() == SwingWorker.StateValue.STARTED) {
                this.setText(subtitleDescriptorBean.getText());
                this.setIcon(ResourceManager.getIcon("action.fetch"));
            } else if (subtitleMapping != null && subtitleMapping.getSubtitleFile() != null) {
                this.setText(subtitleMapping.getSubtitleFile().getName());
                this.setIcon(ResourceManager.getIcon("status.ok"));
            } else {
                this.setText(null);
                this.setIcon(null);
            }
            return this;
        }
    }

    protected static class SubtitleOptionRenderer
    extends DefaultListCellRenderer {
        private final Border padding = BorderFactory.createEmptyBorder(3, 3, 3, 3);
        private final boolean isEditor;

        public SubtitleOptionRenderer(boolean bl) {
            this.isEditor = bl;
        }

        @Override
        public Component getListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
            super.getListCellRendererComponent((JList<?>)jList, (Object)null, n, bl, bl2);
            this.setBorder(this.padding);
            SubtitleDescriptorBean subtitleDescriptorBean = (SubtitleDescriptorBean)object;
            if (this.isEditor && n == jList.getModel().getSize() - 2) {
                HorizontalRule.south(this, 10, ThemeSupport.getPassiveColor(), jList.getBackground());
            }
            if (object == null) {
                this.setText("Cancel selection");
                this.setIcon(ResourceManager.getIcon("dialog.cancel"));
            } else {
                if (subtitleDescriptorBean.getError() == null) {
                    this.setText(subtitleDescriptorBean.getText());
                    this.setIcon(subtitleDescriptorBean.getIcon());
                } else {
                    this.setText(String.format("%s (%s)", Logging.cause(subtitleDescriptorBean.getError()), subtitleDescriptorBean.getText()));
                    this.setIcon(ResourceManager.getIcon("status.warning"));
                }
                if (!bl) {
                    float f = subtitleDescriptorBean.getMatchProbability();
                    if (f < 1.0f) {
                        this.setOpaque(true);
                        this.setBackground(nameMatchColor);
                    }
                    if (f < 0.9f) {
                        this.setOpaque(true);
                        this.setBackground(ThemeSupport.withAlpha(Color.RED, (1.0f - f) * 0.5f));
                    }
                }
            }
            return this;
        }
    }

    protected static class VideoHashSubtitleServiceBean
    extends SubtitleServiceBean {
        public static final float MAX_PROBABILITY = 1.0f;
        private SubtitleLookupService service;

        public VideoHashSubtitleServiceBean(SubtitleLookupService subtitleLookupService) {
            super(subtitleLookupService.getName(), subtitleLookupService.getIcon(), subtitleLookupService.getLink());
            this.service = subtitleLookupService;
        }

        @Override
        public String getDescription() {
            return "Exact Search";
        }

        @Override
        protected Map<File, List<SubtitleDescriptor>> getSubtitleList(Collection<File> collection, Locale locale, Component component) throws Exception {
            return SubtitleUtilities.lookupSubtitlesByHash(this.service, collection, locale, true, false);
        }

        @Override
        public float getMatchProbabilty(File file, SubtitleDescriptor subtitleDescriptor) {
            return 1.0f;
        }
    }

    protected static abstract class SubtitleServiceBean
    extends AbstractBean {
        private final String name;
        private final Icon icon;
        private final URI link;
        private SwingWorker.StateValue state = SwingWorker.StateValue.PENDING;
        private Exception error = null;

        public SubtitleServiceBean(String string, Icon icon, URI uRI) {
            this.name = string;
            this.icon = icon;
            this.link = uRI;
        }

        public String getName() {
            return this.name;
        }

        public Icon getIcon() {
            return this.icon;
        }

        public URI getLink() {
            return this.link;
        }

        public abstract String getDescription();

        public abstract float getMatchProbabilty(File var1, SubtitleDescriptor var2);

        protected abstract Map<File, List<SubtitleDescriptor>> getSubtitleList(Collection<File> var1, Locale var2, Component var3) throws Exception;

        public final Map<File, List<SubtitleDescriptor>> lookupSubtitles(Collection<File> collection, Locale locale, Component component) throws Exception {
            this.setState(SwingWorker.StateValue.STARTED);
            try {
                Map<File, List<SubtitleDescriptor>> map = this.getSubtitleList(collection, locale, component);
                return map;
            }
            catch (Exception exception) {
                this.error = exception;
                throw this.error;
            }
            finally {
                this.setState(SwingWorker.StateValue.DONE);
            }
        }

        private void setState(SwingWorker.StateValue stateValue) {
            this.state = stateValue;
            this.firePropertyChange("state", null, (Object)this.state);
        }

        public SwingWorker.StateValue getState() {
            return this.state;
        }

        public Throwable getError() {
            return this.error;
        }
    }

    protected static class SubtitleProviderBean
    extends SubtitleServiceBean {
        public static final float MAX_PROBABILITY = 0.9f;
        private SubtitleProvider service;
        private SubtitleMetrics metrics = new SubtitleMetrics();

        public SubtitleProviderBean(SubtitleProvider subtitleProvider) {
            super(subtitleProvider.getName(), subtitleProvider.getIcon(), subtitleProvider.getLink());
            this.service = subtitleProvider;
        }

        @Override
        public String getDescription() {
            return "Fuzzy Search";
        }

        @Override
        protected Map<File, List<SubtitleDescriptor>> getSubtitleList(Collection<File> collection, Locale locale, Component component) throws Exception {
            return SubtitleUtilities.findSubtitlesByName(this.service, collection, locale, null, true, false);
        }

        @Override
        public float getMatchProbabilty(File file, SubtitleDescriptor subtitleDescriptor) {
            float f = this.metrics.verification().getSimilarity(file, subtitleDescriptor);
            return f < 0.9f ? f : 0.9f;
        }
    }

    protected static class SubtitleDescriptorBean
    extends AbstractBean {
        private final File videoFile;
        private final SubtitleDescriptor descriptor;
        private final SubtitleServiceBean service;
        private SwingWorker.StateValue state;
        private Exception error;

        public SubtitleDescriptorBean(File file, SubtitleDescriptor subtitleDescriptor, SubtitleServiceBean subtitleServiceBean) {
            this.videoFile = file;
            this.descriptor = subtitleDescriptor;
            this.service = subtitleServiceBean;
        }

        public SubtitleDescriptor getDescriptor() {
            return this.descriptor;
        }

        public float getMatchProbability() {
            return this.service.getMatchProbabilty(this.videoFile, this.descriptor);
        }

        public String getText() {
            return this.descriptor.toString();
        }

        public Icon getIcon() {
            return this.service.getIcon();
        }

        public String getType() {
            return this.descriptor.getType();
        }

        public boolean isReady() {
            if (this.state == null) {
                return true;
            }
            return this.error != null && this.state == SwingWorker.StateValue.DONE;
        }

        public MemoryFile fetch() {
            this.setState(SwingWorker.StateValue.STARTED);
            try {
                MemoryFile memoryFile = SubtitleUtilities.fetchSubtitle(this.descriptor);
                return memoryFile;
            }
            catch (Exception exception) {
                this.error = exception;
                Logging.debug.warning(Logging.cause(this, exception));
            }
            finally {
                this.setState(SwingWorker.StateValue.DONE);
            }
            return null;
        }

        public Exception getError() {
            return this.error;
        }

        public SwingWorker.StateValue getState() {
            return this.state;
        }

        public void setState(SwingWorker.StateValue stateValue) {
            if (stateValue == SwingWorker.StateValue.PENDING) {
                this.error = null;
            }
            this.state = stateValue;
            this.firePropertyChange("state", null, (Object)this.state);
        }

        public String toString() {
            return this.descriptor.toString();
        }
    }

    protected static class DownloadTask
    extends SwingWorker<File, Void> {
        private final File video;
        private final SubtitleDescriptorBean descriptor;
        private final SubtitleNaming naming;
        private final SubtitleCoding coding;
        private final Consumer<File> complete;

        public DownloadTask(File file, SubtitleDescriptorBean subtitleDescriptorBean, SubtitleNaming subtitleNaming, SubtitleCoding subtitleCoding, Consumer<File> consumer) {
            this.video = file;
            this.descriptor = subtitleDescriptorBean;
            this.naming = subtitleNaming;
            this.coding = subtitleCoding;
            this.complete = consumer;
        }

        public SubtitleDescriptorBean getSubtitleBean() {
            return this.descriptor;
        }

        public File getDestination() {
            String string = this.coding == SubtitleCoding.ORIGINAL ? this.descriptor.getType() : SubtitleFormat.SubRip.getFilter().extension();
            String string2 = this.naming.format(this.video, this.descriptor.getDescriptor(), string);
            return new File(this.video.getParentFile(), string2);
        }

        @Override
        protected File doInBackground() {
            MemoryFile memoryFile = this.descriptor.fetch();
            if (memoryFile != null && !this.isCancelled()) {
                try {
                    return FileUtilities.writeFile(this.coding.transcode(memoryFile), this.getDestination());
                }
                catch (Exception exception) {
                    this.descriptor.error = exception;
                    Logging.trace(this.descriptor, exception);
                }
            }
            return null;
        }

        @Override
        protected void done() {
            File file = null;
            try {
                file = (File)this.get();
            }
            catch (Exception exception) {
                this.descriptor.error = exception;
                Logging.trace(this.descriptor, exception);
            }
            this.complete.accept(file);
        }
    }

    protected static class QueryTask
    extends SwingWorker<Collection<File>, Map<File, List<SubtitleDescriptorBean>>> {
        private final Component parent;
        private final Collection<SubtitleServiceBean> services;
        private final Collection<File> remainingVideos;
        private final Locale locale;

        public QueryTask(Collection<SubtitleServiceBean> collection, Collection<File> collection2, Locale locale, Component component) {
            this.parent = component;
            this.services = collection;
            this.remainingVideos = new TreeSet<File>(collection2);
            this.locale = locale;
        }

        @Override
        protected Collection<File> doInBackground() throws Exception {
            for (SubtitleServiceBean subtitleServiceBean : this.services) {
                if (this.isCancelled() || Thread.interrupted()) {
                    throw new CancellationException();
                }
                if (this.remainingVideos.isEmpty()) break;
                try {
                    HashMap<File, List<SubtitleDescriptorBean>> hashMap = new HashMap<>();
                    subtitleServiceBean.lookupSubtitles(this.remainingVideos, this.locale, this.parent).forEach((file, list) -> {
                        SubtitleDescriptor subtitleDescriptor2 = SubtitleUtilities.getBestMatch(file, list, false);
                        Stream<SubtitleDescriptor> stream = Stream.concat(Stream.of(subtitleDescriptor2), list.stream()).filter(Objects::nonNull).distinct();
                        List<SubtitleDescriptorBean> list2 = stream.map(subtitleDescriptor -> new SubtitleDescriptorBean((File)file, (SubtitleDescriptor)subtitleDescriptor, subtitleServiceBean)).collect(Collectors.toList());
                        hashMap.put((File)file, list2);
                    });
                    hashMap.forEach((file, list) -> {
                        if (list != null && list.size() > 0) {
                            this.remainingVideos.remove(file);
                        }
                    });
                    this.publish(hashMap);
                }
                catch (InterruptedException | CancellationException exception) {
                    throw exception;
                }
                catch (Exception exception) {
                    Logging.trace(exception);
                }
            }
            return this.remainingVideos;
        }
    }
}

