package net.filemaid.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.awt.Component;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.filemaid.Logging;
import net.filemaid.platform.windows.COM.COM;
import net.filemaid.platform.windows.COM.FileOpenDialog;
import net.filemaid.platform.windows.COM.FileSaveDialog;
import net.filemaid.platform.windows.COM.IFileDialog;
import net.filemaid.platform.windows.COM.IFileOpenDialog;
import net.filemaid.platform.windows.COM.IFileSaveDialog;
import net.filemaid.platform.windows.COM.IShellItem;
import net.filemaid.platform.windows.COM.ShTypes;
import net.filemaid.platform.windows.COM.Shell32Util;
import net.filemaid.platform.windows.COM.ShellItem;
import net.filemaid.platform.windows.COM.ShellItemArray;

public class FileDialog {
    private boolean multiSelectionEnabled = false;
    private boolean folderSelectionEnabled = false;
    private List<ExtensionFileFilter> filter = new ArrayList<ExtensionFileFilter>();
    private File folder;
    private String file;
    private String title;

    public void setMultiSelectionEnabled(boolean bl) {
        this.multiSelectionEnabled = bl;
    }

    public void setFolderSelectionEnabled(boolean bl) {
        this.folderSelectionEnabled = bl;
    }

    public void addFilter(String string, String ... stringArray) {
        this.filter.add(new ExtensionFileFilter(string, stringArray));
    }

    public void setFolder(File file) {
        this.folder = file;
    }

    public void setFile(String string) {
        this.file = string;
    }

    public void setTitle(String string) {
        this.title = string;
    }

    public List<File> showOpenDialog(Window window) {
        return COM.call(IFileOpenDialog.CLSID_FILEOPENDIALOG, IFileOpenDialog.IID_IFILEOPENDIALOG, FileOpenDialog::new, fileOpenDialog -> {
            WinNT.HRESULT hRESULT = this.show((IFileDialog)fileOpenDialog, window);
            if (W32Errors.FAILED((WinNT.HRESULT)hRESULT)) {
                return Collections.emptyList();
            }
            String[] stringArray = this.getResults((IFileOpenDialog)fileOpenDialog);
            return Arrays.stream(stringArray).map(File::new).collect(Collectors.toList());
        });
    }

    public File showSaveDialog(Window window) {
        return COM.call(IFileSaveDialog.CLSID_FILESAVEDIALOG, IFileSaveDialog.IID_IFILESAVEDIALOG, FileSaveDialog::new, fileSaveDialog -> {
            WinNT.HRESULT hRESULT = this.show((IFileDialog)fileSaveDialog, window);
            if (W32Errors.FAILED((WinNT.HRESULT)hRESULT)) {
                return null;
            }
            String string = this.getResult((IFileDialog)fileSaveDialog);
            return new File(string);
        });
    }

    private WinNT.HRESULT show(IFileDialog iFileDialog, Window window) {
        iFileDialog.SetOptions(this.getOptions());
        if (this.file != null) {
            iFileDialog.SetFileName(new WString(this.file));
        }
        if (this.folder != null && this.folder.isDirectory()) {
            try {
                iFileDialog.SetFolder(Shell32Util.SHCreateItemFromParsingName(this.folder));
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("IFileDialog.SetFolder", exception));
            }
        }
        if (this.filter.size() > 0) {
            iFileDialog.SetFileTypes(this.filter.size(), this.getFileTypes(this.filter));
        }
        if (this.title != null) {
            iFileDialog.SetTitle(new WString(this.title));
        }
        WinDef.HWND hWND = new WinDef.HWND(Native.getComponentPointer((Component)window));
        return iFileDialog.Show(hWND);
    }

    private String getResult(IFileDialog iFileDialog) {
        PointerByReference pointerByReference = new PointerByReference();
        iFileDialog.GetResult(pointerByReference);
        return this.getFilePath(new ShellItem(pointerByReference.getValue()));
    }

    private String[] getResults(IFileOpenDialog iFileOpenDialog) {
        PointerByReference pointerByReference = new PointerByReference();
        iFileOpenDialog.GetResults(pointerByReference);
        ShellItemArray shellItemArray = new ShellItemArray(pointerByReference.getValue());
        IntByReference intByReference = new IntByReference();
        shellItemArray.GetCount(intByReference);
        int n = intByReference.getValue();
        String[] stringArray = new String[n];
        for (int i = 0; i < n; ++i) {
            PointerByReference pointerByReference2 = new PointerByReference();
            shellItemArray.GetItemAt(i, pointerByReference2);
            stringArray[i] = this.getFilePath(new ShellItem(pointerByReference2.getValue()));
        }
        return stringArray;
    }

    private String getFilePath(IShellItem iShellItem) {
        PointerByReference pointerByReference = new PointerByReference();
        WinNT.HRESULT hRESULT = iShellItem.GetDisplayName(-2147123200, pointerByReference);
        if (W32Errors.FAILED((WinNT.HRESULT)hRESULT)) {
            throw new Win32Exception(hRESULT);
        }
        String string = pointerByReference.getValue().getWideString(0L);
        Ole32.INSTANCE.CoTaskMemFree(pointerByReference.getValue());
        return string;
    }

    private int getOptions() {
        int n = 64;
        if (this.multiSelectionEnabled) {
            n |= 0x200;
        }
        if (this.folderSelectionEnabled) {
            n |= 0x20;
        }
        return n;
    }

    private ShTypes.COMDLG_FILTERSPEC[] getFileTypes(List<ExtensionFileFilter> list) {
        ShTypes.COMDLG_FILTERSPEC[] cOMDLG_FILTERSPECArray = (ShTypes.COMDLG_FILTERSPEC[])new ShTypes.COMDLG_FILTERSPEC().toArray(list.size());
        for (int i = 0; i < cOMDLG_FILTERSPECArray.length; ++i) {
            ExtensionFileFilter extensionFileFilter = list.get(i);
            cOMDLG_FILTERSPECArray[i].pszName = new WString(extensionFileFilter.getDescription());
            cOMDLG_FILTERSPECArray[i].pszSpec = new WString(extensionFileFilter.getPattern());
        }
        return cOMDLG_FILTERSPECArray;
    }

    public static class ExtensionFileFilter {
        private final String description;
        private final String[] extensions;

        public ExtensionFileFilter(String string, String ... stringArray) {
            this.description = string;
            this.extensions = stringArray;
        }

        public String getDescription() {
            return this.description;
        }

        public String getPattern() {
            return String.join((CharSequence)"; ", this.extensions);
        }
    }
}

