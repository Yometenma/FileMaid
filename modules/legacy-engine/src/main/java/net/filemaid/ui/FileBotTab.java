package net.filemaid.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import net.filemaid.ui.FileBotTabComponent;
import net.filemaid.util.ui.SwingUI;

public class FileBotTab<T extends JComponent>
extends JComponent {
    private final FileBotTabComponent tabComponent = new FileBotTabComponent();
    private final T component;

    public FileBotTab(T t) {
        this.component = t;
        this.tabComponent.getCloseButton().addActionListener(SwingUI.newAction("Close", actionEvent -> this.close()));
        this.setLayout(new BorderLayout());
        this.add((Component)t, "Center");
    }

    public void addTo(JTabbedPane jTabbedPane) {
        jTabbedPane.addTab(this.getTitle(), this);
        jTabbedPane.setTabComponentAt(jTabbedPane.indexOfComponent(this), this.tabComponent);
    }

    public void close() {
        if (!this.isClosed()) {
            this.getTabbedPane().remove(this);
        }
    }

    public boolean isClosed() {
        JTabbedPane jTabbedPane = this.getTabbedPane();
        if (jTabbedPane == null) {
            return true;
        }
        return this.getTabbedPane().indexOfComponent(this) < 0;
    }

    private JTabbedPane getTabbedPane() {
        return (JTabbedPane)SwingUtilities.getAncestorOfClass(JTabbedPane.class, this);
    }

    public T getComponent() {
        return this.component;
    }

    public FileBotTabComponent getTabComponent() {
        return this.tabComponent;
    }

    public void setTitle(String string) {
        this.tabComponent.setText(string);
    }

    public String getTitle() {
        return this.tabComponent.getText();
    }

    public void setIcon(Icon icon) {
        this.tabComponent.setIcon(icon);
    }

    public Icon getIcon() {
        return this.tabComponent.getIcon();
    }

    public void setLoading(boolean bl) {
        this.tabComponent.setLoading(bl);
    }
}

