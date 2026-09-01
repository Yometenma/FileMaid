package net.filemaid.postprocess;

import groovy.transform.ThreadInterrupt;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.GroovyEngine;
import net.filemaid.Logging;
import net.filemaid.postprocess.ScriptBaseClass;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.syntax.SyntaxException;

public class ScriptEngine {
    private final GroovyEngine engine;
    private static final ScriptEngine INSTANCE = ScriptEngine.createScriptEngine();

    public ScriptEngine(GroovyEngine groovyEngine) {
        this.engine = groovyEngine;
    }

    public CompiledScript compile(String string) throws ScriptException {
        try {
            return this.engine.compile(string);
        }
        catch (ScriptException scriptException) {
            SyntaxException syntaxException;
            MultipleCompilationErrorsException multipleCompilationErrorsException = Logging.findCause(scriptException, MultipleCompilationErrorsException.class);
            if (multipleCompilationErrorsException != null && (syntaxException = multipleCompilationErrorsException.getErrorCollector().getSyntaxError(0)) != null) {
                throw new ScriptException("Syntax Error: " + syntaxException.getOriginalMessage());
            }
            throw scriptException;
        }
    }

    private static ScriptEngine createScriptEngine() {
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.setScriptBaseClass(ScriptBaseClass.class.getName());
        compilerConfiguration.addCompilationCustomizers(new CompilationCustomizer[]{new ASTTransformationCustomizer(ThreadInterrupt.class)});
        GroovyEngine groovyEngine = GroovyEngine.newCachedCompiledScriptEngine(compilerConfiguration, Cache.getCache("postprocess_classes", CacheType.Persistent));
        return new ScriptEngine(groovyEngine);
    }

    public static ScriptEngine getScriptEngine() {
        return INSTANCE;
    }
}

