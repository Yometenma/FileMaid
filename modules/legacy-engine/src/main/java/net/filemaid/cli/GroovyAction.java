package net.filemaid.cli;

import groovy.lang.Closure;
import java.io.File;
import net.filemaid.InvalidInputException;
import net.filemaid.RenameAction;
import net.filemaid.cli.ConflictAction;
import net.filemaid.cli.ScriptShell;
import net.filemaid.util.FileUtilities;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;

public class GroovyAction
implements RenameAction,
ConflictAction {
    private final String name;
    private final String code;
    private transient Closure<?> closure;

    public GroovyAction(String string, String string2) {
        this.name = string;
        this.code = string2;
        this.closure = null;
    }

    public GroovyAction(String string, String string2, Closure<?> closure) {
        this.name = string;
        this.code = string2;
        this.closure = closure;
    }

    private synchronized Closure<?> compile() {
        if (this.closure == null) {
            try {
                this.closure = new ScriptShell().callable(this.code);
            }
            catch (Throwable throwable) {
                throw new InvalidInputException("Invalid Groovy action", throwable);
            }
        }
        return this.closure;
    }

    @Override
    public File rename(File file, File file2) throws Exception {
        return this.apply(file, file2);
    }

    @Override
    public File conflict(File file, File file2) throws Exception {
        File file3 = this.apply(file, file2);
        if (file3 != null && file3.getParentFile() != null) {
            FileUtilities.createFolders(file3.getParentFile());
        }
        return file3;
    }

    public File apply(File file, File file2) throws Exception {
        Object object = this.compile().call(new Object[]{file, file2});
        if (object == null) {
            return null;
        }
        if (object instanceof File) {
            return FileUtilities.resolveSibling(file2, (File)object);
        }
        if (object instanceof CharSequence) {
            return FileUtilities.resolveSibling(file2, new File(object.toString()));
        }
        return (File)DefaultTypeTransformation.castToType((Object)object, File.class);
    }

    public String toString() {
        return this.name;
    }

    public static GroovyAction wrap(Closure<?> closure) {
        return new GroovyAction("CLOSURE", null, closure);
    }
}

