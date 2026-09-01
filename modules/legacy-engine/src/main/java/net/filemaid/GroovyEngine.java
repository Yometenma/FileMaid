package net.filemaid;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.MissingPropertyException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import net.filemaid.ApplicationFolder;
import net.filemaid.Cache;
import net.filemaid.DiskStore;
import net.filemaid.InvalidInputException;
import net.filemaid.MemoryCache;
import net.filemaid.hash.VerificationUtilities;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import org.codehaus.groovy.control.BytecodeProcessor;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.jsr223.GroovyCompiledScript;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

public class GroovyEngine
extends GroovyScriptEngineImpl {
    private static final String GROOVY_SCRIPT_CODE_BASE = "/groovy/script";
    private static final String GROOVY_SCRIPT_CLASS_NAME = "__script_";
    private static final int GROOVY_SCRIPT_CLASS_NAME_LENGTH = "__script_".length() + 32;
    private final MemoryCache<String, CompiledScript> cache = MemoryCache.forObject();

    public GroovyEngine(CompiledScriptClassLoader compiledScriptClassLoader) {
        super((GroovyClassLoader)compiledScriptClassLoader);
    }

    public Object eval(String string, ScriptContext scriptContext) throws ScriptException {
        return this.compile(string).eval(scriptContext);
    }

    public CompiledScript compile(String string) throws ScriptException {
        if (Variable.isIdentifier(string)) {
            return new Variable((ScriptEngine)((Object)this), string);
        }
        String string2 = this.getScriptName(string);
        CompiledScript compiledScript = this.cache.getIfPresent(string2);
        if (compiledScript == null) {
            try {
                compiledScript = new GroovyCompiledScript((GroovyScriptEngineImpl)this, this.getClassLoader().parseClass(new GroovyCodeSource(string, string2, GROOVY_SCRIPT_CODE_BASE), false));
                this.cache.put(string2, compiledScript);
            }
            catch (CompilationFailedException compilationFailedException) {
                throw new ScriptException((Exception)((Object)compilationFailedException));
            }
        }
        return compiledScript;
    }

    public String getScriptName(String string) {
        return GROOVY_SCRIPT_CLASS_NAME + VerificationUtilities.md5(string.getBytes(StandardCharsets.UTF_8));
    }

    public static GroovyEngine newCachedCompiledScriptEngine(CompilerConfiguration compilerConfiguration, Cache cache) {
        return new GroovyEngine(new CompiledScriptClassLoader(Thread.currentThread().getContextClassLoader(), compilerConfiguration, cache));
    }

    public static <T> T asType(Object object, Class<T> clazz) throws ScriptException {
        return (T)DefaultTypeTransformation.castToType((Object)object, clazz);
    }

    public static String sanitizeErrorMessage(Throwable throwable) {
        String string = throwable.getMessage();
        string = string.replaceAll("(?:[a-z]+[.])+(\\w+)Exception", "$1");
        string = string.replaceAll("__script_\\w+", "Script");
        return string;
    }

    public static <T extends Throwable> T sanitizeStackTrace(T t) {
        t.setStackTrace((StackTraceElement[])Stream.of(t.getStackTrace()).map(stackTraceElement -> {
            if (stackTraceElement.getClassName().startsWith(GROOVY_SCRIPT_CLASS_NAME)) {
                return new StackTraceElement("Script", stackTraceElement.getMethodName(), "Script", stackTraceElement.getLineNumber());
            }
            return stackTraceElement;
        }).toArray(StackTraceElement[]::new));
        StackTraceUtils.sanitize(t);
        if (t.getCause() != null) {
            GroovyEngine.sanitizeStackTrace(t.getCause());
        }
        return t;
    }

    public static boolean isGroovyFile(String string) {
        return string.endsWith(".groovy") && !string.startsWith("@") && !string.contains("\n");
    }

    public static String resolveExternalScript(File file) throws IOException, ScriptException {
        return GroovyEngine.resolveScript(FileUtilities.readTextFile(file), file, 0);
    }

    public static String resolveScript(String string) throws ScriptException {
        return GroovyEngine.resolveScript(string, null, 0);
    }

    private static String resolveScript(String string, File file, int n) throws ScriptException {
        if (!string.contains("@") || !string.contains(".groovy")) {
            return string;
        }
        String[] charSequenceArray = RegularExpressions.LINEBREAK.split(string);
        for (int i = 0; i < charSequenceArray.length; ++i) {
            String string2 = charSequenceArray[i];
            if (!string2.startsWith("@") || !string2.endsWith(".groovy")) continue;
            if (n > 64) {
                throw new ScriptException("Nested Recursion: " + string2 + " in " + file);
            }
            File file2 = new File(string2.substring(1));
            if (!file2.isAbsolute()) {
                file2 = file == null ? ApplicationFolder.UserHome.resolve(file2.getPath()) : FileUtilities.resolveSibling(file, file2);
            }
            if (!file2.exists()) {
                throw new ScriptException("@file does not exist: " + file2);
            }
            try {
                charSequenceArray[i] = GroovyEngine.resolveScript(FileUtilities.readTextFile(file2), file2, n + 1);
                continue;
            }
            catch (IOException iOException) {
                throw new ScriptException(iOException);
            }
        }
        return String.join((CharSequence)"\n", charSequenceArray);
    }

    public static class Variable
    extends CompiledScript {
        private final ScriptEngine engine;
        private final String name;
        public static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
        public static final Pattern KEYWORD = Pattern.compile("this|null|true|false");

        public Variable(ScriptEngine scriptEngine, String string) {
            this.engine = scriptEngine;
            this.name = string;
        }

        @Override
        public Object eval(ScriptContext scriptContext) throws ScriptException {
            try {
                Object object = scriptContext.getAttribute(this.name);
                if (object == null) {
                    throw new MissingPropertyException(this.name, this.name, CompiledScript.class);
                }
                return object;
            }
            catch (Exception exception) {
                throw new ScriptException(exception);
            }
        }

        @Override
        public ScriptEngine getEngine() {
            return this.engine;
        }

        public static boolean isIdentifier(String string) {
            return IDENTIFIER.matcher(string).matches() && !KEYWORD.matcher(string).matches();
        }
    }

    public static class CompiledScriptClassLoader
    extends GroovyClassLoader
    implements BytecodeProcessor {
        private final Deque<Map.Entry<String, byte[]>> compilationUnit = new ArrayDeque<Map.Entry<String, byte[]>>();
        private final Cache.TypedCache<byte[]> compilerCache;

        public CompiledScriptClassLoader(ClassLoader classLoader, CompilerConfiguration compilerConfiguration, Cache cache) {
            super(classLoader, compilerConfiguration);
            this.compilerCache = cache.bytes();
            compilerConfiguration.setBytecodePostprocessor((BytecodeProcessor)this);
        }

        public byte[] processBytecode(String string, byte[] byArray) {
            if (!this.isScriptClass(string)) {
                throw new InvalidInputException("Illegal script class: " + string);
            }
            Deque<Map.Entry<String, byte[]>> deque = this.compilationUnit;
            synchronized (deque) {
                this.compilationUnit.add(new AbstractMap.SimpleImmutableEntry<String, byte[]>(string, byArray));
            }
            return byArray;
        }

        private void store() {
            DiskStore diskStore = Cache.DISK_STORE;
            synchronized (diskStore) {
                if (this.compilerCache.isAlive()) {
                    this.compilationUnit.descendingIterator().forEachRemaining(entry -> this.compilerCache.put(entry.getKey(), entry.getValue()));
                    this.compilationUnit.clear();
                    this.compilerCache.flush();
                }
            }
        }

        private byte[] restore(String string) {
            DiskStore diskStore = Cache.DISK_STORE;
            synchronized (diskStore) {
                if (this.compilerCache.isAlive()) {
                    return this.compilerCache.get(string);
                }
            }
            return null;
        }

        private String status() {
            DiskStore diskStore = Cache.DISK_STORE;
            synchronized (diskStore) {
                if (this.compilerCache.isAlive()) {
                    return this.compilerCache.getKeys().stream().map(Object::toString).sorted().collect(Collectors.joining("\n", this.compilerCache + "\n", "\n"));
                }
            }
            return null;
        }

        private void reset() {
            DiskStore diskStore = Cache.DISK_STORE;
            synchronized (diskStore) {
                if (this.compilerCache.isAlive()) {
                    this.compilerCache.clear();
                }
            }
        }

        public Class parseClass(GroovyCodeSource groovyCodeSource, boolean bl) throws CompilationFailedException {
            Class clazz = this.reloadScriptClass(groovyCodeSource.getName());
            if (clazz != null) {
                return clazz;
            }
            Deque<Map.Entry<String, byte[]>> deque = this.compilationUnit;
            synchronized (deque) {
                clazz = super.parseClass(groovyCodeSource, bl);
                this.store();
            }
            return clazz;
        }

        protected Class findClass(String string) throws ClassNotFoundException {
            if (Thread.holdsLock(this.compilationUnit) || !this.isScriptClass(string)) {
                return super.findClass(string);
            }
            Class clazz = this.reloadScriptClass(string);
            if (clazz != null) {
                return clazz;
            }
            try {
                Class clazz2 = super.findClass(string);
                return clazz2;
            }
            catch (ClassNotFoundException classNotFoundException) {
                System.err.println(((Object)((Object)this)).getClass().getName() + ": " + classNotFoundException);
                throw new InconsistentCompilerCacheException("Failed to load " + string + " from cache: " + this.status(), classNotFoundException);
            }
            finally {
                this.reset();
            }
        }

        private boolean isScriptClass(String string) {
            if (Character.isLowerCase(string.charAt(0))) {
                return false;
            }
            if (string.length() == GROOVY_SCRIPT_CLASS_NAME_LENGTH && string.startsWith(GroovyEngine.GROOVY_SCRIPT_CLASS_NAME)) {
                return true;
            }
            if (string.length() >= GROOVY_SCRIPT_CLASS_NAME_LENGTH + 2 && string.startsWith(GroovyEngine.GROOVY_SCRIPT_CLASS_NAME)) {
                return '$' == string.charAt(GROOVY_SCRIPT_CLASS_NAME_LENGTH) && !Character.isLetter(string.charAt(GROOVY_SCRIPT_CLASS_NAME_LENGTH + 1));
            }
            return Character.isUpperCase(string.charAt(0)) && string.indexOf(46) < 0 && !string.endsWith("BeanInfo") && !string.endsWith("Customizer");
        }

        private Class reloadScriptClass(String string) {
            byte[] byArray = this.restore(string);
            if (byArray == null) {
                return null;
            }
            try {
                return super.defineClass(string, byArray);
            }
            catch (LinkageError linkageError) {
                return null;
            }
        }
    }

    public static class InconsistentCompilerCacheException
    extends ClassNotFoundException {
        public InconsistentCompilerCacheException(String string, Throwable throwable) {
            super(string, throwable);
        }
    }
}

