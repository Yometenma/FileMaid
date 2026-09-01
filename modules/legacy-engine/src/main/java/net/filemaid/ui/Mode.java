package net.filemaid.ui;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.Icon;
import javax.swing.JComponent;
import net.filemaid.ResourceManager;
import net.filemaid.Settings;
import net.filemaid.ui.episodelist.EpisodeListPanel;
import net.filemaid.ui.filter.FilterPanel;
import net.filemaid.ui.list.ListPanel;
import net.filemaid.ui.rename.RenamePanel;
import net.filemaid.ui.sfv.SfvPanel;
import net.filemaid.ui.subtitle.SubtitlePanel;

public enum Mode {
    Rename,
    Episodes,
    Subtitles,
    SFV,
    Filter,
    List;


    public Icon getIcon() {
        switch (this) {
            case Rename: {
                return ResourceManager.getIcon("panel.rename");
            }
            case Episodes: {
                return ResourceManager.getIcon("panel.episodelist");
            }
            case Subtitles: {
                return ResourceManager.getIcon("panel.subtitle");
            }
            case SFV: {
                return ResourceManager.getIcon("panel.sfv");
            }
            case Filter: {
                return ResourceManager.getIcon("panel.filter");
            }
            case List: {
                return ResourceManager.getIcon("panel.list");
            }
        }
        return null;
    }

    public JComponent createPanel() {
        switch (this) {
            case Rename: {
                return new RenamePanel();
            }
            case Episodes: {
                return new EpisodeListPanel();
            }
            case Subtitles: {
                return new SubtitlePanel();
            }
            case SFV: {
                return new SfvPanel();
            }
            case Filter: {
                return new FilterPanel();
            }
            case List: {
                return new ListPanel();
            }
        }
        return null;
    }

    public static Mode[] modes() {
        if (Settings.isMacSandbox()) {
            return new Mode[]{Rename, Episodes, SFV, Filter, List};
        }
        return new Mode[]{Rename, Episodes, Subtitles, SFV, Filter, List};
    }

    public static Mode[] episodeHandlerSequence() {
        return new Mode[]{Rename, List};
    }

    public static Mode[] fileHandlerSequence() {
        return new Mode[]{Rename, SFV, List};
    }

    public static Mode[] textHandlerSequence() {
        return new Mode[]{Rename};
    }

    public static List<String> names() {
        return Arrays.stream(Mode.modes()).map(Enum::name).collect(Collectors.toList());
    }

    public static Mode forName(String string) {
        for (Mode mode : Mode.modes()) {
            if (!mode.name().equalsIgnoreCase(string)) continue;
            return mode;
        }
        throw new IllegalArgumentException(string + " not in " + Mode.names());
    }
}

