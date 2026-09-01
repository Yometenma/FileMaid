package net.filemaid.ui.rename;

import com.cedarsoftware.util.io.JsonWriter;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Window;
import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;
import net.filemaid.History;
import net.filemaid.HistorySpooler;
import net.filemaid.Logging;
import net.filemaid.RenameAction;
import net.filemaid.ResourceManager;
import net.filemaid.StandardRenameAction;
import net.filemaid.UserInteraction;
import net.filemaid.media.NamingStandard;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.postprocess.FeedbackSpooler;
import net.filemaid.postprocess.Script;
import net.filemaid.similarity.Match;
import net.filemaid.ui.BaseDialog;
import net.filemaid.ui.HeaderPanel;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.rename.Mode;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.GlassOptionPane;
import net.filemaid.util.ui.GlassProgressMonitor;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class PostProcessScriptEditor
extends BaseDialog {
    private Result result = Result.CANCEL;
    private Script script = null;
    private HeaderPanel scriptNameHeader = new HeaderPanel();
    private RSyntaxTextArea codeEditor = this.createEditor();
    private final Map<File, Match<File, ?>> sampleModel = Collections.synchronizedMap(new LinkedHashMap());
    private final Action run = SwingUI.newAction("Run", ResourceManager.getIcon("script.go"), actionEvent -> SwingUI.withWaitCursor((Object)this, () -> {
        try {
            Script script = this.deriveScript();
            Runner runner = new Runner(script, this.sampleModel, StandardRenameAction.TEST);
            String string = GlassProgressMonitor.runTask(runner, this);
            RTextScrollPane rTextScrollPane = this.createOutputPane(string);
            rTextScrollPane.setMaximumSize(new Dimension(Math.max(this.getWidth() - 200, 200), Math.max(this.getHeight() - 200, 200)));
            GlassOptionPane.showAlertDialog((JComponent)rTextScrollPane, script.getName(), ResourceManager.getIcon("script.go"), this);
        }
        catch (Exception exception) {
            if (Logging.isCancellation(exception)) {
                return;
            }
            Logging.log.warning(Logging.cause(exception));
        }
    }));
    private final JButton ok = SwingUI.newButton("Save Script", ResourceManager.getIcon("dialog.continue"), actionEvent -> SwingUI.withWaitCursor((Object)this, () -> {
        try {
            this.script = this.deriveScript();
            if (this.script.getName().isEmpty()) {
                this.showScriptNameInputDialog();
                return;
            }
            this.result = Result.SET;
            this.setVisible(false);
        }
        catch (Exception exception) {
            Logging.log.warning(Logging.cause(exception));
        }
    }));
    private final JButton delete = SwingUI.newButton("Delete Script", ResourceManager.getIcon("dialog.cancel"), actionEvent -> {
        this.result = Result.DELETE;
        this.setVisible(false);
    });
    private final JButton help = SwingUI.newButton("Examples", ResourceManager.getIcon("script.palette"), actionEvent -> UserInteraction.browse("https://www.filebot.net/help/apply-script.html"));

    public PostProcessScriptEditor(Window window) {
        super(window, "Script Editor", Dialog.ModalityType.DOCUMENT_MODAL);
        JComponent jComponent = (JComponent)this.getContentPane();
        jComponent.setLayout((LayoutManager)new MigLayout("insets dialog, nogrid, fill, hidemode 3", "", "[grow][pref]"));
        jComponent.add((Component)this.scriptNameHeader, "wmin 150px, hmin 75px, growx, dock north");
        jComponent.add((Component)this.wrapEditor("Script", this.codeEditor), "grow, growprio 200, wrap");
        jComponent.add((Component)this.ok, "tag apply");
        jComponent.add((Component)this.delete, "tag cancel");
        jComponent.add((Component)SwingUI.createImageButton(this.run), "tag left, gap indent");
        jComponent.add((Component)this.help, "tag left, gap 15px");
        JLabel jLabel = this.scriptNameHeader.getTitleLabel();
        jLabel.setToolTipText("Edit Script Name");
        jLabel.setCursor(Cursor.getPredefinedCursor(2));
        jLabel.addMouseListener(SwingUI.mouseClicked(mouseEvent -> this.showScriptNameInputDialog()));
        SwingUI.installAction((JComponent)this.codeEditor, 116, this.run);
        SwingUI.installAction((JComponent)this.codeEditor, 82, 128, this.run);
        this.codeEditor.getPopupMenu().add((Component)new JMenuItem(this.run), 0);
        this.codeEditor.getPopupMenu().add((Component)new JPopupMenu.Separator(), 1);
        this.addWindowListener(SwingUI.windowOpened(windowEvent -> {
            if (jLabel.getText().isEmpty()) {
                this.showScriptNameInputDialog();
            }
        }));
        this.setSize(760, 600);
    }

    private void showScriptNameInputDialog() {
        JLabel jLabel = this.scriptNameHeader.getTitleLabel();
        String string = jLabel.getText();
        GlassOptionPane.showInputDialog("Script Name:", string.isEmpty() ? "New Script" : string, "Edit Name", ResourceManager.getIcon("search.literal"), jLabel, jLabel::setText);
    }

    private RSyntaxTextArea createEditor() {
        RSyntaxTextArea rSyntaxTextArea = new RSyntaxTextArea(){

            protected void appendFoldingMenu(JPopupMenu jPopupMenu) {
            }
        };
        try {
            Theme.load((InputStream)Theme.class.getResourceAsStream(ThemeSupport.getTheme().isDark() ? "themes/monokai.xml" : "themes/eclipse.xml")).apply(rSyntaxTextArea);
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
        rSyntaxTextArea.setSyntaxEditingStyle("text/groovy");
        rSyntaxTextArea.setAutoscrolls(false);
        rSyntaxTextArea.setAnimateBracketMatching(false);
        rSyntaxTextArea.setAntiAliasingEnabled(true);
        rSyntaxTextArea.setAutoIndentEnabled(true);
        rSyntaxTextArea.setBracketMatchingEnabled(true);
        rSyntaxTextArea.setCloseCurlyBraces(true);
        rSyntaxTextArea.setClearWhitespaceLinesEnabled(true);
        rSyntaxTextArea.setCodeFoldingEnabled(false);
        rSyntaxTextArea.setHighlightSecondaryLanguages(false);
        rSyntaxTextArea.setRoundedSelectionEdges(false);
        rSyntaxTextArea.setTabsEmulated(false);
        return rSyntaxTextArea;
    }

    private RTextScrollPane wrapEditor(String string, RSyntaxTextArea rSyntaxTextArea) {
        RTextScrollPane rTextScrollPane = new RTextScrollPane((RTextArea)rSyntaxTextArea, true);
        rTextScrollPane.setVerticalScrollBarPolicy(20);
        rTextScrollPane.setHorizontalScrollBarPolicy(30);
        rTextScrollPane.setBorder((Border)BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(string), rTextScrollPane.getBorder()));
        rTextScrollPane.setOpaque(false);
        return rTextScrollPane;
    }

    private RTextScrollPane createOutputPane(String string) {
        RSyntaxTextArea rSyntaxTextArea = this.createEditor();
        rSyntaxTextArea.setHighlightCurrentLine(false);
        rSyntaxTextArea.setEditable(false);
        rSyntaxTextArea.setAutoscrolls(true);
        rSyntaxTextArea.setSyntaxEditingStyle("text/hosts");
        rSyntaxTextArea.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        rSyntaxTextArea.setText(string);
        return this.wrapEditor("Log", rSyntaxTextArea);
    }

    public void setScript(Script script) {
        this.script = script;
        this.help.setVisible(script.getIdentifier() == null);
        this.scriptNameHeader.getTitleLabel().setText(script.getName());
        this.codeEditor.setText(script.getCode());
        int n = script.getCode().indexOf(9);
        if (n > 0) {
            this.codeEditor.setCaretPosition(n + 1);
        }
    }

    public Optional<Script> getScript() {
        return Optional.ofNullable(this.script);
    }

    private String generateScriptIdentifier() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    public Script deriveScript() throws Exception {
        String string;
        String string2;
        String string3 = Optional.ofNullable(this.script).map(Script::getIdentifier).orElseGet(this::generateScriptIdentifier);
        Script script = new Script(string3, string2 = this.scriptNameHeader.getTitleLabel().getText(), string = this.codeEditor.getText());
        String string4 = JsonWriter.objectToJson((Object)script);
        if (string4.length() > 8192) {
            throw new IllegalStateException("8192 character limit exceeded");
        }
        script.compile();
        return script;
    }

    public Result getResult() {
        return this.result;
    }

    static enum Result {
        SET,
        DELETE,
        CANCEL;

    }

    private static class Runner
    implements GlassProgressMonitor.ProgressWorker<String> {
        private final Script script;
        private final Map<File, Match<File, ?>> model;
        private final RenameAction action;

        public Runner(Script script, Map<File, Match<File, ?>> map, RenameAction renameAction) {
            this.script = script;
            this.model = map;
            this.action = renameAction;
        }

        @Override
        public String getName() {
            return this.script.getName();
        }

        @Override
        public Icon getIcon() {
            return ResourceManager.getIcon("script.go");
        }

        @Override
        public String getDescription() {
            return "Running...";
        }

        @Override
        public boolean isIndeterminate() {
            return true;
        }

        @Override
        public String call(Consumer<String> consumer, Consumer<String> consumer2, BiConsumer<Integer, Integer> biConsumer, Supplier<Boolean> supplier) throws Exception {
            FeedbackSpooler feedbackSpooler = new FeedbackSpooler(consumer2, biConsumer, supplier);
            if (this.model.isEmpty()) {
                consumer2.accept("Reading history...");
                this.model.putAll(Runner.createSampleModel(supplier));
                consumer2.accept(this.getDescription());
            }
            feedbackSpooler.trace("Run Test", this.script.getName());
            AtomicInteger atomicInteger = new AtomicInteger(0);
            this.model.forEach((file, match) -> {
                feedbackSpooler.trace("model.source[" + atomicInteger + "] = " + match.getValue(), this.script.getName());
                feedbackSpooler.trace("model.target[" + atomicInteger + "] = " + file, this.script.getName());
                feedbackSpooler.trace("model.object[" + atomicInteger + "] = " + match.getCandidate(), this.script.getName());
                atomicInteger.incrementAndGet();
            });
            this.script.apply(Collections.unmodifiableMap(this.model), this.action, feedbackSpooler);
            feedbackSpooler.trace("DONE", this.script.getName());
            return feedbackSpooler.messages().collect(Collectors.joining("\n"));
        }

        public static Map<File, Match<File, ?>> createSampleModel(Supplier<Boolean> supplier) {
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            HistorySpooler.HISTORY.getCompleteHistory().split(History.DATE_DESCENDING).map(History::getRenameMap).findFirst().ifPresent(map2 -> map2.forEach((arg_0, arg_1) -> Runner.lambda$createSampleModel$1((Supplier)supplier, linkedHashMap, arg_0, arg_1)));
            if (linkedHashMap.isEmpty()) {
                Object object = Mode.Episode.getDefaultSampleObject();
                File file = new File(FileUtilities.UNIX ? "/source/" : "X:/source/", object + ".mkv");
                File file2 = new File(FileUtilities.UNIX ? "/target/" : "X:/target/", NamingStandard.Default.getPath(object).getPath() + ".mkv");
                linkedHashMap.put(file2, new Match<File, Object>(file, object));
            }
            return linkedHashMap;
        }

        private static /* synthetic */ void lambda$createSampleModel$1(Supplier supplier, Map map, File file, File file2) {
            Object object;
            if (((Boolean)supplier.get()).booleanValue()) {
                throw new CancellationException();
            }
            if (file2.exists() && (object = XattrMetaInfo.xattr.getMetaInfo(file2)) != null) {
                map.put(file2, new Match<File, Object>(file, object));
            }
        }
    }
}

