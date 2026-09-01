package net.filemaid.format;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import net.filemaid.Logging;

public class SuppressedThrowables
extends RuntimeException {
    private Throwable[] causes;

    public SuppressedThrowables(String string, Collection<Throwable> collection) {
        this(string, collection.toArray(new Throwable[0]));
    }

    public SuppressedThrowables(String string, Throwable ... throwableArray) {
        super(SuppressedThrowables.getMessage(string, throwableArray), throwableArray.length > 0 ? throwableArray[throwableArray.length - 1] : null);
        this.causes = throwableArray;
    }

    public Throwable[] getCauses() {
        return (Throwable[])this.causes.clone();
    }

    private static String getMessage(String string, Throwable ... throwableArray) {
        if (throwableArray.length == 0) {
            return string;
        }
        if (string == null || string.isEmpty()) {
            return SuppressedThrowables.getMessage(throwableArray);
        }
        return string + ": " + SuppressedThrowables.getMessage(throwableArray);
    }

    private static String getMessage(Throwable ... throwableArray) {
        return Arrays.stream(throwableArray).map(throwable -> Logging.getRootCauseMessage(throwable)).distinct().collect(Collectors.joining(" | "));
    }
}

