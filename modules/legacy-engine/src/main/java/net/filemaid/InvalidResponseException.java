package net.filemaid;

import java.io.IOException;

public class InvalidResponseException
extends IOException {
    public InvalidResponseException(String string) {
        super(string);
    }

    public InvalidResponseException(String string, Object object) {
        super(string + "\n" + object);
    }

    public InvalidResponseException(String string, Object object, Throwable throwable) {
        super(string + ": " + throwable.getMessage() + "\n" + object, throwable);
    }
}

