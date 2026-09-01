package net.filemaid.format;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import javax.script.Bindings;
import net.filemaid.Logging;
import net.filemaid.format.BindingException;
import net.filemaid.format.Define;
import net.filemaid.format.StringBinding;

public class ExpressionBindings
extends AbstractMap<String, Object>
implements Bindings {
    protected final Map<String, MethodBinding> bindings = new HashMap<String, MethodBinding>(256);
    protected final MethodBinding undefined;

    public ExpressionBindings(Object object) {
        for (Method method : object.getClass().getMethods()) {
            Define define = method.getAnnotation(Define.class);
            if (define == null) continue;
            for (String string : define.value()) {
                this.bindings.put(this.definingKey(string), new MethodBinding(string, object, method));
            }
        }
        this.undefined = this.bindings.remove("");
    }

    private String definingKey(Object object) {
        return object.toString().toLowerCase(Locale.ROOT);
    }

    @Override
    public Object get(Object object) {
        try {
            Object object2;
            MethodBinding methodBinding = this.bindings.get(this.definingKey(object));
            Object object3 = object2 = methodBinding == null ? null : methodBinding.call();
            if (this.isUndefined(object2)) {
                object2 = this.undefined == null ? null : this.undefined.invoke(object);
            }
            return object2;
        }
        catch (Exception exception) {
            BindingException bindingException = Logging.findCause(exception, BindingException.class);
            if (bindingException != null) {
                throw bindingException;
            }
            throw new BindingException(object, (Object)Logging.cause(exception), exception, BindingException.Flag.UNDEFINED);
        }
    }

    protected boolean isUndefined(Object object) {
        if (object == null) {
            return true;
        }
        if (object instanceof StringBinding) {
            return false;
        }
        if (object instanceof CharSequence) {
            CharSequence charSequence = (CharSequence)object;
            return charSequence.length() == 0;
        }
        if (object instanceof Collection) {
            Collection collection = (Collection)object;
            return collection.isEmpty();
        }
        return false;
    }

    @Override
    public Object put(String string, Object object) {
        return null;
    }

    @Override
    public Object remove(Object object) {
        return null;
    }

    @Override
    public boolean containsKey(Object object) {
        return this.bindings.containsKey(this.definingKey(object));
    }

    @Override
    public Set<String> keySet() {
        return this.bindings.values().stream().map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return this.bindings.values().stream().collect(Collectors.toSet());
    }

    private static class MethodBinding
    implements Callable<Object>,
    Map.Entry<String, Object> {
        private final String name;
        private final Object object;
        private final Method method;
        private Object result;

        public MethodBinding(String string, Object object, Method method) {
            this.name = string;
            this.object = object;
            this.method = method;
        }

        @Override
        public Object call() throws Exception {
            if (this.result == null) {
                this.result = this.method.invoke(this.object, new Object[0]);
            }
            return this.result;
        }

        public Object invoke(Object ... objectArray) throws Exception {
            return this.method.invoke(this.object, objectArray);
        }

        @Override
        public String getKey() {
            return this.name;
        }

        @Override
        public Object getValue() {
            try {
                return this.call();
            }
            catch (Exception exception) {
                return null;
            }
        }

        @Override
        public Object setValue(Object object) {
            return null;
        }

        public String toString() {
            return "{" + this.name + "}";
        }
    }
}

