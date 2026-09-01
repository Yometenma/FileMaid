package net.filemaid.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.ui.notification.SeparatorBorder;

public class HeaderPanel
extends JComponent {
    private JLabel titleLabel = new JLabel();

    public HeaderPanel() {
        this.setLayout(new BorderLayout());
        this.setBackground(ThemeSupport.getPanelBackground());
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setOpaque(false);
        this.titleLabel.setHorizontalAlignment(0);
        this.titleLabel.setVerticalAlignment(0);
        this.titleLabel.setOpaque(false);
        this.titleLabel.setForeground(ThemeSupport.getLabelForeground());
        this.titleLabel.setFont(new Font("SansSerif", 0, 24));
        jPanel.setBorder(BorderFactory.createEmptyBorder());
        jPanel.add((Component)this.titleLabel, "Center");
        this.add((Component)jPanel, "Center");
        this.setBorder(ThemeSupport.getSeparatorBorder(SeparatorBorder.Position.BOTTOM));
    }

    public void setTitle(String string) {
        this.titleLabel.setText(string);
    }

    public JLabel getTitleLabel() {
        return this.titleLabel;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D)graphics;
        graphics2D.setPaint(ThemeSupport.getPanelBackgroundGradient(0, 0, this.getWidth(), 0));
        graphics2D.fill(this.getBounds());
    }
}

