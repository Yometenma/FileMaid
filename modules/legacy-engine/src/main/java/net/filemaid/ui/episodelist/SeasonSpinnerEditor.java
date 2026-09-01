package net.filemaid.ui.episodelist;

import javax.swing.JSpinner;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import net.filemaid.util.StringUtilities;

class SeasonSpinnerEditor
extends JSpinner.DefaultEditor {
    public SeasonSpinnerEditor(JSpinner jSpinner) {
        super(jSpinner);
        this.getTextField().setFormatterFactory(new DefaultFormatterFactory(new DefaultFormatter(){

            @Override
            public Object stringToValue(String string) {
                Integer n = StringUtilities.matchInteger(string);
                return n == null ? 0 : n;
            }

            @Override
            public String valueToString(Object object) {
                Integer n = (Integer)object;
                return n.equals(0) ? "All Seasons" : "Season " + n;
            }
        }));
        this.getTextField().setHorizontalAlignment(4);
        this.getTextField().setOpaque(false);
        this.setOpaque(false);
    }
}

