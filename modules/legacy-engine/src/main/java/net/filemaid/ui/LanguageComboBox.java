package net.filemaid.ui;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.JComboBox;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import net.filemaid.Language;
import net.filemaid.UserData;
import net.filemaid.ui.LanguageComboBoxCellRenderer;
import net.filemaid.ui.LanguageComboBoxModel;
import net.filemaid.util.PreferencesList;
import net.filemaid.util.PreferencesMap;

public class LanguageComboBox
extends JComboBox {
    private final PreferencesMap.PreferencesEntry<String> persistentSelectedLanguage;
    private final PreferencesList<String> persistentFavoriteLanguages;

    public LanguageComboBox(Language language) {
        this(language, null);
    }

    public LanguageComboBox(Language language2, UserData userData) {
        super(new LanguageComboBoxModel(language2, language2));
        this.setRenderer(new LanguageComboBoxCellRenderer(super.getRenderer()));
        this.persistentSelectedLanguage = userData != null ? userData.entry("language.selected") : null;
        PreferencesList<String> preferencesList = this.persistentFavoriteLanguages = userData != null ? userData.node("language.favorites").asList() : null;
        if (userData != null) {
            try {
                this.getModel().setSelectedItem(Language.getLanguage(this.persistentSelectedLanguage.getValue()));
            }
            catch (Exception exception) {
                this.getModel().setSelectedItem(language2);
            }
        }
        List<Language> list = this.getModel().favorites();
        if (userData != null) {
            for (String string : this.persistentFavoriteLanguages) {
                Language language3 = Language.getLanguage(string);
                if (language3 == null) continue;
                list.add(list.size(), language3);
            }
        }
        if (list.isEmpty()) {
            list.add(Language.getLanguage(Locale.ENGLISH.getLanguage()));
            list.add(Language.getLanguage(Locale.getDefault().getLanguage()));
        }
        if (userData != null) {
            this.onChange(language -> {
                if (list.add((Language)language)) {
                    this.persistentFavoriteLanguages.set(list.stream().map(Language::getCode).collect(Collectors.toList()));
                }
                this.persistentSelectedLanguage.setValue(language.getCode());
            });
        }
    }

    public LanguageComboBoxModel getModel() {
        return (LanguageComboBoxModel)super.getModel();
    }

    public void onChange(final Consumer<Language> consumer) {
        this.addPopupMenuListener(new PopupMenuListener(){
            private Object previous = null;

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                JComboBox jComboBox = (JComboBox)popupMenuEvent.getSource();
                this.previous = jComboBox.getSelectedItem();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                JComboBox jComboBox = (JComboBox)popupMenuEvent.getSource();
                if (this.previous != jComboBox.getSelectedItem()) {
                    consumer.accept((Language)jComboBox.getSelectedItem());
                }
                this.previous = null;
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                this.previous = null;
            }
        });
    }
}

