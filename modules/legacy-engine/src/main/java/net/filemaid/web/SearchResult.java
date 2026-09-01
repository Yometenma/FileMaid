package net.filemaid.web;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.filemaid.util.FunctionList;

public class SearchResult
implements Serializable {
    protected int id;
    protected String name;
    protected String[] aliasNames;
    protected static final String[] EMPTY_STRING_ARRAY = new String[0];

    public SearchResult() {
    }

    public SearchResult(SearchResult searchResult) {
        this.id = searchResult.id;
        this.name = searchResult.name;
        this.aliasNames = searchResult.aliasNames;
    }

    public SearchResult(int n) {
        this(n, null, EMPTY_STRING_ARRAY);
    }

    public SearchResult(int n, String string) {
        this(n, string, EMPTY_STRING_ARRAY);
    }

    public SearchResult(int n, String string, Collection<String> collection) {
        this(n, string, collection.toArray(EMPTY_STRING_ARRAY));
    }

    public SearchResult(int n, String string, String[] stringArray) {
        this.id = n;
        this.name = string;
        this.aliasNames = stringArray;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String[] getAliasNames() {
        return this.aliasNames == null ? EMPTY_STRING_ARRAY : (String[])this.aliasNames.clone();
    }

    public List<String> getEffectiveNames() {
        if (this.name == null || this.name.isEmpty()) {
            return Collections.emptyList();
        }
        if (this.aliasNames == null || this.aliasNames.length == 0) {
            return Collections.singletonList(this.name);
        }
        return FunctionList.of(n -> n == 0 ? this.name : this.aliasNames[n - 1], 1 + this.aliasNames.length);
    }

    public int hashCode() {
        return this.id;
    }

    public boolean equals(Object object) {
        if (object instanceof SearchResult) {
            SearchResult searchResult = (SearchResult)object;
            return this.id == searchResult.id;
        }
        return false;
    }

    public String toString() {
        return this.name != null ? this.name : Integer.toString(this.id);
    }

    public SearchResult clone() {
        return new SearchResult(this);
    }
}

