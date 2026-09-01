package net.filemaid.ui.rename;

import com.cedarsoftware.util.io.JsonWriter;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.FileVisitResult;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.text.JTextComponent;
import net.filemaid.Language;
import net.filemaid.Logging;
import net.filemaid.RenameAction;
import net.filemaid.ResourceManager;
import net.filemaid.Settings;
import net.filemaid.StandardRenameAction;
import net.filemaid.UserFiles;
import net.filemaid.UserInteraction;
import net.filemaid.WebServices;
import net.filemaid.format.ExpressionFileComparator;
import net.filemaid.format.ExpressionFileFilter;
import net.filemaid.format.ExpressionFileFormat;
import net.filemaid.format.MediaBindingBean;
import net.filemaid.media.LocalDatasource;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.ui.BaseDialog;
import net.filemaid.ui.HeaderPanel;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.rename.BindingDialog;
import net.filemaid.ui.rename.FormatDialog;
import net.filemaid.ui.rename.FormatExpressionTextArea;
import net.filemaid.ui.rename.MatchMode;
import net.filemaid.ui.rename.Mode;
import net.filemaid.ui.rename.Preset;
import net.filemaid.ui.rename.SmartMode;
import net.filemaid.util.ReadOnlyFile;
import net.filemaid.util.ui.GlassOptionPane;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.Datasource;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.MovieLookupService;
import net.filemaid.web.MusicLookupService;
import net.filemaid.web.SortOrder;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class PresetEditor
extends BaseDialog {
    private Result result = Result.CANCEL;
    private Preset preset = null;
    private HeaderPanel presetNameHeader = new HeaderPanel();
    private RSyntaxTextArea formatEditor = new FormatExpressionTextArea();
    private JComboBox<Datasource> providerCombo = PresetEditor.createComboBox(Preset.getSupportedServices());
    private JComboBox<SortOrder> sortOrderCombo = PresetEditor.createComboBox(SortOrder.values());
    private JComboBox<Language> languageCombo = PresetEditor.createComboBox(Preset.getSupportedLanguages());
    private JComboBox<MatchMode> matchModeCombo = PresetEditor.createComboBox(MatchMode.values());
    private JComboBox<RenameAction> actionCombo = PresetEditor.createComboBox(Preset.getSupportedActions());
    private JComboBox<KeyStroke> keyCodeCombo = PresetEditor.createComboBox(Preset.getSupportedKeyboardShortcuts());
    private JRadioButton selectRadioButton = new JRadioButton("<html><nobr>Do <b>Select</b> files</nobr></html>");
    private JRadioButton inheritRadioButton = new JRadioButton("<html><nobr>Use <b>Original Files</b> selection</nobr></html>");
    private InputFolderTabbedPane inputFolderTabbedPane = new InputFolderTabbedPane();
    private final Action editFormatExpression = SwingUI.newAction("Open Format Editor", ResourceManager.getIcon("action.format"), actionEvent -> {
        Datasource datasource = (Datasource)this.providerCombo.getSelectedItem();
        Mode mode2 = datasource == LocalDatasource.XATTR || datasource instanceof SmartMode ? null : Mode.getMode(datasource);
        Mode mode3 = mode2 == null ? Mode.Episode : mode2;
        Object object = null;
        File file = null;
        if (mode3 == Mode.File) {
            List<File> list = SwingUI.withWaitCursor(actionEvent, () -> this.inputFolderTabbedPane.selectSampleFiles(1)).orElse(Collections.emptyList());
            if (list.isEmpty()) {
                String string2 = Mode.File.persistentSample().getValue();
                list = Stream.of(string2).filter(string -> string != null && !string.isEmpty()).map(File::new).filter(File::exists).collect(Collectors.toList());
            }
            if (list.isEmpty()) {
                list = UserFiles.showLoadDialogSelectFiles(false, false, null, null, "Select Sample File", actionEvent);
                if (list.isEmpty()) {
                    return;
                }
                Mode.File.persistentSample().setValue(((File)list.get(0)).getAbsolutePath());
            }
            file = (File)list.get(0);
            object = file;
        } else {
            object = mode3.getDefaultSampleObject();
        }
        FormatDialog.open(actionEvent, mode3, mode2 != null, this.formatEditor.getText(), new MediaBindingBean(object, file, Collections.singletonMap(file, object)), false, (mode, string) -> this.formatEditor.setText(string));
    });
    private final JButton ok = SwingUI.newButton("Save Preset", ResourceManager.getIcon("dialog.continue"), actionEvent -> SwingUI.withWaitCursor((Object)this, () -> {
        try {
            this.preset = this.derivePreset();
            if (this.preset.getName().isEmpty()) {
                this.showPresetNameInputDialog();
                return;
            }
            this.result = Result.SET;
            this.setVisible(false);
        }
        catch (Exception exception) {
            Logging.log.warning(Logging.cause(exception));
        }
    }));
    private final JButton delete = SwingUI.newButton("Delete Preset", ResourceManager.getIcon("dialog.cancel"), actionEvent -> {
        this.result = Result.DELETE;
        this.setVisible(false);
    });
    private final JButton help = SwingUI.newButton("Examples", ResourceManager.getIcon("script.palette"), actionEvent -> UserInteraction.browse("https://www.filebot.net/help/presets.html"));

    public PresetEditor(Window window) {
        super(window, "Preset Editor", Dialog.ModalityType.DOCUMENT_MODAL);
        JComponent jComponent = (JComponent)this.getContentPane();
        JComponent jComponent2 = PresetEditor.createGroupPanel("Files");
        jComponent2.add(this.selectRadioButton);
        jComponent2.add((Component)this.inheritRadioButton, "wrap 12px");
        jComponent2.add((Component)this.inputFolderTabbedPane, "gap 4px, hmin 80px");
        JComponent jComponent3 = PresetEditor.createGroupPanel("Format");
        jComponent3.add((Component)PresetEditor.wrapEditor(this.formatEditor), "growx, gap rel");
        jComponent3.add((Component)SwingUI.createImageButton(this.editFormatExpression), "gap 10px");
        JComponent jComponent4 = PresetEditor.createGroupPanel("Options");
        jComponent4.add((Component)new JLabel("Datasource:"), "sg label");
        jComponent4.add(this.providerCombo, "sg combo");
        jComponent4.add((Component)new JLabel("Episode Order:"), "sg label, gap indent");
        jComponent4.add(this.sortOrderCombo, "sg combo, wrap");
        jComponent4.add((Component)new JLabel("Language:"), "sg label");
        jComponent4.add(this.languageCombo, "sg combo");
        jComponent4.add((Component)new JLabel("Match Mode:"), "sg label, gap indent");
        jComponent4.add(this.matchModeCombo, "sg combo, wrap");
        jComponent4.add((Component)new JLabel("Rename Action:"), "sg label");
        jComponent4.add(this.actionCombo, "sg combo");
        jComponent4.add((Component)new JLabel("Keyboard Shortcut:"), "sg label, gap indent");
        jComponent4.add(this.keyCodeCombo, "sg combo, wrap");
        jComponent.setLayout((LayoutManager)new MigLayout("insets dialog, hidemode 3, nogrid, fill"));
        jComponent.add((Component)this.presetNameHeader, "wmin 150px, hmin 75px, growx, dock north");
        jComponent.add((Component)jComponent2, "growx, wrap");
        jComponent.add((Component)jComponent3, "growx, wrap");
        jComponent.add((Component)jComponent4, "growx, wrap push");
        jComponent.add((Component)this.ok, "tag apply");
        jComponent.add((Component)this.delete, "tag cancel");
        jComponent.add((Component)this.help, "tag left, gap indent");
        this.inheritRadioButton.setOpaque(false);
        this.selectRadioButton.setOpaque(false);
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(this.inheritRadioButton);
        buttonGroup.add(this.selectRadioButton);
        this.inheritRadioButton.setSelected(true);
        this.selectRadioButton.addItemListener(itemEvent -> this.updateComponentStates());
        this.providerCombo.addItemListener(itemEvent -> this.updateComponentStates());
        this.updateComponentStates();
        JLabel jLabel = this.presetNameHeader.getTitleLabel();
        jLabel.setToolTipText("Edit Preset Name");
        jLabel.setCursor(Cursor.getPredefinedCursor(2));
        jLabel.addMouseListener(SwingUI.mouseClicked(mouseEvent -> this.showPresetNameInputDialog()));
        this.addWindowListener(SwingUI.windowOpened(windowEvent -> {
            if (jLabel.getText().isEmpty()) {
                this.showPresetNameInputDialog();
            }
        }));
        this.setSize(770, 630);
    }

    private void showPresetNameInputDialog() {
        JLabel jLabel = this.presetNameHeader.getTitleLabel();
        String string = jLabel.getText();
        GlassOptionPane.showInputDialog("Preset Name:", string.isEmpty() ? "New Preset" : string, "Edit Name", ResourceManager.getIcon("search.literal"), jLabel, jLabel::setText);
    }

    public void updateComponentStates() {
        this.inputFolderTabbedPane.setVisible(this.selectRadioButton.isSelected());
        Datasource datasource = (Datasource)this.providerCombo.getSelectedItem();
        this.sortOrderCombo.setEnabled(datasource instanceof EpisodeListProvider || datasource == SmartMode.Automatic);
        this.languageCombo.setEnabled(datasource instanceof EpisodeListProvider || datasource instanceof MovieLookupService || datasource == SmartMode.Automatic);
        this.matchModeCombo.setEnabled(datasource instanceof EpisodeListProvider || datasource instanceof MovieLookupService || datasource == SmartMode.Automatic);
        for (JComboBox jComboBox : Arrays.asList(this.sortOrderCombo, this.languageCombo, this.matchModeCombo)) {
            if (jComboBox.isEnabled()) {
                if (jComboBox.getSelectedIndex() >= 0) continue;
                jComboBox.setSelectedIndex(0);
                continue;
            }
            jComboBox.setSelectedIndex(-1);
        }
    }

    public void setPreset(Preset preset) {
        this.preset = preset;
        this.delete.setEnabled(preset.getKey() != null);
        this.help.setVisible(preset.getKey() == null);
        this.presetNameHeader.getTitleLabel().setText(preset.getName());
        this.inputFolderTabbedPane.setInputFolder(preset.getInputFolder());
        this.inputFolderTabbedPane.setFileFilter(preset.getIncludeFilterExpression());
        this.inputFolderTabbedPane.setFileOrder(preset.getFileOrderExpression());
        this.formatEditor.setText(preset.getFormatExpression() == null ? "" : preset.getFormatExpression());
        this.providerCombo.setSelectedItem(preset.getDatasource() == null ? WebServices.getDefaultSeriesDB() : preset.getDatasource());
        this.sortOrderCombo.setSelectedItem((Object)(preset.getSortOrder() == null ? SortOrder.Airdate : preset.getSortOrder()));
        this.matchModeCombo.setSelectedItem((Object)(preset.getMatchMode() == null ? MatchMode.Opportunistic : preset.getMatchMode()));
        this.actionCombo.setSelectedItem(preset.getRenameAction() == null ? StandardRenameAction.MOVE : preset.getRenameAction());
        this.keyCodeCombo.setSelectedItem(preset.getKeyStroke() == null ? null : preset.getKeyStroke());
        if (preset.getLanguage() != null && !preset.getLanguage().matches((Language)this.languageCombo.getSelectedItem())) {
            for (int i = 0; i < this.languageCombo.getModel().getSize(); ++i) {
                if (!preset.getLanguage().matches((Language)this.languageCombo.getModel().getElementAt(i))) continue;
                this.languageCombo.setSelectedIndex(i);
                break;
            }
        }
        this.selectRadioButton.setSelected(preset.getInputFolder() != null || preset.getIncludeFilterExpression() != null || preset.getFileOrderExpression() != null);
        this.updateComponentStates();
    }

    public Optional<Preset> getPreset() {
        return Optional.ofNullable(this.preset);
    }

    private String generatePresetIdentifier() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    public Preset derivePreset() throws Exception {
        KeyStroke keyStroke;
        StandardRenameAction standardRenameAction;
        Language language;
        MatchMode matchMode;
        SortOrder sortOrder;
        Datasource datasource;
        ExpressionFileFormat expressionFileFormat;
        ExpressionFileComparator expressionFileComparator;
        ExpressionFileFilter expressionFileFilter;
        File file;
        String string;
        String string2 = Optional.ofNullable(this.preset).map(Preset::getIdentifier).orElseGet(this::generatePresetIdentifier);
        Preset preset = new Preset(string2, string = this.presetNameHeader.getTitleLabel().getText(), file = this.selectRadioButton.isSelected() ? this.inputFolderTabbedPane.getInputFolder() : null, expressionFileFilter = this.selectRadioButton.isSelected() ? this.inputFolderTabbedPane.getFileFilter() : null, expressionFileComparator = this.selectRadioButton.isSelected() ? this.inputFolderTabbedPane.getFileOrder() : null, expressionFileFormat = this.formatEditor.getText().trim().isEmpty() ? null : new ExpressionFileFormat(this.formatEditor.getText().trim()), datasource = (Datasource)this.providerCombo.getSelectedItem(), sortOrder = this.sortOrderCombo.isEnabled() ? (SortOrder)((Object)this.sortOrderCombo.getSelectedItem()) : null, matchMode = this.matchModeCombo.isEnabled() ? (MatchMode)((Object)this.matchModeCombo.getSelectedItem()) : null, language = this.languageCombo.isEnabled() ? (Language)this.languageCombo.getSelectedItem() : null, standardRenameAction = this.actionCombo.isEnabled() ? (StandardRenameAction)this.actionCombo.getSelectedItem() : null, keyStroke = (KeyStroke)this.keyCodeCombo.getSelectedItem());
        String string3 = JsonWriter.objectToJson((Object)preset);
        if (string3.length() > 8192) {
            throw new IllegalStateException("8192 character limit exceeded");
        }
        return preset;
    }

    public Result getResult() {
        return this.result;
    }

    private static JComponent createGroupPanel(String string) {
        return SwingUI.newPanel(string, (LayoutManager)new MigLayout("insets dialog, hidemode 3, nogrid, fill"));
    }

    private static RTextScrollPane wrapEditor(RSyntaxTextArea rSyntaxTextArea) {
        RTextScrollPane rTextScrollPane = new RTextScrollPane((RTextArea)rSyntaxTextArea, false);
        rTextScrollPane.setLineNumbersEnabled(false);
        rTextScrollPane.setFoldIndicatorEnabled(false);
        rTextScrollPane.setIconRowHeaderEnabled(false);
        rTextScrollPane.setVerticalScrollBarPolicy(20);
        rTextScrollPane.setHorizontalScrollBarPolicy(31);
        rTextScrollPane.setOpaque(true);
        rSyntaxTextArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        rTextScrollPane.setBorder(ThemeSupport.getEditorBorder());
        rTextScrollPane.setBackground(ThemeSupport.getEditorBackground());
        rTextScrollPane.setMinimumSize(new Dimension(300, rTextScrollPane.getPreferredSize().height));
        return rTextScrollPane;
    }

    private static <E> JComboBox<E> createComboBox(E[] EArray) {
        JComboBox<E> jComboBox = new JComboBox<E>(EArray);
        jComboBox.setLightWeightPopupEnabled(false);
        Stream.of(EArray).filter(Objects::nonNull).max(Comparator.comparing(Object::toString, Comparator.comparingInt(String::length))).ifPresent(jComboBox::setPrototypeDisplayValue);
        ListCellRenderer listCellRenderer = jComboBox.getRenderer();
        jComboBox.setRenderer((jList, object, n, bl, bl2) -> {
            JLabel jLabel = (JLabel)listCellRenderer.getListCellRendererComponent(jList, null, n, bl, bl2);
            if (object instanceof Datasource) {
                Datasource datasource = (Datasource)object;
                jLabel.setText(datasource.getIdentifier().length() > datasource.getName().length() ? datasource.getIdentifier() : datasource.getName());
                jLabel.setIcon(datasource.getIcon());
                if (datasource instanceof EpisodeListProvider) {
                    jLabel.setToolTipText("Episode Mode: " + datasource.getName());
                } else if (datasource instanceof MovieLookupService) {
                    jLabel.setToolTipText("Movie Mode: " + datasource.getName());
                } else if (datasource instanceof MusicLookupService) {
                    jLabel.setToolTipText("Music Mode: " + datasource.getName());
                } else if (datasource instanceof LocalDatasource) {
                    jLabel.setToolTipText("File Mode: " + datasource.getName());
                } else if (datasource instanceof SmartMode) {
                    jLabel.setToolTipText("Smart Mode: " + datasource.getName());
                } else {
                    jLabel.setToolTipText(null);
                }
                return jLabel;
            }
            if (object instanceof Language) {
                Language language = (Language)object;
                jLabel.setText(language.getName());
                jLabel.setIcon(ResourceManager.getFlagIcon(language.getCode()));
                jLabel.setToolTipText(null);
                return jLabel;
            }
            if (object instanceof StandardRenameAction) {
                StandardRenameAction standardRenameAction = (StandardRenameAction)object;
                jLabel.setText(standardRenameAction.getDisplayName());
                jLabel.setIcon(ResourceManager.getIcon("rename.action." + standardRenameAction.toString().toLowerCase(Locale.ROOT)));
                jLabel.setToolTipText(null);
                return jLabel;
            }
            if (object instanceof KeyStroke) {
                KeyStroke keyStroke = (KeyStroke)object;
                jLabel.setText(KeyEvent.getKeyText(keyStroke.getKeyCode()).replace("\u2328", "NumPad"));
                jLabel.setIcon(null);
                jLabel.setToolTipText(null);
                return jLabel;
            }
            jLabel.setText(object == null ? "none" : object.toString());
            jLabel.setIcon(null);
            jLabel.setToolTipText(null);
            return jLabel;
        });
        return jComboBox;
    }

    static enum Result {
        SET,
        DELETE,
        CANCEL;

    }

    private static class InputFolderTabbedPane
    extends JTabbedPane {
        private JTextField inputFolderField = new JTextField(40);
        private RSyntaxTextArea fileFilterEditor = new FormatExpressionTextArea(new RSyntaxDocument("text/groovy"), true);
        private RSyntaxTextArea fileOrderEditor = new FormatExpressionTextArea(new RSyntaxDocument("text/groovy"), true);
        private final Action selectInputFolder = SwingUI.newAction("Select Input Folder", ResourceManager.getIcon("action.load"), actionEvent -> {
            File file = UserFiles.showOpenDialogSelectFolder(null, "Select Input Folder", actionEvent);
            if (file != null) {
                this.inputFolderField.setText(file.getAbsolutePath());
            }
        });
        private final Action filterFiles = SwingUI.newAction("Filter Files", ResourceManager.getIcon("action.search"), actionEvent -> SwingUI.withWaitCursor(actionEvent, () -> {
            try {
                if (this.getFileFilter() == null) {
                    UserInteraction.browse(FILE_FILTER_HELP);
                    return;
                }
                List<File> list = this.selectSampleFiles(50);
                this.showSampleFiles(list, (EventObject)actionEvent);
            }
            catch (Exception exception) {
                Logging.log.warning(Logging.cause(exception));
            }
        }));
        private final Action sortFiles = SwingUI.newAction("Sort Files", ResourceManager.getIcon("action.sort"), actionEvent -> SwingUI.withWaitCursor(actionEvent, () -> {
            try {
                if (this.getFileOrder() == null) {
                    UserInteraction.browse(FILE_ORDER_HELP);
                    return;
                }
                List<File> list = this.selectSampleFiles(50);
                list.sort(this.getFileOrder());
                this.showSampleFiles(list, (EventObject)actionEvent);
            }
            catch (Exception exception) {
                Logging.log.warning(Logging.cause(exception));
            }
        }));
        private static final int SAMPLE_FILE_LIMIT = 50;
        private static final String FILE_FILTER_TOOLTIP = "<html>e.g.<br>\u2022 fn =~ /Alias/<br>\u2022 ext =~ /mp4/<br>\u2022 minutes &gt; 100<br>\u2022 age &lt; 7<br>\u2022 f.video<br>\u2022 f.episode<br>\u2022 \u2026<br></html>";
        private static final String FILE_ORDER_TOOLTIP = "<html>e.g.<br>\u2022 height<br>\u2022 duration<br>\u2022 bytes<br>\u2022 ^ct<br>\u2022 ^[ext, fn]<br>\u2022 [height, ct]<br>\u2022 \u2026<br></html>";
        private static final String FILE_FILTER_HELP = "https://www.filebot.net/help/file-filter.html";
        private static final String FILE_ORDER_HELP = "https://www.filebot.net/help/file-order.html";

        public InputFolderTabbedPane() {
            super(1, 1);
            this.fileFilterEditor.setToolTipText(FILE_FILTER_TOOLTIP);
            this.fileOrderEditor.setToolTipText(FILE_ORDER_TOOLTIP);
            this.setFocusable(false);
            this.setOpaque(false);
            this.addTab("Input Folder", ResourceManager.getIcon("action.load"), this.createTab(this.inputFolderField, this.selectInputFolder));
            this.addTab("File Filter", ResourceManager.getIcon("action.search"), this.createTab((JComponent)PresetEditor.wrapEditor(this.fileFilterEditor), this.filterFiles));
            this.addTab("File Order", ResourceManager.getIcon("action.sort"), this.createTab((JComponent)PresetEditor.wrapEditor(this.fileOrderEditor), this.sortFiles));
            JTextComponent[] jTextComponentArray = new JTextComponent[]{this.inputFolderField, this.fileFilterEditor, this.fileOrderEditor};
            Icon[] iconArray = (Icon[])IntStream.range(0, this.getTabCount()).mapToObj(n -> this.getIconAt(n)).toArray(Icon[]::new);
            Icon[] iconArray2 = (Icon[])IntStream.range(0, this.getTabCount()).mapToObj(n -> this.getDisabledIconAt(n)).toArray(Icon[]::new);
            this.addChangeListener(changeEvent -> {
                for (int i = 0; i < jTextComponentArray.length; ++i) {
                    this.setIconAt(i, i == this.getSelectedIndex() || jTextComponentArray[i].getText().length() > 0 ? iconArray[i] : iconArray2[i]);
                }
            });
            this.addComponentListener(SwingUI.componentShown(componentEvent -> {
                for (int i = 0; i < jTextComponentArray.length; ++i) {
                    if (jTextComponentArray[i].getText().length() <= 0) continue;
                    this.setSelectedIndex(i);
                    jTextComponentArray[i].requestFocus();
                    return;
                }
                this.setSelectedIndex(0);
                this.inputFolderField.requestFocus();
            }));
            this.setSelectedIndex(-1);
        }

        private JComponent createTab(JComponent jComponent, Action action) {
            JPanel jPanel = new JPanel((LayoutManager)new MigLayout("nogrid, fill"));
            jPanel.setOpaque(false);
            jPanel.add((Component)jComponent, "growx, hmin 28px");
            jPanel.add((Component)SwingUI.createImageButton(action), "gap rel");
            return jPanel;
        }

        public void setInputFolder(File file) {
            this.inputFolderField.setText(file == null ? "" : file.getPath());
        }

        public File getInputFolder() {
            String string = this.inputFolderField.getText().trim();
            return string.isEmpty() ? null : new File(string);
        }

        public void setFileFilter(String string) {
            this.fileFilterEditor.setText(string == null ? "" : string);
        }

        public ExpressionFileFilter getFileFilter() throws Exception {
            String string = this.fileFilterEditor.getText().trim();
            return string.isEmpty() ? null : new ExpressionFileFilter(string, XattrMetaInfo.xattr::getMetaInfo);
        }

        public void setFileOrder(String string) {
            this.fileOrderEditor.setText(string == null ? "" : string);
        }

        public ExpressionFileComparator getFileOrder() throws Exception {
            String string = this.fileOrderEditor.getText().trim();
            return string.isEmpty() ? null : new ExpressionFileComparator(string);
        }

        private List<File> selectSampleFiles(int n) throws Exception {
            File file2 = this.getInputFolder();
            if (file2 == null || !file2.exists()) {
                return Collections.emptyList();
            }
            if (Settings.isMacSandbox()) {
                MacAppUtilities.askUnlockFolders(SwingUI.getWindow(this), Collections.singleton(file2));
            }
            AtomicInteger atomicInteger = new AtomicInteger(0);
            ExpressionFileFilter expressionFileFilter = this.getFileFilter();
            return ReadOnlyFile.find(file2, file -> file.isFile() && (expressionFileFilter == null || expressionFileFilter.accept(file)), file -> (file.isDirectory() ? atomicInteger.get() : atomicInteger.incrementAndGet()) <= n ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SIBLINGS);
        }

        private void showSampleFiles(List<File> list, EventObject eventObject) {
            JPopupMenu jPopupMenu = SwingUI.newPopupMenu("Files");
            for (File file : list) {
                jPopupMenu.add(SwingUI.newAction(file.getPath(), actionEvent -> {
                    BindingDialog bindingDialog = new BindingDialog(SwingUI.getWindow(this), Mode.File, new MediaBindingBean(file, file), false);
                    bindingDialog.setLocation(SwingUI.getOffsetLocation(bindingDialog));
                    bindingDialog.setVisible(true);
                }));
            }
            if (list.isEmpty()) {
                jPopupMenu.add("No files selected").setEnabled(false);
            }
            SwingUI.showDropDown(jPopupMenu, eventObject);
        }
    }
}

