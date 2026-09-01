package net.filemaid.media;

import java.text.CollationKey;
import java.util.function.Function;

class IndexEntry<T> {
    private final T object;
    private final String lenientName;
    private final String strictName;
    private final Function<String, CollationKey[]> prepare;
    private CollationKey[] lenientKey;
    private CollationKey[] strictKey;

    public IndexEntry(T t, String string, String string2, Function<String, CollationKey[]> function) {
        this.object = t;
        this.lenientName = string;
        this.strictName = string2;
        this.prepare = function;
    }

    public T getObject() {
        return this.object;
    }

    public String getLenientName() {
        return this.lenientName;
    }

    public String getStrictName() {
        return this.strictName;
    }

    public CollationKey[] getLenientKey() {
        if (this.lenientKey == null && this.lenientName != null) {
            this.lenientKey = (CollationKey[])this.prepare.apply(this.lenientName).clone();
        }
        return this.lenientKey;
    }

    public CollationKey[] getStrictKey() {
        if (this.strictKey == null && this.strictName != null) {
            this.strictKey = (CollationKey[])this.prepare.apply(this.strictName).clone();
        }
        return this.strictKey;
    }

    public String toString() {
        return this.strictName != null ? this.strictName : this.lenientName;
    }
}

