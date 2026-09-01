package net.filemaid.format;

import javax.script.Bindings;
import javax.script.ScriptException;
import net.filemaid.format.AssociativeScriptObject;
import net.filemaid.format.DynamicBindings;
import net.filemaid.format.ExpressionBindings;
import net.filemaid.format.ExpressionFormat;
import net.filemaid.format.StringBinding;
import net.filemaid.similarity.Normalization;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;

public class ExpressionFileFormat
extends ExpressionFormat {
    public ExpressionFileFormat(String string) throws ScriptException {
        super(string);
    }

    @Override
    public Bindings getBindings(Object object) {
        return new ExpressionBindings(object){

            @Override
            public Object get(Object object) {
                return ExpressionFileFormat.this.normalizeBindingValue(super.get(object));
            }
        };
    }

    @Override
    protected Object normalizeBindingValue(Object object) {
        if (object instanceof StringBinding) {
            return object;
        }
        if (object instanceof CharSequence) {
            return FileUtilities.replacePathSeparators(object.toString(), " ");
        }
        if (object instanceof DynamicBindings) {
            DynamicBindings dynamicBindings = (DynamicBindings)((Object)object);
            return dynamicBindings.normalize(this::normalizeBindingValue);
        }
        if (object instanceof AssociativeScriptObject) {
            AssociativeScriptObject associativeScriptObject = (AssociativeScriptObject)object;
            return associativeScriptObject.normalize(this::normalizeBindingValue);
        }
        return object;
    }

    @Override
    protected String normalizeResult(CharSequence charSequence) {
        return FileUtilities.normalizePathSeparators(Normalization.replaceSpace(RegularExpressions.NEWLINE.matcher(charSequence).replaceAll(""), " ").trim());
    }
}

