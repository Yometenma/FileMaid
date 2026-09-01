package net.filemaid.ui.rename;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.script.ScriptException;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import net.filemaid.CategoryFileFilter;
import net.filemaid.InvalidInputException;
import net.filemaid.Logging;
import net.filemaid.Parallelism;
import net.filemaid.ResourceManager;
import net.filemaid.Settings;
import net.filemaid.UserFiles;
import net.filemaid.UserInteraction;
import net.filemaid.format.BindingException;
import net.filemaid.format.ExpressionFileFormat;
import net.filemaid.format.MediaBindingBean;
import net.filemaid.format.SuppressedThrowables;
import net.filemaid.media.MetaAttributes;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.ui.BaseDialog;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.rename.BindingDialog;
import net.filemaid.ui.rename.FormatExpressionTextArea;
import net.filemaid.ui.rename.Mode;
import net.filemaid.ui.rename.ModeSpinner;
import net.filemaid.ui.rename.Preset;
import net.filemaid.ui.rename.UserPresets;
import net.filemaid.util.DefaultThreadFactory;
import net.filemaid.util.ExtensionFileFilter;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.StringUtilities;
import net.filemaid.util.SystemProperty;
import net.filemaid.util.ui.LinkButton;
import net.filemaid.util.ui.ProgressIndicator;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.util.ui.notification.SeparatorBorder;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class FormatDialog
extends BaseDialog {
    private boolean submit = false;
    private Mode mode;
    private MediaBindingBean sample = null;
    private boolean lockSample = false;
    private ExecutorService executor = this.createExecutor();
    private RunnableFuture<String> currentPreviewFuture;
    private JLabel preview = new JLabel();
    private JLabel status = new JLabel();
    private FormatExpressionTextArea editor = new FormatExpressionTextArea();
    private ProgressIndicator progressIndicator = ThemeSupport.getProgressIndicator();
    private JLabel title = new JLabel();
    private JPanel help = new JPanel((LayoutManager)new MigLayout("insets 0, nogrid, novisualpadding, fillx"));
    private final Action changeSampleAction = SwingUI.newAction("View Bindings", ResourceManager.getIcon("action.variables"), actionEvent -> {
        BindingDialog bindingDialog = new BindingDialog(SwingUI.getWindow(actionEvent.getSource()), this.mode, this.sample, !this.lockSample);
        bindingDialog.setLocationRelativeTo((Component)actionEvent.getSource());
        bindingDialog.setVisible(true);
        if (bindingDialog.submit()) {
            Object object = bindingDialog.getInfoObject();
            File file = bindingDialog.getMediaFile();
            this.sample = this.mode != Mode.File ? new MediaBindingBean(object, file, Collections.singletonMap(file, object)) : new MediaBindingBean(file, file, Collections.singletonMap(file, file));
            try {
                if (this.mode != Mode.File) {
                    this.mode.persistentSample().setValue(object == null ? "" : MetaAttributes.toJson(object, false));
                }
                Mode.File.persistentSample().setValue(file == null ? "" : this.sample.getFileObject().getAbsolutePath());
            }
            catch (Exception exception) {
                Logging.trace(exception);
            }
            this.fireSampleChanged();
        }
    });
    private final Action selectFolderAction = SwingUI.newAction("Select Output Folder", ResourceManager.getIcon("action.load"), actionEvent -> {
        File file;
        if (SwingUI.isShiftOrAltDown(actionEvent)) {
            List<File> list = UserFiles.showLoadDialogSelectFiles(false, false, null, new CategoryFileFilter("Format", new ExtensionFileFilter("groovy")), "Select Format File", actionEvent);
            if (list != null && !list.isEmpty()) {
                String string = this.getExpression();
                String string2 = "@" + list.get(0);
                this.editor.setText(String.join((CharSequence)"\n", string, string2));
            }
            return;
        }
        String string = this.getExpression();
        File object = null;
        if (string.length() > 0) {
            File fileParent = null;
            file = new File(string);
            if (file.isAbsolute()) {
                for (File file2 : FileUtilities.listPath(file)) {
                    if (fileParent != null && !file2.exists()) {
                        object = fileParent;
                        string = string.substring(fileParent.getPath().length() + 1);
                        break;
                    }
                    fileParent = file2;
                }
            }
            Matcher matcher = Pattern.compile("\\s*(?:^[~]|[{]\\s*(?:drive|home|folder|output)\\s*[}])\\s*[\\\\/]+", 2).matcher(string);
            if (matcher.find()) {
                string = string.substring(matcher.end());
            }
        }
        if ((file = UserFiles.showOpenDialogSelectFolder(object, "Select Folder", actionEvent)) != null) {
            this.editor.setText(FileUtilities.normalizePathSeparators(file.getAbsolutePath()) + "/" + string);
        }
    });
    private final Action showRecentAction = SwingUI.newAction("Recent Formats", ResourceManager.getIcon("action.paste"), actionEvent -> SwingUI.showDropDown(SwingUI.newPopupMenu("Recent Formats", jPopupMenu -> {
        List<String> list = this.mode.persistentFormatHistory().stream().collect(Collectors.toList());
        List<Preset> list2 = UserPresets.USER_PRESETS.list().filter(preset -> this.mode == Mode.getMode(preset.getDatasource()) && preset.getFormatExpression() != null).collect(Collectors.toList());
        if (list.isEmpty() && list2.isEmpty()) {
            jPopupMenu.add("No recent formats").setEnabled(false);
            return;
        }
        list.forEach(string -> jPopupMenu.add(SwingUI.newAction(string, evt -> this.setFormatCode((String)string))).setFont(new Font("Monospaced", 0, 11)));
        if (list.size() > 0 && SwingUI.isShiftOrAltDown(actionEvent)) {
            jPopupMenu.addSeparator();
            jPopupMenu.add(SwingUI.newAction("Clear recent formats", ResourceManager.getIcon("edit.clear"), evt -> this.mode.persistentFormatHistory().clear()));
            return;
        }
        if (list.size() > 0 && list2.size() > 0) {
            jPopupMenu.addSeparator();
        }
        list2.forEach(preset -> jPopupMenu.add(SwingUI.newAction(preset.getName(), preset.getIcon(), evt -> this.setFormatCode(preset.getFormatExpression()))));
    }), actionEvent));
    private final Action cancelAction = SwingUI.newAction("Cancel", ResourceManager.getIcon("dialog.cancel"), actionEvent -> this.finish(false));
    private final Action approveFormatAction = SwingUI.newAction("Use Format", ResourceManager.getIcon("dialog.continue"), actionEvent -> {
        try {
            String string = this.getExpression();
            if (string.isEmpty()) {
                throw new ScriptException("Expression is empty");
            }
            if (string.length() > 8192) {
                throw new ScriptException("8192 character limit exceeded");
            }
            new ExpressionFileFormat(string);
            LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>();
            linkedHashSet.add(string);
            linkedHashSet.addAll(this.mode.persistentFormatHistory());
            this.mode.persistentFormatHistory().set(linkedHashSet.stream().filter(Objects::nonNull).limit(RECENT_LIMIT).collect(Collectors.toList()));
            this.finish(true);
        }
        catch (Exception exception) {
            Logging.log.warning(Logging.cause(exception));
        }
    });
    private static final String SAMPLE_FILE_NOT_SET_MESSAGE = "Sample file has not been set. Click \"View Bindings\" and select a sample file.";
    private static final String SAMPLE_PROPERTY = "sample";
    private static final int RECENT_LIMIT = SystemProperty.get("net.filemaid.format.recent.limit", Integer::parseInt, 8);

    public FormatDialog(Window window, Mode mode, boolean bl, String string, MediaBindingBean mediaBindingBean, boolean bl2) {
        super(window, "Format Editor");
        this.lockSample = bl2;
        this.title.setFont(this.title.getFont().deriveFont(1));
        JPanel jPanel = new JPanel((LayoutManager)new MigLayout("insets dialog, nogrid, novisualpadding"));
        jPanel.setBackground(ThemeSupport.getPanelBackground());
        jPanel.setBorder(ThemeSupport.getSeparatorBorder(SeparatorBorder.Position.BOTTOM));
        jPanel.add((Component)this.progressIndicator, "pos 1al 0al, hidemode 3");
        jPanel.add((Component)this.title, "wmin 150px, wrap unrel:push");
        jPanel.add((Component)this.preview, "wmin 150px, hmin 16px, gap indent, hidemode 3, wmax 90%");
        jPanel.add((Component)this.status, "wmin 150px, hmin 16px, gap indent, hidemode 3, wmax 90%, newline");
        JPanel jPanel2 = new JPanel((LayoutManager)new MigLayout("insets dialog, nogrid, fill, hidemode 1"));
        RTextScrollPane rTextScrollPane = new RTextScrollPane((RTextArea)this.editor, false);
        rTextScrollPane.setLineNumbersEnabled(false);
        rTextScrollPane.setFoldIndicatorEnabled(false);
        rTextScrollPane.setIconRowHeaderEnabled(false);
        rTextScrollPane.setVerticalScrollBarPolicy(20);
        rTextScrollPane.setHorizontalScrollBarPolicy(31);
        rTextScrollPane.setOpaque(true);
        this.editor.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6));
        rTextScrollPane.setBorder(ThemeSupport.getEditorBorder());
        rTextScrollPane.setBackground(ThemeSupport.getEditorBackground());
        rTextScrollPane.setMinimumSize(new Dimension(300, rTextScrollPane.getPreferredSize().height));
        jPanel2.add((Component)rTextScrollPane, "w 120px:min(pref, 420px), h pref, growx, shrinkprio 0, wrap 6px, id editor");
        if (ThemeSupport.getTheme().isDark()) {
            jPanel2.add((Component)SwingUI.createImageButton(this.changeSampleAction), "sg action, w 32!, h 28!, pos n editor.y2+1 editor.x2 n");
            jPanel2.add((Component)SwingUI.createImageButton(this.selectFolderAction), "sg action, w 32!, h 28!, pos n editor.y2+1 editor.x2-(30*1) n");
            jPanel2.add((Component)SwingUI.createImageButton(this.showRecentAction), "sg action, w 32!, h 28!, pos n editor.y2+1 editor.x2-(30*2) n");
        } else {
            jPanel2.add((Component)SwingUI.createImageButton(this.changeSampleAction), "sg action, w 28!, h 24!, pos n editor.y2+2 editor.x2 n");
            jPanel2.add((Component)SwingUI.createImageButton(this.selectFolderAction), "sg action, w 28!, h 24!, pos n editor.y2+2 editor.x2-(30*1) n");
            jPanel2.add((Component)SwingUI.createImageButton(this.showRecentAction), "sg action, w 28!, h 24!, pos n editor.y2+2 editor.x2-(30*2) n");
        }
        jPanel2.add((Component)this.help, "growx, wrap 25px:push");
        ModeSpinner modeSpinner = new ModeSpinner(mode, !bl);
        modeSpinner.addPropertyChangeListener("value", propertyChangeEvent -> this.setState((Mode)((Object)((Object)propertyChangeEvent.getNewValue())), null, null));
        jPanel2.add((Component)modeSpinner, "tag left");
        jPanel2.add((Component)SwingUI.newButton(this.approveFormatAction), "tag apply");
        jPanel2.add((Component)SwingUI.newButton(this.cancelAction), "tag cancel");
        JComponent jComponent = (JComponent)this.getContentPane();
        jComponent.setLayout((LayoutManager)new MigLayout("insets 0, fill"));
        jComponent.add((Component)jPanel, "h 60px, growx, dock north");
        jComponent.add((Component)jPanel2, "grow");
        this.preview.setCursor(Cursor.getPredefinedCursor(12));
        this.preview.addMouseListener(SwingUI.mouseClicked(mouseEvent -> UserInteraction.copy(this.preview.getText())));
        this.status.setCursor(Cursor.getPredefinedCursor(12));
        this.status.addMouseListener(SwingUI.mouseClicked(mouseEvent -> {
            if (SAMPLE_FILE_NOT_SET_MESSAGE.equals(this.status.getText())) {
                this.changeSampleAction.actionPerformed(new ActionEvent(this, 1001, SAMPLE_FILE_NOT_SET_MESSAGE));
                return;
            }
            UserInteraction.copy(this.status.getText());
        }));
        this.editor.onChange(0, documentEvent -> this.setHelpVisible());
        this.editor.onChange(documentEvent -> this.checkFormatInBackground());
        this.addPropertyChangeListener(SAMPLE_PROPERTY, propertyChangeEvent -> {
            if (Settings.isMacSandbox() && this.sample != null && this.sample.getFileObject() != null && this.sample.getFileObject().exists()) {
                MacAppUtilities.askUnlockFolders(SwingUI.getWindow(propertyChangeEvent.getSource()), Collections.singleton(this.sample.getFileObject()));
            }
            this.checkFormatInBackground();
        });
        this.addWindowListener(new WindowAdapter(){

            @Override
            public void windowGainedFocus(WindowEvent windowEvent) {
                SwingUtilities.invokeLater(() -> FormatDialog.this.editor.requestFocusInWindow());
            }

            @Override
            public void windowActivated(WindowEvent windowEvent) {
            }

            @Override
            public void windowClosing(WindowEvent windowEvent) {
                SwingUtilities.invokeLater(() -> FormatDialog.this.finish(false));
            }
        });
        this.setDefaultCloseOperation(0);
        this.setMinimumSize(new Dimension(680, 560));
        this.setState(mode, string, mediaBindingBean);
        SwingUI.installAction(this, 112, SwingUI.newAction("Help", actionEvent -> this.help.setVisible(!this.help.isVisible())));
    }

    public void setState(Mode mode, String string, MediaBindingBean mediaBindingBean) {
        this.mode = mode;
        this.setTitle(mode + " Format");
        this.title.setText(mode + " Format");
        this.preview.setText("");
        this.status.setText("");
        this.status.setVisible(false);
        this.updateHelpPanel(mode);
        this.sample = mediaBindingBean != null ? mediaBindingBean : this.restoreSample(mode);
        this.setFormatCode(string != null ? string : mode.getSelectedFormatExpression());
        this.fireSampleChanged();
    }

    private JComponent updateHelpPanel(Mode mode) {
        this.help.removeAll();
        if (this.help.isVisible()) {
            this.help.add((Component)new JLabel("Syntax"), "gap indent+unrel, wrap 0");
            this.help.add((Component)this.createSyntaxPanel(mode), "gapx indent indent, wrap 8px");
            this.help.add((Component)new JLabel("Examples"), "gap indent+unrel, wrap 0");
            this.help.add((Component)this.createExamplesPanel(mode), "growx, h pref!, gapx indent indent");
        }
        return this.help;
    }

    public void setFormatCode(String string) {
        try {
            this.editor.setText(string);
            if (string != null && !string.isEmpty()) {
                this.editor.scrollRectToVisible(new Rectangle(0, 0));
                this.editor.setCaretPosition(this.editor.getText().length());
                this.editor.requestFocusInWindow();
                this.setHelpVisible();
            }
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(exception));
        }
    }

    private void setHelpVisible() {
        this.help.setVisible(StringUtilities.lineCount(this.editor.getText()) <= 1L);
    }

    private JComponent createSyntaxPanel(Mode mode) {
        JPanel jPanel = new JPanel((LayoutManager)new MigLayout("fill, nogrid, novisualpadding", "[pref]", "[fill, min]"));
        jPanel.setBorder(ThemeSupport.getHelpPanelBorder());
        jPanel.setBackground(ThemeSupport.getHelpPanelBackground());
        jPanel.setOpaque(true);
        jPanel.add((Component)new LinkButton(SwingUI.newAction(ResourceBundle.getBundle(FormatDialog.class.getName()).getString(mode.key() + ".syntax"), actionEvent -> UserInteraction.browse(ResourceBundle.getBundle(FormatDialog.class.getName()).getString("help.url")))), "h min!");
        return jPanel;
    }

    private JComponent createExamplesPanel(Mode mode) {
        JPanel jPanel = new JPanel((LayoutManager)new MigLayout("fill, wrap 3"));
        jPanel.setBorder(ThemeSupport.getHelpPanelBorder());
        jPanel.setBackground(ThemeSupport.getHelpPanelBackground());
        jPanel.setOpaque(true);
        for (String string : mode.getSampleExpressions()) {
            LinkButton linkButton = new LinkButton(SwingUI.newAction(string, actionEvent -> this.setFormatCode((String)string)));
            linkButton.setFont(new Font("Monospaced", 0, 11));
            JLabel jLabel = new JLabel("[evaluate]");
            this.addPropertyChangeListener(SAMPLE_PROPERTY, propertyChangeEvent -> SwingUI.onSwingWorker(() -> new ExpressionFileFormat(string).format(this.sample), result -> jLabel.setText(FileUtilities.abbreviatePath(new File((String)result))), exception -> jLabel.setText(Logging.cause(exception).toString())));
            jPanel.add(linkButton);
            jPanel.add(new JLabel("\u2026"));
            jPanel.add((Component)jLabel, "wmin 150px");
        }
        return jPanel;
    }

    protected MediaBindingBean restoreSample(Mode mode) {
        String string;
        Object object = null;
        File file = null;
        Map<File, Object> map = Collections.emptyMap();
        try {
            if (mode != Mode.File) {
                object = MetaAttributes.toObject(mode.persistentSample().getValue());
            }
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
        if (object == null) {
            object = mode.getDefaultSampleObject();
        }
        if ((string = Mode.File.persistentSample().getValue()) != null && !string.isEmpty()) {
            file = new File(string);
            if (mode == Mode.File) {
                object = file;
            }
            map = Collections.singletonMap(file, object);
        }
        return new MediaBindingBean(object, file, map);
    }

    private ExecutorService createExecutor() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1), new DefaultThreadFactory("Preview", Parallelism.THREAD_POOL_PRIORITY.min(), false));
        threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        return threadPoolExecutor;
    }

    private void checkFormatInBackground() {
        try {
            final ExpressionFileFormat expressionFileFormat = new ExpressionFileFormat(this.getExpression());
            final Timer timer = SwingUI.invokeLater(400, () -> this.progressIndicator.setVisible(true));
            RunnableFuture<String> runnableFuture = this.currentPreviewFuture;
            this.currentPreviewFuture = new SwingWorker<String, Void>(){

                @Override
                protected String doInBackground() throws Exception {
                    return expressionFileFormat.format(FormatDialog.this.sample);
                }

                @Override
                protected void done() {
                    try {
                        FormatDialog.this.preview.setText(FileUtilities.abbreviatePath(new File((String)this.get())));
                        if (expressionFileFormat.suppressed() != null) {
                            throw expressionFileFormat.suppressed();
                        }
                        FormatDialog.this.status.setText("");
                        FormatDialog.this.status.setVisible(false);
                    }
                    catch (CancellationException cancellationException) {
                        FormatDialog.this.preview.setVisible(FormatDialog.this.preview.getText().trim().length() > 0);
                        FormatDialog.this.editor.setForeground(FormatDialog.this.preview.getForeground());
                        timer.stop();
                        if (this == FormatDialog.this.currentPreviewFuture) {
                            FormatDialog.this.progressIndicator.setVisible(false);
                        }
                    }
                    catch (Exception exception) {
                        try {
                            SuppressedThrowables suppressedThrowables = Logging.findCause(exception, SuppressedThrowables.class);
                            if (suppressedThrowables != null) {
                                BindingException bindingException = Logging.findCause(suppressedThrowables, BindingException.class);
                                if (bindingException != null && !FormatDialog.this.lockSample && bindingException.has(BindingException.Flag.SAMPLE_FILE_NOT_SET)) {
                                    FormatDialog.this.status.setText(FormatDialog.SAMPLE_FILE_NOT_SET_MESSAGE);
                                    FormatDialog.this.status.setIcon(ResourceManager.getIcon("action.variables"));
                                } else if (bindingException != null && bindingException.has(BindingException.Flag.UNDEFINED)) {
                                    FormatDialog.this.status.setText(bindingException.getMessage());
                                    FormatDialog.this.status.setIcon(ResourceManager.getIcon("status.info"));
                                } else if (bindingException != null) {
                                    FormatDialog.this.status.setText(bindingException.getMessage());
                                    FormatDialog.this.status.setIcon(ResourceManager.getIcon("status.error"));
                                } else {
                                    InvalidInputException invalidInputException = Logging.findCause(suppressedThrowables, InvalidInputException.class);
                                    if (invalidInputException != null) {
                                        FormatDialog.this.status.setText(invalidInputException.getMessage());
                                        FormatDialog.this.status.setIcon(ResourceManager.getIcon("status.info"));
                                    } else {
                                        FormatDialog.this.status.setText(suppressedThrowables.getMessage());
                                        FormatDialog.this.status.setIcon(ResourceManager.getIcon("status.warning"));
                                    }
                                }
                            } else {
                                FormatDialog.this.status.setText(exception.toString());
                                FormatDialog.this.status.setIcon(ResourceManager.getIcon("status.warning"));
                            }
                            FormatDialog.this.status.setVisible(true);
                            FormatDialog.this.preview.setVisible(FormatDialog.this.preview.getText().trim().length() > 0);
                        }
                        catch (Throwable throwable) {
                            FormatDialog.this.preview.setVisible(FormatDialog.this.preview.getText().trim().length() > 0);
                            FormatDialog.this.editor.setForeground(FormatDialog.this.preview.getForeground());
                            timer.stop();
                            if (this == FormatDialog.this.currentPreviewFuture) {
                                FormatDialog.this.progressIndicator.setVisible(false);
                            }
                            throw throwable;
                        }
                        FormatDialog.this.editor.setForeground(FormatDialog.this.preview.getForeground());
                        timer.stop();
                        if (this == FormatDialog.this.currentPreviewFuture) {
                            FormatDialog.this.progressIndicator.setVisible(false);
                        }
                    }
                    FormatDialog.this.preview.setVisible(FormatDialog.this.preview.getText().trim().length() > 0);
                    FormatDialog.this.editor.setForeground(FormatDialog.this.preview.getForeground());
                    timer.stop();
                    if (this == FormatDialog.this.currentPreviewFuture) {
                        FormatDialog.this.progressIndicator.setVisible(false);
                    }
                }
            };
            if (runnableFuture != null) {
                runnableFuture.cancel(true);
            }
            this.executor.execute(this.currentPreviewFuture);
        }
        catch (Exception exception) {
            this.status.setText(StringUtilities.printable(Logging.getRootCauseMessage(exception)));
            this.status.setIcon(ResourceManager.getIcon("status.error"));
            this.status.setVisible(true);
            this.preview.setVisible(false);
            this.editor.setForeground(ThemeSupport.getErrorColor());
        }
    }

    public boolean submit() {
        return this.submit;
    }

    public Mode getMode() {
        return this.mode;
    }

    public String getExpression() {
        return this.editor.getText().trim();
    }

    private void finish(boolean bl) {
        this.submit = bl;
        this.executor.shutdownNow();
        this.setVisible(false);
        this.dispose();
    }

    private void fireSampleChanged() {
        this.firePropertyChange(SAMPLE_PROPERTY, null, this.sample);
    }

    public static void open(Object object, Mode mode, boolean bl, String string, MediaBindingBean mediaBindingBean, boolean bl2, BiConsumer<Mode, String> biConsumer) {
        SwingUI.withWaitCursor(object, () -> {
            FormatDialog formatDialog = new FormatDialog(SwingUI.getWindow(object), mode, bl, string, mediaBindingBean, bl2);
            formatDialog.setLocation(SwingUI.getOffsetLocation(formatDialog));
            SwingUI.invokeLater(20, () -> {
                formatDialog.setVisible(true);
                if (formatDialog.submit()) {
                    biConsumer.accept(formatDialog.getMode(), formatDialog.getExpression());
                }
            });
        });
    }
}

