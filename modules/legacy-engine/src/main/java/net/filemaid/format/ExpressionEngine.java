package net.filemaid.format;

import groovy.lang.Closure;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import groovy.transform.ThreadInterrupt;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.GroovyEngine;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.format.BindingException;
import net.filemaid.format.ExpressionException;
import net.filemaid.util.ByteBufferInputStream;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.syntax.SyntaxException;

public final class ExpressionEngine {
    private final GroovyEngine engine;
    private static final ExpressionEngine INSTANCE;

    public ExpressionEngine(GroovyEngine groovyEngine) {
        this.engine = groovyEngine;
    }

    public CompiledScript compileScriptlet(String string) throws ScriptException {
        try {
            return this.engine.compile(string.trim());
        }
        catch (ScriptException scriptException) {
            SyntaxException syntaxException;
            MultipleCompilationErrorsException multipleCompilationErrorsException = Logging.findCause(scriptException, MultipleCompilationErrorsException.class);
            if (multipleCompilationErrorsException != null && (syntaxException = multipleCompilationErrorsException.getErrorCollector().getSyntaxError(0)) != null) {
                throw new ExpressionException("Syntax Error: " + syntaxException.getOriginalMessage(), string, scriptException);
            }
            throw scriptException;
        }
    }

    public Object evaluate(String string, Bindings bindings, final Script script) throws ScriptException {
        return this.compileScriptlet(string).eval(new SimpleBindings(bindings){

            @Override
            public boolean containsKey(Object object) {
                return true;
            }

            @Override
            public Object get(Object object) {
                if (super.containsKey(object)) {
                    return super.get(object);
                }
                return script.getProperty(object.toString());
            }
        });
    }

    public static Object evaluateScriptlet(CompiledScript compiledScript, ScriptContext scriptContext) throws Exception {
        try {
            Object object = compiledScript.eval(scriptContext);
            while (object instanceof Closure) {
                object = ((Closure)object).call();
            }
            return object;
        }
        catch (ScriptException scriptException) {
            MissingPropertyException missingPropertyException = Logging.findCause(scriptException, MissingPropertyException.class);
            if (missingPropertyException != null) {
                throw new BindingException((Object)missingPropertyException.getProperty(), (Object)"No such property", new BindingException.Flag[0]);
            }
            GroovyRuntimeException groovyRuntimeException = Logging.findCause(scriptException, GroovyRuntimeException.class);
            if (groovyRuntimeException != null) {
                throw new ExpressionException(groovyRuntimeException.getMessage(), null, scriptException);
            }
            Exception exception = Logging.findCause(scriptException.getCause(), Exception.class);
            if (exception != null) {
                throw exception;
            }
            throw scriptException;
        }
    }

    public static <T> T evaluateScriptlet(CompiledScript compiledScript, ScriptContext scriptContext, Class<T> clazz) throws Exception {
        Object object = ExpressionEngine.evaluateScriptlet(compiledScript, scriptContext);
        return GroovyEngine.asType(object, clazz);
    }

    public static ExpressionEngine getExpressionEngine() {
        return INSTANCE;
    }

    static {
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.addCompilationCustomizers(new CompilationCustomizer[]{new ASTTransformationCustomizer(ThreadInterrupt.class)});
        GroovyEngine groovyEngine = GroovyEngine.newCachedCompiledScriptEngine(compilerConfiguration, Cache.getCache("expression_classes", CacheType.Persistent));
        try {
            groovyEngine.eval(new InputStreamReader((InputStream)new ByteBufferInputStream(ResourceManager.getResource("script.palette")), StandardCharsets.UTF_8));
        }
        catch (Throwable throwable) {
            Logging.debug.finest(Logging.cause(throwable));
        }
        finally {
            Logging.flushLog();
        }
        INSTANCE = new ExpressionEngine(groovyEngine);
    }
}

