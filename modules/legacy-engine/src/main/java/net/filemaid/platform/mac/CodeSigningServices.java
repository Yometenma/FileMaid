package net.filemaid.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import net.filemaid.platform.mac.SecTaskRef;

public interface CodeSigningServices
extends Library {
    public static final CodeSigningServices INSTANCE = (CodeSigningServices)Native.load(null, CodeSigningServices.class);

    public SecTaskRef SecTaskCreateFromSelf(CoreFoundation.CFAllocatorRef var1);

    public CoreFoundation.CFStringRef SecTaskCopySigningIdentifier(SecTaskRef var1, Pointer var2);

    public CoreFoundation.CFTypeRef SecTaskCopyValueForEntitlement(SecTaskRef var1, CoreFoundation.CFStringRef var2, Pointer var3);
}

