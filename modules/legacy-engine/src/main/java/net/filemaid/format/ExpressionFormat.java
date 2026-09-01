package net.filemaid.format;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import net.filemaid.GroovyEngine;
import net.filemaid.format.ExpressionBindings;
import net.filemaid.format.ExpressionEngine;
import net.filemaid.format.SuppressedThrowables;

public class ExpressionFormat
extends Format {
    private final String source;
    private final String expression;
    private final Object[] compilation;
    private SuppressedThrowables suppressed;

    public ExpressionFormat(String string) throws ScriptException {
        this.source = string;
        this.expression = GroovyEngine.resolveScript(this.source);
        this.compilation = this.compile(this.expression);
    }

    public String getSource() {
        return this.source;
    }

    public String getExpression() {
        return this.expression;
    }

    protected Object[] compile(String string) throws ScriptException {
        ArrayList<Object> arrayList = new ArrayList<Object>();
        StringBuilder stringBuilder = new StringBuilder();
        int n = 0;
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (c == '{') {
                if (n == 0) {
                    if (stringBuilder.length() > 0) {
                        arrayList.add(stringBuilder.toString());
                        stringBuilder.setLength(0);
                    }
                } else {
                    stringBuilder.append(c);
                }
                ++n;
            } else if (c == '}') {
                if (n == 1) {
                    if (stringBuilder.length() > 0) {
                        try {
                            arrayList.add(ExpressionEngine.getExpressionEngine().compileScriptlet(stringBuilder.toString()));
                        }
                        finally {
                            stringBuilder.setLength(0);
                        }
                    }
                } else {
                    stringBuilder.append(c);
                }
                --n;
            } else {
                stringBuilder.append(c);
            }
            if (n >= 0) continue;
            throw new ScriptException("SyntaxError: unexpected token: }");
        }
        if (n != 0) {
            throw new ScriptException("SyntaxError: missing token: }");
        }
        if (stringBuilder.length() > 0) {
            arrayList.add(stringBuilder.toString());
        }
        return arrayList.toArray();
    }

    public boolean isConstant() {
        return Arrays.stream(this.compilation).noneMatch(object -> object instanceof CompiledScript);
    }

    public boolean isEmpty() {
        return this.compilation.length == 0;
    }

    public Bindings getBindings(Object object) {
        return new ExpressionBindings(object);
    }

    @Override
    public StringBuffer format(Object object, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        return stringBuffer.append(this.format(this.getBindings(object)));
    }

    public String format(Bindings bindings) {
        SimpleScriptContext simpleScriptContext = new SimpleScriptContext();
        simpleScriptContext.setBindings(bindings, 200);
        ArrayList<Throwable> arrayList = new ArrayList<Throwable>();
        StringBuilder stringBuilder = new StringBuilder();
        for (Object object : this.compilation) {
            if (object instanceof CompiledScript) {
                try {
                    Object object2 = ExpressionEngine.evaluateScriptlet((CompiledScript)object, simpleScriptContext);
                    if (object2 == null) continue;
                    stringBuilder.append(object2);
                }
                catch (Exception exception) {
                    arrayList.add(exception);
                }
                continue;
            }
            stringBuilder.append(object);
        }
        String string = this.normalizeResult(stringBuilder);
        if (string.isEmpty()) {
            throw new SuppressedThrowables("Expression yields empty value", arrayList);
        }
        this.suppressed = arrayList.isEmpty() ? null : new SuppressedThrowables("Suppressed", arrayList);
        return string;
    }

    public SuppressedThrowables suppressed() {
        return this.suppressed;
    }

    protected Object normalizeBindingValue(Object object) {
        return object;
    }

    protected String normalizeResult(CharSequence charSequence) {
        return charSequence.toString();
    }

    @Override
    public Object parseObject(String string, ParsePosition parsePosition) {
        throw new UnsupportedOperationException();
    }

    public boolean sameExpression(String string) {
        try {
            return this.getExpression().equals(GroovyEngine.resolveScript(string));
        }
        catch (Exception exception) {
            return false;
        }
    }

    public String toString() {
        return this.getSource();
    }
}

