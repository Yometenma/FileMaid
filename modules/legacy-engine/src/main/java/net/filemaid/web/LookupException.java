package net.filemaid.web;

public class LookupException
extends IllegalArgumentException {
    public LookupException(Object object, Object object2) {
        super("Invalid Lookup: " + object + " [" + object2 + "]");
    }

    public LookupException(String string) {
        super(string);
    }
}

