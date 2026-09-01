package net.filemaid.format;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import net.filemaid.GroovyEngine;
import net.filemaid.format.ExpressionBindings;
import net.filemaid.format.ExpressionEngine;

public class ExpressionMapper {
    private final String expression;
    private final CompiledScript scriptlet;

    public ExpressionMapper(String string) throws ScriptException {
        this.expression = GroovyEngine.resolveScript(string);
        this.scriptlet = ExpressionEngine.getExpressionEngine().compileScriptlet(this.expression);
    }

    public String getExpression() {
        return this.expression;
    }

    public <T> T map(Object object, Class<T> clazz) throws Exception {
        return this.map(new ExpressionBindings(object), clazz);
    }

    public <T> T map(Bindings bindings, Class<T> clazz) throws Exception {
        SimpleScriptContext simpleScriptContext = new SimpleScriptContext();
        simpleScriptContext.setBindings(bindings, 200);
        return ExpressionEngine.evaluateScriptlet(this.scriptlet, simpleScriptContext, clazz);
    }
}

