package net.filemaid.util.ui;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.ui.ContextAction;
import net.filemaid.util.ui.LinkButton;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

public class ActionPopup
extends JPopupMenu {
    protected final JLabel headerLabel = new JLabel();
    protected final JLabel descriptionLabel = new JLabel();
    protected final JLabel statusLabel = new JLabel();
    protected final JPanel actionPanel = new JPanel((LayoutManager)new MigLayout("nogrid, insets 0, fill"));

    public ActionPopup(String string, Icon icon) {
        this.setForeground(new JMenuItem().getForeground());
        this.headerLabel.setText(string);
        this.headerLabel.setForeground(this.getForeground());
        this.headerLabel.setIcon(icon);
        this.headerLabel.setIconTextGap(5);
        this.actionPanel.setOpaque(false);
        this.statusLabel.setFont(this.statusLabel.getFont().deriveFont(10.0f));
        this.statusLabel.setForeground(ThemeSupport.getPassiveColor());
        this.setLayout((LayoutManager)new MigLayout("nogrid, fill, insets 0"));
        this.add((Component)this.headerLabel, "gapx 5px 5px, gapy 3px 1px, wrap 3px");
        this.add((Component)new JSeparator(), "growx, wrap 1px");
        this.add((Component)this.actionPanel, "growx, wrap 0px");
        this.add((Component)new JSeparator(), "growx, wrap 0px");
        this.add((Component)this.statusLabel, "growx, h 11px!, gapx 3px, wrap 1px");
        this.setLightWeightPopupEnabled(false);
    }

    @Override
    public void show(Component component, int n, int n2) {
        Point point = this.adjustPopupLocationToFitScreen(component, n, n2);
        super.show(component, point.x, point.y);
    }

    protected JLabel createLabel(String string) {
        JLabel jLabel = new JLabel(string);
        jLabel.setForeground(this.getForeground());
        return jLabel;
    }

    public void addDescription(String string) {
        this.actionPanel.add((Component)this.createLabel(string), "gapx 4px 4px, growx, wrap 3px");
    }

    @Override
    public void addSeparator() {
        this.actionPanel.add((Component)new JSeparator(), "growx, wrap 1px");
    }

    public int count() {
        return this.actionPanel.getComponentCount();
    }

    @Override
    public JMenuItem add(Action action) {
        this.addLinkButton(action);
        return null;
    }

    public void addGroup(Action ... actionArray) {
        if (actionArray.length == 0) {
            return;
        }
        if (this.count() > 0) {
            this.addSeparator();
        }
        for (Action action : actionArray) {
            this.addLinkButton(action);
        }
    }

    public LinkButton addLinkButton(Action action) {
        LinkButton linkButton = new LinkButton(action);
        linkButton.setText("<html><nobr><u>" + linkButton.getText() + "</u></nobr></html>");
        linkButton.setRolloverEnabled(false);
        linkButton.setColor(linkButton.getRolloverColor());
        linkButton.addActionListener(actionEvent -> this.setVisible(false));
        if (action instanceof ContextAction) {
            linkButton.addMouseListener(SwingUI.mousePopupTriggerClicked(mouseEvent -> {
                ((ContextAction)action).contextActionPerformed((MouseEvent)mouseEvent);
                this.setVisible(false);
            }));
        }
        this.actionPanel.add((Component)linkButton, "gapx 12px 12px, growx, wrap");
        return linkButton;
    }

    @Override
    public String getLabel() {
        return this.headerLabel.getText();
    }

    public Icon getIcon() {
        return this.headerLabel.getIcon();
    }

    public void setStatus(String string) {
        this.statusLabel.setText(string);
    }

    private Point adjustPopupLocationToFitScreen(Component component, int n, int n2) {
        Point point = new Point(n, n2);
        Point point2 = component.getLocationOnScreen();
        point.x += point2.x;
        point.y += point2.y;
        Rectangle rectangle = SwingUI.getScreenBounds(component);
        Rectangle rectangle2 = new Rectangle(point, this.getPreferredSize());
        point = SwingUI.adjustLocationToFitScreen(rectangle, rectangle2);
        point.x -= point2.x;
        point.y -= point2.y;
        return point;
    }
}

