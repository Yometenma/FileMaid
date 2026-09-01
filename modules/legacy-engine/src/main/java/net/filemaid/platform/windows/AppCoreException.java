package net.filemaid.platform.windows;

public class AppCoreException
extends IllegalStateException {
    private final int error;

    public AppCoreException(int n) {
        this.error = n;
    }

    public int getError() {
        return this.error;
    }

    @Override
    public String getMessage() {
        switch (this.error) {
            case 15700: {
                return "The process has no package identity.";
            }
            case 15701: {
                return "The package runtime information is corrupted.";
            }
            case 15702: {
                return "The package identity is corrupted.";
            }
            case 15703: {
                return "The process has no application identity.";
            }
        }
        return "APPMODEL_ERROR(" + this.error + ")";
    }
}

