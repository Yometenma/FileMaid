package net.filemaid.ui.rename;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.Icon;
import net.filemaid.ResourceManager;

public enum MatchMode {
    Opportunistic,
    Strict;


    public String getDescription() {
        switch (this) {
            case Opportunistic: {
                return "Find the best possible match";
            }
        }
        return "Find the perfect match or none at all";
    }

    public Icon getIcon() {
        switch (this) {
            case Opportunistic: {
                return ResourceManager.getIcon("action.match");
            }
        }
        return ResourceManager.getIcon("action.match.strict");
    }

    public static List<String> names() {
        return Arrays.stream(MatchMode.values()).map(Enum::name).collect(Collectors.toList());
    }

    public static MatchMode forName(String string) {
        for (MatchMode matchMode : MatchMode.values()) {
            if (!matchMode.name().equalsIgnoreCase(string)) continue;
            return matchMode;
        }
        throw new IllegalArgumentException(string + " not in " + MatchMode.names());
    }
}

