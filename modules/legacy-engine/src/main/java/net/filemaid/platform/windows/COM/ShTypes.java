package net.filemaid.platform.windows.COM;

import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid;

public interface ShTypes
extends com.sun.jna.platform.win32.ShTypes {

    public static interface SICHINTF {
        public static final int SICHINT_DISPLAY = 0;
        public static final int SICHINT_ALLFIELDS = Integer.MIN_VALUE;
        public static final int SICHINT_CANONICAL = 0x10000000;
        public static final int SICHINT_TEST_FILESYSPATH_IF_NOT_EQUAL = 0x20000000;
    }

    public static interface SIGDN {
        public static final int SIGDN_NORMALDISPLAY = 0;
        public static final int SIGDN_PARENTRELATIVEPARSING = -2147385343;
        public static final int SIGDN_DESKTOPABSOLUTEPARSING = -2147319808;
        public static final int SIGDN_PARENTRELATIVEEDITING = -2147282943;
        public static final int SIGDN_DESKTOPABSOLUTEEDITING = -2147172352;
        public static final int SIGDN_FILESYSPATH = -2147123200;
        public static final int SIGDN_URL = -2147057664;
        public static final int SIGDN_PARENTRELATIVEFORADDRESSBAR = -2146975743;
        public static final int SIGDN_PARENTRELATIVE = -2146959359;
        public static final int SIGDN_PARENTRELATIVEFORUI = -2146877439;
    }

    public static interface GETPROPERTYSTOREFLAGS {
        public static final int GPS_DEFAULT = 0;
        public static final int GPS_HANDLERPROPERTIESONLY = 1;
        public static final int GPS_READWRITE = 2;
        public static final int GPS_TEMPORARY = 4;
        public static final int GPS_FASTPROPERTIESONLY = 8;
        public static final int GPS_OPENSLOWITEM = 16;
        public static final int GPS_DELAYCREATION = 32;
        public static final int GPS_BESTEFFORT = 64;
        public static final int GPS_NO_OPLOCK = 128;
        public static final int GPS_MASK_VALID = 255;
    }

    public static interface SFGAOF {
        public static final int SFGAO_CANCOPY = 1;
        public static final int SFGAO_CANMOVE = 2;
        public static final int SFGAO_CANLINK = 4;
        public static final int SFGAO_STORAGE = 8;
        public static final int SFGAO_CANRENAME = 16;
        public static final int SFGAO_CANDELETE = 32;
        public static final int SFGAO_HASPROPSHEET = 64;
        public static final int SFGAO_DROPTARGET = 256;
        public static final int SFGAO_CAPABILITYMASK = 375;
        public static final int SFGAO_ENCRYPTED = 8192;
        public static final int SFGAO_ISSLOW = 16384;
        public static final int SFGAO_GHOSTED = 32768;
        public static final int SFGAO_LINK = 65536;
        public static final int SFGAO_SHARE = 131072;
        public static final int SFGAO_READONLY = 262144;
        public static final int SFGAO_HIDDEN = 524288;
        public static final int SFGAO_DISPLAYATTRMASK = 1032192;
        public static final int SFGAO_FILESYSANCESTOR = 0x10000000;
        public static final int SFGAO_FOLDER = 0x20000000;
        public static final int SFGAO_FILESYSTEM = 0x40000000;
        public static final int SFGAO_HASSUBFOLDER = Integer.MIN_VALUE;
        public static final int SFGAO_CONTENTSMASK = Integer.MIN_VALUE;
        public static final int SFGAO_VALIDATE = 0x1000000;
        public static final int SFGAO_REMOVABLE = 0x2000000;
        public static final int SFGAO_COMPRESSED = 0x4000000;
        public static final int SFGAO_BROWSABLE = 0x8000000;
        public static final int SFGAO_NONENUMERATED = 0x100000;
        public static final int SFGAO_NEWCONTENT = 0x200000;
        public static final int SFGAO_CANMONIKER = 0x400000;
        public static final int SFGAO_HASSTORAGE = 0x400000;
        public static final int SFGAO_STREAM = 0x400000;
        public static final int SFGAO_STORAGEANCESTOR = 0x800000;
        public static final int SFGAO_STORAGECAPMASK = 1891958792;
    }

    public static interface SIATTRIBFLAGS {
        public static final int SIATTRIBFLAGS_AND = 1;
        public static final int SIATTRIBFLAGS_OR = 2;
        public static final int SIATTRIBFLAGS_APPCOMPAT = 3;
        public static final int SIATTRIBFLAGS_MASK = 3;
        public static final int SIATTRIBFLAGS_ALLITEMS = 16384;
    }

    public static interface FILEOPENDIALOGOPTIONS {
        public static final int FOS_OVERWRITEPROMPT = 2;
        public static final int FOS_STRICTFILETYPES = 4;
        public static final int FOS_NOCHANGEDIR = 8;
        public static final int FOS_PICKFOLDERS = 32;
        public static final int FOS_FORCEFILESYSTEM = 64;
        public static final int FOS_ALLNONSTORAGEITEMS = 128;
        public static final int FOS_NOVALIDATE = 256;
        public static final int FOS_ALLOWMULTISELECT = 512;
        public static final int FOS_PATHMUSTEXIST = 2048;
        public static final int FOS_FILEMUSTEXIST = 4096;
        public static final int FOS_CREATEPROMPT = 8192;
        public static final int FOS_SHAREAWARE = 16384;
        public static final int FOS_NOREADONLYRETURN = 32768;
        public static final int FOS_NOTESTFILECREATE = 65536;
        public static final int FOS_HIDEMRUPLACES = 131072;
        public static final int FOS_HIDEPINNEDPLACES = 262144;
        public static final int FOS_NODEREFERENCELINKS = 0x100000;
        public static final int FOS_OKBUTTONNEEDSINTERACTION = 0x200000;
        public static final int FOS_DONTADDTORECENT = 0x2000000;
        public static final int FOS_FORCESHOWHIDDEN = 0x10000000;
        public static final int FOS_DEFAULTNOMINIMODE = 0x20000000;
        public static final int FOS_FORCEPREVIEWPANEON = 0x40000000;
        public static final int FOS_SUPPORTSTREAMABLEITEMS = Integer.MIN_VALUE;
    }

    @Structure.FieldOrder(value={"fmdid", "pid"})
    public static class PROPERTYKEY
    extends Structure {
        public Guid.GUID fmtid;
        public int pid;
    }

    @Structure.FieldOrder(value={"pszName", "pszSpec"})
    public static class COMDLG_FILTERSPEC
    extends Structure {
        public WString pszName;
        public WString pszSpec;
    }
}

