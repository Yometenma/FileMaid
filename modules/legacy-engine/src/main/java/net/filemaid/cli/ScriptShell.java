package net.filemaid.cli;

import groovy.lang.Closure;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;
import javax.script.Bindings;
import javax.script.ScriptException;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.GroovyEngine;
import net.filemaid.cli.CmdlineInterface;
import net.filemaid.cli.GroovyAction;
import net.filemaid.cli.GroovyComparator;
import net.filemaid.cli.ScriptProvider;
import net.filemaid.format.ExpressionFilter;
import net.filemaid.format.ExpressionFormat;
import net.filemaid.format.ExpressionMapper;
import net.filemaid.format.QueryExpression;
import net.filemaid.util.RegularExpressions;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

public class ScriptShell {
    public static final String ARGV_BINDING_NAME = "args";
    public static final String SHELL_BINDING_NAME = "__shell";
    public static final String SHELL_CLI_BINDING_NAME = "__cli";
    public static final String SHELL_ARGS_BINDING_NAME = "__args";
    public static final String SHELL_DEFS_BINDING_NAME = "__defs";
    private final GroovyEngine engine;
    private final ScriptProvider resolver;

    private static GroovyEngine createScriptEngine(Cache cache, CompilationCustomizer ... compilationCustomizerArray) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle(ScriptShell.class.getName());
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.setScriptBaseClass(resourceBundle.getString("scriptBaseClass"));
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStarImports(RegularExpressions.COMMA.split(resourceBundle.getString("starImport")));
        importCustomizer.addStaticStars(RegularExpressions.COMMA.split(resourceBundle.getString("starStaticImport")));
        compilerConfiguration.addCompilationCustomizers(new CompilationCustomizer[]{importCustomizer});
        for (CompilationCustomizer compilationCustomizer : compilationCustomizerArray) {
            compilerConfiguration.addCompilationCustomizers(new CompilationCustomizer[]{compilationCustomizer});
        }
        return GroovyEngine.newCachedCompiledScriptEngine(compilerConfiguration, cache);
    }

    public ScriptShell() {
        this(null, null, Collections.emptyMap());
    }

    public ScriptShell(ScriptProvider scriptProvider, CmdlineInterface cmdlineInterface, Map<String, ?> map) {
        this(scriptProvider, cmdlineInterface, map, Cache.getCache("script_classes", CacheType.Persistent), new CompilationCustomizer[0]);
    }

    public ScriptShell(ScriptProvider scriptProvider, CmdlineInterface cmdlineInterface, Map<String, ?> map, Cache cache, CompilationCustomizer ... compilationCustomizerArray) {
        this.engine = ScriptShell.createScriptEngine(cache, compilationCustomizerArray);
        this.resolver = scriptProvider;
        Bindings bindings = this.engine.createBindings();
        bindings.putAll((Map<? extends String, ? extends Object>)map);
        bindings.put(SHELL_BINDING_NAME, (Object)this);
        bindings.put(SHELL_CLI_BINDING_NAME, (Object)cmdlineInterface);
        this.engine.getContext().setBindings(bindings, 200);
    }

    public Object runScript(String string, Bindings bindings) throws Throwable {
        return this.evaluate(this.resolver.getScript(string), bindings);
    }

    public Closure<?> callable(String string) throws Throwable {
        Object object = this.evaluate(string, this.engine.createBindings());
        return GroovyEngine.asType(object, Closure.class);
    }

    public Object evaluate(String string, Bindings bindings) throws Throwable {
        try {
            return this.engine.eval(string, bindings);
        }
        catch (Throwable throwable) {
            throw this.sanitize(throwable);
        }
    }

    public Throwable sanitize(Throwable throwable) {
        while (throwable.getClass() == ScriptException.class && throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        return GroovyEngine.sanitizeStackTrace(throwable);
    }

    public GroovyComparator comparator(String string) throws Throwable {
        return GroovyComparator.wrap(this.callable(string));
    }

    public GroovyAction action(String string) throws Throwable {
        return GroovyAction.wrap(this.callable(string));
    }

    public ExpressionFormat format(String string) throws Throwable {
        return new ExpressionFormat(string);
    }

    public ExpressionFilter filter(String string) throws Throwable {
        return new ExpressionFilter(string);
    }

    public ExpressionMapper mapper(String string) throws Throwable {
        return new ExpressionMapper(string);
    }

    public QueryExpression query(String string) throws Throwable {
        return new QueryExpression(string);
    }
}

