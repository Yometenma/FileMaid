package net.filemaid.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import net.filemaid.ResourceManager;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.ui.ProgressIndicator;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

public class FileBotTabComponent
extends JComponent {
    private ProgressIndicator progressIndicator = ThemeSupport.getProgressIndicator();
    private JLabel textLabel = new JLabel();
    private JLabel iconLabel = new JLabel();
    private AbstractButton closeButton = this.createCloseButton();
    private boolean loading = false;

    public FileBotTabComponent() {
        this.iconLabel.setHorizontalAlignment(0);
        this.textLabel.setHorizontalAlignment(2);
        this.progressIndicator.setVisible(this.loading);
        this.progressIndicator.setMinimumSize(new Dimension(16, 16));
        this.setLayout((LayoutManager)new MigLayout("nogrid, insets 0 0 1 3"));
        this.add((Component)this.progressIndicator, "hidemode 3");
        this.add((Component)this.iconLabel, "hidemode 3");
        this.add((Component)this.textLabel, "gap rel, align left");
        this.add((Component)this.closeButton, "gap unrel:push, hidemode 3, align center 45%");
    }

    public void setLoading(boolean bl) {
        this.loading = bl;
        this.progressIndicator.setVisible(bl);
        this.iconLabel.setVisible(!bl);
    }

    public boolean isLoading() {
        return this.loading;
    }

    public void setIcon(Icon icon) {
        this.iconLabel.setIcon(icon);
        this.progressIndicator.setPreferredSize(icon != null ? SwingUI.getDimension(icon) : this.progressIndicator.getMinimumSize());
    }

    public Icon getIcon() {
        return this.iconLabel.getIcon();
    }

    public void setText(String string) {
        this.textLabel.setText(string);
    }

    public String getText() {
        return this.textLabel.getText();
    }

    public AbstractButton getCloseButton() {
        return this.closeButton;
    }

    protected AbstractButton createCloseButton() {
        Icon icon = ResourceManager.getIcon("tab.close");
        Icon icon2 = ResourceManager.getIcon("tab.close.hover");
        JButton jButton = new JButton(icon);
        jButton.setRolloverIcon(icon2);
        jButton.setPreferredSize(SwingUI.getDimension(icon2));
        jButton.setMaximumSize(jButton.getPreferredSize());
        jButton.setContentAreaFilled(false);
        jButton.setBorderPainted(false);
        jButton.setFocusable(false);
        jButton.setRolloverEnabled(true);
        return jButton;
    }
}

