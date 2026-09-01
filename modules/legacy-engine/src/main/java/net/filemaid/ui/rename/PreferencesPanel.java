package net.filemaid.ui.rename;

import java.awt.Component;
import java.awt.LayoutManager;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import net.filemaid.Language;
import net.filemaid.ResourceManager;
import net.filemaid.ui.rename.MatchMode;
import net.filemaid.ui.rename.Preset;
import net.filemaid.ui.rename.RadioGroup;
import net.filemaid.util.ui.PrototypeCellSize;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.SortOrder;
import net.miginfocom.swing.MigLayout;

class PreferencesPanel
extends JPanel {
    private final RadioGroup<MatchMode> mode = new RadioGroup<MatchMode>("Match Mode", MatchMode.values(), MatchMode::getDescription);
    private final RadioGroup<SortOrder> order = new RadioGroup<SortOrder>("Episode Order", SortOrder.values(), SortOrder::getDescription);
    private final JList<Language> language = new JList<Language>(Preset.getSupportedLanguages());

    public PreferencesPanel() {
        super((LayoutManager)new MigLayout("insets 0, nogrid, fill, flowy"));
        this.add((Component)SwingUI.createScrollPaneGroup("Language", this.language), "wmin 200px, grow, wrap");
        this.add(this.mode, "wmin 150px, grow");
        this.add(this.order, "wmin 150px, grow");
        this.language.setCellRenderer(new DefaultListCellRenderer(){

            @Override
            public Component getListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
                super.getListCellRendererComponent((JList<?>)jList, object, n, bl, bl2);
                if (object != null) {
                    this.setText(((Language)object).getName());
                    this.setIcon(ResourceManager.getFlagIcon(((Language)object).getCode()));
                }
                return this;
            }
        });
        PrototypeCellSize.fixedCellSize(this.language);
    }

    public void setMatchMode(MatchMode matchMode) {
        this.mode.setSelectedItem(matchMode);
    }

    public void setOrder(SortOrder sortOrder) {
        this.order.setSelectedItem(sortOrder);
    }

    public void setLanguage(Language language) {
        for (int i = 0; i < this.language.getModel().getSize(); ++i) {
            Language language2 = this.language.getModel().getElementAt(i);
            if (!language2.matches(language)) continue;
            this.language.setSelectedValue(language2, true);
            return;
        }
    }

    public MatchMode getMatchMode() {
        return this.mode.getSelectedItem();
    }

    public SortOrder getOrder() {
        return this.order.getSelectedItem();
    }

    public Language getLanguage() {
        return this.language.getSelectedValue();
    }
}

