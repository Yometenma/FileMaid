package net.filemaid.format;

import com.sun.jna.Platform;
import groovy.json.JsonGenerator;
import groovy.lang.Closure;
import groovy.lang.Script;
import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.SimpleBindings;
import net.filemaid.ApplicationFolder;
import net.filemaid.InvalidInputException;
import net.filemaid.format.DataResource;
import net.filemaid.format.ExpressionEngine;
import net.filemaid.format.StringBinding;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.JsonUtilities;
import net.filemaid.util.RegularExpressions;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

public class ExpressionFormatFunctions {
    private static Object call(Script script, Object object) {
        if (object instanceof Closure) {
            try {
                return ExpressionFormatFunctions.call(script, ((Closure)object).call());
            }
            catch (Exception exception) {
                return null;
            }
        }
        if (ExpressionFormatFunctions.isEmptyValue(script, object)) {
            return null;
        }
        return object;
    }

    public static boolean isEmptyValue(Script script, Object object) {
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

    public static boolean none(Script script, Object object, Object ... objectArray) {
        return ExpressionFormatFunctions.stream(script, object, null, objectArray).noneMatch(DefaultTypeTransformation::castToBoolean);
    }

    public static Object any(Script script, Object object, Object object2, Object ... objectArray) {
        return ExpressionFormatFunctions.stream(script, object, object2, objectArray).findFirst().orElse(null);
    }

    public static List<Object> allOf(Script script, Object object, Object object2, Object ... objectArray) {
        return ExpressionFormatFunctions.stream(script, object, object2, objectArray).collect(Collectors.toList());
    }

    public static List<?> list(Script script, Object object, Object ... objectArray) {
        return DefaultGroovyMethods.flatten(ExpressionFormatFunctions.allOf(script, object, null, objectArray));
    }

    public static long count(Script script, Object object2, Object ... objectArray) {
        return ExpressionFormatFunctions.list(script, object2, objectArray).stream().filter(object -> !ExpressionFormatFunctions.isEmptyValue(script, object)).count();
    }

    public static String concat(Script script, Object object, Object object2, Object ... objectArray) {
        return ExpressionFormatFunctions.stream(script, object, object2, objectArray).map(Objects::toString).collect(Collectors.joining());
    }

    public static String component(Script script, Object object, Object ... objectArray) {
        return FileUtilities.replacePathSeparators(ExpressionFormatFunctions.concat(script, object, null, objectArray), "");
    }

    public static double abs(Script script, Number number) {
        return Math.abs(number.doubleValue());
    }

    public static List<Object> milliseconds(Script script, Object object, Object ... objectArray) {
        long l = System.currentTimeMillis();
        List<Object> list = ExpressionFormatFunctions.allOf(script, object, null, objectArray);
        list.add(System.currentTimeMillis() - l);
        return list;
    }

    private static Stream<Object> stream(Script script, Object object2, Object object3, Object ... objectArray) {
        return Stream.concat(Stream.of(object2, object3), Stream.of(objectArray)).map(object -> ExpressionFormatFunctions.call(script, object)).filter(Objects::nonNull);
    }

    public static String quote(Script script, Object object, Object ... objectArray) {
        return Platform.isWindows() ? ExpressionFormatFunctions.quotePowerShell(script, object, objectArray) : ExpressionFormatFunctions.quoteBash(script, object, objectArray);
    }

    public static String quoteBash(Script script, Object object2, Object ... objectArray) {
        return ExpressionFormatFunctions.stream(script, object2, null, objectArray).map(object -> ExpressionFormatFunctions.argument(script, object)).map(string -> "'" + string.replace("'", "'\"'\"'") + "'").collect(Collectors.joining(" "));
    }

    public static String quotePowerShell(Script script, Object object2, Object ... objectArray) {
        return ExpressionFormatFunctions.stream(script, object2, null, objectArray).map(object -> ExpressionFormatFunctions.argument(script, object)).map(string -> "@'\n" + string + "\n'@").collect(Collectors.joining(" "));
    }

    private static String argument(Script script, Object object) {
        if (object instanceof Map || object instanceof Iterable) {
            return ExpressionFormatFunctions.toJson(script, object);
        }
        return Objects.toString(object, "");
    }

    public static String toJson(final Script script, Object object) {
        JsonGenerator.Options options = new JsonGenerator.Options();
        options.disableUnicodeEscaping();
        options.excludeNulls();
        options.addConverter(new JsonGenerator.Converter(){

            public boolean handles(Class<?> clazz) {
                return Stream.of(Map.class, Iterable.class, CharSequence.class, Number.class, Boolean.class).noneMatch(clazz2 -> clazz2.isAssignableFrom(clazz));
            }

            public Object convert(Object object, String string) {
                return ExpressionFormatFunctions.argument(script, ExpressionFormatFunctions.call(script, object));
            }
        });
        return options.build().toJson(object);
    }

    public static Map<Object, Object> csv(Script script, Object object) throws Exception {
        return ExpressionFormatFunctions.getDataResource(script, object).csv();
    }

    public static List<String> lines(Script script, Object object) throws Exception {
        return ExpressionFormatFunctions.getDataResource(script, object).lines();
    }

    public static Object xml(Script script, Object object) throws Exception {
        return ExpressionFormatFunctions.getDataResource(script, object).xml();
    }

    public static Object json(Script script, Object object) throws Exception {
        return ExpressionFormatFunctions.getDataResource(script, object).json();
    }

    public static Object json(Script script, Map<String, String> map, String string, Object object) throws Exception {
        return new DataResource.Post(new URI(string), ExpressionFormatFunctions.toJson(script, object), "application/json", map).json();
    }

    public static Object html(Script script, Object object) throws Exception {
        return ExpressionFormatFunctions.getDataResource(script, object).html();
    }

    public static String text(Script script, Object object) throws Exception {
        return ExpressionFormatFunctions.getDataResource(script, object).text();
    }

    public static Object include(Script script, Object object) throws Exception {
        DataResource dataResource = ExpressionFormatFunctions.getDataResource(script, object);
        SimpleBindings simpleBindings = new SimpleBindings();
        simpleBindings.put("__file__", dataResource.getResource());
        return ExpressionEngine.getExpressionEngine().evaluate(dataResource.text(), simpleBindings, script);
    }

    private static DataResource getDataResource(Script script, Object object) throws Exception {
        String string;
        String string2 = string = object == null ? null : object.toString();
        if (string == null || string.isEmpty()) {
            throw new InvalidInputException("Please specify a local file path or remote HTTP URL");
        }
        File file = new File(string);
        if (file.isAbsolute()) {
            if (file.getParent() == null) {
                throw new InvalidInputException("Bad file path: " + file);
            }
            return DataResource.local(file);
        }
        if (string.startsWith("https://") || string.startsWith("http://")) {
            return DataResource.remote(new URI(string));
        }
        Object object2 = null;
        try {
            object2 = script.getProperty("__file__");
        }
        catch (Exception exception) {
            // empty catch block
        }
        if (object2 instanceof File) {
            File file2 = new File(((File)object2).getParentFile(), string);
            return DataResource.local(file2);
        }
        if (object2 instanceof URI) {
            URI uRI = ((URI)object2).resolve("..").resolve(string);
            return DataResource.remote(uRI);
        }
        File file3 = ApplicationFolder.UserHome.resolve(string);
        return DataResource.local(file3);
    }

    public static String OpenAI(Script script, Map<String, Object> map2) throws Exception {
        if (!Stream.of("system", "user", "url", "model", "key").allMatch(map2::containsKey)) {
            throw new InvalidInputException("Usage: OpenAI(system: '...', user: '...', url: '...', model: '...', key: '...')");
        }
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>(2);
        linkedHashMap.put("model", map2.get("model"));
        linkedHashMap.put("messages", Stream.of("system", "user").map(string -> {
            LinkedHashMap<String, String> messageMap = new LinkedHashMap<String, String>(2);
            messageMap.put("role", (String)string);
            messageMap.put("content", RegularExpressions.LINEBREAK.splitAsStream(map2.get(string).toString().trim()).map(String::trim).collect(Collectors.joining("\n")));
            return messageMap;
        }).collect(Collectors.toList()));
        LinkedHashMap<String, String> linkedHashMap2 = new LinkedHashMap<String, String>(1);
        linkedHashMap2.put("Authorization", "Bearer " + map2.get("key"));
        Object object = ExpressionFormatFunctions.json(script, linkedHashMap2, map2.get("url") + "/chat/completions", linkedHashMap);
        return JsonUtilities.streamJsonObjects(object, "choices").map(map -> JsonUtilities.getMap(map, "message")).map(map -> JsonUtilities.getString(map, "content")).findFirst().orElse(null);
    }
}

