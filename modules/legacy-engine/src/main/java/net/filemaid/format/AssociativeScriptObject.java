package net.filemaid.format;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.filemaid.format.BindingException;
import net.filemaid.format.LazyLookup;

public class AssociativeScriptObject
extends AbstractMap<String, Object> {
    private final LazyLookup lookup;
    private final Function<String, Object> defaultValue;
    private final Function<Object, Object> normalize;

    public AssociativeScriptObject(Map<?, ?> map, Function<String, Object> function) {
        this(new LazyLookup(map), function, Function.identity());
    }

    private AssociativeScriptObject(LazyLookup lazyLookup, Function<String, Object> function, Function<Object, Object> function2) {
        this.lookup = lazyLookup;
        this.defaultValue = function;
        this.normalize = function2;
    }

    public AssociativeScriptObject normalize(Function<Object, Object> function) {
        return new AssociativeScriptObject(this.lookup, this.defaultValue, function);
    }

    @Override
    public Object get(Object object) {
        if (object == null) {
            return null;
        }
        try {
            Object object2 = this.lookup.get(object);
            if (object2 != null) {
                return this.normalize.apply(object2);
            }
        }
        catch (BindingException bindingException) {
            // empty catch block
        }
        return this.defaultValue.apply(object.toString());
    }

    @Override
    public boolean containsKey(Object object) {
        return this.lookup.contains(object);
    }

    @Override
    public boolean containsValue(Object object) {
        return false;
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return new AbstractSet<Map.Entry<String, Object>>(){

            @Override
            public Iterator<Map.Entry<String, Object>> iterator() {
                return AssociativeScriptObject.this.lookup.stream().iterator();
            }

            @Override
            public int size() {
                return AssociativeScriptObject.this.lookup.size();
            }
        };
    }

    @Override
    public int hashCode() {
        return this.lookup.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return this.lookup.equals(object);
    }

    @Override
    public String toString() {
        return this.lookup.toString();
    }
}

