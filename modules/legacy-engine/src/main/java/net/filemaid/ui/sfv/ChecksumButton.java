package net.filemaid.ui.sfv;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import net.filemaid.ResourceManager;

public class ChecksumButton
extends JToggleButton {
    private static final Icon contentArea = ResourceManager.getIcon("button.checksum");
    private static final Icon contentAreaSelected = ResourceManager.getIcon("button.checksum.selected");

    public ChecksumButton(Action action) {
        super(action);
        this.setPreferredSize(new Dimension(Math.max(contentAreaSelected.getIconWidth(), contentArea.getIconWidth()), Math.max(contentAreaSelected.getIconHeight(), contentArea.getIconHeight())));
        this.setMinimumSize(this.getPreferredSize());
        this.setMaximumSize(this.getPreferredSize());
        this.setForeground(Color.white);
        this.setFont(new Font("Dialog", 0, 11));
        this.setBorderPainted(false);
        this.setContentAreaFilled(false);
        this.setFocusPainted(false);
        this.setEnabled(true);
    }

    @Override
    public void setEnabled(boolean bl) {
        super.setEnabled(bl);
        this.setCursor(Cursor.getPredefinedCursor(bl ? 12 : 0));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D)graphics;
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (this.isSelected()) {
            contentAreaSelected.paintIcon(this, graphics2D, (int)Math.round((double)(this.getWidth() - contentAreaSelected.getIconWidth()) / 2.0), (int)Math.round((double)(this.getHeight() - contentAreaSelected.getIconHeight()) / 2.0));
        } else {
            contentArea.paintIcon(this, graphics2D, (int)Math.round((double)(this.getWidth() - contentArea.getIconWidth()) / 2.0), (int)Math.round((double)(this.getHeight() - contentArea.getIconHeight()) / 2.0));
        }
        Rectangle2D rectangle2D = graphics2D.getFontMetrics().getStringBounds(this.getText(), graphics2D);
        graphics2D.drawString(this.getText(), (float)(Math.round(((double)this.getWidth() - rectangle2D.getWidth()) / 2.0) + 1L), (float)Math.round((double)(this.getHeight() / 2) - rectangle2D.getY() - rectangle2D.getHeight() / 2.0));
    }
}

