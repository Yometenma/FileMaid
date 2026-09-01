package net.filemaid.util;

import java.util.Optional;
import java.util.function.Function;
import net.filemaid.Logging;

public class SystemProperty<T> {
    private final String key;
    private final Function<String, T> valueFunction;
    private final T defaultValue;

    public static <T> T get(String string, Function<String, T> function, T t) {
        return new SystemProperty<T>(string, function, t).get();
    }

    public static <T> Optional<T> optional(String string, Function<String, T> function) {
        return new SystemProperty<T>(string, function, null).optional();
    }

    public static <T> SystemProperty<T> of(String string, Function<String, T> function, T t) {
        return new SystemProperty<T>(string, function, t);
    }

    private SystemProperty(String string, Function<String, T> function, T t) {
        this.key = string;
        this.valueFunction = function;
        this.defaultValue = t;
    }

    public T get() {
        String string = System.getProperty(this.key);
        if (string != null && !string.isEmpty()) {
            try {
                return this.valueFunction.apply(string);
            }
            catch (Exception exception) {
                Logging.error("SystemProperty", this.key, exception);
            }
        }
        return this.defaultValue;
    }

    public Optional<T> optional() {
        return Optional.ofNullable(this.get());
    }
}

