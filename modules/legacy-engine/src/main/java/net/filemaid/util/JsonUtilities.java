package net.filemaid.util;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import net.filemaid.Logging;
import net.filemaid.util.StringUtilities;

public class JsonUtilities {
    public static final Object[] EMPTY_ARRAY = new Object[0];

    public static Object readJson(CharSequence charSequence) {
        if (charSequence.length() == 0) {
            return Collections.EMPTY_MAP;
        }
        return JsonReader.jsonToJava((String)charSequence.toString(), Collections.singletonMap("USE_MAPS", true));
    }

    public static Object readJson(InputStream inputStream) {
        return JsonReader.jsonToJava((InputStream)inputStream, Collections.singletonMap("USE_MAPS", true));
    }

    public static String json(Object object) {
        return JsonWriter.objectToJson((Object)object, Collections.singletonMap("TYPE", false));
    }

    public static String json(Object object, boolean bl, boolean bl2) {
        HashMap<String, Object> hashMap = new HashMap<String, Object>(4);
        hashMap.put("PRETTY_PRINT", bl2);
        hashMap.put("SKIP_NULL", true);
        hashMap.put("TYPE", bl);
        hashMap.put("TYPE_NAME_MAP", Collections.singletonMap(object.getClass().getName(), object.getClass().getSimpleName()));
        return JsonWriter.objectToJson((Object)object, hashMap);
    }

    public static Map<Object, Object> asMap(Object object) {
        if (object instanceof Map) {
            return (Map)object;
        }
        return Collections.EMPTY_MAP;
    }

    public static Object[] asArray(Object object) {
        JsonObject jsonObject;
        if (object instanceof JsonObject && (jsonObject = (JsonObject)object).isArray()) {
            return jsonObject.getArray();
        }
        if (object instanceof Object[]) {
            return (Object[])object;
        }
        if (object instanceof Collection) {
            return ((Collection)object).toArray(EMPTY_ARRAY);
        }
        return EMPTY_ARRAY;
    }

    public static Map<Object, Object>[] asMapArray(Object object) {
        return (Map[])Arrays.stream(JsonUtilities.asArray(object)).map(JsonUtilities::asMap).filter(map -> map.size() > 0).toArray(Map[]::new);
    }

    public static Stream<Map<Object, Object>> streamJsonObjects(Object object) {
        return Arrays.stream(JsonUtilities.asMapArray(object));
    }

    public static Object[] getArray(Object object, String string) {
        return JsonUtilities.asArray(JsonUtilities.asMap(object).get(string));
    }

    public static String[] getStringArray(Object object, String string) {
        return (String[])Arrays.stream(JsonUtilities.getArray(object, string)).map(StringUtilities::asNonEmptyString).filter(Objects::nonNull).toArray(String[]::new);
    }

    public static Map<Object, Object> getMap(Object object, String string) {
        return JsonUtilities.asMap(JsonUtilities.asMap(object).get(string));
    }

    public static Map<Object, Object>[] getMapArray(Object object, String string) {
        return JsonUtilities.asMapArray(JsonUtilities.asMap(object).get(string));
    }

    public static Stream<Map<Object, Object>> streamJsonObjects(Object object, String string) {
        return Arrays.stream(JsonUtilities.getMapArray(object, string));
    }

    public static Stream<Map<Object, Object>> streamJsonObjects(Object object, String ... stringArray) {
        return Arrays.stream(stringArray).flatMap(string -> Arrays.stream(JsonUtilities.getMapArray(object, string)));
    }

    public static Map<Object, Object> getFirstMap(Object object, String string) {
        Object[] objectArray = JsonUtilities.getArray(object, string);
        if (objectArray.length > 0) {
            return JsonUtilities.asMap(objectArray[0]);
        }
        return Collections.EMPTY_MAP;
    }

    public static String getString(Object object, String string) {
        return StringUtilities.asNonEmptyString(JsonUtilities.asMap(object).get(string));
    }

    public static String getString(Object object, String ... stringArray) {
        return Arrays.stream(stringArray).map(string -> JsonUtilities.getString(object, string)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    public static Optional<String> optionalString(Object object, String string) {
        return Optional.ofNullable(JsonUtilities.getString(object, string));
    }

    public static Integer getInteger(Object object, String string) {
        return JsonUtilities.getStringValue(object, string, Integer::parseInt);
    }

    public static Double getDouble(Object object, String string) {
        return JsonUtilities.getStringValue(object, string, Double::parseDouble);
    }

    public static BigDecimal getDecimal(Object object, String string) {
        return JsonUtilities.getStringValue(object, string, BigDecimal::new);
    }

    public static Instant getEpochTime(Object object, String string2) {
        return JsonUtilities.getStringValue(object, string2, string -> Instant.ofEpochSecond(Long.parseLong(string)));
    }

    public static <V> V getStringValue(Object object, String string, Function<String, V> function) {
        String string2 = JsonUtilities.getString(object, string);
        if (string2 != null) {
            try {
                return function.apply(string2);
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.format("Bad %s value: %s => %s", string, string2, exception));
            }
        }
        return null;
    }

    public static <K extends Enum<K>> EnumMap<K, String> getEnumMap(Object object, Class<K> clazz) {
        return JsonUtilities.getEnumMap(object, clazz, StringUtilities::asNonEmptyString);
    }

    public static <K extends Enum<K>, V> EnumMap<K, V> getEnumMap(Object object, Class<K> clazz, Function<Object, V> function) {
        Map<Object, Object> map = JsonUtilities.asMap(object);
        EnumMap<K, V> enumMap = new EnumMap<K, V>(clazz);
        for (K enum_ : clazz.getEnumConstants()) {
            V v;
            Object object2 = map.get(enum_.name());
            if (object2 == null || (v = function.apply(object2)) == null) continue;
            enumMap.put(enum_, v);
        }
        return enumMap;
    }

    private JsonUtilities() {
        throw new UnsupportedOperationException();
    }
}

