package net.filemaid.ui.list;

import java.util.Locale;
import net.filemaid.UserData;
import net.filemaid.util.PreferencesMap;

public enum ListMode {
    Sequence,
    Episode,
    File;


    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public PreferencesMap.PreferencesEntry<String> persistentFormat() {
        return UserData.forPackage(ListMode.class).entry("list.format." + this.key());
    }

    public String getDefaultFormatExpression() {
        switch (this) {
            case Sequence: {
                return "Sequence - {i.pad(2)}";
            }
            case Episode: {
                return "{n} - {s00e00} - [{absolute}] - [{airdate}] - {t}";
            }
            case File: {
                return "{fn}";
            }
        }
        return null;
    }
}

