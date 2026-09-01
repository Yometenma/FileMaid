package net.filemaid.cli;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class ApplyOptionsHandler
extends OptionHandler<String> {
    public ApplyOptionsHandler(CmdLineParser cmdLineParser, OptionDef optionDef, Setter<String> setter) {
        super(cmdLineParser, optionDef, setter);
    }

    @Override
    public int parseArguments(Parameters parameters) throws CmdLineException {
        for (int i = 0; i < parameters.size(); ++i) {
            String string = parameters.getParameter(i);
            if (string.startsWith("-")) {
                return i;
            }
            this.setter.addValue(string);
        }
        return parameters.size();
    }

    @Override
    public String getDefaultMetaVariable() {
        return "VALUE ...";
    }
}

