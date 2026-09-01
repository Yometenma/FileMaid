package net.filemaid.format;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.util.RegularExpressions;

class LazyLookup {
    private final Map<?, ?> properties;

    public LazyLookup(Map<?, ?> map) {
        this.properties = map;
    }

    private String definingKey(Object object) {
        return RegularExpressions.NON_WORD.matcher(object.toString()).replaceAll("").toLowerCase(Locale.ROOT);
    }

    public Object get(Object object) {
        return this.find(object).map(this.properties::get).orElse(null);
    }

    public boolean contains(Object object) {
        return this.find(object).isPresent();
    }

    private Optional<?> find(Object object2) {
        if (this.properties.containsKey(object2)) {
            return Optional.of(object2);
        }
        String string = this.definingKey(object2);
        return this.properties.keySet().stream().filter(object -> this.definingKey(object).equals(string)).findFirst();
    }

    public Stream<Map.Entry<String, Object>> stream() {
        return this.properties.keySet().stream().map(object -> new LazyEntry(object));
    }

    public int size() {
        return this.properties.size();
    }

    public String toString() {
        return this.properties.keySet().stream().map(Object::toString).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.joining(", ", "{", "}"));
    }

    private class LazyEntry
    implements Map.Entry<String, Object> {
        private final Object key;

        public LazyEntry(Object object) {
            this.key = object;
        }

        @Override
        public String getKey() {
            return String.valueOf(this.key);
        }

        @Override
        public Object getValue() {
            return new LazyValue(this.key);
        }

        @Override
        public Object setValue(Object object) {
            throw new UnsupportedOperationException();
        }

        public String toString() {
            return this.getKey() + "=" + this.getValue();
        }
    }

    private class LazyValue
    implements Callable<Object> {
        private final Object key;

        public LazyValue(Object object) {
            this.key = object;
        }

        @Override
        public Object call() throws Exception {
            return LazyLookup.this.get(this.key);
        }

        public String toString() {
            return "{" + this.key + "}";
        }
    }
}

