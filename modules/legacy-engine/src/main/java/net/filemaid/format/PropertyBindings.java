package net.filemaid.format;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.filemaid.Logging;
import net.filemaid.format.BindingException;

public class PropertyBindings
extends AbstractMap<String, Object> {
    private final Object object;
    private final Map<String, Object> properties = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);

    public PropertyBindings(Object object) {
        this.object = object;
        for (Method method : object.getClass().getMethods()) {
            if (method.getReturnType() == Void.TYPE || method.getParameterTypes().length != 0 || method.getDeclaringClass().getName().startsWith("java")) continue;
            if (method.getName().length() > 3 && method.getName().substring(0, 3).equalsIgnoreCase("get")) {
                this.properties.put(method.getName().substring(3), method);
            }
            if (method.getName().length() <= 2 || !method.getName().substring(0, 2).equalsIgnoreCase("is")) continue;
            this.properties.put(method.getName().substring(2), method);
        }
    }

    @Override
    public Object get(Object object) {
        Object object2 = this.properties.get(object);
        if (object2 instanceof Method) {
            try {
                object2 = ((Method)object2).invoke(this.object, new Object[0]);
            }
            catch (Exception exception) {
                throw new BindingException(object, (Object)Logging.cause(exception), exception, BindingException.Flag.UNDEFINED);
            }
        }
        return object2;
    }

    @Override
    public Object put(String string, Object object) {
        return this.properties.put(string, object);
    }

    @Override
    public Object remove(Object object) {
        return this.properties.remove(object);
    }

    @Override
    public boolean containsKey(Object object) {
        return this.properties.containsKey(object);
    }

    @Override
    public Set<String> keySet() {
        return this.properties.keySet();
    }

    @Override
    public boolean isEmpty() {
        return this.properties.isEmpty();
    }

    @Override
    public String toString() {
        return this.properties.toString();
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        HashSet<Map.Entry<String, Object>> hashSet = new HashSet<Map.Entry<String, Object>>();
        for (final String string : this.keySet()) {
            hashSet.add(new Map.Entry<String, Object>(){

                @Override
                public String getKey() {
                    return string;
                }

                @Override
                public Object getValue() {
                    return PropertyBindings.this.get(string);
                }

                @Override
                public Object setValue(Object object) {
                    return PropertyBindings.this.put(string, object);
                }
            });
        }
        return hashSet;
    }
}

