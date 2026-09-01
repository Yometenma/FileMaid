package net.filemaid.format;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import net.filemaid.GroovyEngine;
import net.filemaid.format.ExpressionBindings;
import net.filemaid.format.ExpressionEngine;

public class ExpressionFilter {
    private final String source;
    private final String expression;
    private final CompiledScript scriptlet;

    public ExpressionFilter(String string) throws ScriptException {
        this.source = string;
        this.expression = GroovyEngine.resolveScript(this.source);
        this.scriptlet = ExpressionEngine.getExpressionEngine().compileScriptlet(this.expression);
    }

    public String getSource() {
        return this.source;
    }

    public String getExpression() {
        return this.expression;
    }

    public boolean matches(Object object) throws Exception {
        return this.evaluate(new ExpressionBindings(object), Boolean.TYPE);
    }

    public <T> T apply(Object object, Class<T> clazz) throws Exception {
        return this.evaluate(new ExpressionBindings(object), clazz);
    }

    public <T> T evaluate(Bindings bindings, Class<T> clazz) throws Exception {
        SimpleScriptContext simpleScriptContext = new SimpleScriptContext();
        simpleScriptContext.setBindings(bindings, 200);
        return ExpressionEngine.evaluateScriptlet(this.scriptlet, simpleScriptContext, clazz);
    }

    public String toString() {
        return this.getSource();
    }
}

