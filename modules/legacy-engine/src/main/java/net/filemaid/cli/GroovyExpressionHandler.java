package net.filemaid.cli;

import java.io.File;
import java.io.IOException;
import javax.script.ScriptException;
import net.filemaid.GroovyEngine;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class GroovyExpressionHandler
extends OptionHandler {
    public GroovyExpressionHandler(CmdLineParser cmdLineParser, OptionDef optionDef, Setter<String> setter) {
        super(cmdLineParser, optionDef, setter);
    }

    @Override
    public int parseArguments(Parameters parameters) throws CmdLineException {
        this.setter.addValue(this.getStringValue(parameters.getParameter(0)));
        return 1;
    }

    private String getStringValue(String string) throws CmdLineException {
        try {
            if (GroovyEngine.isGroovyFile(string)) {
                return GroovyEngine.resolveExternalScript(new File(string).getAbsoluteFile());
            }
            return GroovyEngine.resolveScript(string);
        }
        catch (IOException iOException) {
            throw new CmdLineException(this.owner, "Bad file path: " + iOException.getMessage(), iOException);
        }
        catch (ScriptException scriptException) {
            throw new CmdLineException(this.owner, "Bad expression: " + scriptException.getMessage(), scriptException);
        }
    }

    @Override
    public String getDefaultMetaVariable() {
        return "{expression}";
    }
}

