package net.filemaid.platform.windows;

import com.sun.jna.platform.EnumUtils;

public enum PackageOrigin {
    Unknown,
    Unsigned,
    Inbox,
    Store,
    DeveloperUnsigned,
    DeveloperSigned,
    LineOfBusiness;


    public static class ByReference
    extends com.sun.jna.ptr.ByReference {
        public ByReference() {
            super(4);
            this.getPointer().setInt(0L, -1);
        }

        public ByReference(PackageOrigin packageOrigin) {
            super(4);
            this.setValue(packageOrigin);
        }

        public void setValue(PackageOrigin packageOrigin) {
            this.getPointer().setInt(0L, EnumUtils.toInteger((Enum)packageOrigin));
        }

        public PackageOrigin getValue() {
            return (PackageOrigin)EnumUtils.fromInteger((int)this.getPointer().getInt(0L), PackageOrigin.class);
        }
    }
}

