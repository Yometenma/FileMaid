package net.filemaid.cli;

import groovy.lang.Closure;
import java.util.Comparator;

public class GroovyComparator
implements Comparator<Object> {
    private final Closure<?> closure;

    public GroovyComparator(Closure<?> closure) {
        this.closure = closure;
    }

    @Override
    public int compare(Object object, Object object2) {
        Object object3 = this.closure.call(new Object[]{object, object2});
        if (object3 instanceof Number) {
            double d = ((Number)object3).doubleValue();
            if (d > 0.0) {
                return 1;
            }
            if (d < 0.0) {
                return -1;
            }
            return 0;
        }
        throw new IllegalArgumentException("Invalid comparator: '" + object3 + "' is not a Number object");
    }

    public String toString() {
        return "CLOSURE";
    }

    public static GroovyComparator wrap(Closure<?> closure) {
        return new GroovyComparator(closure);
    }
}

