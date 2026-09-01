package net.filemaid.ui.rename;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

class OriginalOrder<T>
implements Comparator<T> {
    private Map<T, Integer> index;

    public static <T> Comparator<T> of(Collection<T> collection) {
        return new OriginalOrder<T>(collection);
    }

    public OriginalOrder(Collection<T> collection) {
        this.index = new HashMap<T, Integer>(collection.size());
        int n = 0;
        for (T t : collection) {
            this.index.put(t, n++);
        }
    }

    @Override
    public int compare(T t, T t2) {
        Integer n = this.index.get(t);
        Integer n2 = this.index.get(t2);
        if (n == null) {
            return n2 == null ? 0 : 1;
        }
        if (n2 == null) {
            return -1;
        }
        return n.compareTo(n2);
    }
}

