package net.filemaid.ui;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import net.filemaid.Language;
import net.filemaid.ResourceManager;
import net.filemaid.ui.LanguageComboBoxModel;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.ui.HorizontalRule;

public class LanguageComboBoxCellRenderer
implements ListCellRenderer {
    private Border defaultPadding = new EmptyBorder(2, 2, 2, 2);
    private Border favoritePadding = new EmptyBorder(0, 6, 0, 6);
    private ListCellRenderer base;

    public LanguageComboBoxCellRenderer(ListCellRenderer listCellRenderer) {
        this.base = listCellRenderer;
        this.defaultPadding = new CompoundBorder(this.defaultPadding, ((JLabel)((Object)listCellRenderer)).getBorder());
    }

    public Component getListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
        JLabel jLabel = (JLabel)this.base.getListCellRendererComponent(jList, object, n, bl, bl2);
        Language language = (Language)object;
        jLabel.setText(language.getName());
        jLabel.setIcon(ResourceManager.getFlagIcon(language.getCode()));
        jLabel.setBorder(this.defaultPadding);
        LanguageComboBoxModel languageComboBoxModel = (LanguageComboBoxModel)jList.getModel();
        if (n > 0 && n <= languageComboBoxModel.favorites().size()) {
            jLabel.setBorder(new CompoundBorder(this.favoritePadding, jLabel.getBorder()));
        }
        if (n == 0 || n == languageComboBoxModel.favorites().size()) {
            HorizontalRule.south(jLabel, 10, ThemeSupport.getPassiveColor(), jList.getBackground());
        }
        return jLabel;
    }
}

