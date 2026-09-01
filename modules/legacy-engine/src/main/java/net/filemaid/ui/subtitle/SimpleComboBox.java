package net.filemaid.ui.subtitle;

import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import net.filemaid.ui.ThemeSupport;

public class SimpleComboBox
extends JComboBox {
    public SimpleComboBox(Icon icon) {
        this.setUI(new SimpleComboBoxUI(icon));
        this.setBorder(BorderFactory.createEmptyBorder());
    }

    private static class SimpleComboBoxUI
    extends BasicComboBoxUI {
        private final Icon dropDownArrowIcon;

        public SimpleComboBoxUI(Icon icon) {
            this.dropDownArrowIcon = icon;
        }

        @Override
        protected JButton createArrowButton() {
            JButton jButton = new JButton(this.dropDownArrowIcon);
            jButton.setContentAreaFilled(false);
            jButton.setBorderPainted(false);
            jButton.setFocusPainted(false);
            jButton.setOpaque(false);
            return jButton;
        }

        @Override
        protected ComboPopup createPopup() {
            return new BasicComboPopup(this.comboBox){

                @Override
                protected Rectangle computePopupBounds(int n, int n2, int n3, int n4) {
                    Rectangle rectangle = super.computePopupBounds(n, n2, n3, n4);
                    rectangle.width = Math.max(rectangle.width, this.list.getPreferredSize().width);
                    return rectangle;
                }

                @Override
                protected void configurePopup() {
                    super.configurePopup();
                    this.setOpaque(true);
                    this.setBackground(ThemeSupport.getPanelBackground());
                    this.list.setBackground(this.getBackground());
                    this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ThemeSupport.getColor(0xEEEEEE), 1), BorderFactory.createEmptyBorder(1, 1, 1, 1)));
                }
            };
        }
    }
}

