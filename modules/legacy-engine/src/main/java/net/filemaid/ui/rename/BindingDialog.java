package net.filemaid.ui.rename;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.script.ScriptException;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import net.filemaid.ResourceManager;
import net.filemaid.UserFiles;
import net.filemaid.WebServices;
import net.filemaid.format.ExpressionEngine;
import net.filemaid.format.ExpressionFormat;
import net.filemaid.format.MediaBindingBean;
import net.filemaid.ui.BaseDialog;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.rename.MediaInfoDialog;
import net.filemaid.ui.rename.MetaObjectDialog;
import net.filemaid.ui.rename.Mode;
import net.filemaid.util.JsonUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

class BindingDialog
extends BaseDialog {
    private final JTextField infoTextField = new JTextField();
    private final JTextField mediaFileTextField = new JTextField();
    private final BindingTableModel bindingModel = new BindingTableModel();
    private boolean submit = false;
    private MediaBindingBean sample;
    private final Action selectMatchObjectAction = SwingUI.newAction("Select Match", ResourceManager.getIcon("action.match.select"), actionEvent -> SwingUI.withWaitCursor((Object)this, () -> MetaObjectDialog.showSelectDialog(this.getInfoObject(), this.getMediaFile(), null, "Select Match", ResourceManager.getIcon("action.match.select"), this, object -> {
        if (object instanceof File) {
            this.setSample(new MediaBindingBean(object, (File)object));
        } else {
            this.setSample(new MediaBindingBean(object, this.getMediaFile()));
        }
    })));
    private final Action selectMediaFileAction = SwingUI.newAction("Select Media File", ResourceManager.getIcon("action.load"), actionEvent -> SwingUI.withWaitCursor((Object)this, () -> {
        List<File> list = UserFiles.showLoadDialogSelectFiles(false, false, this.getMediaFile(), null, "Select Media File", actionEvent);
        if (list.size() > 0) {
            File file = list.get(0).getAbsoluteFile();
            this.setSample(new MediaBindingBean(this.getInfoObject(), file));
        }
    }));
    private final Action showMediaInfoAction = SwingUI.newAction("Open MediaInfo", ResourceManager.getIcon("action.properties"), actionEvent -> MediaInfoDialog.show(this.getMediaFile(), this));

    public BindingDialog(Window window, Mode mode, MediaBindingBean mediaBindingBean, boolean bl) {
        super(window, mode + " Bindings");
        this.sample = mediaBindingBean;
        JComponent jComponent = (JComponent)this.getContentPane();
        jComponent.setLayout((LayoutManager)new MigLayout("nogrid, novisualpadding, fill, insets dialog", "", "[pref!]paragraph[pref!]2px[grow,fill]paragraph[pref!]"));
        JTabbedPane jTabbedPane = new JTabbedPane();
        jTabbedPane.setFocusable(false);
        JPanel jPanel = new JPanel((LayoutManager)new MigLayout("nogrid, fill"));
        jPanel.setOpaque(false);
        JButton jButton = SwingUI.createImageButton(this.selectMatchObjectAction);
        JButton jButton2 = SwingUI.createImageButton(this.selectMediaFileAction);
        JButton jButton3 = SwingUI.createImageButton(this.showMediaInfoAction);
        jPanel.add((Component)new JLabel("Match Object:"), "wrap 2px");
        jPanel.add((Component)this.infoTextField, "hmin 20px, growx");
        jPanel.add((Component)jButton, "gap rel, wrap paragraph, hidemode 2");
        jPanel.add((Component)new JLabel("Media File:"), "wrap 2px");
        jPanel.add((Component)this.mediaFileTextField, "hmin 20px, growx");
        jPanel.add((Component)jButton3, "gap rel");
        jPanel.add((Component)jButton2, "gap rel, wrap paragraph, hidemode 2");
        jTabbedPane.add("Bindings", jPanel);
        jComponent.add((Component)jTabbedPane, "growx, wrap");
        jComponent.add((Component)new JLabel("Preview:"), "gap 5px, wrap");
        jComponent.add((Component)new JScrollPane(this.createBindingTable(this.bindingModel)), "grow, growprio 200, wrap");
        if (bl) {
            jComponent.add((Component)SwingUI.newButton("Use Bindings", ResourceManager.getIcon("dialog.continue"), actionEvent -> this.finish(true)), "tag apply");
            jComponent.add((Component)SwingUI.newButton("Cancel", ResourceManager.getIcon("dialog.cancel"), actionEvent -> this.finish(false)), "tag cancel");
        } else {
            jComponent.add((Component)SwingUI.newButton("Close", ResourceManager.getIcon("dialog.continue"), actionEvent -> this.finish(false)), "tag ok");
            jButton.setVisible(false);
            jButton2.setVisible(false);
        }
        this.infoTextField.setEditable(false);
        this.mediaFileTextField.setEditable(false);
        this.addWindowListener(SwingUI.windowClosed(windowEvent -> this.finish(false)));
        this.setDefaultCloseOperation(0);
        this.setSize(420, 520);
        this.setMinimumSize(new Dimension(320, 420));
        this.setSample(mediaBindingBean);
    }

    private JTable createBindingTable(TableModel tableModel) {
        JTable jTable = new JTable(tableModel);
        jTable.setAutoCreateRowSorter(true);
        jTable.setAutoCreateColumnsFromModel(true);
        jTable.setFillsViewportHeight(true);
        jTable.setBackground(ThemeSupport.getPanelBackground());
        jTable.setGridColor(ThemeSupport.getColor(0xEEEEEE));
        jTable.setRowHeight(22);
        jTable.setDefaultRenderer(Future.class, new DefaultTableCellRenderer(){

            @Override
            public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
                block4: {
                    super.getTableCellRendererComponent(jTable, null, bl, bl2, n, n2);
                    Future future = (Future)object;
                    this.setForeground(bl ? jTable.getSelectionForeground() : jTable.getForeground());
                    try {
                        this.setText((String)future.get(0L, TimeUnit.MILLISECONDS));
                    }
                    catch (TimeoutException timeoutException) {
                        this.setText("Pending \u2026");
                        if (!bl) {
                            this.setForeground(ThemeSupport.getActiveColor());
                        }
                    }
                    catch (Exception exception) {
                        this.setText("undefined");
                        if (bl) break block4;
                        this.setForeground(ThemeSupport.getPassiveColor());
                    }
                }
                return this;
            }
        });
        return jTable;
    }

    public boolean submit() {
        return this.submit;
    }

    private void finish(boolean bl) {
        this.submit = bl;
        this.setVisible(false);
        this.dispose();
    }

    public void setSample(MediaBindingBean mediaBindingBean) {
        File file;
        this.sample = mediaBindingBean;
        Object object = this.getInfoObject();
        if (object != null) {
            if (object instanceof File) {
                file = (File)object;
                this.infoTextField.setText(file.getName());
                this.infoTextField.setToolTipText(file.getAbsolutePath());
            } else {
                this.infoTextField.setText(object.toString());
                this.infoTextField.setToolTipText("<html><pre>" + SwingUI.escapeHTML(JsonUtilities.json(object, true, true)) + "</pre></html>");
            }
            this.infoTextField.setEnabled(true);
        } else {
            this.infoTextField.setText("none");
            this.infoTextField.setToolTipText(null);
            this.infoTextField.setEnabled(false);
        }
        file = this.getMediaFile();
        if (file != null) {
            this.mediaFileTextField.setText(file.getPath());
            this.mediaFileTextField.setEnabled(true);
            this.showMediaInfoAction.setEnabled(true);
        } else {
            this.mediaFileTextField.setText("none");
            this.mediaFileTextField.setEnabled(false);
            this.showMediaInfoAction.setEnabled(false);
        }
        this.updatePreviewModel();
    }

    public MediaBindingBean getSample() {
        return this.sample;
    }

    public Object getInfoObject() {
        return this.sample == null ? null : this.sample.getInfoObject();
    }

    public File getMediaFile() {
        return this.sample == null ? null : this.sample.getFileObject();
    }

    private void updatePreviewModel() {
        if (this.sample == null) {
            return;
        }
        String[] stringArray = RegularExpressions.COMMA.split(ResourceBundle.getBundle(this.getClass().getName()).getString("expressions"));
        this.bindingModel.setModel(Arrays.asList(stringArray), this.sample);
    }

    private static class BindingTableModel
    extends AbstractTableModel {
        private final List<Evaluator> model = new ArrayList<Evaluator>();

        private BindingTableModel() {
        }

        public void setModel(Collection<String> collection, Object object) {
            this.clear();
            for (String string : collection) {
                Evaluator evaluator = new Evaluator(string, object){

                    @Override
                    protected void done() {
                        BindingTableModel.this.fireTableCellUpdated(this);
                    }
                };
                WebServices.requestPool().async(evaluator);
                this.model.add(evaluator);
            }
            this.fireTableDataChanged();
        }

        public void clear() {
            this.model.forEach(evaluator -> evaluator.cancel(true));
            this.model.clear();
            this.fireTableDataChanged();
        }

        public void fireTableCellUpdated(Evaluator evaluator) {
            int n = this.model.indexOf(evaluator);
            if (n >= 0) {
                this.fireTableCellUpdated(n, 1);
            }
        }

        @Override
        public String getColumnName(int n) {
            switch (n) {
                case 0: {
                    return "Expression";
                }
                case 1: {
                    return "Value";
                }
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return this.model.size();
        }

        @Override
        public Class<?> getColumnClass(int n) {
            switch (n) {
                case 0: {
                    return String.class;
                }
                case 1: {
                    return Future.class;
                }
            }
            return null;
        }

        @Override
        public Object getValueAt(int n, int n2) {
            switch (n2) {
                case 0: {
                    return this.model.get(n).getExpression();
                }
                case 1: {
                    return this.model.get(n);
                }
            }
            return null;
        }
    }

    private static class Evaluator
    extends SwingWorker<String, Void> {
        private final String expression;
        private final Object bindingBean;

        private Evaluator(String string, Object object) {
            this.expression = string;
            this.bindingBean = object;
        }

        public String getExpression() {
            return this.expression;
        }

        @Override
        protected String doInBackground() throws Exception {
            ExpressionFormat expressionFormat = new ExpressionFormat(this.expression){

                @Override
                protected Object[] compile(String string) throws ScriptException {
                    return new Object[]{ExpressionEngine.getExpressionEngine().compileScriptlet(string)};
                }
            };
            try {
                return expressionFormat.format(this.bindingBean);
            }
            catch (Exception exception) {
                return null;
            }
        }

        public String toString() {
            try {
                return (String)this.get(0L, TimeUnit.SECONDS);
            }
            catch (Exception exception) {
                return null;
            }
        }
    }
}

