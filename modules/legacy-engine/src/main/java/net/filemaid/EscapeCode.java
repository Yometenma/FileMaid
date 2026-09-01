package net.filemaid;

import net.filemaid.Settings;
import net.filemaid.platform.windows.WinCon;
import net.filemaid.util.SystemProperty;

public class EscapeCode {
    public static final boolean COLOR_TERMINAL = SystemProperty.get("net.filemaid.logging.color", Boolean::parseBoolean, EscapeCode.isSupported());
    public static final EscapeCode CHERRY_RED = EscapeCode.newColorStyle(196);
    public static final EscapeCode ORANGE_RED = EscapeCode.newColorStyle(202);
    public static final EscapeCode LIME_GREEN = EscapeCode.newColorStyle(40);
    public static final EscapeCode ROYAL_BLUE = EscapeCode.newColorStyle(39);
    public static final EscapeCode ORANGE_ONE = EscapeCode.newColorStyle(214);
    public static final EscapeCode BOLD = EscapeCode.newFontStyle(1);
    public static final EscapeCode ITALIC = EscapeCode.newFontStyle(2);
    public static final EscapeCode UNDERLINE = EscapeCode.newFontStyle(4);
    public static final EscapeCode STRIKEOUT = EscapeCode.newFontStyle(9);
    public static final EscapeCode NONE = new EscapeCode("", "");
    public final String begin;
    public final String end;

    public static boolean isSupported() {
        if (System.console() != null) {
            if ("xterm-256color".equals(System.getenv("TERM"))) {
                return true;
            }
            if (Settings.isWindowsApp()) {
                return WinCon.configureVirtualTerminal();
            }
        }
        return false;
    }

    public static String color(EscapeCode escapeCode, String string) {
        return COLOR_TERMINAL ? escapeCode.apply(string) : string;
    }

    public static EscapeCode newColorStyle(int n) {
        return new EscapeCode("38;5;" + n);
    }

    public static EscapeCode newFontStyle(int n) {
        return new EscapeCode("" + n);
    }

    public EscapeCode(String string) {
        this("\u001b[" + string + "m", "\u001b[0m");
    }

    public EscapeCode(String string, String string2) {
        this.begin = string;
        this.end = string2;
    }

    public String apply(String string) {
        return this.begin + string + this.end;
    }
}

