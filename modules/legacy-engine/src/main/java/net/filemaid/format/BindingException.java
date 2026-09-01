package net.filemaid.format;

import java.util.Arrays;

public class BindingException
extends RuntimeException {
    private final Flag[] flags;

    public BindingException(String string, Throwable throwable, Flag ... flagArray) {
        super(string, throwable);
        this.flags = flagArray;
    }

    public BindingException(Object object, Object object2, Flag ... flagArray) {
        this(object, object2, (Throwable)null, flagArray);
    }

    public BindingException(Object object, Object object2, Throwable throwable, Flag ... flagArray) {
        this("Binding \"" + object + "\": " + object2, throwable, flagArray);
    }

    public boolean has(Flag flag) {
        return Arrays.stream(this.flags).anyMatch(flag2 -> flag2 == flag);
    }

    public static enum Flag {
        UNDEFINED,
        SAMPLE_FILE_NOT_SET;

    }
}

