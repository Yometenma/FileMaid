package net.filemaid.ui.console;

import groovy.transform.ThreadInterrupt;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.SimpleBindings;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.text.JTextComponent;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.Settings;
import net.filemaid.UserData;
import net.filemaid.UserInteraction;
import net.filemaid.cli.ArgumentProcessor;
import net.filemaid.cli.CmdlineOperations;
import net.filemaid.cli.ScriptShell;
import net.filemaid.cli.ScriptShellMethods;
import net.filemaid.cli.ScriptSource;
import net.filemaid.ui.BaseFrame;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.StringUtilities;
import net.filemaid.util.TeePrintStream;
import net.filemaid.util.ui.SwingUI;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.fife.ui.rsyntaxtextarea.FileLocation;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class GroovyPad
extends BaseFrame {
    private static final String SYSINFO = "runScript 'sysinfo'";
    protected MessageConsole console;
    protected TextEditorPane editor;
    protected TextEditorPane output;
    protected final ScriptShell shell;
    protected final Action run = SwingUI.newToolBarAction("Run", ResourceManager.getIcon("script.go"), actionEvent -> this.runScript());
    protected final Action cancel = SwingUI.newToolBarAction("Cancel", ResourceManager.getIcon("script.cancel"), actionEvent -> this.cancelScript());
    private Runner currentRunner = null;

    public GroovyPad(Window window, boolean bl) throws IOException {
        super("Groovy Pad");
        RTextScrollPane rTextScrollPane = this.createEditor(Theme.load((InputStream)Theme.class.getResourceAsStream(ThemeSupport.getTheme().isDark() ? "themes/monokai.xml" : "themes/eclipse.xml")));
        RTextScrollPane rTextScrollPane2 = this.createOutputLog(Theme.load((InputStream)Theme.class.getResourceAsStream("themes/dark.xml")));
        JSplitPane jSplitPane = new JSplitPane(0, true, (Component)rTextScrollPane, (Component)rTextScrollPane2);
        jSplitPane.setResizeWeight(0.35);
        JComponent jComponent = (JComponent)this.getContentPane();
        jComponent.setLayout(new BorderLayout(0, 0));
        jComponent.add((Component)jSplitPane, "Center");
        JToolBar jToolBar = new JToolBar("Run", 0);
        jToolBar.setFloatable(false);
        jToolBar.add(this.run);
        jToolBar.add(this.cancel);
        jToolBar.addSeparator();
        jToolBar.add(SwingUI.newToolBarAction("View System Information", ResourceManager.getIcon("status.info"), actionEvent -> this.runScript(SYSINFO)));
        jToolBar.add(SwingUI.newToolBarAction("View Error Log", ResourceManager.getIcon("status.warning"), actionEvent -> this.displayErrorLog(UserData.getErrorLog())));
        jToolBar.addSeparator();
        jToolBar.add(SwingUI.newToolBarAction("Application Data", ResourceManager.getIcon("window.icon16"), actionEvent -> UserInteraction.showUserDataPopup(actionEvent)));
        jComponent.add((Component)jToolBar, "North");
        this.run.setEnabled(true);
        this.cancel.setEnabled(false);
        SwingUI.installAction(jComponent, 116, this.run);
        SwingUI.installAction(jComponent, 82, 128, this.run);
        SwingUI.installAction(jComponent, 27, SwingUI.newAction("Close", actionEvent -> this.dispose()));
        this.addWindowListener(SwingUI.windowClosed(windowEvent -> {
            try {
                this.cancelScript();
                this.editor.save();
                this.console.unhook();
            }
            catch (Exception exception) {
                Logging.trace(exception);
            }
        }));
        this.console = new MessageConsole((JTextComponent)this.output);
        this.console.hook();
        this.shell = this.createScriptShell();
        this.editor.requestFocusInWindow();
        this.setModalExclusionType(Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
        this.setDefaultCloseOperation(2);
        this.setIconImages(ResourceManager.getApplicationIconImages());
        this.setBounds(window.getBounds());
        this.setFileLocation(UserData.getGroovyPad());
        if (bl) {
            this.runScript(SYSINFO);
        } else {
            this.displayErrorLog(UserData.getErrorLog());
        }
    }

    protected RTextScrollPane createEditor(Theme theme) {
        this.editor = new TextEditorPane(0, false);
        theme.apply((RSyntaxTextArea)this.editor);
        this.editor.setSyntaxEditingStyle("text/groovy");
        this.editor.setAutoscrolls(false);
        this.editor.setAnimateBracketMatching(false);
        this.editor.setAntiAliasingEnabled(true);
        this.editor.setAutoIndentEnabled(true);
        this.editor.setBracketMatchingEnabled(true);
        this.editor.setCloseCurlyBraces(true);
        this.editor.setClearWhitespaceLinesEnabled(true);
        this.editor.setCodeFoldingEnabled(true);
        this.editor.setHighlightSecondaryLanguages(false);
        this.editor.setRoundedSelectionEdges(false);
        this.editor.setTabsEmulated(false);
        return new RTextScrollPane((RTextArea)this.editor, true);
    }

    public void setFileLocation(File file) {
        if (file.length() <= 0L) {
            try {
                ScriptShellMethods.saveAs(SYSINFO, file);
            }
            catch (Exception exception) {
                Logging.trace(exception);
            }
        }
        try {
            this.editor.load(FileLocation.create((File)file), StandardCharsets.UTF_8);
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
    }

    protected RTextScrollPane createOutputLog(Theme theme) throws IOException {
        this.output = new TextEditorPane(0, false);
        theme.apply((RSyntaxTextArea)this.output);
        this.output.setEditable(false);
        this.output.setReadOnly(true);
        this.output.setAutoscrolls(true);
        this.output.setSyntaxEditingStyle("text/hosts");
        this.output.setAnimateBracketMatching(false);
        this.output.setAntiAliasingEnabled(true);
        this.output.setAutoIndentEnabled(false);
        this.output.setBracketMatchingEnabled(false);
        this.output.setCloseCurlyBraces(false);
        this.output.setClearWhitespaceLinesEnabled(false);
        this.output.setCodeFoldingEnabled(false);
        this.output.setHighlightCurrentLine(false);
        this.output.setHighlightSecondaryLanguages(false);
        this.output.setRoundedSelectionEdges(false);
        this.output.setTabsEmulated(false);
        this.output.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        this.createDebugDropTarget((JComponent)this.output);
        RTextScrollPane rTextScrollPane = new RTextScrollPane((RTextArea)this.output, true);
        rTextScrollPane.setBorder(BorderFactory.createEmptyBorder());
        return rTextScrollPane;
    }

    protected DropTarget createDebugDropTarget(JComponent jComponent) {
        return new DropTarget(jComponent, new DropTargetAdapter(){

            @Override
            public void drop(DropTargetDropEvent dropTargetDropEvent) {
                dropTargetDropEvent.acceptDrop(1);
                try {
                    GroovyPad.this.console.clear();
                    GroovyPad.this.console.println("DROP " + dropTargetDropEvent);
                    GroovyPad.this.console.println("----------------------------------------");
                    Transferable transferable = dropTargetDropEvent.getTransferable();
                    Stream.of(DataFlavor.stringFlavor, DataFlavor.javaFileListFlavor).forEach(dataFlavor -> {
                        if (transferable.isDataFlavorSupported((DataFlavor)dataFlavor)) {
                            try {
                                GroovyPad.this.console.println("FLAVOR " + dataFlavor);
                                GroovyPad.this.console.println("DATA " + transferable.getTransferData((DataFlavor)dataFlavor));
                                GroovyPad.this.console.println("----------------------------------------");
                            }
                            catch (Exception exception) {
                                GroovyPad.this.console.println(exception);
                            }
                        }
                    });
                    Stream.of(transferable.getTransferDataFlavors()).forEach(dataFlavor -> {
                        if (dataFlavor.isRepresentationClassCharBuffer()) {
                            try {
                                GroovyPad.this.console.println("FLAVOR " + dataFlavor);
                                GroovyPad.this.console.println("DATA " + transferable.getTransferData((DataFlavor)dataFlavor));
                                GroovyPad.this.console.println("----------------------------------------");
                            }
                            catch (Exception exception) {
                                GroovyPad.this.console.println(exception);
                            }
                        }
                    });
                    Stream.of(transferable.getTransferDataFlavors()).forEach(dataFlavor -> GroovyPad.this.console.println("FLAVOR " + dataFlavor));
                }
                catch (Exception exception) {
                    GroovyPad.this.console.println(exception);
                }
                finally {
                    GroovyPad.this.console.flush();
                }
                dropTargetDropEvent.dropComplete(true);
            }
        });
    }

    public void displayErrorLog(File file) {
        this.console.clear();
        this.console.append("# " + file + "\n");
        SwingUI.onSwingWorker(() -> {
            Logging.flushLog();
            return file.exists() ? FileUtilities.readTextFile(file) : "";
        }, string -> this.console.append((String)string), exception -> this.console.append(exception.getMessage()));
    }

    protected ScriptShell createScriptShell() {
        try {
            return new ScriptShell(ScriptSource.GITHUB_STABLE.getResolver(null), new CmdlineOperations(), Collections.emptyMap(), Cache.getCache("script_classes", CacheType.Daily), new CompilationCustomizer[]{new ASTTransformationCustomizer(ThreadInterrupt.class)});
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public void runScript() {
        try {
            this.editor.save();
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
        this.runScript(this.editor.getText().trim());
    }

    public void runScript(String string) {
        if (this.currentRunner == null || this.currentRunner.isDone()) {
            this.currentRunner = new Runner(string){

                @Override
                protected void done() {
                    GroovyPad.this.run.setEnabled(true);
                    GroovyPad.this.cancel.setEnabled(false);
                }
            };
            this.run.setEnabled(false);
            this.cancel.setEnabled(true);
            this.console.clear();
            this.currentRunner.execute();
        }
    }

    protected void cancelScript() {
        if (this.currentRunner != null && !this.currentRunner.isDone()) {
            this.currentRunner.cancel(true);
        }
    }

    public static void open(Window window, boolean bl) {
        SwingUI.withWaitCursor((Object)window, () -> {
            GroovyPad groovyPad = new GroovyPad(window, bl);
            groovyPad.addWindowListener(SwingUI.windowOpened(windowEvent -> window.setVisible(false)));
            groovyPad.addWindowListener(SwingUI.windowClosed(windowEvent -> SwingUI.showLater(window)));
            SwingUI.showLater(groovyPad);
        });
    }

    protected class Runner
    extends SwingWorker<Object, Object> {
        private final String script;

        public Runner(String string) {
            this.script = string;
        }

        @Override
        protected Object doInBackground() throws Exception {
            try {
                ArgumentProcessor argumentProcessor = ArgumentProcessor.parseCommand(RegularExpressions.SPACE.splitAsStream(this.script).collect(Collectors.toList()));
                if (argumentProcessor != null) {
                    int n = argumentProcessor.run();
                    GroovyPad.this.console.println("Exit Code: " + n);
                    Object var3_5 = null;
                    return var3_5;
                }
                SimpleBindings simpleBindings = new SimpleBindings();
                simpleBindings.put("__args", (Object)Settings.getApplicationArguments());
                simpleBindings.put("args", (Object)Settings.getApplicationArguments().getFileArguments());
                Object object = GroovyPad.this.shell.evaluate(this.script, simpleBindings);
                if (object != null) {
                    GroovyPad.this.console.println("Result: " + object);
                } else {
                    GroovyPad.this.console.println(null);
                }
            }
            catch (Throwable throwable) {
                GroovyPad.this.console.trace(Logging.getRootCause(throwable));
            }
            finally {
                GroovyPad.this.console.flush();
            }
            return null;
        }
    }

    public static class MessageConsole {
        public final int MAX_LENGTH = 1000000;
        private final TeePrintStream out = TeePrintStream.pipe(new ConsoleOutputStream(), StandardCharsets.UTF_8, System.out);
        private final TeePrintStream err = TeePrintStream.pipe(new ConsoleOutputStream(), StandardCharsets.UTF_8, System.err);
        private final Handler evt = new ConsoleLogHandler();
        private final Level level = Logging.log.getLevel();
        private final Handler[] handlers = Logging.log.getHandlers();
        private JTextComponent textComponent;

        public MessageConsole(JTextComponent jTextComponent) {
            this.textComponent = jTextComponent;
        }

        public void clear() {
            this.textComponent.setText("");
        }

        public void append(String string) {
            if (this.textComponent.getDocument().getLength() > 1000000) {
                this.textComponent.setText("");
            }
            try {
                int n = this.textComponent.getDocument().getLength();
                this.textComponent.getDocument().insertString(n, string, null);
                this.textComponent.setCaretPosition(this.textComponent.getDocument().getLength());
            }
            catch (Exception exception) {
                this.trace(exception);
            }
        }

        public void println(Object object) {
            this.out.println(object == null ? "" : object.toString());
        }

        public void trace(Throwable throwable) {
            throwable.printStackTrace(this.out);
        }

        public void hook() {
            System.setOut(this.out);
            System.setErr(this.err);
            Arrays.stream(this.handlers).forEach(Logging.log::removeHandler);
            Logging.log.setLevel(Level.ALL);
            Logging.log.addHandler(this.evt);
        }

        public void unhook() {
            System.setOut(this.out.getSink());
            System.setErr(this.err.getSink());
            Logging.log.setLevel(this.level);
            Logging.log.removeHandler(this.evt);
            Arrays.stream(this.handlers).forEach(Logging.log::addHandler);
        }

        public void flush() {
            this.out.flush();
            this.err.flush();
        }

        private class ConsoleOutputStream
        extends ByteArrayOutputStream {
            private ConsoleOutputStream() {
            }

            @Override
            public void flush() {
                String string = StringUtilities.printable(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(this.buf, 0, this.count)));
                this.reset();
                SwingUtilities.invokeLater(() -> MessageConsole.this.append(string));
            }
        }

        private class ConsoleLogHandler
        extends Handler {
            public ConsoleLogHandler() {
                this.setLevel(Level.ALL);
                this.setFormatter(new Logging.ConsoleFormatter(null, true, false, null));
            }

            @Override
            public void publish(LogRecord logRecord) {
                String string = this.getFormatter().format(logRecord);
                SwingUtilities.invokeLater(() -> MessageConsole.this.append(string));
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        }
    }
}

