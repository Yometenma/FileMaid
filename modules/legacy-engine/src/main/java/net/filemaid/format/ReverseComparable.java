package net.filemaid.format;

import net.filemaid.format.StringBinding;

public class ReverseComparable
implements StringBinding,
Comparable<Object> {
    private final Comparable<Object> object;

    public ReverseComparable(Comparable<Object> comparable) {
        this.object = comparable;
    }

    @Override
    public int compareTo(Object object) {
        if (object instanceof ReverseComparable) {
            return ((ReverseComparable)object).object.compareTo(this.object);
        }
        return ((Comparable)object).compareTo(this.object);
    }

    @Override
    public String toString() {
        return "-" + this.object;
    }
}

