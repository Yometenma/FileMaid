package net.filemaid.ui.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.KeyStroke;
import net.filemaid.Logging;
import net.filemaid.UserData;
import net.filemaid.ui.rename.Preset;
import net.filemaid.util.PreferencesMap;

public enum UserPresets {
    USER_PRESETS{

        @Override
        public Stream<Preset> list() {
            return PERSISTENT_PRESETS.values().stream();
        }
    }
    ,
    DEFAULT_PRESETS{

        @Override
        public Stream<Preset> list() {
            Preset preset = this.newExamplePreset("Organize Movies for Plex", "{ drive }/Media/{ plex.id }", "TheMovieDB", null);
            Preset preset2 = this.newExamplePreset("Organize Episodes for Plex", "{ drive }/Media/{ plex.id }", "TheMovieDB::TV", "Airdate");
            Preset preset3 = this.newExamplePreset("Organize Movies for Jellyfin", "{ drive }/Media/{ jellyfin.id }", "TheMovieDB", null);
            Preset preset4 = this.newExamplePreset("Organize Episodes for Jellyfin", "{ drive }/Media/{ jellyfin.id }", "TheMovieDB::TV", "Airdate");
            Preset preset5 = this.newExamplePreset("Match by Episode Title", "{ order.airdate.plex.name }", "TheMovieDB::TV", "Date");
            Preset preset6 = this.newExamplePreset("Match by Absolute Number", "{ order.airdate.plex.name }", "TheMovieDB::TV", "Absolute");
            Preset preset7 = this.newExamplePreset("Convert DVD to Airdate Order", "{ order.airdate.plex.name }", "TheMovieDB::TV", "DVD");
            Preset preset8 = this.newExamplePreset("Convert AniDB to TheMovieDB numbers", "{ db.TheMovieDB.plex.name }", "AniDB", "Absolute");
            Preset preset9 = this.newExamplePreset("Convert AniDB to TheTVDB numbers", "{ db.TheTVDB.plex.name }", "AniDB", "Absolute");
            Preset preset10 = this.newExamplePreset("Sort Photos by Date Taken", "{ dt.format('yyyy-MM-dd HH\u2236mm\u2236ss') } [{ camera.model }]", "exif", null);
            Preset preset11 = this.newExamplePreset("Match via Extended Attributes", "{ drive }/Media/{ plex.id }", "xattr", null);
            Preset preset12 = this.newExamplePreset("Append Media Properties", "{ fn }{ allOf{vf}{vc}{hdr}{ac}{channels}.joining(' ', ' [', ']') }", "file", null);
            Preset preset13 = this.newExamplePreset("Original Media Title", "{ any{ original }{ mediaTitle }{ fn } }", "file", null);
            Preset preset14 = this.newExamplePreset("File to Folder Name", "{ folder }/{ fn }/{ fn }", "file", null);
            Preset preset15 = this.newExamplePreset("Clean Name", "{ fn.clean() }", "file", null);
            Preset preset16 = this.newExamplePreset("Transliterate non-ASCII characters", "{ fn.ascii().space('_') }", "file", null);
            return Stream.of(preset, preset3, preset2, preset4, preset5, preset6, preset7, preset8, preset9, preset10, preset11, preset12, preset13, preset14, preset15, preset16);
        }

        private Preset newExamplePreset(String string, String string2, String string3, String string4) {
            return new Preset(null, string, null, null, null, string2, string3, string4, "Opportunistic", "en", "MOVE", null);
        }
    };

    public static final int PRESET_GROUP_KEYSTROKE = 0;
    public static final int PRESET_GROUP_NAME = 1;
    private static final PreferencesMap<Preset> PERSISTENT_PRESETS;

    public abstract Stream<Preset> list();

    public List<Preset[]> getPresetGroups() {
        LinkedHashMap<KeyStroke, Preset> linkedHashMap = new LinkedHashMap<KeyStroke, Preset>();
        ArrayList<Preset> arrayList = new ArrayList<Preset>();
        try {
            this.list().sorted(Preset.KEYSTROKE_ORDER).forEach(preset -> {
                KeyStroke keyStroke = preset.getKeyStroke();
                if (keyStroke != null && !linkedHashMap.containsKey(keyStroke)) {
                    linkedHashMap.put(keyStroke, preset);
                } else {
                    arrayList.add(preset);
                }
            });
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
        Preset[] presetArray = linkedHashMap.values().toArray(new Preset[0]);
        Preset[] presetArray2 = arrayList.toArray(new Preset[0]);
        return Arrays.asList(presetArray, presetArray2);
    }

    public static void save(Preset preset) {
        PERSISTENT_PRESETS.put(preset.getKey(), preset);
    }

    public static void delete(Preset preset) {
        PERSISTENT_PRESETS.remove(preset.getKey());
    }

    static {
        PERSISTENT_PRESETS = UserData.forPackage(Preset.class).node("presets").asMap(Preset.class);
    }
}

