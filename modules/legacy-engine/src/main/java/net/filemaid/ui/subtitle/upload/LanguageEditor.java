package net.filemaid.ui.subtitle.upload;

import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import net.filemaid.Language;
import net.filemaid.ui.LanguageComboBox;

class LanguageEditor
extends DefaultCellEditor {
    public LanguageEditor() {
        super(LanguageEditor.createLanguageComboBox());
    }

    @Override
    public Component getTableCellEditorComponent(JTable jTable, Object object, boolean bl, int n, int n2) {
        LanguageComboBox languageComboBox = (LanguageComboBox)super.getTableCellEditorComponent(jTable, object, bl, n, n2);
        languageComboBox.getModel().setSelectedItem(object);
        return languageComboBox;
    }

    public static LanguageComboBox createLanguageComboBox() {
        LanguageComboBox languageComboBox = new LanguageComboBox(Language.defaultLanguage());
        languageComboBox.setFocusable(false);
        return languageComboBox;
    }
}

