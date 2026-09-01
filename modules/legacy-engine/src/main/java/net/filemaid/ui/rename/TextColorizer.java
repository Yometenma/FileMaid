package net.filemaid.ui.rename;

import java.awt.Color;
import java.io.File;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.SwingUI;

public class TextColorizer {
    private Color pathRainbowBeginColor;
    private Color pathRainbowEndColor;
    private String before;
    private String after;

    public TextColorizer() {
        this("<html><nobr>", "</nobr></html>");
    }

    public TextColorizer(String string, String string2) {
        this(string, string2, new Color(0xCC3300), new Color(32896));
    }

    public TextColorizer(String string, String string2, Color color, Color color2) {
        this.before = string;
        this.after = string2;
        this.pathRainbowBeginColor = color;
        this.pathRainbowEndColor = color2;
    }

    public StringBuilder colorizePath(StringBuilder stringBuilder, File file, boolean bl) {
        return this.colorizePath(stringBuilder, FileUtilities.abbreviatePath(file), bl);
    }

    public StringBuilder colorizePath(StringBuilder stringBuilder, String string, boolean bl) {
        stringBuilder.append(this.before);
        String[] stringArray = string.split("/");
        String string2 = stringArray.length > 0 ? stringArray[stringArray.length - 1] : string;
        for (int i = 0; i < stringArray.length - 1; ++i) {
            Object object = i == 0 ? stringArray[i] : "/" + stringArray[i];
            Color color = SwingUI.interpolateHSB(this.pathRainbowBeginColor, this.pathRainbowEndColor, stringArray.length <= 2 ? 1.0f : (float)i / ((float)stringArray.length - 2.0f));
            stringBuilder.append(SwingUI.formatHTML("<span style='color:%s'>%s</span>", SwingUI.toHex(color), object));
        }
        if (stringArray.length > 1) {
            stringBuilder.append("<span style='color:#607080'>/</span>");
        }
        if (bl) {
            stringBuilder.append(SwingUI.escapeHTML(FileUtilities.getNameWithoutExtension(string2)));
            String string3 = FileUtilities.getExtension(string2);
            if (string3 != null) {
                stringBuilder.append(SwingUI.formatHTML("<span style='color:#607080'>.%s</span>", string3));
            }
        } else {
            stringBuilder.append(string2);
        }
        return stringBuilder.append(this.after);
    }

    public static String colorizeFilePath(File file) {
        return new TextColorizer().colorizePath(new StringBuilder(256), file, true).toString();
    }

    public static String colorizeFilePath(String string, boolean bl) {
        return new TextColorizer().colorizePath(new StringBuilder(256), string, bl).toString();
    }
}

