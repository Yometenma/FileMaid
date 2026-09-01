package net.filemaid.util;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.Logging;

public class PreferencesMap<T>
implements Map<String, T> {
    private final Preferences prefs;
    private final Adapter<T> adapter;
    public static final Adapter<String> STRING = Adapter.create(String::toString, String::toString);
    public static final Adapter<Integer> INTEGER = Adapter.create(Integer::parseInt, String::valueOf);

    public PreferencesMap(Preferences preferences, Adapter<T> adapter) {
        this.prefs = preferences;
        this.adapter = adapter;
    }

    @Override
    public T get(Object object) {
        return this.adapter.get(this.prefs, object.toString());
    }

    @Override
    public T put(String string, T t) {
        this.adapter.put(this.prefs, string, t);
        return null;
    }

    @Override
    public T remove(Object object) {
        this.adapter.remove(this.prefs, object.toString());
        return null;
    }

    public String[] keys() {
        try {
            return this.adapter.keys(this.prefs);
        }
        catch (BackingStoreException backingStoreException) {
            throw new RuntimeException(backingStoreException);
        }
    }

    @Override
    public void clear() {
        for (String string : this.keys()) {
            this.adapter.remove(this.prefs, string);
        }
    }

    @Override
    public boolean containsKey(Object object) {
        if (object instanceof String) {
            return Stream.of(this.keys()).anyMatch(object::equals);
        }
        return false;
    }

    @Override
    public boolean containsValue(Object object) {
        for (String string : this.keys()) {
            if (!object.equals(this.get(string))) continue;
            return true;
        }
        return false;
    }

    @Override
    public Set<Map.Entry<String, T>> entrySet() {
        LinkedHashSet<Map.Entry<String, T>> linkedHashSet = new LinkedHashSet<Map.Entry<String, T>>();
        for (String string : this.keys()) {
            linkedHashSet.add(new PreferencesEntry<T>(this.prefs, string, this.adapter));
        }
        return linkedHashSet;
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public Set<String> keySet() {
        return Stream.of(this.keys()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public void putAll(Map<? extends String, ? extends T> map) {
        for (Map.Entry<? extends String, ? extends T> entry : map.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public int size() {
        return this.keys().length;
    }

    @Override
    public List<T> values() {
        ArrayList<T> arrayList = new ArrayList<T>();
        for (String string : this.keys()) {
            arrayList.add(this.get(string));
        }
        return arrayList;
    }

    public Stream<T> get() {
        return Stream.of(this.keys()).map(this::get).filter(Objects::nonNull);
    }

    public static PreferencesMap<String> map(Preferences preferences) {
        return PreferencesMap.map(preferences, STRING);
    }

    public static <T> PreferencesMap<T> map(Preferences preferences, Adapter<T> adapter) {
        return new PreferencesMap<T>(preferences, adapter);
    }

    public static interface Adapter<T> {
        public T get(Preferences var1, String var2);

        public void put(Preferences var1, String var2, T var3);

        default public String[] keys(Preferences preferences) throws BackingStoreException {
            return preferences.keys();
        }

        default public void remove(Preferences preferences, String string) {
            preferences.remove(string);
        }

        public static <T> Adapter<T> create(final Function<String, T> function, final Function<T, String> function2) {
            return new Adapter<T>(){

                @Override
                public T get(Preferences preferences, String string) {
                    String string2 = preferences.get(string, null);
                    if (string2 != null) {
                        try {
                            return function.apply(string2);
                        }
                        catch (Exception exception) {
                            Logging.trace(exception);
                        }
                    }
                    return null;
                }

                @Override
                public void put(Preferences preferences, String string, T t) {
                    preferences.put(string, (String)function2.apply(t));
                }
            };
        }
    }

    public static class PreferencesEntry<T>
    implements Map.Entry<String, T> {
        private final String key;
        private final Preferences prefs;
        private final Adapter<T> adapter;
        private T defaultValue = null;

        public PreferencesEntry(Preferences preferences, String string, Adapter<T> adapter) {
            this.key = string;
            this.prefs = preferences;
            this.adapter = adapter;
        }

        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public T getValue() {
            T t = this.adapter.get(this.prefs, this.key);
            if (t != null) {
                return t;
            }
            return this.defaultValue;
        }

        public PreferencesEntry<T> defaultValue(T t) {
            this.defaultValue = t;
            return this;
        }

        @Override
        public T setValue(T t) {
            this.adapter.put(this.prefs, this.key, t);
            return null;
        }

        public void remove() {
            this.adapter.remove(this.prefs, this.key);
        }

        public void flush() {
            try {
                this.prefs.flush();
            }
            catch (Exception exception) {
                Logging.trace(exception);
            }
        }
    }

    public static class EnumSetAdapter<E extends Enum<E>>
    implements Adapter<Set<E>> {
        private static final String SEPARATOR = " ";
        private final Class<E> type;

        public EnumSetAdapter(Class<E> clazz) {
            this.type = clazz;
        }

        private E value(String string) {
            if (string != null && !string.isEmpty()) {
                try {
                    return Enum.valueOf(this.type, string);
                }
                catch (Exception exception) {
                    Logging.trace(exception);
                }
            }
            return null;
        }

        private Set<E> none() {
            return EnumSet.noneOf(this.type);
        }

        @Override
        public Set<E> get(Preferences preferences, String string) {
            return Pattern.compile(SEPARATOR, 16).splitAsStream(preferences.get(string, "")).map(this::value).filter(Objects::nonNull).collect(Collectors.toCollection(this::none));
        }

        @Override
        public void put(Preferences preferences, String string, Set<E> set) {
            preferences.put(string, set.stream().map(Enum::name).collect(Collectors.joining(SEPARATOR)));
        }
    }

    public static class EnumValueAdapter<E extends Enum<E>>
    implements Adapter<E> {
        private final Class<E> type;

        public EnumValueAdapter(Class<E> clazz) {
            this.type = clazz;
        }

        @Override
        public E get(Preferences preferences, String string) {
            String string2 = preferences.get(string, null);
            if (string2 != null) {
                try {
                    return Enum.valueOf(this.type, string2);
                }
                catch (Exception exception) {
                    Logging.trace(exception);
                }
            }
            return null;
        }

        @Override
        public void put(Preferences preferences, String string, E e) {
            preferences.put(string, ((Enum)e).name());
        }
    }

    public static class JsonAdapter<T>
    implements Adapter<T> {
        private final Class<T> type;

        public JsonAdapter(Class<T> clazz) {
            this.type = clazz;
        }

        @Override
        public T get(Preferences preferences, String string) {
            String string2 = preferences.get(string, null);
            if (string2 != null) {
                try {
                    return this.type.cast(this.toObject(string2));
                }
                catch (Exception exception) {
                    Logging.trace(string2, exception);
                }
            }
            return null;
        }

        @Override
        public void put(Preferences preferences, String string, T t) {
            preferences.put(string, this.toJson(t));
        }

        private Object toObject(String string) {
            HashMap<String, Object> hashMap = new HashMap<String, Object>(2);
            hashMap.put("TYPE_NAME_MAP", Collections.singletonMap(this.type.getName(), this.type.getSimpleName()));
            hashMap.put("USE_MAPS", false);
            return JsonReader.jsonToJava((String)string, hashMap);
        }

        private String toJson(Object object) {
            HashMap<String, Object> hashMap = new HashMap<String, Object>(2);
            hashMap.put("TYPE_NAME_MAP", Collections.singletonMap(this.type.getName(), this.type.getSimpleName()));
            hashMap.put("SKIP_NULL", true);
            String string = JsonWriter.objectToJson((Object)object, hashMap);
            if (string.length() > 8192) {
                throw new IllegalStateException("8192 character limit exceeded");
            }
            return string;
        }
    }
}

