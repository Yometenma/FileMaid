package net.filemaid.format;

import java.io.File;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import javax.script.ScriptException;
import net.filemaid.GroovyEngine;
import net.filemaid.Logging;
import net.filemaid.MemoryCache;
import net.filemaid.format.ExpressionFilter;
import net.filemaid.format.MediaBindingBean;

public class ExpressionFileComparator
implements Comparator<File>,
Function<File, Comparable> {
    private final String source;
    private final ExpressionFilter filter;
    private final Comparator order;
    private final MemoryCache<File, Optional<Comparable>> cache = MemoryCache.weak();

    public ExpressionFileComparator(String string) throws ScriptException {
        this.source = string;
        if (string.startsWith("^")) {
            this.filter = new ExpressionFilter(string.substring(1));
            this.order = Comparator.nullsLast(Comparator.naturalOrder());
        } else {
            this.filter = new ExpressionFilter(string);
            this.order = Comparator.nullsLast(Comparator.reverseOrder());
        }
    }

    public String getSource() {
        return this.source;
    }

    @Override
    public Comparable apply(File file2) {
        return this.cache.get(file2, file -> {
            try {
                Object object = this.filter.apply(new MediaBindingBean(file, (File)file), Object.class);
                if (object instanceof Collection) {
                    object = new ComparableCollection((Collection)object);
                }
                return Optional.ofNullable(GroovyEngine.asType(object, Comparable.class));
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause(this, file, exception));
                return Optional.empty();
            }
        }).orElse(null);
    }

    @Override
    public int compare(File file, File file2) {
        return this.order.compare(this.apply(file), this.apply(file2));
    }

    public String toString() {
        return this.getSource();
    }

    private static class ComparableCollection
    extends AbstractList<Comparable>
    implements Comparable<Collection<?>> {
        private final Comparator order = Comparator.nullsFirst(Comparator.naturalOrder());
        private final Comparable[] values;

        public ComparableCollection(Collection<?> collection) {
            this.values = (Comparable[])collection.stream().map(object -> object instanceof Collection ? new ComparableCollection((Collection)object) : (Comparable)object).toArray(Comparable[]::new);
        }

        @Override
        public Comparable get(int n) {
            return n < this.values.length ? this.values[n] : null;
        }

        @Override
        public int size() {
            return this.values.length;
        }

        @Override
        public int compareTo(Collection<?> collection) {
            int n = 0;
            for (Object obj : collection) {
                int n2;
                if ((n2 = this.order.compare(this.get(n++), obj)) == 0) continue;
                return n2;
            }
            return this.order.compare(this.size(), collection.size());
        }
    }
}

