package net.filemaid.ui;

import com.google.common.eventbus.Subscribe;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.filemaid.Cache;
import net.filemaid.Logging;
import net.filemaid.Settings;
import net.filemaid.UserData;
import net.filemaid.ui.BaseFrame;
import net.filemaid.ui.HeaderPanel;
import net.filemaid.ui.Mode;
import net.filemaid.ui.TargetTransferable;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.console.GroovyPad;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.PreferencesMap;
import net.filemaid.util.ui.DefaultFancyListCellRenderer;
import net.filemaid.util.ui.PrototypeCellSize;
import net.filemaid.util.ui.SwingEventBus;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

public class MainFrame
extends BaseFrame {
    private static final PreferencesMap.PreferencesEntry<Integer> persistentSelectedPanel = UserData.forPackage(MainFrame.class).entry("panel.selected", 0);
    private JList selectionList;
    private HeaderPanel headerPanel;

    public MainFrame(Mode[] modeArray) {
        super(Settings.getWindowTitle());
        this.selectionList = new PanelSelectionList(modeArray);
        this.headerPanel = new HeaderPanel();
        JScrollPane jScrollPane = new JScrollPane(this.selectionList, 21, 31);
        jScrollPane.setOpaque(false);
        jScrollPane.setBorder(SwingUI.shadow(Settings.isMacApp() || ThemeSupport.getTheme().isDark() ? BorderFactory.createLineBorder(ThemeSupport.getColor(8428984), 1, false) : jScrollPane.getBorder()));
        this.headerPanel.getTitleLabel().setBorder(BorderFactory.createEmptyBorder(8, 90, 10, 0));
        JComponent jComponent = (JComponent)this.getContentPane();
        jComponent.setLayout((LayoutManager)new MigLayout("insets 0, fill, hidemode 3", (Settings.isLinuxApp() ? 110 : 95) + "px[fill]", "fill"));
        jComponent.add((Component)new HiddenLabel(mouseEvent -> GroovyPad.open(this, true)), "pos 1al 0al n n");
        jComponent.add((Component)jScrollPane, "pos 6px 10px n 100%-12px");
        jComponent.add((Component)this.headerPanel, "growx, hmin 50px, dock north");
        try {
            this.selectionList.setSelectedIndex(persistentSelectedPanel.getValue());
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
        this.selectionList.addListSelectionListener(listSelectionEvent -> {
            this.showPanel((Mode)((Object)((Object)this.selectionList.getSelectedValue())));
            if (!listSelectionEvent.getValueIsAdjusting()) {
                persistentSelectedPanel.setValue(this.selectionList.getSelectedIndex());
            }
        });
        this.setSize(1060, 640);
        this.setMinimumSize(new Dimension(740, 340));
        SwingUI.installAction((JComponent)this.getRootPane(), 127, 192, SwingUI.newAction("Clear Cache", actionEvent -> this.clearCaches()));
        SwingUI.installAction((JComponent)this.getRootPane(), 116, SwingUI.newAction("Run", actionEvent -> GroovyPad.open(this, false)));
        SwingEventBus.getInstance().register(this);
    }

    @Override
    public void setVisible(boolean bl) {
        if (bl) {
            this.showPanel((Mode)((Object)this.selectionList.getSelectedValue()));
        }
        super.setVisible(bl);
    }

    public void clearCaches() {
        SwingUI.withWaitCursor((Object)this, () -> {
            Cache.DISK_STORE.clear(string -> !string.startsWith("data"));
            Cache.DISK_STORE.flush();
            Logging.flushLog();
        });
    }

    @Subscribe
    public void selectPanel(TargetTransferable targetTransferable) {
        this.showPanel(targetTransferable.getTarget());
        this.selectionList.setSelectedValue((Object)targetTransferable.getTarget(), false);
        SwingEventBus.getInstance().post(targetTransferable.getTransferable());
    }

    private void showPanel(Mode mode) {
        if (mode == null) {
            return;
        }
        JComponent jComponent = (JComponent)this.getContentPane();
        Component component = null;
        for (Component component2 : jComponent.getComponents()) {
            JComponent jComponent2 = (JComponent)component2;
            Mode mode2 = (Mode)((Object)jComponent2.getClientProperty(Mode.class));
            if (mode2 == null) continue;
            if (mode.equals((Object)mode2)) {
                component = jComponent2;
                continue;
            }
            if (!jComponent2.isVisible()) continue;
            jComponent2.setVisible(false);
            SwingEventBus.getInstance().unregister(jComponent2);
        }
        if (component == null) {
            component = mode.createPanel();
            ((JComponent)component).putClientProperty(Mode.class, (Object)mode);
            ((JComponent)component).setVisible(false);
            jComponent.add(component);
        }
        if (!component.isVisible()) {
            this.headerPanel.setTitle(mode.toString());
            ((JComponent)component).setVisible(true);
            SwingEventBus.getInstance().register(component);
            SwingUtilities.invokeLater(((JComponent)component)::requestFocusInWindow);
        }
    }

    private static class PanelSelectionList
    extends JList {
        private static final int SELECTDELAY_ON_DRAG_OVER = 300;

        public PanelSelectionList(Mode[] modeArray) {
            super(modeArray);
            this.setCellRenderer(new PanelCellRenderer());
            PrototypeCellSize.fixedCellSize(this);
            this.setSelectionMode(0);
            this.setBorder(BorderFactory.createEmptyBorder(4, 5, 4, 5));
            this.setTransferHandler(null);
            new DropTarget(this, new DragDropListener());
        }

        private class DragDropListener
        extends DropTargetAdapter {
            private boolean selectEnabled = false;
            private Timer dragEnterTimer;

            private DragDropListener() {
            }

            @Override
            public void dragOver(DropTargetDragEvent dropTargetDragEvent) {
                if (this.selectEnabled) {
                    int n = PanelSelectionList.this.locationToIndex(dropTargetDragEvent.getLocation());
                    PanelSelectionList.this.setSelectedIndex(n);
                }
            }

            @Override
            public void dragEnter(DropTargetDragEvent dropTargetDragEvent) {
                this.dragEnterTimer = SwingUI.invokeLater(300, () -> {
                    this.selectEnabled = true;
                    SwingUI.requestForeground();
                });
            }

            @Override
            public void dragExit(DropTargetEvent dropTargetEvent) {
                this.selectEnabled = false;
                if (this.dragEnterTimer != null) {
                    this.dragEnterTimer.stop();
                }
            }

            @Override
            public void drop(DropTargetDropEvent dropTargetDropEvent) {
            }
        }
    }

    private static class HiddenLabel
    extends JPanel {
        public HiddenLabel(Consumer<MouseEvent> consumer) {
            this.setLayout(new BoxLayout(this, 1));
            this.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            this.setCursor(Cursor.getPredefinedCursor(12));
            this.setOpaque(false);
            this.addMouseListener(SwingUI.mouseClicked(mouseEvent -> {
                if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
                    if (this.getComponentCount() == 0) {
                        this.toggle(true);
                    } else {
                        consumer.accept((MouseEvent)mouseEvent);
                    }
                } else {
                    this.toggle(false);
                }
            }));
            this.setMinimumSize(new Dimension(120, 40));
        }

        public void toggle(boolean bl) {
            if (bl) {
                if (this.getComponentCount() == 0) {
                    this.add(this.newLine(Settings.getApplicationIdentifier()));
                    this.add(this.newLine(Settings.getSystemIdentifier() + " " + Settings.DEPLOYMENT));
                    this.add(this.newLine(" "));
                    this.add(this.newLine(() -> {
                        long l = Runtime.getRuntime().totalMemory();
                        long l2 = l - Runtime.getRuntime().freeMemory();
                        return "Memory Usage: " + FileUtilities.formatSize(l2) + " / " + FileUtilities.formatSize(l);
                    }));
                }
            } else {
                this.removeAll();
            }
            this.getParent().validate();
        }

        private JLabel newLine(String string) {
            JLabel jLabel = new JLabel(string);
            jLabel.setForeground(ThemeSupport.getLabelForeground());
            jLabel.setFont(jLabel.getFont().deriveFont(8.0f));
            jLabel.setAlignmentX(1.0f);
            return jLabel;
        }

        private JLabel newLine(Supplier<String> supplier) {
            JLabel jLabel = this.newLine(supplier.get());
            Timer timer = new Timer(2000, actionEvent -> jLabel.setText((String)supplier.get()));
            jLabel.addHierarchyListener(SwingUI.hierarchyChanged(4, hierarchyEvent -> {
                if (jLabel.isShowing()) {
                    timer.start();
                } else {
                    timer.stop();
                }
            }));
            return jLabel;
        }
    }

    private static class PanelCellRenderer
    extends DefaultFancyListCellRenderer {
        public PanelCellRenderer() {
            super(10, 0, ThemeSupport.getPanelSelectionBorderColor());
            this.setLayout(new FlowLayout(1, 0, 0));
            this.setHighlightingEnabled(false);
            this.setVerticalTextPosition(3);
            this.setHorizontalTextPosition(0);
        }

        @Override
        public void configureListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
            super.configureListCellRendererComponent(jList, object, n, bl, bl2);
            Mode mode = (Mode)((Object)object);
            this.setText(mode.toString());
            this.setIcon(mode.getIcon());
        }
    }
}

