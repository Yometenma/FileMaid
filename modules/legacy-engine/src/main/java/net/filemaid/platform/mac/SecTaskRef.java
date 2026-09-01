package net.filemaid.platform.mac;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;

public class SecTaskRef
extends CoreFoundation.CFTypeRef {
    public SecTaskRef() {
    }

    public SecTaskRef(Pointer pointer) {
        super(pointer);
    }
}

