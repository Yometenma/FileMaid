package net.filemaid.cli;

public class CmdlineException
extends RuntimeException {
    public CmdlineException(String string) {
        super(string);
    }

    public CmdlineException(String string, Object object) {
        super(string + ": " + object);
    }

    public CmdlineException(String string, Object object, Throwable throwable) {
        super(string + ": " + object, throwable);
    }
}

