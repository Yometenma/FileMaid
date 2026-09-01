package net.filemaid.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.NoSuchElementException;
import net.filemaid.EscapeCode;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.PGP;
import net.filemaid.util.RegularExpressions;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class LicenseOptionHandler
extends OptionHandler<String> {
    public LicenseOptionHandler(CmdLineParser cmdLineParser, OptionDef optionDef, Setter<String> setter) {
        super(cmdLineParser, optionDef, setter);
    }

    @Override
    public int parseArguments(Parameters parameters) throws CmdLineException {
        if (parameters.size() == 0) {
            try {
                return this.parseStandardInput();
            }
            catch (NoSuchElementException noSuchElementException) {
                throw new CmdLineException(this.owner, "Standard Input is not a license key: " + noSuchElementException.getMessage(), noSuchElementException);
            }
        }
        String string = parameters.getParameter(0).trim();
        if (RegularExpressions.NEWLINE.matcher(string).find()) {
            try {
                return this.parseText(string);
            }
            catch (NoSuchElementException noSuchElementException) {
                throw new CmdLineException(this.owner, "Option Value is not a license key: " + noSuchElementException.getMessage(), noSuchElementException);
            }
        }
        File file = new File(string).getAbsoluteFile();
        try {
            return this.parseFile(file);
        }
        catch (NoSuchFileException noSuchFileException) {
            throw new CmdLineException(this.owner, "File does not exist: " + file, noSuchFileException);
        }
        catch (IOException iOException) {
            throw new CmdLineException(this.owner, "File is not readable: " + file, iOException);
        }
        catch (NoSuchElementException noSuchElementException) {
            throw new CmdLineException(this.owner, "File is not a license key: " + noSuchElementException.getMessage(), noSuchElementException);
        }
    }

    private int parseFile(File file) throws CmdLineException, IOException, NoSuchElementException {
        this.setter.addValue(PGP.findClearSignMessage(FileUtilities.readTextFile(file)));
        return 1;
    }

    private int parseText(String string) throws CmdLineException, NoSuchElementException {
        this.setter.addValue(PGP.findClearSignMessage(string));
        return 1;
    }

    private int parseStandardInput() throws CmdLineException, NoSuchElementException {
        if (System.console() == null) {
            this.setter.addValue(PGP.findClearSignMessage(new InputStreamReader(System.in, StandardCharsets.UTF_8)));
            return 0;
        }
        System.console().writer().println(EscapeCode.color(EscapeCode.ORANGE_ONE, "* Please copy and paste your license key..."));
        System.console().writer().flush();
        this.setter.addValue(PGP.findClearSignMessage(System.console().reader()));
        System.console().writer().println(EscapeCode.color(EscapeCode.ORANGE_ONE, "* Please wait..."));
        System.console().writer().flush();
        return 0;
    }

    @Override
    public String getDefaultMetaVariable() {
        return "*.psm";
    }
}

