package net.filemaid;

public class LicenseError
extends Error {
    public LicenseError(String string) {
        super(string);
    }

    public LicenseError(String string, Throwable throwable) {
        super(string, throwable);
    }

    public boolean isNetworkError() {
        return this.getCause() != null;
    }
}

