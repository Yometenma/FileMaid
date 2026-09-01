package net.filemaid;

public class InvalidInputException
extends IllegalStateException {
    public InvalidInputException(String string) {
        super(string);
    }

    public InvalidInputException(String string, Throwable throwable) {
        super(string, throwable);
    }
}

