package net.filemaid.web;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.filemaid.web.SearchResult;
import net.filemaid.web.XDB;

public class Series
extends SearchResult
implements Serializable {
    public static final int UNDEFINED = -1;
    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = Integer.MAX_VALUE;
    private final int year;
    private final int score;
    private final Map<XDB, Integer> ids;

    public Series(int n, String string, String[] stringArray, int n2, int n3, Map<XDB, Integer> map) {
        super(n, string, stringArray);
        this.year = n2;
        this.score = n3;
        this.ids = map;
    }

    public int getYear() {
        return this.year;
    }

    public int getScore() {
        return this.score;
    }

    public Integer getExternalId(XDB xDB) {
        return this.ids.get((Object)xDB);
    }

    @Override
    public int hashCode() {
        if (this.id > 0) {
            return this.id;
        }
        if (this.ids.size() > 0) {
            return this.ids.hashCode();
        }
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Series) {
            Series series = (Series)object;
            if (this.id > 0 && series.id > 0) {
                return this.id == series.id;
            }
            if (this.ids.size() > 0 && series.ids.size() > 0) {
                return this.ids.equals(series.ids);
            }
            return this.name.equals(series.name);
        }
        return false;
    }

    @Override
    public Series clone() {
        return new Series(this.id, this.name, this.aliasNames, this.year, this.score, this.ids);
    }

    @Override
    public String toString() {
        if (this.name.isEmpty()) {
            return this.ids.entrySet().stream().map(entry -> entry.getKey() + "::" + entry.getValue()).collect(Collectors.joining(" "));
        }
        if (this.ids.isEmpty()) {
            return this.name.toLowerCase();
        }
        if (this.year > 0) {
            return this.name + " (" + this.year + ")";
        }
        return this.name;
    }

    public Series withConfidence(int n) {
        return new Series(this.id, this.name, this.aliasNames, this.year, n, this.ids);
    }

    public static Series TMDB(int n, String string, double d) {
        return new Series(n, string, null, -1, (int)d, Collections.singletonMap(XDB.TheMovieDB, n));
    }

    public static Series XDB(XDB xDB, int n, String string) {
        return new Series(-1, string, null, -1, 0, Collections.singletonMap(xDB, n));
    }

    public static Series XDB(String string, Integer n, Number number, Map<XDB, Integer> map) {
        return new Series(-1, string, null, n != null ? n : -1, number != null ? number.intValue() : 0, map);
    }

    public static Series QUERY(String string) {
        return string == null || string.isEmpty() ? null : new Series(-1, string, null, -1, 0, Collections.emptyMap());
    }

    public static Map<XDB, Integer> XID(Integer ... integerArray) {
        EnumMap<XDB, Integer> enumMap = new EnumMap<XDB, Integer>(XDB.class);
        XDB[] xDBArray = XDB.values();
        for (int i = 0; i < integerArray.length; ++i) {
            if (integerArray[i] == null || integerArray[i] <= 0) continue;
            enumMap.put(xDBArray[i], integerArray[i]);
        }
        return enumMap;
    }
}

