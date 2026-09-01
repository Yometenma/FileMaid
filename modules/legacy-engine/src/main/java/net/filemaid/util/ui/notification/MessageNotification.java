package net.filemaid.util.ui.notification;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.ui.notification.NotificationWindow;

public class MessageNotification
extends NotificationWindow {
    private int margin = 10;
    private Border marginBorder = BorderFactory.createEmptyBorder(this.margin, this.margin, this.margin, this.margin);
    private Border border = BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(ThemeSupport.getColor(16096015), ThemeSupport.getPanelBackground()), this.marginBorder);
    private JLabel headLabel;
    private JTextPane textArea;
    private JLabel imageLabel;

    public MessageNotification(String string, String string2, Icon icon, int n) {
        super(n, true);
        JComponent jComponent = (JComponent)this.getContentPane();
        jComponent.setLayout(new BorderLayout(5, 2));
        jComponent.setBackground(ThemeSupport.getPanelBackground());
        jComponent.setBorder(this.border);
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setOpaque(false);
        jPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        this.headLabel = new JLabel(string);
        this.headLabel.setHorizontalAlignment(0);
        this.headLabel.setFont(this.headLabel.getFont().deriveFont(1));
        jPanel.add((Component)this.headLabel, "North");
        this.textArea = new JTextPane();
        this.textArea.setText(string2);
        this.textArea.setEditable(false);
        this.textArea.setOpaque(false);
        jPanel.add((Component)this.textArea, "Center");
        if (icon != null) {
            this.imageLabel = new JLabel(icon);
            jComponent.add((Component)this.imageLabel, "West");
        }
        jComponent.add((Component)jPanel, "Center");
        this.pack();
        this.setBackground(jComponent.getBackground());
    }
}

