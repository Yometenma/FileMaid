package net.filemaid.format;

import javax.script.ScriptException;

public class ExpressionException
extends ScriptException {
    private final String message;
    private final String expression;

    public ExpressionException(String string, String string2, ScriptException scriptException) {
        super(scriptException);
        this.message = string;
        this.expression = string2;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public String getExpression() {
        return this.expression;
    }
}

