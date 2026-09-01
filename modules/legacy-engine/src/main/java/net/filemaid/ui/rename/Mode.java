package net.filemaid.ui.rename;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeMap;
import net.filemaid.Logging;
import net.filemaid.UserData;
import net.filemaid.WebServices;
import net.filemaid.media.MetaAttributes;
import net.filemaid.ui.rename.FormatDialog;
import net.filemaid.ui.rename.MatchFormatterType;
import net.filemaid.util.PreferencesList;
import net.filemaid.util.PreferencesMap;
import net.filemaid.web.AudioTrack;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieLookupService;
import net.filemaid.web.MusicLookupService;

public enum Mode {
    Episode,
    Movie,
    Music,
    File;


    public Datasource[] getDatasources() {
        switch (this) {
            case Episode: {
                return WebServices.getEpisodeListProviders();
            }
            case Movie: {
                return WebServices.getMovieLookupServices();
            }
            case Music: {
                return WebServices.getMusicLookupServices();
            }
        }
        return WebServices.getLocalDatasources();
    }

    public MatchFormatterType getFormatterType() {
        switch (this) {
            case Episode: {
                return MatchFormatterType.EPISODE;
            }
            case Movie: {
                return MatchFormatterType.MOVIE;
            }
            case Music: {
                return MatchFormatterType.MUSIC;
            }
        }
        return MatchFormatterType.FILE;
    }

    public Mode cycle(int n) {
        Mode[] modeArray = Mode.values();
        int n2 = this.ordinal() + n;
        if (n2 >= modeArray.length) {
            n2 = 0;
        } else if (n2 < 0) {
            n2 = modeArray.length - 1;
        }
        return modeArray[n2];
    }

    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public PreferencesMap.PreferencesEntry<String> persistentFormat() {
        return UserData.forPackage(Mode.class).entry("rename.format." + this.key());
    }

    public PreferencesList<String> persistentFormatHistory() {
        return UserData.forPackage(Mode.class).node("format.recent." + this.key()).asList();
    }

    public PreferencesMap.PreferencesEntry<String> persistentSample() {
        return UserData.forPackage(Mode.class).entry("format.sample." + this.key());
    }

    public Object getDefaultSampleObject() {
        try {
            ResourceBundle resourceBundle = ResourceBundle.getBundle(FormatDialog.class.getName());
            String string = resourceBundle.getString(this.key() + ".sample");
            return MetaAttributes.toObject(string);
        }
        catch (MissingResourceException missingResourceException) {
        }
        catch (Exception exception) {
            Logging.debug.severe(Logging.cause("Invalid Sample", exception));
        }
        return null;
    }

    public String getDefaultFormatExpression() {
        return this.getSampleExpressions().iterator().next();
    }

    public String getSelectedFormatExpression() {
        return this.persistentFormatHistory().isEmpty() ? this.getDefaultFormatExpression() : this.persistentFormatHistory().get(0);
    }

    public Iterable<String> getSampleExpressions() {
        ResourceBundle resourceBundle = ResourceBundle.getBundle(FormatDialog.class.getName());
        TreeMap<String, String> treeMap = new TreeMap<String, String>();
        String string = this.key() + ".example";
        for (String string2 : resourceBundle.keySet()) {
            if (!string2.startsWith(string)) continue;
            treeMap.put(string2, resourceBundle.getString(string2));
        }
        return treeMap.values();
    }

    public static Mode getMode(Datasource datasource) {
        if (datasource instanceof MovieLookupService) {
            return Movie;
        }
        if (datasource instanceof EpisodeListProvider) {
            return Episode;
        }
        if (datasource instanceof MusicLookupService) {
            return Music;
        }
        return File;
    }

    public static Mode getMode(Object object) {
        if (object instanceof Episode) {
            return Episode;
        }
        if (object instanceof Movie) {
            return Movie;
        }
        if (object instanceof AudioTrack) {
            return Music;
        }
        return File;
    }
}

