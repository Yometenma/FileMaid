package net.filemaid.ui.rename;

import java.util.Collections;
import java.util.Set;
import net.filemaid.ui.rename.FormatExpressionTokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;

public class FormatExpressionTokenMakerFactory
extends TokenMakerFactory {
    public static final String SYNTAX_STYLE_GROOVY_FORMAT_EXPRESSION = "text/groovy-format-expression";

    public FormatExpressionTokenMaker getTokenMakerImpl(String string) {
        return new FormatExpressionTokenMaker();
    }

    public Set<String> keySet() {
        return Collections.singleton(SYNTAX_STYLE_GROOVY_FORMAT_EXPRESSION);
    }
}

