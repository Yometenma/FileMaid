package net.filemaid.platform.posix;

import com.sun.jna.Native;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.filemaid.CategoryFileFilter;
import net.filemaid.Execute;
import net.filemaid.ExecuteException;
import net.filemaid.Logging;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.ui.SwingUI;

public class Zenity {
    private final String zenity;

    public Zenity(String string) {
        this.zenity = string;
    }

    public String version() throws Exception {
        return Execute.execute(this.zenity, "--version").toString();
    }

    public List<File> selectFiles(boolean bl, boolean bl2, File file, CategoryFileFilter categoryFileFilter, String string, EventObject eventObject) {
        return this.zenity(bl, bl2, file, categoryFileFilter, string, false, eventObject);
    }

    public File selectFile(File file, CategoryFileFilter categoryFileFilter, String string, boolean bl, EventObject eventObject) {
        List<File> list = this.zenity(false, false, file, categoryFileFilter, string, bl, eventObject);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<File> zenity(boolean bl, boolean bl2, File file, CategoryFileFilter categoryFileFilter, String string2, boolean bl3, EventObject eventObject) {
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add("--file-selection");
        arrayList.add("--modal");
        if (bl) {
            arrayList.add("--directory");
        }
        if (bl2) {
            arrayList.add("--multiple");
        }
        if (bl3) {
            arrayList.add("--save");
        }
        if (file != null) {
            arrayList.add("--filename=" + file);
        }
        if (string2 != null) {
            arrayList.add("--title=" + string2);
        }
        if (categoryFileFilter != null && categoryFileFilter.hasTypes()) {
            categoryFileFilter.each((string, extensionFileFilter) -> arrayList.add("--file-filter=" + string + " | " + String.join((CharSequence)" ", extensionFileFilter.glob())));
        }
        arrayList.add("--separator=\n");
        try {
            CharSequence charSequence = Execute.execute(this.zenity, arrayList, null, this.getEnvironment(eventObject), false);
            return RegularExpressions.LINEBREAK.splitAsStream(charSequence).map(File::new).filter(File::isAbsolute).collect(Collectors.toList());
        }
        catch (ExecuteException executeException) {
            Logging.debug.finest(Logging.cause(this.zenity, executeException));
        }
        catch (IOException iOException) {
            throw new IllegalStateException(iOException);
        }
        return Collections.emptyList();
    }

    private Map<String, String> getEnvironment(EventObject eventObject) {
        try {
            Window window = SwingUI.getWindow(eventObject);
            if (window != null) {
                long l = Native.getWindowID((Window)window);
                return Collections.singletonMap("WINDOWID", Long.toString(l));
            }
        }
        catch (Throwable throwable) {
            Logging.debug.warning(Logging.cause("Failed to retrieve parent window ID", throwable));
        }
        return Collections.emptyMap();
    }
}

