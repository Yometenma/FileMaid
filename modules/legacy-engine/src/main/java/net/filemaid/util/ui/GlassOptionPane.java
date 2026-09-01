package net.filemaid.util.ui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.filemaid.UserData;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.ui.HorizontalRule;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.util.ui.notification.SeparatorBorder;
import net.miginfocom.swing.MigLayout;

public class GlassOptionPane
extends JPanel {
    public final JLabel headerText = new JLabel();
    public final JLabel contentText = new JLabel();
    public final JTextField contentTextField = new JTextField(18);
    public final JPanel headerPanel = new JPanel((LayoutManager)new MigLayout("fill, insets dialog"));
    public final JPanel contentPanel = new JPanel((LayoutManager)new MigLayout("fill, insets dialog, nogrid"));
    public final JPanel trailerPanel = new JPanel((LayoutManager)new MigLayout("fill, insets 0, nogrid"));
    public final Action confirm = SwingUI.newAction("OK", this::confirm);
    public final Action cancel = SwingUI.newAction("Cancel", this::close);
    public final JButton confirmButton = SwingUI.newButton(this.confirm);
    public final JButton cancelButton = SwingUI.newButton(this.cancel);

    public GlassOptionPane() {
        super((LayoutManager)new MigLayout("insets 0"));
        this.headerPanel.setBackground(ThemeSupport.getPanelBackground());
        this.headerPanel.setBorder(ThemeSupport.getSeparatorBorder(SeparatorBorder.Position.BOTTOM));
        this.headerPanel.add(this.headerText);
        this.add((Component)this.headerPanel, "growx, dock north");
        this.contentPanel.add((Component)this.contentText, "growx, wrap rel, hidemode 3");
        this.contentPanel.add((Component)this.contentTextField, "grow, push, wrap paragraph, hidemode 3");
        this.contentPanel.add((Component)this.trailerPanel, "growx, wrap paragraph, hidemode 3");
        this.contentPanel.add((Component)this.confirmButton, "tag ok");
        this.contentPanel.add((Component)this.cancelButton, "tag cancel");
        this.add((Component)this.contentPanel, "dock center");
        this.contentText.setVisible(false);
        this.contentTextField.setVisible(false);
        this.trailerPanel.setVisible(false);
        this.setBorder(SwingUI.shadow());
        this.setOpaque(false);
        this.setCursor(Cursor.getPredefinedCursor(0));
        SwingUI.installAction((JComponent)this, 10, this.confirm);
        SwingUI.installAction((JComponent)this, 27, this.cancel);
    }

    private void init(String string, Icon icon, JComponent jComponent) {
        this.headerText.setText(string);
        this.headerText.setIcon(icon);
        this.headerText.setIconTextGap(10);
        SwingUtilities.invokeLater(jComponent::requestFocusInWindow);
    }

    public void initTrailer(JComponent ... jComponentArray) {
        HorizontalRule.north(this.trailerPanel, 20, ThemeSupport.getPassiveColor(), this.trailerPanel.getBackground());
        for (JComponent jComponent : jComponentArray) {
            this.trailerPanel.add((Component)jComponent, "growx, gapy rel, wrap rel");
        }
        this.trailerPanel.setVisible(true);
    }

    public void initConfirmDialog(String string, Icon icon, String string2) {
        this.init(string, icon, this.confirmButton);
        this.contentText.setVisible(true);
        this.contentText.setText(string2);
        this.contentText.setMaximumSize(new Dimension(1060, 640));
    }

    public void initInputDialog(String string, Icon icon, String string2, String string3) {
        this.init(string, icon, this.contentTextField);
        this.contentText.setVisible(true);
        this.contentText.setText(string2);
        this.contentTextField.setVisible(true);
        this.contentTextField.setText(string3);
        this.contentTextField.setFont(this.contentTextField.getFont().deriveFont(18.0f));
        this.contentTextField.selectAll();
    }

    public void initAlertDialog(String string, Icon icon, JComponent jComponent) {
        this.init(string, icon, this.confirmButton);
        this.contentPanel.add(jComponent, "grow, wmin 220px, hmin 150px, push, wrap paragraph", 0);
        this.cancelButton.setVisible(false);
    }

    public void initConfigurationDialog(String string, Icon icon, JComponent jComponent, JComponent jComponent2) {
        this.init(string, icon, jComponent);
        this.contentPanel.add(jComponent, "grow, wmin 220px, push, wrap paragraph", 0);
        if (jComponent2 != null) {
            this.contentPanel.add((Component)jComponent2, "tag left, gap indent, aligny center");
        }
    }

    public void confirm(ActionEvent actionEvent) {
        this.setVisible(false);
        this.confirm.setEnabled(false);
    }

    public void close(ActionEvent actionEvent) {
        this.setVisible(false);
        this.cancel.setEnabled(false);
    }

    public boolean isConfirmed() {
        return !this.confirm.isEnabled();
    }

    public boolean isCancelled() {
        return !this.cancel.isEnabled();
    }

    private void resize(Component component, Window window2) {
        String string;
        if (component instanceof JComponent) {
            SwingUI.adjustSizeToFit(window2, this.getPreferredSize());
            this.addComponentListener(SwingUI.componentResized(componentEvent -> SwingUI.adjustSizeToFit(window2, this.getSize())));
        } else if (component instanceof JDialog && (string = this.headerText.getText()) != null && !string.isEmpty()) {
            UserData.forPackage(GlassOptionPane.class).node("dialog").node(string).restoreWindowBounds((JDialog)component, window -> SwingUI.getCenterLocation(window));
        }
    }

    public void open(Component component) {
        Window window = SwingUI.getWindow(component);
        Component component2 = window.getMostRecentFocusOwner();
        Component component3 = SwingUI.createGlassPane(this, window);
        this.resize(component3, window);
        component3.setVisible(true);
        if (component3 instanceof JComponent) {
            SecondaryLoop secondaryLoop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
            component3.addComponentListener(SwingUI.componentHidden(componentEvent -> secondaryLoop.exit()));
            secondaryLoop.enter();
            if (component2 != null) {
                component2.requestFocusInWindow();
            }
        }
    }

    public void open(Component component, Consumer<GlassOptionPane> consumer) {
        Window window = SwingUI.getWindow(component);
        Component component2 = window.getMostRecentFocusOwner();
        Component component3 = SwingUI.createGlassPane(this, window);
        this.resize(component3, window);
        if (component3 instanceof JComponent) {
            component3.addComponentListener(SwingUI.componentHidden(componentEvent -> {
                consumer.accept(this);
                if (component2 != null) {
                    component2.requestFocusInWindow();
                }
            }));
        } else if (component3 instanceof JDialog) {
            ((JDialog)component3).addWindowListener(SwingUI.windowClosed(windowEvent -> consumer.accept(this)));
        }
        component3.setVisible(true);
    }

    public static boolean showConfirmDialog(String string, String string2, Icon icon, Component component) {
        GlassOptionPane glassOptionPane = new GlassOptionPane();
        glassOptionPane.initConfirmDialog(string2, icon, string);
        glassOptionPane.open(component);
        return glassOptionPane.isConfirmed();
    }

    public static Optional<String> showInputDialog(String string, String string2, String string3, Icon icon, Component component) {
        GlassOptionPane glassOptionPane = new GlassOptionPane();
        glassOptionPane.initInputDialog(string3, icon, string, string2);
        glassOptionPane.open(component);
        if (glassOptionPane.isConfirmed()) {
            return Optional.of(glassOptionPane.contentTextField.getText());
        }
        return Optional.empty();
    }

    public static boolean showAlertDialog(JComponent jComponent, String string, Icon icon, Component component) {
        GlassOptionPane glassOptionPane = new GlassOptionPane();
        glassOptionPane.initAlertDialog(string, icon, jComponent);
        glassOptionPane.open(component);
        return glassOptionPane.isConfirmed();
    }

    public static GlassOptionPane showConfirmDialog(String string, String string2, Icon icon, Component component, Runnable runnable) {
        GlassOptionPane glassOptionPane2 = new GlassOptionPane();
        glassOptionPane2.initConfirmDialog(string2, icon, string);
        glassOptionPane2.open(component, glassOptionPane -> {
            if (glassOptionPane.isConfirmed()) {
                runnable.run();
            }
        });
        return glassOptionPane2;
    }

    public static GlassOptionPane showInputDialog(String string, String string2, String string3, Icon icon, Component component, Consumer<String> consumer) {
        GlassOptionPane glassOptionPane = new GlassOptionPane();
        glassOptionPane.initInputDialog(string3, icon, string, string2);
        glassOptionPane.open(component, glassOptionPane2 -> {
            if (glassOptionPane2.isConfirmed()) {
                consumer.accept(glassOptionPane.contentTextField.getText());
            }
        });
        return glassOptionPane;
    }

    public static GlassOptionPane showSuggestionDialog(String string, String string2, String string3, Icon icon, Component component, Runnable runnable) {
        GlassOptionPane glassOptionPane2 = new GlassOptionPane();
        glassOptionPane2.initConfirmDialog(string3, icon, string);
        glassOptionPane2.initTrailer(new JLabel(string2));
        glassOptionPane2.confirm.putValue("Name", "OK");
        glassOptionPane2.cancel.putValue("Name", "Close");
        glassOptionPane2.open(component, glassOptionPane -> {
            if (glassOptionPane.isConfirmed()) {
                runnable.run();
            }
        });
        return glassOptionPane2;
    }

    public static <T extends JComponent> GlassOptionPane showConfigurationDialog(T t, JComponent jComponent, String string, Icon icon, Component component, Consumer<T> consumer) {
        GlassOptionPane glassOptionPane2 = new GlassOptionPane();
        glassOptionPane2.initConfigurationDialog(string, icon, t, jComponent);
        glassOptionPane2.open(component, glassOptionPane -> {
            if (glassOptionPane.isConfirmed()) {
                consumer.accept(t);
            }
        });
        return glassOptionPane2;
    }
}

