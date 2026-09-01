package net.filemaid.ui;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import net.filemaid.Language;

public class LanguageComboBoxModel
extends AbstractListModel
implements ComboBoxModel {
    public static final Language ALL_LANGUAGES = new Language("undefined", "und", "und", "und", new String[]{"All Languages"});
    private Language defaultLanguage;
    private Language selection;
    private List<Language> favorites = new Favorites(2);
    private List<Language> values = Language.availableLanguages();

    public LanguageComboBoxModel(Language language, Language language2) {
        this.defaultLanguage = language;
        this.selection = language2;
    }

    @Override
    public Language getElementAt(int n) {
        if (n == 0) {
            return this.defaultLanguage;
        }
        if (--n < this.favorites.size()) {
            return this.favorites.get(n);
        }
        return this.values.get(n -= this.favorites.size());
    }

    @Override
    public int getSize() {
        return 1 + this.favorites.size() + this.values.size();
    }

    public List<Language> favorites() {
        return this.favorites;
    }

    @Override
    public Language getSelectedItem() {
        return this.selection;
    }

    @Override
    public void setSelectedItem(Object object) {
        if (object instanceof Language) {
            Language language = (Language)object;
            this.selection = ALL_LANGUAGES.matches(language) ? ALL_LANGUAGES : language;
        }
    }

    protected int convertFavoriteIndexToModel(int n) {
        return 1 + n;
    }

    protected void fireFavoritesAdded(int n, int n2) {
        this.fireIntervalAdded(this, this.convertFavoriteIndexToModel(n), this.convertFavoriteIndexToModel(n2));
    }

    protected void fireFavoritesRemoved(int n, int n2) {
        this.fireIntervalRemoved(this, this.convertFavoriteIndexToModel(n), this.convertFavoriteIndexToModel(n2));
    }

    private class Favorites
    extends AbstractList<Language> {
        private final List<Language> data;
        private final int capacity;

        public Favorites(int n) {
            this.data = new ArrayList<Language>(n);
            this.capacity = n;
        }

        @Override
        public Language get(int n) {
            return this.data.get(n);
        }

        @Override
        public boolean add(Language language) {
            return this.addIfAbsent(0, language);
        }

        @Override
        public void add(int n, Language language) {
            this.addIfAbsent(n, language);
        }

        public boolean addIfAbsent(int n, Language language) {
            if (language == null || language == ALL_LANGUAGES || language.matches(LanguageComboBoxModel.this.defaultLanguage) || this.contains(language) || n >= this.capacity) {
                return false;
            }
            if (this.data.size() >= this.capacity) {
                this.remove(this.data.size() - 1);
            }
            this.data.add(n, language.clone());
            LanguageComboBoxModel.this.fireFavoritesAdded(n, n);
            return true;
        }

        @Override
        public boolean contains(Object object) {
            if (object instanceof Language) {
                Language language = (Language)object;
                for (Language language2 : this.data) {
                    if (!language.matches(language2)) continue;
                    return true;
                }
            }
            return false;
        }

        @Override
        public Language remove(int n) {
            Language language = this.data.remove(n);
            LanguageComboBoxModel.this.fireFavoritesRemoved(n, n);
            return language;
        }

        @Override
        public int size() {
            return this.data.size();
        }
    }
}

