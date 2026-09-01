package net.filemaid.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Vector;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import net.filemaid.ResourceManager;
import net.filemaid.ui.BaseDialog;
import net.filemaid.ui.RepeatToggle;
import net.filemaid.ui.ToolTip;
import net.filemaid.util.ui.DefaultFancyListCellRenderer;
import net.filemaid.util.ui.LazyThumbnailListModel;
import net.filemaid.util.ui.ListSearchKeyListener;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

public class SelectDialog<T>
extends BaseDialog {
    public final JLabel messageLabel = new JLabel();
    public final RepeatToggle repeatToggle = new RepeatToggle(ResourceManager.getIcon("button.repeat.selected"), ResourceManager.getIcon("button.repeat"));
    private JList<T> list;
    private String command;
    public static final String SELECT = "Select";
    public static final String CANCEL = "Cancel";
    public static final String CLOSE = null;
    private final Action selectAction = SwingUI.newAction("Select", ResourceManager.getIcon("dialog.continue"), actionEvent -> {
        this.command = SELECT;
        this.close();
    });
    private final Action cancelAction = SwingUI.newAction("Cancel", ResourceManager.getIcon("dialog.cancel"), actionEvent -> {
        this.command = CANCEL;
        this.close();
    });
    private final Action closeAction = SwingUI.newAction(CLOSE, actionEvent -> {
        this.command = CLOSE;
        this.close();
    });

    public SelectDialog(Component component, Collection<? extends T> collection) {
        this(component, collection, null, null, false, false, null);
    }

    public SelectDialog(Component component, Collection<? extends T> collection, Function<T, Icon> function, Function<T, CompletableFuture<Icon>> function2, boolean bl, boolean bl2, JComponent jComponent) {
        super(SwingUI.getWindow(component), SELECT);
        this.setDefaultCloseOperation(2);
        if (function2 == null) {
            this.list = new JList<T>(new Vector<T>(collection));
        } else {
            this.list = new JList<T>(new LazyThumbnailListModel<T>(collection.toArray(), function, function2));
            this.list.setFixedCellHeight(59);
            this.list.setFixedCellWidth(370);
        }
        this.list.setSelectionMode(0);
        this.list.setSelectedIndex(0);
        DefaultFancyListCellRenderer defaultFancyListCellRenderer = new DefaultFancyListCellRenderer(4){

            @Override
            public Component getListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
                super.getListCellRendererComponent(jList, SelectDialog.this.getText((T)object), n, bl, bl2);
                if (jList.getModel() instanceof LazyThumbnailListModel) {
                    LazyThumbnailListModel lazyThumbnailListModel = (LazyThumbnailListModel)jList.getModel();
                    if (jList.isValid()) {
                        this.setIcon(lazyThumbnailListModel.getIcon(n));
                    } else {
                        this.setIcon(lazyThumbnailListModel.getPreviewIcon(n));
                    }
                } else {
                    this.setIcon(SelectDialog.this.getIcon((T)object));
                }
                ToolTip.HTML.setToolTip(this, object);
                return this;
            }

            @Override
            public String getToolTipText(MouseEvent mouseEvent) {
                return ToolTip.HTML.getToolTip(this);
            }
        };
        defaultFancyListCellRenderer.setHighlightingEnabled(false);
        this.list.setCellRenderer(defaultFancyListCellRenderer);
        this.repeatToggle.setHelpText("Select once and ask again next time", "Select and remember for next time");
        this.list.addMouseListener(SwingUI.mouseDoubleClicked(mouseEvent -> this.selectAction.actionPerformed(new ActionEvent(mouseEvent.getSource(), 1001, SELECT))));
        this.list.addKeyListener(new ListSearchKeyListener());
        JComponent jComponent2 = (JComponent)this.getContentPane();
        jComponent2.setLayout((LayoutManager)new MigLayout("insets 5px, nogrid, fill", "", "[grow 0]2px[grow]0px"));
        if (jComponent != null) {
            jComponent2.add((Component)jComponent, "dock north, wmin 300px");
        }
        jComponent2.add((Component)this.messageLabel, "gap 2px, wrap");
        jComponent2.add((Component)new JScrollPane(this.list), "hmin 150px, grow");
        JComponent jComponent3 = SwingUI.newPanel((LayoutManager)new MigLayout("insets 10px, fill, nogrid", "align center", "align center"));
        jComponent3.add((Component)SwingUI.newButton(this.selectAction), "tag ok");
        jComponent3.add((Component)SwingUI.newButton(this.cancelAction), "tag cancel");
        jComponent2.add((Component)jComponent3, "dock south");
        if (bl) {
            jComponent3.add((Component)this.repeatToggle, "tag left, wmin 200px");
        }
        this.repeatToggle.setName("repeat");
        this.setMinimumSize(new Dimension(440, 400));
        SwingUI.installAction(this.list, 10, this.selectAction);
        SwingUI.installAction(this.list, 27, this.closeAction);
    }

    protected String getText(T t) {
        return t.toString();
    }

    protected Icon getIcon(T t) {
        return null;
    }

    public JLabel getMessageLabel() {
        return this.messageLabel;
    }

    public String getSelectedAction() {
        return this.command;
    }

    public T getSelectedValue() {
        return SELECT.equals(this.command) ? (T)this.list.getSelectedValue() : null;
    }

    public void close() {
        this.setVisible(false);
        this.dispose();
    }

    public Action getSelectAction() {
        return this.selectAction;
    }

    public Action getCancelAction() {
        return this.cancelAction;
    }
}

