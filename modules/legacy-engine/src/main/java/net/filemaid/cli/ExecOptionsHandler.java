package net.filemaid.cli;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class ExecOptionsHandler
extends OptionHandler<String> {
    public ExecOptionsHandler(CmdLineParser cmdLineParser, OptionDef optionDef, Setter<String> setter) {
        super(cmdLineParser, optionDef, setter);
    }

    @Override
    public int parseArguments(Parameters parameters) throws CmdLineException {
        for (int i = 0; i < parameters.size(); ++i) {
            String string = parameters.getParameter(i);
            if (";".equals(string)) {
                return i + 1;
            }
            if ("+".equals(string) || "*".equals(string) || "+*".equals(string)) {
                this.setter.addValue(string);
                return i + 1;
            }
            this.setter.addValue(string);
        }
        return parameters.size();
    }

    @Override
    public String getDefaultMetaVariable() {
        return "utility [argument ...] {} +";
    }
}

