package net.filemaid.ui.rename;

import java.awt.AWTKeyStroke;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import net.filemaid.CachedResource;
import net.filemaid.Language;
import net.filemaid.Logging;
import net.filemaid.StandardRenameAction;
import net.filemaid.WebServices;
import net.filemaid.format.ExpressionFileComparator;
import net.filemaid.format.ExpressionFileFilter;
import net.filemaid.format.ExpressionFileFormat;
import net.filemaid.ui.rename.MatchMode;
import net.filemaid.ui.rename.SmartMode;
import net.filemaid.util.AlphanumComparator;
import net.filemaid.web.Datasource;
import net.filemaid.web.SortOrder;

public class Preset {
    public String id;
    public String name;
    public String path;
    public String includes;
    public String fileOrder;
    public String format;
    public String database;
    public String sortOrder;
    public String matchMode;
    public String language;
    public String action;
    public String keyStroke;
    public static final Comparator<Preset> NAME_ORDER = Comparator.comparing(Preset::getName, Comparator.nullsLast(AlphanumComparator.getInstance()));
    public static final Comparator<Preset> KEYSTROKE_ORDER = Comparator.comparing(Preset::getKeyStroke, Comparator.nullsLast(Comparator.comparing(AWTKeyStroke::getKeyCode))).thenComparing(NAME_ORDER);

    protected Preset(String string, String string2, String string3, String string4, String string5, String string6, String string7, String string8, String string9, String string10, String string11, String string12) {
        this.id = string;
        this.name = string2;
        this.path = string3;
        this.includes = string4;
        this.fileOrder = string5;
        this.format = string6;
        this.database = string7;
        this.sortOrder = string8;
        this.matchMode = string9;
        this.language = string10;
        this.action = string11;
        this.keyStroke = string12;
    }

    public Preset(String string, String string2, File file, ExpressionFileFilter expressionFileFilter, ExpressionFileComparator expressionFileComparator, ExpressionFileFormat expressionFileFormat, Datasource datasource, SortOrder sortOrder, MatchMode matchMode, Language language, StandardRenameAction standardRenameAction, KeyStroke keyStroke) {
        this.id = string;
        this.name = string2;
        this.path = file == null ? null : file.getPath();
        this.includes = expressionFileFilter == null ? null : expressionFileFilter.getSource();
        this.fileOrder = expressionFileComparator == null ? null : expressionFileComparator.getSource();
        this.format = expressionFileFormat == null ? null : expressionFileFormat.getSource();
        this.database = datasource == null ? null : datasource.getIdentifier();
        this.sortOrder = sortOrder == null ? null : sortOrder.name();
        this.matchMode = matchMode == null ? null : matchMode.name();
        this.language = language == null ? null : language.getCode();
        this.action = standardRenameAction == null ? null : standardRenameAction.name();
        this.keyStroke = keyStroke == null ? null : keyStroke.toString();
    }

    public String getKey() {
        return this.id != null ? this.id : this.name;
    }

    public String getIdentifier() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public KeyStroke getKeyStroke() {
        return this.getValue(this.keyStroke, KeyStroke::getKeyStroke);
    }

    public File getInputFolder() {
        return this.getValue(this.path, File::new);
    }

    public String getIncludeFilterExpression() {
        return this.getValue(this.includes, String::toString);
    }

    public String getFileOrderExpression() {
        return this.getValue(this.fileOrder, String::toString);
    }

    public String getFormatExpression() {
        return this.getValue(this.format, String::toString);
    }

    public MatchMode getMatchMode() {
        return this.getValue(this.matchMode, MatchMode::forName);
    }

    public SortOrder getSortOrder() {
        return this.getValue(this.sortOrder, SortOrder::forName);
    }

    public Language getLanguage() {
        return this.getValue(this.language, Language::getLanguage);
    }

    public StandardRenameAction getRenameAction() {
        return this.getValue(this.action, StandardRenameAction::forName);
    }

    public Datasource getDatasource() {
        return this.getValue(this.database, string -> WebServices.getService((String)string, (Datasource[])Preset.getSupportedServices()));
    }

    public Icon getIcon() {
        return this.getValue(this.database, string -> WebServices.getService((String)string, (Datasource[])Preset.getSupportedServices()).getIcon());
    }

    private <T> T getValue(String string, CachedResource.Transform<String, T> transform) {
        try {
            return string == null || string.isEmpty() ? null : (T)transform.transform(string);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(this, string, exception));
            return null;
        }
    }

    public Preset deleteKeyStroke() {
        this.keyStroke = null;
        return this;
    }

    public String toString() {
        return this.name;
    }

    public static Datasource[] getSupportedServices() {
        return (Datasource[])Stream.of(WebServices.getEpisodeListProviders(), WebServices.getMovieLookupServices(), WebServices.getMusicLookupServices(), SmartMode.values(), WebServices.getLocalDatasources()).flatMap(Stream::of).toArray(Datasource[]::new);
    }

    public static StandardRenameAction[] getSupportedActions() {
        return new StandardRenameAction[]{StandardRenameAction.MOVE, StandardRenameAction.COPY, StandardRenameAction.KEEPLINK, StandardRenameAction.SYMLINK, StandardRenameAction.HARDLINK};
    }

    public static Language[] getSupportedLanguages() {
        LinkedHashMap<String, Language> linkedHashMap = new LinkedHashMap<String, Language>(64);
        Language.preferredLanguages().forEach(language -> linkedHashMap.put(language.getCode(), language));
        Language.availableLanguages().forEach(language -> linkedHashMap.put(language.getCode(), language));
        return linkedHashMap.values().toArray(new Language[0]);
    }

    public static KeyStroke[] getSupportedKeyboardShortcuts() {
        int n;
        ArrayList<KeyStroke> arrayList = new ArrayList<KeyStroke>(19);
        arrayList.add(null);
        for (n = 49; n <= 57; ++n) {
            arrayList.add(KeyStroke.getKeyStroke(n, 0));
        }
        for (n = 97; n <= 105; ++n) {
            arrayList.add(KeyStroke.getKeyStroke(n, 0));
        }
        return arrayList.toArray(new KeyStroke[0]);
    }
}

