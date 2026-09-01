package net.filemaid.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.filemaid.platform.windows.COM.COM;
import net.filemaid.platform.windows.COM.IFileOperation;
import net.filemaid.platform.windows.COM.Shell32Util;

public class FileOperation {
    private final List<Operation> operations = new ArrayList<Operation>();
    private int flags = 8391184;

    public void move(File file, File file2) {
        this.operations.add(new Operation(Kind.MOVE, file, file2));
    }

    public void copy(File file, File file2) {
        this.operations.add(new Operation(Kind.COPY, file, file2));
    }

    public void setFlags(int n) {
        this.flags = n;
    }

    public boolean perform(Window window) {
        if (this.operations.isEmpty()) {
            return true;
        }
        return COM.call(IFileOperation.CLSID_FILEOPERATION, IFileOperation.IID_IFILEOPERATION, net.filemaid.platform.windows.COM.FileOperation::new, fileOperation -> {
            fileOperation.SetOwnerWindow(new WinDef.HWND(Native.getWindowPointer((Window)window)));
            fileOperation.SetOperationFlags(this.flags);
            Iterator<Operation> iterator = this.operations.iterator();
            while (iterator.hasNext()) {
                Operation operation = iterator.next();
                File file = operation.source.getParentFile();
                File file2 = operation.destination.getParentFile();
                Pointer pointer = Shell32Util.SHCreateItemFromParsingName(operation.source);
                WString wString = new WString(operation.destination.getName());
                switch (operation.kind) {
                    case MOVE: {
                        if (file.equals(file2)) {
                            fileOperation.RenameItem(pointer, wString, null);
                            break;
                        }
                        Pointer pointer2 = Shell32Util.SHCreateItemFromParsingName(file2);
                        fileOperation.MoveItem(pointer, pointer2, wString, null);
                        break;
                    }
                    case COPY: {
                        Pointer pointer2 = Shell32Util.SHCreateItemFromParsingName(file2);
                        fileOperation.CopyItem(pointer, pointer2, wString, null);
                    }
                }
            }
            WinDef.BOOLByReference bOOLByReference = new WinDef.BOOLByReference();
            if (W32Errors.SUCCEEDED((WinNT.HRESULT)fileOperation.PerformOperations()) && W32Errors.SUCCEEDED((WinNT.HRESULT)fileOperation.GetAnyOperationsAborted(bOOLByReference))) {
                return !bOOLByReference.getValue().booleanValue();
            }
            return false;
        });
    }

    private static class Operation {
        public final Kind kind;
        public final File source;
        public final File destination;

        public Operation(Kind kind, File file, File file2) {
            this.kind = kind;
            this.source = file;
            this.destination = file2;
        }
    }

    public static enum Kind {
        MOVE,
        COPY;

    }
}

