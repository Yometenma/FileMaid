package net.filemaid.ui;

import java.awt.Cursor;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.plaf.basic.BasicCheckBoxUI;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.ui.RoundDecoration;
import net.filemaid.util.ui.SwingUI;

public class RepeatToggle
extends JCheckBox {
    protected boolean helpTextVisible = false;
    protected String helpTextUnselected = null;
    protected String helpTextSelected = null;

    public RepeatToggle(Icon icon, Icon icon2) {
        this.setUI(new BasicCheckBoxUI());
        this.setCursor(Cursor.getPredefinedCursor(12));
        this.setFont(this.getFont().deriveFont(8.0f));
        this.setIcon(this.createAutoRepeatCheckBoxIcon(icon2));
        this.setSelectedIcon(this.createAutoRepeatCheckBoxIcon(icon));
        this.addChangeListener(changeEvent -> this.updateHelpText());
        this.addMouseListener(SwingUI.mouseHover(this::setHelpText));
    }

    protected Icon createAutoRepeatCheckBoxIcon(Icon icon) {
        return new RoundDecoration(icon, 28, 28, ThemeSupport.getPanelBackground(), ThemeSupport.getColor(0xD7D7D7));
    }

    protected void updateHelpText() {
        this.setText(this.helpTextVisible ? (this.isSelected() ? this.helpTextSelected : this.helpTextUnselected) : null);
    }

    public void setHelpText(String string, String string2) {
        this.helpTextUnselected = string;
        this.helpTextSelected = string2;
        this.updateHelpText();
    }

    public void setHelpText(boolean bl) {
        this.helpTextVisible = bl;
        this.updateHelpText();
    }
}

