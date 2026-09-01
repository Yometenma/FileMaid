package net.filemaid.platform.posix;

import java.io.File;
import java.io.IOException;
import net.filemaid.Logging;

public class XDG {
    public static boolean trash(File file) throws IOException {
        if (Command.trash.exists()) {
            try {
                return Command.trash.execute(file.getAbsolutePath()).waitFor() == 0;
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause((Object)Command.trash, exception));
            }
        }
        if (Command.gio.exists()) {
            try {
                return Command.gio.execute("trash", file.getAbsolutePath()).waitFor() == 0;
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause((Object)Command.gio, exception));
            }
        }
        return false;
    }

    public static boolean open(File file) {
        return XDG.open(file.getAbsolutePath());
    }

    public static boolean reveal(File file) {
        return XDG.open(file.getParentFile().getAbsolutePath());
    }

    public static boolean open(String string) {
        try {
            Command.open.execute(string);
            return true;
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause((Object)Command.open, exception));
            return false;
        }
    }

    public static enum Command {
        open,
        gio,
        trash;


        public String getCommand() {
            switch (this) {
                case open: {
                    return System.getProperty("xdg.open", "/usr/bin/xdg-open");
                }
                case gio: {
                    return System.getProperty("xdg.gio", "/usr/bin/gio");
                }
                case trash: {
                    return System.getProperty("xdg.trash", "/usr/bin/trash");
                }
            }
            return null;
        }

        public boolean exists() {
            return new File(this.getCommand()).canExecute();
        }

        public Process execute(String ... stringArray) throws IOException {
            ProcessBuilder processBuilder = new ProcessBuilder(this.getCommand());
            for (String string : stringArray) {
                processBuilder.command().add(string);
            }
            return processBuilder.inheritIO().start();
        }
    }
}

