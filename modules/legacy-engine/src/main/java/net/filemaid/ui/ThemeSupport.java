package net.filemaid.ui;

import com.bulenkov.darcula.DarculaLaf;
import com.bulenkov.iconloader.util.ColorUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.text.StyleContext;
import net.filemaid.Logging;
import net.filemaid.Settings;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.platform.windows.WinAppUtilities;
import net.filemaid.util.SystemProperty;
import net.filemaid.util.ui.GradientStyle;
import net.filemaid.util.ui.ProgressIndicator;
import net.filemaid.util.ui.RoundBorder;
import net.filemaid.util.ui.SelectionPainter;
import net.filemaid.util.ui.notification.SeparatorBorder;

public class ThemeSupport {
    private static Theme theme = SystemProperty.optional("net.filemaid.theme", Theme::forName).orElseGet(ThemeSupport::getSystemDefaultTheme);

    private static Theme getSystemDefaultTheme() {
        if (Settings.isWindowsApp()) {
            return WinAppUtilities.isAppsUseLightTheme() && !WinAppUtilities.isRDP() ? Theme.System : Theme.Darcula;
        }
        if (Settings.isMacApp()) {
            return MacAppUtilities.isAppleInterfaceStyleDark() ? Theme.Darcula : Theme.System;
        }
        return Theme.Darcula;
    }

    private static Optional<Font> getCustomFont() {
        return SystemProperty.optional("net.filemaid.theme.font", string -> StyleContext.getDefaultStyleContext().getFont((String)string, 0, SystemProperty.get("net.filemaid.theme.font.size", Integer::parseInt, 12)));
    }

    public static Theme getTheme() {
        return theme;
    }

    public static void setTheme() {
        ThemeSupport.setTheme(theme);
        ThemeSupport.getCustomFont().ifPresent(theme::setFont);
    }

    public static void setTheme(Theme theme) {
        try {
            ThemeSupport.theme = theme;
            ThemeSupport.theme.setLookAndFeel();
        }
        catch (Exception exception) {
            Logging.trace("Failed to set LaF", exception);
        }
    }

    public static Color getColor(int n) {
        return theme.getColor(n);
    }

    public static Color getPanelBackground() {
        return ThemeSupport.getColor(0xFFFFFF);
    }

    public static Color getLabelForeground() {
        return ThemeSupport.getColor(0x101010);
    }

    public static Border getEditorBorder() {
        return BorderFactory.createLineBorder(theme.isDark() ? new Color(0x646464) : new Color(9149100));
    }

    public static Color getEditorBackground() {
        return theme.isDark() ? new Color(4540746) : new Color(0xFFFFFF);
    }

    public static Color getHelpPanelBackground() {
        return theme.isDark() ? new Color(0x313131) : new Color(0xFFFFE1);
    }

    public static Border getHelpPanelBorder() {
        return BorderFactory.createLineBorder(ThemeSupport.getColor(11315353));
    }

    public static Color getErrorColor() {
        return Color.red;
    }

    public static Color getLinkColor() {
        return theme.getLinkSelectionForeground();
    }

    public static Color getActiveColor() {
        return new Color(6591981);
    }

    public static Color getPassiveColor() {
        return Color.lightGray;
    }

    public static Color getVerificationColor() {
        return new Color(39168);
    }

    public static Color getPanelSelectionBorderColor() {
        return theme.isDark() ? new Color(1645606) : new Color(1454692);
    }

    public static Color getBlankBackgroundColor() {
        return ThemeSupport.getColor(0xF8F8FF);
    }

    public static LinearGradientPaint getPanelBackgroundGradient(int n, int n2, int n3, int n4) {
        float[] fArray = new float[]{0.0f, 0.5f, 1.0f};
        Color[] colorArray = new Color[]{ThemeSupport.getColor(0xF6F6F6), ThemeSupport.getColor(0xF8F8F8), ThemeSupport.getColor(0xF3F3F3)};
        return new LinearGradientPaint(n, n2, n3, n4, fArray, colorArray);
    }

    public static Border getRoundBorder() {
        return new RoundBorder(ThemeSupport.getColor(0xACACAC), 12, new Insets(1, 1, 1, 1));
    }

    public static Border getSeparatorBorder(SeparatorBorder.Position position) {
        return new SeparatorBorder(1, ThemeSupport.getColor(0xB4B4B4), ThemeSupport.getColor(0xACACAC), GradientStyle.LEFT_TO_RIGHT, position);
    }

    public static Border getHorizontalRule() {
        return new SeparatorBorder(2, new Color(0, 0, 0, 90), GradientStyle.TOP_TO_BOTTOM, SeparatorBorder.Position.BOTTOM);
    }

    public static ProgressIndicator getProgressIndicator() {
        return new ProgressIndicator(Color.orange, ThemeSupport.withAlpha(ThemeSupport.getLabelForeground(), 0.25f));
    }

    public static Color withAlpha(Color color, float f) {
        return new Color((int)(f * 255.0f) << 24 | color.getRGB() & 0xFFFFFF, true);
    }

    public static enum Theme {
        System{

            @Override
            public void setLookAndFeel() throws Exception {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        }
        ,
        Darcula{

            @Override
            public void setLookAndFeel() throws Exception {
                Font font = UIManager.getFont("Label.font");
                UIManager.setLookAndFeel((LookAndFeel)new DarculaLaf());
                ColorUIResource colorUIResource = new ColorUIResource(3762570);
                ColorUIResource colorUIResource2 = new ColorUIResource(3816766);
                ColorUIResource colorUIResource3 = new ColorUIResource(0xFFFFFF);
                UIManager.put("List.selectionForeground", colorUIResource3);
                UIManager.put("List.selectionBackground", colorUIResource);
                UIManager.put("ComboBox.selectionBackground", colorUIResource);
                UIManager.put("Table.selectionBackground", colorUIResource);
                UIManager.put("Menu.selectionBackground", colorUIResource);
                UIManager.put("MenuItem.selectionBackground", colorUIResource);
                UIManager.put("MenuItem.selectedBackgroundPainter", new SelectionPainter(colorUIResource));
                UIManager.put("PopupMenu.selectionBackground", colorUIResource);
                UIManager.put("Tree.selectionBackground", colorUIResource);
                UIManager.put("Tree.selectionInactiveBackground", colorUIResource);
                UIManager.put("Table.background", colorUIResource2);
                UIManager.put("TabbedPane.selected", colorUIResource2);
                UIManager.put("ProgressBar.selectionBackground", this.getLinkSelectionForeground());
                if (Settings.isWindowsApp()) {
                    if (Stream.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()).allMatch(graphicsDevice -> graphicsDevice.getDefaultConfiguration().getDefaultTransform().getScaleX() > 1.0)) {
                        font = StyleContext.getDefaultStyleContext().getFont("Segoe UI", 0, 12);
                    }
                    this.setFont(font);
                }
            }

            @Override
            public Color getColor(int n) {
                return this.getDarkColor(new Color(n));
            }

            public Color getDarkColor(Color color) {
                return ColorUtil.shift((Color)color, (double)(ColorUtil.isDark((Color)color) ? 9.0 : 0.2));
            }

            @Override
            public boolean isDark() {
                return true;
            }

            @Override
            public Color getLinkSelectionForeground() {
                return new Color(5346756);
            }
        }
        ,
        Nimbus{

            @Override
            public void setLookAndFeel() throws Exception {
                UIManager.setLookAndFeel(new NimbusLookAndFeel());
            }
        }
        ,
        Metal{

            @Override
            public void setLookAndFeel() throws Exception {
                UIManager.setLookAndFeel(new MetalLookAndFeel());
            }
        };


        public Color getColor(int n) {
            return new Color(n);
        }

        public boolean isDark() {
            return false;
        }

        public Color getLinkSelectionForeground() {
            return new Color(0x3399FF);
        }

        public abstract void setLookAndFeel() throws Exception;

        public void setFont(Font font) {
            UIManager.put("Label.font", font);
            UIManager.put("Button.font", font);
            UIManager.put("ComboBox.font", font);
            UIManager.put("TextField.font", font);
            UIManager.put("ToolTip.font", font);
            UIManager.put("Table.font", font);
            UIManager.put("Tree.font", font);
            UIManager.put("ProgressBar.font", font);
        }

        public static List<String> names() {
            return Arrays.stream(Theme.values()).map(Enum::name).collect(Collectors.toList());
        }

        public static Theme forName(String string) {
            for (Theme theme : Theme.values()) {
                if (!theme.name().equalsIgnoreCase(string)) continue;
                return theme;
            }
            throw new IllegalArgumentException(string + " not in " + Theme.names());
        }
    }
}

