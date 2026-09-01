package net.filemaid.util.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URI;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import net.filemaid.UserInteraction;
import net.filemaid.ui.ThemeSupport;

public class LinkButton
extends JButton {
    private Color color = this.getForeground();
    private Color rolloverColor = ThemeSupport.getLinkColor();
    protected final MouseListener rolloverListener = new MouseAdapter(){

        @Override
        public void mouseEntered(MouseEvent mouseEvent) {
            LinkButton.this.setForeground(LinkButton.this.rolloverColor);
        }

        @Override
        public void mouseExited(MouseEvent mouseEvent) {
            LinkButton.this.setForeground(LinkButton.this.color);
        }
    };

    public LinkButton(String string, String string2, Icon icon, URI uRI) {
        this(new OpenUriAction(string, string2, icon, uRI));
    }

    public LinkButton(Action action) {
        this.setAction(action);
        this.setFocusPainted(false);
        this.setOpaque(false);
        this.setContentAreaFilled(false);
        this.setBorder(null);
        this.setHorizontalAlignment(2);
        this.setIconTextGap(6);
        this.setRolloverEnabled(true);
        this.setCursor(Cursor.getPredefinedCursor(12));
    }

    @Override
    public void setRolloverEnabled(boolean bl) {
        super.setRolloverEnabled(bl);
        this.removeMouseListener(this.rolloverListener);
        if (bl) {
            this.addMouseListener(this.rolloverListener);
        }
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
        this.setForeground(color);
    }

    public Color getRolloverColor() {
        return this.rolloverColor;
    }

    public void setRolloverColor(Color color) {
        this.rolloverColor = color;
    }

    protected static class OpenUriAction
    extends AbstractAction {
        public static final String URI = "uri";

        public OpenUriAction(String string, String string2, Icon icon, URI uRI) {
            super(string, icon);
            if (uRI != null) {
                this.putValue(URI, uRI);
            }
            if (string2 != null) {
                this.putValue("ShortDescription", string2);
            }
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            Object object = this.getValue(URI);
            if (object != null) {
                UserInteraction.browse(object.toString());
            }
        }
    }
}

