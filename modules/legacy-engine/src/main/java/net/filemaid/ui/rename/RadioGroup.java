package net.filemaid.ui.rename;

import java.awt.Component;
import java.awt.LayoutManager;
import java.util.function.Function;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;
import net.miginfocom.swing.MigLayout;

class RadioGroup<E>
extends JPanel {
    private static final String MODEL_PROPERTY = "model";

    public RadioGroup(String string, E[] EArray, Function<E, String> function) {
        super((LayoutManager)new MigLayout("insets 3px, flowy"));
        this.setBorder(new TitledBorder(string));
        ButtonGroup buttonGroup = new ButtonGroup();
        for (E e : EArray) {
            JRadioButton jRadioButton = new JRadioButton(e.toString());
            jRadioButton.putClientProperty(MODEL_PROPERTY, e);
            jRadioButton.setToolTipText(function.apply(e));
            jRadioButton.setOpaque(false);
            buttonGroup.add(jRadioButton);
            this.add(jRadioButton);
        }
    }

    public E getSelectedItem() {
        for (Component component : this.getComponents()) {
            JRadioButton jRadioButton = (JRadioButton)component;
            if (!jRadioButton.isSelected()) continue;
            return (E)jRadioButton.getClientProperty(MODEL_PROPERTY);
        }
        return null;
    }

    public void setSelectedItem(E e) {
        for (Component component : this.getComponents()) {
            JRadioButton jRadioButton = (JRadioButton)component;
            if (!e.equals(jRadioButton.getClientProperty(MODEL_PROPERTY))) continue;
            jRadioButton.setSelected(true);
            return;
        }
    }
}

