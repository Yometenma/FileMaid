package net.filemaid.format;

import groovy.lang.GroovyObjectSupport;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import net.filemaid.Logging;
import net.filemaid.format.BindingException;

public class DynamicBindings
extends GroovyObjectSupport {
    private final Supplier<Collection<?>> keys;
    private final Get<String, Object> properties;
    private final Function<Object, Object> normalize;

    public DynamicBindings(Supplier<Collection<?>> supplier, Get<String, Object> get) {
        this(supplier, get, Function.identity());
    }

    private DynamicBindings(Supplier<Collection<?>> supplier, Get<String, Object> get, Function<Object, Object> function) {
        this.keys = supplier;
        this.properties = get;
        this.normalize = function;
    }

    public DynamicBindings normalize(Function<Object, Object> function) {
        return new DynamicBindings(this.keys, this.properties, function);
    }

    public Object getAt(Object object) {
        return this.getProperty(object.toString());
    }

    public Object getProperty(String string) {
        try {
            return this.normalize.apply(this.properties.get(string));
        }
        catch (BindingException bindingException) {
            throw bindingException;
        }
        catch (Exception exception) {
            throw new BindingException((Object)string, (Object)Logging.cause(exception), exception, BindingException.Flag.UNDEFINED);
        }
    }

    public String toString() {
        return this.keys.get().toString();
    }

    @FunctionalInterface
    public static interface Get<T, R> {
        public R get(T var1) throws Exception;
    }
}

