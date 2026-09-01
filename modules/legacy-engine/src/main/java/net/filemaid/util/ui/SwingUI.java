package net.filemaid.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SecondaryLoop;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.MouseInputListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;
import net.filemaid.Logging;
import net.filemaid.Settings;
import net.filemaid.ui.BaseDialog;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.ui.GlassOptionPane;
import net.filemaid.util.ui.GlassPane;
import net.filemaid.util.ui.ShadowBorder;

public final class SwingUI {
    private static int windowProgressValue = Integer.MIN_VALUE;
    private static final boolean GLASS_EFFECT = Boolean.parseBoolean(System.getProperty("net.filemaid.glass.effect", "true"));
    public static final Color TRANSLUCENT = new Color(255, 255, 255, 0);
    private static final ReentrantLock INPUT_DIALOG_LOCK = new ReentrantLock(true);

    public static void requestForeground() {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) {
            Desktop.getDesktop().requestForeground(true);
        } else {
            Arrays.stream(Window.getWindows()).filter(Component::isVisible).forEach(Window::toFront);
        }
    }

    public static void setWindowProgressState(Component component, boolean bl) {
        if (bl && windowProgressValue != Integer.MAX_VALUE) {
            if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.PROGRESS_STATE_WINDOW)) {
                Taskbar.getTaskbar().setWindowProgressState(SwingUI.getWindow(component), Taskbar.State.INDETERMINATE);
            }
            windowProgressValue = Integer.MAX_VALUE;
        } else if (!bl && windowProgressValue != Integer.MIN_VALUE) {
            if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.PROGRESS_STATE_WINDOW)) {
                Taskbar.getTaskbar().setWindowProgressState(SwingUI.getWindow(component), Taskbar.State.OFF);
            }
            windowProgressValue = Integer.MIN_VALUE;
        }
    }

    public static void setWindowProgressValue(Component component, int n, int n2) {
        int n3 = n * 100 / n2;
        if (windowProgressValue != n3) {
            if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.PROGRESS_VALUE_WINDOW)) {
                Taskbar.getTaskbar().setWindowProgressValue(SwingUI.getWindow(component), n3);
            }
            windowProgressValue = n3;
        }
    }

    public static void disableSuddenTermination(Object object, BackgroundRunnable backgroundRunnable) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_SUDDEN_TERMINATION)) {
            try {
                Desktop.getDesktop().disableSuddenTermination();
                SwingUI.withWaitCursor(object, backgroundRunnable);
            }
            finally {
                Desktop.getDesktop().enableSuddenTermination();
            }
        } else {
            SwingUI.withWaitCursor(object, backgroundRunnable);
        }
    }

    public static Component createGlassPane(JComponent jComponent, Window window) {
        if (GLASS_EFFECT) {
            return GlassPane.install(jComponent, SwingUtilities.getRootPane(window), GlassPane.BLUR);
        }
        BaseDialog baseDialog = new BaseDialog(window);
        JComponent jComponent2 = (JComponent)baseDialog.getContentPane();
        jComponent2.add((Component)jComponent, "Center");
        jComponent.setVisible(true);
        jComponent.addComponentListener(SwingUI.componentHidden(componentEvent -> baseDialog.dispose()));
        jComponent.setBorder(BorderFactory.createEmptyBorder());
        jComponent.requestFocusInWindow();
        baseDialog.pack();
        baseDialog.setLocation(SwingUI.getCenterLocation(baseDialog));
        baseDialog.setMinimumSize(baseDialog.getPreferredSize());
        baseDialog.setResizable(true);
        return baseDialog;
    }

    public static Border shadow() {
        return SwingUI.shadow(BorderFactory.createLineBorder(ThemeSupport.getColor(8428984), 1, false));
    }

    public static Border shadow(Border border) {
        return BorderFactory.createCompoundBorder(GLASS_EFFECT ? new ShadowBorder() : BorderFactory.createEmptyBorder(4, 4, 4, 4), border);
    }

    public static Color interpolateHSB(Color color, Color color2, float f) {
        float[] fArray = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float[] fArray2 = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);
        float[] fArray3 = new float[3];
        for (int i = 0; i < fArray3.length; ++i) {
            fArray3[i] = fArray[i] + (fArray2[i] - fArray[i]) * f;
        }
        return Color.getHSBColor(fArray3[0], fArray3[1], fArray3[2]);
    }

    public static String formatHTML(String string, Object ... objectArray) {
        return String.format(string, Arrays.stream(objectArray).map(object -> object == null ? null : SwingUI.escapeHTML(object.toString())).toArray());
    }

    public static String escapeHTML(String string) {
        char[] cArray;
        for (char c : cArray = new char[]{'&', '<', '>', '\"', '\''}) {
            string = string.replace(Character.toString(c), "&#" + Integer.toString(c) + ";");
        }
        return string;
    }

    public static String toHex(Color color) {
        return color == null ? "inherit" : String.format(Locale.ROOT, "#%06x", color.getRGB() & 0xFFFFFF);
    }

    public static boolean isShiftOrAltDown(EventObject eventObject) {
        if (eventObject instanceof InputEvent) {
            InputEvent inputEvent = (InputEvent)eventObject;
            return SwingUI.checkModifiers(inputEvent.getModifiersEx(), 64) || SwingUI.checkModifiers(inputEvent.getModifiersEx(), 512);
        }
        if (eventObject instanceof ActionEvent) {
            ActionEvent actionEvent = (ActionEvent)eventObject;
            return SwingUI.checkModifiers(actionEvent.getModifiers(), 1) || SwingUI.checkModifiers(actionEvent.getModifiers(), 8);
        }
        return false;
    }

    public static boolean isControlOrMetaDown(EventObject eventObject) {
        if (eventObject instanceof InputEvent) {
            InputEvent inputEvent = (InputEvent)eventObject;
            return SwingUI.checkModifiers(inputEvent.getModifiersEx(), 128) || SwingUI.checkModifiers(inputEvent.getModifiersEx(), 256);
        }
        if (eventObject instanceof ActionEvent) {
            ActionEvent actionEvent = (ActionEvent)eventObject;
            return SwingUI.checkModifiers(actionEvent.getModifiers(), 2) || SwingUI.checkModifiers(actionEvent.getModifiers(), 4);
        }
        return false;
    }

    public static boolean checkModifiers(int n, int n2) {
        return (n & n2) == n2;
    }

    public static JButton createImageButton(Action action) {
        JButton jButton = new JButton(action);
        jButton.setHideActionText(true);
        jButton.setToolTipText(String.valueOf(action.getValue("Name")));
        jButton.setVerticalTextPosition(3);
        jButton.setOpaque(false);
        jButton.setMaximumSize(new Dimension(36, 36));
        if (Settings.isMacApp()) {
            jButton.setPreferredSize(new Dimension(28, 27));
        } else {
            jButton.setPreferredSize(new Dimension(26, 26));
        }
        return jButton;
    }

    public static JScrollPane createScrollPaneGroup(String string, Component component) {
        JScrollPane jScrollPane = new JScrollPane(component, 20, 30);
        jScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0), BorderFactory.createTitledBorder(string)), jScrollPane.getBorder()));
        if (Settings.isMacApp()) {
            jScrollPane.setOpaque(false);
        }
        return jScrollPane;
    }

    public static JTabbedPane createTabbedPaneGroup(String string) {
        JTabbedPane jTabbedPane = new JTabbedPane();
        jTabbedPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0), BorderFactory.createTitledBorder(string)), jTabbedPane.getBorder()));
        if (Settings.isMacApp()) {
            jTabbedPane.setOpaque(false);
        }
        return jTabbedPane;
    }

    public static void installAction(Window window, int n, Action action) {
        if (window instanceof RootPaneContainer) {
            SwingUI.installAction((JComponent)((RootPaneContainer)((Object)window)).getRootPane(), n, 0, action);
        }
    }

    public static void installAction(JComponent jComponent, int n, Action action) {
        SwingUI.installAction(jComponent, n, 0, action);
    }

    public static void installAction(JComponent jComponent, int n, int n2, Action action) {
        SwingUI.installAction(jComponent, KeyStroke.getKeyStroke(n, n2), action);
        if (n2 == 128) {
            SwingUI.installAction(jComponent, n, 256, action);
        }
        if (n2 == 64) {
            SwingUI.installAction(jComponent, n, 512, action);
        }
        if (n == 127) {
            SwingUI.installAction(jComponent, 8, n2, action);
        }
    }

    public static void installAction(JComponent jComponent, KeyStroke keyStroke, Action action) {
        SwingUI.installAction(jComponent, 1, keyStroke, action);
    }

    public static void installAction(JComponent jComponent, int n, KeyStroke keyStroke, Action action) {
        jComponent.getInputMap(n).put(keyStroke, keyStroke);
        jComponent.getActionMap().put(keyStroke, action);
    }

    public static UndoManager installUndoSupport(JTextComponent jTextComponent) {
        UndoManager undoManager = new UndoManager();
        jTextComponent.getDocument().addUndoableEditListener(undoManager);
        SwingUI.installAction((JComponent)jTextComponent, 90, 128, SwingUI.newAction("Undo", actionEvent -> {
            if (undoManager.canUndo()) {
                undoManager.undo();
            }
        }));
        SwingUI.installAction((JComponent)jTextComponent, 89, 128, SwingUI.newAction("Redo", actionEvent -> {
            if (undoManager.canRedo()) {
                undoManager.redo();
            }
        }));
        return undoManager;
    }

    public static boolean isMaximized(Frame frame) {
        return (frame.getExtendedState() & 6) != 0;
    }

    public static boolean isOnScreen(Rectangle rectangle) {
        for (GraphicsDevice graphicsDevice : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            if (!graphicsDevice.getDefaultConfiguration().getBounds().contains(rectangle.getCenterX(), rectangle.getCenterY())) continue;
            return true;
        }
        return false;
    }

    public static <T> T showInputDialog(Callable<T> callable) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return callable.call();
        }
        INPUT_DIALOG_LOCK.lockInterruptibly();
        try {
            FutureTask<T> futureTask = new FutureTask<T>(callable);
            SwingUtilities.invokeLater(futureTask);
            T t = futureTask.get();
            return t;
        }
        finally {
            INPUT_DIALOG_LOCK.unlock();
        }
    }

    public static String showInputDialog(Object object, String string, String string2, Icon icon, Component component) throws Exception {
        return SwingUI.showInputDialog(() -> GlassOptionPane.showInputDialog(object.toString(), string, string2, icon, component).map(String::trim).filter(s -> !s.isEmpty()).orElse(null));
    }

    public static List<String> showMultiValueInputDialog(Object object, String string2, String string3, Icon icon, Component component) throws Exception {
        String string4 = SwingUI.showInputDialog(object, string2, string3, icon, component);
        if (string4 == null) {
            return Collections.emptyList();
        }
        for (Pattern pattern : new Pattern[]{RegularExpressions.PIPE, RegularExpressions.SEMICOLON}) {
            if (!pattern.matcher(string4).find()) continue;
            return pattern.splitAsStream(string4).map(String::trim).filter(string -> !string.isEmpty()).collect(Collectors.toList());
        }
        return Collections.singletonList(string4);
    }

    public static Window getMainWindow() {
        return Arrays.stream(Window.getOwnerlessWindows()).filter(Component::isVisible).findFirst().orElse(null);
    }

    public static Window getWindow(Object object) {
        if (object instanceof Window) {
            return (Window)object;
        }
        if (object instanceof Component) {
            Component component = (Component)object;
            return SwingUI.getWindow(SwingUtilities.getWindowAncestor(component));
        }
        if (object instanceof EventObject) {
            EventObject eventObject = (EventObject)object;
            return SwingUI.getWindow(eventObject.getSource());
        }
        return SwingUI.getMainWindow();
    }

    public static Point getOffsetLocation(Window window) {
        Window window2 = window.getOwner();
        if (window2 == null) {
            return new Point(120, 80);
        }
        Rectangle rectangle = window2.getBounds();
        Rectangle rectangle2 = new Rectangle(rectangle.x + rectangle.width / 4, rectangle.y + rectangle.height / 7, window.getWidth(), window.getHeight());
        return SwingUI.adjustLocationToFitScreen(SwingUI.getScreenBounds(window2), rectangle2);
    }

    public static Point getCenterLocation(Window window) {
        Window window2 = window.getOwner();
        Rectangle rectangle = window2.getBounds();
        Rectangle rectangle2 = new Rectangle(rectangle.x + (rectangle.width - window.getWidth()) / 2, rectangle.y + (rectangle.height - window.getHeight()) / 2, window.getWidth(), window.getHeight());
        return SwingUI.adjustLocationToFitScreen(SwingUI.getScreenBounds(window2), rectangle2);
    }

    public static void adjustSizeToFit(Window window, Dimension dimension) {
        dimension.width += 100;
        dimension.height += 100;
        if (dimension.width > window.getWidth() || dimension.height > window.getHeight()) {
            Rectangle rectangle = window.getBounds();
            dimension.width = Math.max(dimension.width, rectangle.width);
            dimension.height = Math.max(dimension.height, rectangle.height);
            rectangle.x -= (dimension.width - rectangle.width) / 3;
            rectangle.y -= (dimension.height - rectangle.height) / 3;
            rectangle.width = dimension.width;
            rectangle.height = dimension.height;
            Point point = SwingUI.adjustLocationToFitScreen(SwingUI.getScreenBounds(window), rectangle);
            rectangle.setLocation(point);
            window.setBounds(rectangle);
        }
    }

    public static Point adjustLocationToFitScreen(Rectangle rectangle, Rectangle rectangle2) {
        Point point = new Point(rectangle2.x, rectangle2.y);
        if (point.x + rectangle2.width > rectangle.x + rectangle.width) {
            point.x = rectangle.x + rectangle.width - rectangle2.width;
        }
        if (point.y + rectangle2.height > rectangle.y + rectangle.height) {
            point.y = rectangle.y + rectangle.height - rectangle2.height;
        }
        if (point.x < rectangle.x) {
            point.x = rectangle.x;
        }
        if (point.y < rectangle.y) {
            point.y = rectangle.y;
        }
        return point;
    }

    public static Rectangle getScreenBounds(Component component) {
        GraphicsConfiguration graphicsConfiguration = component.getGraphicsConfiguration();
        if (graphicsConfiguration == null) {
            graphicsConfiguration = SwingUI.getScreenDevice(component.getLocationOnScreen());
        }
        if (graphicsConfiguration == null) {
            return new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        }
        Rectangle rectangle = graphicsConfiguration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);
        rectangle.x += insets.left;
        rectangle.y += insets.top;
        rectangle.width -= insets.left + insets.right;
        rectangle.height -= insets.top + insets.bottom;
        return rectangle;
    }

    public static GraphicsConfiguration getScreenDevice(Point point) {
        for (GraphicsDevice graphicsDevice : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            GraphicsConfiguration graphicsConfiguration;
            if (graphicsDevice.getType() != 0 || !(graphicsConfiguration = graphicsDevice.getDefaultConfiguration()).getBounds().contains(point)) continue;
            return graphicsConfiguration;
        }
        return null;
    }

    public static Image getImage(Icon icon) {
        if (icon == null) {
            return null;
        }
        if (icon instanceof ImageIcon) {
            return ((ImageIcon)icon).getImage();
        }
        BufferedImage bufferedImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        icon.paintIcon(null, graphics2D, 0, 0);
        graphics2D.dispose();
        return bufferedImage;
    }

    public static Dimension getDimension(Icon icon) {
        return new Dimension(icon.getIconWidth(), icon.getIconHeight());
    }

    public static Timer invokeLater(int n, Runnable runnable) {
        Timer timer = new Timer(n, actionEvent -> runnable.run());
        timer.setRepeats(false);
        timer.start();
        return timer;
    }

    public static void showLater(Window window) {
        SwingUtilities.invokeLater(() -> {
            SwingUtilities.getRootPane(window).setDoubleBuffered(false);
            window.setVisible(true);
            SwingUtilities.invokeLater(() -> SwingUtilities.getRootPane(window).setDoubleBuffered(true));
        });
    }

    public static <T> Timer animate(int n, int n2, Consumer<T> consumer, T ... TArray) {
        Iterator<T> iterator = Arrays.asList(TArray).iterator();
        Timer timer = new Timer(n2, actionEvent -> {
            if (iterator.hasNext()) {
                consumer.accept(iterator.next());
            } else {
                ((Timer)actionEvent.getSource()).stop();
            }
        });
        timer.setInitialDelay(n);
        timer.setRepeats(true);
        timer.setCoalesce(false);
        timer.start();
        return timer;
    }

    public static void withWaitCursor(Object object, BackgroundRunnable backgroundRunnable) {
        Optional<Window> optional = Optional.ofNullable(SwingUI.getWindow(object));
        try {
            optional.ifPresent(window -> window.setCursor(Cursor.getPredefinedCursor(3)));
            backgroundRunnable.run();
        }
        catch (Throwable throwable) {
            Logging.trace(throwable);
        }
        finally {
            optional.ifPresent(window -> window.setCursor(Cursor.getDefaultCursor()));
        }
    }

    public static <V> Optional<V> withWaitCursor(Object object, Callable<V> callable) {
        Optional<Window> optional = Optional.ofNullable(SwingUI.getWindow(object));
        try {
            optional.ifPresent(window -> window.setCursor(Cursor.getPredefinedCursor(3)));
            Optional<V> optional2 = Optional.ofNullable(callable.call());
            return optional2;
        }
        catch (Exception exception) {
            Logging.trace(exception);
            Optional optional3 = Optional.empty();
            return optional3;
        }
        finally {
            optional.ifPresent(window -> window.setCursor(Cursor.getDefaultCursor()));
        }
    }

    public static WindowListener windowOpened(final Consumer<WindowEvent> consumer) {
        return new WindowAdapter(){

            @Override
            public void windowOpened(WindowEvent windowEvent) {
                consumer.accept(windowEvent);
            }
        };
    }

    public static WindowListener windowClosed(final Consumer<WindowEvent> consumer) {
        return new WindowAdapter(){
            private boolean open = true;

            @Override
            public void windowClosing(WindowEvent windowEvent) {
                this.windowClosed(windowEvent);
            }

            @Override
            public void windowClosed(WindowEvent windowEvent) {
                if (this.open) {
                    this.open = false;
                    consumer.accept(windowEvent);
                }
            }
        };
    }

    public static ComponentListener componentShown(final Consumer<ComponentEvent> consumer) {
        return new ComponentAdapter(){

            @Override
            public void componentShown(ComponentEvent componentEvent) {
                consumer.accept(componentEvent);
            }
        };
    }

    public static ComponentListener componentHidden(final Consumer<ComponentEvent> consumer) {
        return new ComponentAdapter(){

            @Override
            public void componentHidden(ComponentEvent componentEvent) {
                consumer.accept(componentEvent);
            }
        };
    }

    public static HierarchyListener hierarchyChanged(final int n, final Consumer<HierarchyEvent> consumer) {
        return new HierarchyListener(){

            @Override
            public void hierarchyChanged(HierarchyEvent hierarchyEvent) {
                if ((hierarchyEvent.getChangeFlags() & (long)n) != 0L) {
                    consumer.accept(hierarchyEvent);
                }
            }
        };
    }

    public static ComponentListener componentResized(final Consumer<ComponentEvent> consumer) {
        return new ComponentAdapter(){

            @Override
            public void componentResized(ComponentEvent componentEvent) {
                consumer.accept(componentEvent);
            }
        };
    }

    public static MouseListener mouseClicked(final Consumer<MouseEvent> consumer) {
        return new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() > 0) {
                    consumer.accept(mouseEvent);
                }
            }
        };
    }

    public static MouseListener mouseDoubleClicked(final Consumer<MouseEvent> consumer) {
        return new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(mouseEvent) && (mouseEvent.getModifiersEx() & 0x1000) == 0) {
                    consumer.accept(mouseEvent);
                }
            }
        };
    }

    public static MouseListener mousePopupTriggerClicked(final Consumer<MouseEvent> consumer) {
        return new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger() && (mouseEvent.getModifiersEx() & 0x400) == 0) {
                    consumer.accept(mouseEvent);
                }
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger() && (mouseEvent.getModifiersEx() & 0x400) == 0) {
                    consumer.accept(mouseEvent);
                }
            }
        };
    }

    public static MouseListener mousePopupMenu(Function<MouseEvent, JPopupMenu> function) {
        return SwingUI.mousePopupTriggerClicked(mouseEvent -> {
            JPopupMenu jPopupMenu = (JPopupMenu)function.apply((MouseEvent)mouseEvent);
            if (jPopupMenu != null) {
                jPopupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            }
        });
    }

    public static MouseListener mouseHover(final Consumer<Boolean> consumer) {
        return new MouseAdapter(){

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                consumer.accept(true);
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                consumer.accept(false);
            }
        };
    }

    public static JPopupMenu newPopupMenu(String string) {
        JPopupMenu jPopupMenu = new JPopupMenu(string);
        jPopupMenu.setLightWeightPopupEnabled(false);
        return jPopupMenu;
    }

    public static JPopupMenu newPopupMenu(String string, final Consumer<JPopupMenu> consumer) {
        final JPopupMenu jPopupMenu = SwingUI.newPopupMenu(string);
        jPopupMenu.addPopupMenuListener(new PopupMenuListener(){

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                jPopupMenu.removeAll();
                consumer.accept(jPopupMenu);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                jPopupMenu.removeAll();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                jPopupMenu.removeAll();
            }
        });
        return jPopupMenu;
    }

    public static PopupMenuListener popupMenuWillBecomeVisible(final Consumer<PopupMenuEvent> consumer) {
        return new PopupMenuListener(){

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                consumer.accept(popupMenuEvent);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
            }
        };
    }

    public static PopupMenuListener popupMenuCanceled(final Consumer<PopupMenuEvent> consumer) {
        return new PopupMenuListener(){

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                consumer.accept(popupMenuEvent);
            }
        };
    }

    public static JComponent newPanel(String string, LayoutManager layoutManager) {
        JComponent jComponent = SwingUI.newPanel(layoutManager);
        jComponent.setBorder(BorderFactory.createTitledBorder(string));
        return jComponent;
    }

    public static JComponent newPanel(LayoutManager layoutManager) {
        JPanel jPanel = new JPanel(layoutManager);
        jPanel.setOpaque(false);
        if (Settings.isMacApp() && !ThemeSupport.getTheme().isDark()) {
            jPanel.setOpaque(true);
        }
        return jPanel;
    }

    public static JButton newButton(Action action) {
        JButton jButton = new JButton(action);
        jButton.setOpaque(false);
        return jButton;
    }

    public static JButton newButton(String string, Consumer<ActionEvent> consumer) {
        return SwingUI.newButton(SwingUI.newAction(string, null, consumer));
    }

    public static JButton newButton(String string, Icon icon, Consumer<ActionEvent> consumer) {
        return SwingUI.newButton(SwingUI.newAction(string, icon, consumer));
    }

    public static Action newAction(String string, Consumer<ActionEvent> consumer) {
        return new LambdaAction(string, null, consumer);
    }

    public static Action newAction(String string, Icon icon, Consumer<ActionEvent> consumer) {
        return new LambdaAction(string, icon, consumer);
    }

    public static Action newToolBarAction(String string, Icon icon, Consumer<ActionEvent> consumer) {
        LambdaAction lambdaAction = new LambdaAction(string, icon, consumer);
        lambdaAction.putValue("ShortDescription", string);
        return lambdaAction;
    }

    public static Action newPopupAction(String string, Icon icon, Supplier<JPopupMenu> supplier) {
        return SwingUI.newAction(string, icon, actionEvent -> {
            JComponent jComponent = (JComponent)actionEvent.getSource();
            SwingUI.showDropDown((JPopupMenu)supplier.get(), jComponent);
        });
    }

    public static void showDropDown(JPopupMenu jPopupMenu, EventObject eventObject) {
        SwingUI.showDropDown(jPopupMenu, (Component)eventObject.getSource());
    }

    public static void showDropDown(JPopupMenu jPopupMenu, Component component) {
        while (component != null && !component.isShowing()) {
            JPopupMenu jPopupMenu2 = (JPopupMenu)SwingUtilities.getAncestorOfClass(JPopupMenu.class, component);
            component = jPopupMenu2.getInvoker();
        }
        jPopupMenu.show(component, -3, component.getHeight() + 4);
    }

    public static <T> T onSecondaryLoop(BackgroundSupplier<T> backgroundSupplier) throws ExecutionException, InterruptedException {
        SecondaryLoop secondaryLoop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
        Future<T> future = SwingUI.onSwingWorker(backgroundSupplier, null, null, secondaryLoop::exit);
        secondaryLoop.enter();
        return future.get();
    }

    public static Future<?> onSwingWorker(BackgroundRunnable backgroundRunnable) {
        SwingRunnable swingRunnable = new SwingRunnable(backgroundRunnable);
        swingRunnable.execute();
        return swingRunnable;
    }

    public static <T> Future<T> onSwingWorker(BackgroundSupplier<T> backgroundSupplier, Consumer<T> consumer) {
        return SwingUI.onSwingWorker(backgroundSupplier, consumer, null, null);
    }

    public static <T> Future<T> onSwingWorker(BackgroundSupplier<T> backgroundSupplier, Consumer<T> consumer, Consumer<Exception> consumer2) {
        return SwingUI.onSwingWorker(backgroundSupplier, consumer, consumer2, null);
    }

    public static <T> Future<T> onSwingWorker(BackgroundSupplier<T> backgroundSupplier, Consumer<T> consumer, Consumer<Exception> consumer2, Runnable runnable) {
        SwingLambda swingLambda = new SwingLambda(backgroundSupplier, consumer, consumer2, runnable);
        swingLambda.execute();
        return swingLambda;
    }

    public static boolean useJavaFX() {
        if (null != System.getProperty("javafx.runtime.version")) {
            return true;
        }
        if (null != System.getProperty("javafx.runtime.error")) {
            return false;
        }
        try {
            Logging.debug.finest("Initialize JavaFX");
            new JFXPanel();
            Platform.setImplicitExit((boolean)false);
            return true;
        }
        catch (Throwable throwable) {
            Logging.debug.finest(Logging.cause("Failed to initialize JavaFX", throwable));
            System.setProperty("javafx.runtime.error", throwable.toString());
            return false;
        }
    }

    public static <T> T invokeJavaFX(Callable<T> callable) {
        SwingUI.useJavaFX();
        FutureTask<T> futureTask = new FutureTask<T>(callable);
        Platform.runLater(futureTask);
        try {
            return futureTask.get();
        }
        catch (ExecutionException executionException) {
            throw new RuntimeException(executionException.getCause());
        }
        catch (InterruptedException interruptedException) {
            throw new CancellationException();
        }
    }

    private SwingUI() {
        throw new UnsupportedOperationException();
    }

    @FunctionalInterface
    public static interface BackgroundRunnable {
        public void run() throws Exception;
    }

    private static class LambdaAction
    extends AbstractAction {
        private final Consumer<ActionEvent> action;

        public LambdaAction(String string, Icon icon, Consumer<ActionEvent> consumer) {
            super(string, icon);
            this.action = consumer;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            this.action.accept(actionEvent);
        }
    }

    @FunctionalInterface
    public static interface BackgroundSupplier<T> {
        public T get() throws Exception;
    }

    private static class SwingRunnable
    extends SwingWorker<Void, Void> {
        private BackgroundRunnable doInBackground;

        public SwingRunnable(BackgroundRunnable backgroundRunnable) {
            this.doInBackground = backgroundRunnable;
        }

        @Override
        protected Void doInBackground() throws Exception {
            this.doInBackground.run();
            return null;
        }
    }

    private static class SwingLambda<T, V>
    extends SwingWorker<T, V> {
        private BackgroundSupplier<T> doInBackground;
        private Consumer<T> done;
        private Consumer<Exception> error;
        private Runnable close;

        public SwingLambda(BackgroundSupplier<T> backgroundSupplier, Consumer<T> consumer, Consumer<Exception> consumer2, Runnable runnable) {
            this.doInBackground = backgroundSupplier;
            this.done = consumer;
            this.error = consumer2;
            this.close = runnable;
        }

        @Override
        protected T doInBackground() throws Exception {
            return this.doInBackground.get();
        }

        @Override
        protected void done() {
            try {
                if (!this.isCancelled() && this.done != null) {
                    this.done.accept(this.get());
                }
            }
            catch (Exception exception) {
                if (this.error != null) {
                    this.error.accept(exception);
                }
            }
            finally {
                if (this.close != null) {
                    this.close.run();
                }
            }
        }
    }

    public static class DragDropRowTableUI
    extends BasicTableUI {
        @Override
        protected MouseInputListener createMouseInputListener() {
            return new DragDropRowMouseInputHandler();
        }

        protected class DragDropRowMouseInputHandler
        extends BasicTableUI.MouseInputHandler {
            protected DragDropRowMouseInputHandler() {
                super();
            }

            @Override
            public void mouseDragged(MouseEvent mouseEvent) {
                if (DragDropRowTableUI.this.table.getDragEnabled() && DragDropRowTableUI.this.table.getSelectionModel().getSelectionMode() == 2) {
                    DragDropRowTableUI.this.table.getTransferHandler().exportAsDrag(DragDropRowTableUI.this.table, mouseEvent, 1);
                } else {
                    super.mouseDragged(mouseEvent);
                }
            }
        }
    }

    public static abstract class DynamicMenu
    extends JMenu
    implements MenuListener {
        public DynamicMenu(String string) {
            super(string);
            this.addMenuListener(this);
        }

        protected abstract void populate();

        @Override
        public void menuSelected(MenuEvent menuEvent) {
            this.removeAll();
            this.populate();
        }

        @Override
        public void menuDeselected(MenuEvent menuEvent) {
        }

        @Override
        public void menuCanceled(MenuEvent menuEvent) {
        }
    }
}

