package net.filemaid.format;

import java.io.File;
import java.io.FileFilter;
import java.util.function.Function;
import javax.script.ScriptException;
import net.filemaid.Logging;
import net.filemaid.format.BindingException;
import net.filemaid.format.ExpressionFilter;
import net.filemaid.format.MediaBindingBean;

public class ExpressionFileFilter
extends ExpressionFilter
implements FileFilter {
    private final Function<File, Object> match;

    public ExpressionFileFilter(String string) throws ScriptException {
        this(string, file -> file);
    }

    public ExpressionFileFilter(String string, Function<File, Object> function) throws ScriptException {
        super(string);
        this.match = function;
    }

    @Override
    public boolean accept(File file) {
        try {
            return this.matches(new MediaBindingBean(this.match.apply(file), file));
        }
        catch (BindingException bindingException) {
            if (bindingException.has(BindingException.Flag.UNDEFINED)) {
                return false;
            }
            Logging.debug.warning(Logging.cause(this, file, bindingException));
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(this, file, exception));
        }
        return false;
    }
}

