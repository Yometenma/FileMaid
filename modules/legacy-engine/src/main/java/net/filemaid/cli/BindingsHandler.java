package net.filemaid.cli;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.script.ScriptException;
import net.filemaid.GroovyEngine;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.FieldSetter;
import org.kohsuke.args4j.spi.MapOptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class BindingsHandler
extends MapOptionHandler {
    public BindingsHandler(CmdLineParser cmdLineParser, OptionDef optionDef, Setter<? super Map<?, ?>> setter) {
        super(cmdLineParser, optionDef, setter);
    }

    @Override
    public String getDefaultMetaVariable() {
        return "name=value";
    }

    @Override
    public int parseArguments(Parameters parameters) throws CmdLineException {
        int n;
        FieldSetter fieldSetter = this.setter.asFieldSetter();
        Map map = (Map)fieldSetter.getValue();
        if (map == null) {
            map = this.createNewCollection(fieldSetter.getType());
            fieldSetter.addValue(map);
        }
        for (n = 0; n < parameters.size(); ++n) {
            if (parameters.getParameter(n).startsWith("-")) {
                return n;
            }
            String[] stringArray = RegularExpressions.EQUALS.split(parameters.getParameter(n), 2);
            if (stringArray.length < 2) {
                return n;
            }
            String string = this.getName(stringArray[0]);
            String string2 = this.getValue(stringArray[1]);
            this.addToMap(map, string, string2);
        }
        return n;
    }

    public String getName(String string) throws CmdLineException {
        if (!this.isIdentifier(string)) {
            throw new CmdLineException(this.owner, "\"" + string + "\" is not a valid identifier", null);
        }
        return string;
    }

    public String getValue(String string) throws CmdLineException {
        if (string.startsWith("@")) {
            File file = new File(string.substring(1));
            try {
                if (GroovyEngine.isGroovyFile(file.getPath())) {
                    return GroovyEngine.resolveExternalScript(file.getAbsoluteFile());
                }
                if (file.isAbsolute() || file.isFile()) {
                    return FileUtilities.readTextFile(file.getAbsoluteFile());
                }
            }
            catch (IOException iOException) {
                throw new CmdLineException(this.owner, "Bad @file path: " + iOException.getMessage(), iOException);
            }
            catch (ScriptException scriptException) {
                throw new CmdLineException(this.owner, "Bad expression: " + scriptException.getMessage(), scriptException);
            }
        }
        return string;
    }

    public boolean isIdentifier(String string) {
        if (string == null || string.isEmpty()) {
            return false;
        }
        return Character.isUnicodeIdentifierStart(string.codePointAt(0));
    }

    @Override
    protected Map createNewCollection(Class<? extends Map> clazz) {
        return new LinkedHashMap();
    }
}

