package net.filemaid;

import java.io.IOException;
import java.util.List;

public class ExecuteException
extends IOException {
    private int exitCode;

    public ExecuteException(String string, int n) {
        super(string);
        this.exitCode = n;
    }

    public ExecuteException(List<String> list, int n) {
        this(list + " failed (" + n + ")", n);
    }

    public int getExitCode() {
        return this.exitCode;
    }
}

