package net.filemaid.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.RandomAccess;
import java.util.prefs.Preferences;
import net.filemaid.util.PreferencesMap;

public class PreferencesList<T>
extends AbstractList<T>
implements RandomAccess {
    private final PreferencesMap<T> prefs;

    public PreferencesList(PreferencesMap<T> preferencesMap) {
        this.prefs = preferencesMap;
    }

    @Override
    public T get(int n) {
        return this.prefs.get(this.key(n));
    }

    private String key(int n) {
        return Integer.toString(n);
    }

    @Override
    public int size() {
        return this.prefs.size();
    }

    @Override
    public boolean add(T t) {
        this.setImpl(this.size(), t);
        return true;
    }

    @Override
    public void add(int n, T t) {
        int n2 = this.size();
        if (n > n2) {
            throw new IndexOutOfBoundsException(String.format("Index: %s, Size: %s", n, n2));
        }
        this.copy(n, n + 1, n2 - n);
        this.setImpl(n, t);
    }

    private T setImpl(int n, T t) {
        return this.prefs.put(this.key(n), t);
    }

    @Override
    public T remove(int n) {
        int n2 = this.size() - 1;
        this.copy(n + 1, n, n2 - n);
        this.prefs.remove(this.key(n2));
        return null;
    }

    @Override
    public T set(int n, T t) {
        if (n < 0 || n >= this.size()) {
            throw new IndexOutOfBoundsException();
        }
        return this.setImpl(n, t);
    }

    private void copy(int n, int n2, int n3) {
        if (n3 == 0 || n == n2) {
            return;
        }
        ArrayList<T> arrayList = new ArrayList<>(this.subList(n, n + n3));
        int n4 = n2;
        for (int i = 0; i < n3; ++i) {
            this.setImpl(n4, arrayList.get(i));
            ++n4;
        }
    }

    public void trimToSize(int n) {
        for (int i = this.size() - 1; i >= n; --i) {
            this.remove(i);
        }
    }

    public void set(Collection<T> collection) {
        this.trimToSize(collection.size());
        int n = 0;
        for (T t : collection) {
            this.setImpl(n++, t);
        }
    }

    @Override
    public void clear() {
        this.prefs.clear();
    }

    public static PreferencesList<String> map(Preferences preferences) {
        return new PreferencesList<String>(PreferencesMap.map(preferences));
    }

    public static <T> PreferencesList<T> map(Preferences preferences, PreferencesMap.Adapter<T> adapter) {
        return new PreferencesList<T>(PreferencesMap.map(preferences, adapter));
    }
}

