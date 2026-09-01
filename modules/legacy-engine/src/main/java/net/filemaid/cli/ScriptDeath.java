package net.filemaid.cli;

public class ScriptDeath
extends Throwable {
    public final int exitCode;

    public ScriptDeath(int n, String string) {
        super(string);
        this.exitCode = n;
    }

    public ScriptDeath(int n, Throwable throwable) {
        super(throwable);
        this.exitCode = n;
    }

    public int getExitCode() {
        return this.exitCode;
    }
}

