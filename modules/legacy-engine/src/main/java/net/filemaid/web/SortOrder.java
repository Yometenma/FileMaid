package net.filemaid.web;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public enum SortOrder {
    Airdate,
    DVD,
    Absolute,
    Digital,
    Story,
    Production,
    Official,
    Date;


    public String toString() {
        switch (this) {
            case Airdate: {
                return "Airdate";
            }
            case DVD: {
                return "DVD";
            }
            case Absolute: {
                return "Absolute";
            }
            case Digital: {
                return "Digital";
            }
            case Story: {
                return "Story Arc";
            }
            case Production: {
                return "Production";
            }
            case Official: {
                return "Official";
            }
        }
        return "Date and Title";
    }

    public String getDescription() {
        switch (this) {
            case Airdate: {
                return "Default Order";
            }
            case DVD: {
                return "DVD Order";
            }
            case Absolute: {
                return "Absolute Order";
            }
            case Digital: {
                return "Digital / Alternate Order";
            }
            case Story: {
                return "Story Arc / Alternate DVD Order";
            }
            case Production: {
                return "Production / Regional Order";
            }
            case Official: {
                return "Official Order";
            }
        }
        return "Absolute Airdate Order";
    }

    public List<String> keys() {
        switch (this) {
            case Airdate: {
                return Arrays.asList("Default", "Airdate");
            }
            case DVD: {
                return Arrays.asList("DVD");
            }
            case Absolute: {
                return Arrays.asList("Absolute");
            }
            case Digital: {
                return Arrays.asList("Digital", "Alternate");
            }
            case Story: {
                return Arrays.asList("Story", "Story Arc", "Alternate DVD");
            }
            case Production: {
                return Arrays.asList("Production", "Regional");
            }
            case Official: {
                return Arrays.asList("Official");
            }
        }
        return Arrays.asList("Date", "Date and Title", "Absolute Airdate");
    }

    public boolean equals(String string) {
        return this.name().equals(string);
    }

    public boolean matches(String string) {
        for (String string2 : this.keys()) {
            if (!string2.equalsIgnoreCase(string)) continue;
            return true;
        }
        return false;
    }

    public static List<String> names() {
        return Arrays.stream(SortOrder.values()).map(SortOrder::keys).flatMap(Collection::stream).collect(Collectors.toList());
    }

    public static SortOrder forName(String string) {
        for (SortOrder sortOrder : SortOrder.values()) {
            if (!sortOrder.matches(string)) continue;
            return sortOrder;
        }
        throw new IllegalArgumentException(string + " not in " + SortOrder.names());
    }
}

