package net.filemaid.format;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.script.ScriptException;
import net.filemaid.Logging;
import net.filemaid.format.ExpressionFormat;
import net.filemaid.format.MediaBindingBean;
import net.filemaid.similarity.Normalization;

public class QueryExpression
extends ExpressionFormat
implements Function<File, String> {
    public QueryExpression(String string) throws ScriptException {
        super(string);
    }

    @Override
    public String apply(File file) {
        try {
            return this.format(new MediaBindingBean(file, file));
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(this, file, exception));
            return null;
        }
    }

    @Override
    protected String normalizeResult(CharSequence charSequence) {
        return Normalization.replaceSpace(charSequence, " ");
    }

    public String value() {
        return this.isConstant() ? this.normalizeResult(this.getExpression()) : this.apply(null);
    }

    public Map<String, List<File>> group(Collection<File> collection) {
        if (this.isConstant()) {
            return Collections.singletonMap(this.getExpression(), new ArrayList<File>(collection));
        }
        LinkedHashMap<String, List<File>> linkedHashMap = new LinkedHashMap<String, List<File>>();
        for (File file : collection) {
            String string2 = this.apply(file);
            if (string2 == null || string2.isEmpty()) continue;
            linkedHashMap.computeIfAbsent(string2, string -> new ArrayList()).add(file);
        }
        return linkedHashMap;
    }
}

