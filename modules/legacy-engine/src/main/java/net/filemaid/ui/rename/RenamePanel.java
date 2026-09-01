package net.filemaid.ui.rename;

import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import com.google.common.eventbus.Subscribe;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.FileVisitResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import net.filemaid.Cache;
import net.filemaid.History;
import net.filemaid.HistorySpooler;
import net.filemaid.InvalidInputException;
import net.filemaid.InvalidResponseException;
import net.filemaid.Language;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.Settings;
import net.filemaid.StandardPostProcessAction;
import net.filemaid.StandardRenameAction;
import net.filemaid.UserData;
import net.filemaid.UserInteraction;
import net.filemaid.WebServices;
import net.filemaid.format.ExpressionEngine;
import net.filemaid.format.ExpressionFileComparator;
import net.filemaid.format.ExpressionFileFilter;
import net.filemaid.format.MediaBindingBean;
import net.filemaid.media.LocalDatasource;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.Script;
import net.filemaid.similarity.DeltaEdit;
import net.filemaid.similarity.Match;
import net.filemaid.ui.RepeatToggle;
import net.filemaid.ui.rename.AutoCompleteMatcher;
import net.filemaid.ui.rename.AutoDetectionMode;
import net.filemaid.ui.rename.AutoSelectionMode;
import net.filemaid.ui.rename.EpisodeListMatcher;
import net.filemaid.ui.rename.ExpressionFormatter;
import net.filemaid.ui.rename.FilesListTransferablePolicy;
import net.filemaid.ui.rename.FormatDialog;
import net.filemaid.ui.rename.FormattedFuture;
import net.filemaid.ui.rename.HistoryDialog;
import net.filemaid.ui.rename.LocalFileMatcher;
import net.filemaid.ui.rename.MatchAction;
import net.filemaid.ui.rename.MatchDetailPanel;
import net.filemaid.ui.rename.MatchFormatterType;
import net.filemaid.ui.rename.MatchMode;
import net.filemaid.ui.rename.MatchType;
import net.filemaid.ui.rename.MetaObjectDialog;
import net.filemaid.ui.rename.Mode;
import net.filemaid.ui.rename.MovieMatcher;
import net.filemaid.ui.rename.MusicMatcher;
import net.filemaid.ui.rename.NamesListExportHandler;
import net.filemaid.ui.rename.NamesListTransferablePolicy;
import net.filemaid.ui.rename.PostProcessConfigurationDialog;
import net.filemaid.ui.rename.PreferencesPanel;
import net.filemaid.ui.rename.Preset;
import net.filemaid.ui.rename.PresetEditor;
import net.filemaid.ui.rename.RenameAction;
import net.filemaid.ui.rename.RenameList;
import net.filemaid.ui.rename.RenameListCellRenderer;
import net.filemaid.ui.rename.RenameModel;
import net.filemaid.ui.rename.ScrollPaneSynchronizer;
import net.filemaid.ui.rename.SmartMode;
import net.filemaid.ui.rename.UserPresets;
import net.filemaid.ui.transfer.LoadAction;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.PreferencesList;
import net.filemaid.util.PreferencesMap;
import net.filemaid.util.ReadOnlyFile;
import net.filemaid.util.ui.ActionPopup;
import net.filemaid.util.ui.ContextAction;
import net.filemaid.util.ui.GlassOptionPane;
import net.filemaid.util.ui.LoadingOverlayPane;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.MovieLookupService;
import net.filemaid.web.MusicLookupService;
import net.filemaid.web.SortOrder;
import net.miginfocom.swing.MigLayout;

public class RenamePanel
extends JComponent {
    protected final RenameModel renameModel = new RenameModel();
    protected final RenameList<FormattedFuture> namesList = new RenameList<FormattedFuture>(this.renameModel.names());
    protected final RenameList<File> filesList = new RenameList<File>(this.renameModel.files());
    protected final MatchAction matchAction = new MatchAction(this.renameModel);
    protected final RenameAction renameAction = new RenameAction(this.renameModel);
    private static final UserData userData = UserData.forPackage(RenamePanel.class);
    private static final PreferencesMap.PreferencesEntry<Mode> persistentLastFormatState = userData.entry("rename.format.mode", Mode.Episode);
    private static final PreferencesMap.PreferencesEntry<MatchMode> persistentPreferredMatchMode = userData.entry("rename.match.mode", MatchMode.Opportunistic);
    private static final PreferencesMap.PreferencesEntry<SortOrder> persistentPreferredEpisodeOrder = userData.entry("rename.episode.order", SortOrder.Airdate);
    private static final PreferencesMap.PreferencesEntry<Language> persistentPreferredLanguage = userData.entry("rename.language", Language.defaultLanguage(), Language::getLanguage, Language::getCode);
    private static final PreferencesMap.PreferencesEntry<Set<StandardPostProcessAction>> persistentPostProcessActions = userData.entrySet("rename.action.apply", StandardPostProcessAction.class);
    private static final PreferencesList<Script> persistentPostProcessScripts = userData.node("rename.action.apply.scripts").asList(Script.class);
    private MatchDetailPanel matchDetailPanel;
    private final Action clearFilesAction = SwingUI.newAction("Clear", ResourceManager.getIcon("action.clear"), actionEvent2 -> {
        ((FilesListTransferablePolicy)this.filesList.getTransferablePolicy()).reset();
        ((NamesListTransferablePolicy)this.namesList.getTransferablePolicy()).reset();
        this.resetMatcher();
        if (this.renameModel.files().isEmpty() || this.renameModel.names().isEmpty()) {
            this.renameModel.clear();
            this.resetPrototypeCellSize();
            return;
        }
        ActionPopup actionPopup = new ActionPopup("Clear", ResourceManager.getIcon("action.clear"));
        actionPopup.add(SwingUI.newAction("Original Files", ResourceManager.getIcon("selection.left"), actionEvent -> {
            this.filesList.getSelectionModel().clearSelection();
            this.renameModel.files().clear();
            this.resetPrototypeCellSize();
        }));
        actionPopup.add(SwingUI.newAction("New Names", ResourceManager.getIcon("selection.right"), actionEvent -> {
            this.namesList.getSelectionModel().clearSelection();
            this.renameModel.names().clear();
            this.resetPrototypeCellSize();
        }));
        actionPopup.add(SwingUI.newAction("Clear All", ResourceManager.getIcon("selection.all"), actionEvent -> {
            this.renameModel.clear();
            this.resetPrototypeCellSize();
        }));
        SwingUI.showDropDown((JPopupMenu)actionPopup, actionEvent2);
    });
    private final Action openHistoryAction = SwingUI.newAction("Open History", ResourceManager.getIcon("action.report"), actionEvent -> SwingUI.withWaitCursor(actionEvent, () -> {
        History history = HistorySpooler.HISTORY.getCompleteHistory();
        HistoryDialog historyDialog = new HistoryDialog(SwingUI.getWindow(this));
        historyDialog.setLocationRelativeTo(this);
        historyDialog.setModel(history);
        historyDialog.setVisible(true);
    }));
    private final Action editSelectedFormat = SwingUI.newAction("Edit Format", ResourceManager.getIcon("action.format"), actionEvent -> this.withMatchSelection((n, match) -> {
        if (MatchType.getType(match).canFormat()) {
            this.showFormatEditor((Match<Object, File>)match, true);
        } else {
            Logging.log.info(Logging.format("Cannot format %s type matches.", new Object[]{MatchType.getType(match)}));
        }
    }));
    private final Action editSelectedMatch = SwingUI.newAction("Edit Match", ResourceManager.getIcon("action.match.select"), actionEvent -> this.withMatchSelection((n, match) -> {
        if (MatchType.getType(match).canEdit()) {
            SwingUI.withWaitCursor((Object)this, () -> MetaObjectDialog.showSelectDialog(match.getValue(), (File)match.getCandidate(), null, "Edit Match", ResourceManager.getIcon("action.match.select"), this, object -> {
                if (SwingUI.isShiftOrAltDown(actionEvent)) {
                    this.renameModel.set((int)n, object);
                } else {
                    this.renameModel.fill((int)n, object);
                }
            }));
        } else {
            Logging.log.info(Logging.format("Cannot edit %s type matches.", new Object[]{MatchType.getType(match)}));
        }
    }));
    private final Action editSelectedName = SwingUI.newAction("Edit Name", ResourceManager.getIcon("action.match.rename"), actionEvent -> this.withMatchSelection((n, match) -> {
        FormattedFuture formattedFuture = (FormattedFuture)this.renameModel.names().get(n.intValue());
        if (formattedFuture != null && formattedFuture.isDone()) {
            SwingUI.withWaitCursor((Object)this, () -> {
                RepeatToggle repeatToggle = new RepeatToggle(ResourceManager.getIcon("action.sort"), ResourceManager.getIcon("button.cell.edit"));
                repeatToggle.setHelpText("Edit only this item", "Edit this and subsequent items");
                repeatToggle.setSelected(!SwingUI.isShiftOrAltDown(actionEvent));
                repeatToggle.setMinimumSize(new Dimension(170, 30));
                MetaObjectDialog.showSelectDialog(formattedFuture.toString(), (File)match.getCandidate(), repeatToggle, "Edit Name", ResourceManager.getIcon("action.match.rename"), this, object -> {
                    if (repeatToggle.isSelected()) {
                        DeltaEdit deltaEdit = DeltaEdit.of(formattedFuture.toString(), object.toString());
                        if (deltaEdit != null) {
                            this.renameModel.fill((int)n, deltaEdit, future -> EpisodeUtilities.isSeriesMatch(match.getValue(), future.getMatch().getValue()) || future.toString().startsWith(deltaEdit.getPrefix()));
                        }
                    } else {
                        this.renameModel.set((int)n, object.toString(), match.getValue());
                    }
                });
            });
        }
    }));
    private final Action deleteItem = SwingUI.newAction("Exclude Selected Items", ResourceManager.getIcon("dialog.cancel"), actionEvent -> {
        int n;
        int n2;
        RenameList renameList = actionEvent.getSource() instanceof RenameList ? (RenameList)actionEvent.getSource() : this.filesList;
        boolean bl = SwingUI.isShiftOrAltDown(actionEvent);
        int[] nArray = renameList.getSelectedIndices();
        if (nArray.length == 0) {
            Logging.log.info("No match selected. Please select a match first.");
            return;
        }
        for (n2 = nArray.length - 1; n2 >= 0; --n2) {
            n = nArray[n2];
            if (bl) {
                if (n >= renameList.getModel().size()) continue;
                renameList.getModel().remove(n);
                continue;
            }
            this.renameModel.removeMatch(n);
        }
        n2 = nArray[0];
        n = bl ? renameList.getModel().size() - 1 : this.renameModel.size() - 1;
        renameList.setSelectedIndices(n2 < n ? n2 : n);
    });
    private final Action openSelectedFile = SwingUI.newAction("Open File", actionEvent -> this.withFileSelection((n, file) -> UserInteraction.open(file)));
    private final Action revealSelectedFile = SwingUI.newAction("Reveal File", actionEvent -> this.withFileSelection((n, file) -> UserInteraction.reveal(file)));
    private final Map<Object, Set<AutoSelectionMode>> workerList = Collections.synchronizedMap(new IdentityHashMap(1));

    public RenamePanel() {
        DefaultEventSelectionModel defaultEventSelectionModel = new DefaultEventSelectionModel(this.renameModel.matches());
        defaultEventSelectionModel.setSelectionMode(2);
        this.namesList.setTitle("New Names");
        this.namesList.setTransferablePolicy(new NamesListTransferablePolicy((List<Object>)this.renameModel.values(), (ListSelectionModel)defaultEventSelectionModel));
        this.namesList.getTransferHandler().setClipboardHandler(new NamesListExportHandler(this.namesList));
        this.filesList.setTitle("Original Files");
        this.filesList.setTransferablePolicy(new FilesListTransferablePolicy((List<File>)this.renameModel.files()));
        this.renameAction.configure(StandardRenameAction.MOVE);
        this.filesList.getModel().addListEventListener(listEvent -> {
            if (listEvent.next() && (listEvent.getType() == 0 || listEvent.getType() == 2)) {
                this.resetMatcher();
            }
        });
        this.restoreState();
        this.restoreFormatter();
        RenameListCellRenderer renameListCellRenderer = RenameListCellRenderer.create(this.renameModel);
        this.filesList.getListComponent().setCellRenderer(renameListCellRenderer);
        this.namesList.getListComponent().setCellRenderer(renameListCellRenderer);
        this.filesList.getListComponent().setSelectionModel((ListSelectionModel)defaultEventSelectionModel);
        this.namesList.getListComponent().setSelectionModel((ListSelectionModel)defaultEventSelectionModel);
        ScrollPaneSynchronizer scrollPaneSynchronizer = new ScrollPaneSynchronizer(this.filesList, this.namesList);
        scrollPaneSynchronizer.updatePreferredSize();
        this.filesList.setRemoveAction(this.deleteItem);
        this.namesList.setRemoveAction(this.deleteItem);
        JButton jButton = SwingUI.newButton(this.matchAction);
        jButton.setVerticalTextPosition(3);
        jButton.setHorizontalTextPosition(0);
        JButton jButton2 = SwingUI.newButton(this.renameAction);
        jButton2.setVerticalTextPosition(3);
        jButton2.setHorizontalTextPosition(0);
        this.filesList.getListComponent().addMouseListener(SwingUI.mousePopupMenu(mouseEvent -> this.createFetchPopup(this.filesList.getSelectedIndices((MouseEvent)mouseEvent))));
        this.namesList.getListComponent().addMouseListener(SwingUI.mousePopupMenu(mouseEvent -> this.createFetchPopup(this.namesList.getSelectedIndices((MouseEvent)mouseEvent))));
        jButton.addMouseListener(SwingUI.mousePopupMenu(mouseEvent -> this.createFetchPopup()));
        jButton2.addMouseListener(SwingUI.mousePopupMenu(mouseEvent -> this.createSettingsPopup()));
        JButton jButton3 = SwingUI.newButton(SwingUI.newPopupAction("Fetch Data", ResourceManager.getIcon("action.fetch"), this::createFetchPopup));
        JButton jButton4 = SwingUI.createImageButton(SwingUI.newPopupAction("Load Names", ResourceManager.getIcon("action.paste"), this::createPastePopup));
        this.namesList.getButtonPanel().add((Component)jButton3, "gap 10px, sgy button");
        this.namesList.getButtonPanel().add((Component)jButton4, "gap 0, sgy button");
        JButton jButton5 = SwingUI.createImageButton(SwingUI.newPopupAction("Settings", ResourceManager.getIcon("action.settings"), this::createSettingsPopup));
        this.namesList.getButtonPanel().add((Component)jButton5, "gap indent, sg button");
        JButton jButton6 = SwingUI.createImageButton(SwingUI.newPopupAction("Edit Match", ResourceManager.getIcon("action.match.small"), () -> this.createEditMatchPopup("Edit Match", ResourceManager.getIcon("action.match.small"))));
        this.namesList.getButtonPanel().add(jButton6, "gap 0, sg button", 2);
        this.filesList.getButtonPanel().add((Component)SwingUI.createImageButton(this.deleteItem), "gap 0, sgy button");
        this.filesList.getButtonPanel().add((Component)SwingUI.newButton(new LoadAction(this.filesList::getTransferablePolicy)), "gap 10px, sgy button");
        this.filesList.getButtonPanel().add((Component)SwingUI.createImageButton(this.clearFilesAction), "gap 0, sgy button");
        this.filesList.getButtonPanel().add((Component)SwingUI.createImageButton(this.openHistoryAction), "gap indent, sgy button");
        JButton jButton7 = SwingUI.createImageButton(SwingUI.newAction("Presets", ResourceManager.getIcon("action.script"), this::showPresetsPopup));
        this.filesList.getButtonPanel().add((Component)jButton7, "gap 0, sgy button");
        jButton.addActionListener(actionEvent -> {
            if (this.renameModel.names().isEmpty() || this.renameModel.files().isEmpty()) {
                SwingUI.showDropDown((JPopupMenu)this.createFetchPopup(), jButton);
            }
            this.namesList.getListComponent().requestFocus();
        });
        this.filesList.getListComponent().addMouseListener(SwingUI.mouseDoubleClicked(mouseEvent -> this.withFileSelection((n, file) -> SwingUI.onSwingWorker(() -> XattrMetaInfo.xattr.getMetaInfo((File)file), object -> {
            ActionPopup actionPopup = this.createEditFilePopup((int)n, (File)file, object);
            actionPopup.show((Component)mouseEvent.getSource(), mouseEvent.getX(), mouseEvent.getY());
        }))));
        this.namesList.getListComponent().addMouseListener(SwingUI.mouseDoubleClicked(mouseEvent -> this.withMatchSelection((n, match) -> {
            ActionPopup actionPopup = this.createEditMatchPopup("Edit " + MatchType.getType(match), MatchType.getIcon(match));
            actionPopup.show((Component)mouseEvent.getSource(), mouseEvent.getX(), mouseEvent.getY());
        })));
        SwingUtilities.invokeLater(() -> {
            for (Preset preset : UserPresets.USER_PRESETS.getPresetGroups().get(0)) {
                SwingUI.installAction((JComponent)this, preset.getKeyStroke(), (Action)new ApplyPresetAction(preset));
            }
            WebServices.requestPool().async(() -> {
                ExpressionEngine expressionEngine = ExpressionEngine.getExpressionEngine();
                Set<String> set = Collections.singleton("");
                MediaDetection.stripReleaseInfo(set, false);
                MediaDetection.matchSeriesByName(set, 0, false);
                MediaDetection.matchMovieName(set, true, 0);
                Cache.DISK_STORE.flush();
                return expressionEngine.compileScriptlet("");
            });
            SwingUI.installAction((JComponent)this, 113, SwingUI.newAction("Edit Name", actionEvent -> {
                if (!this.namesList.getModel().isEmpty()) {
                    this.editSelectedName.actionPerformed((ActionEvent)actionEvent);
                    return;
                }
                SwingUI.withWaitCursor((Object)this, () -> {
                    List<File> list = ReadOnlyFile.of(this.renameModel.files());
                    Map<File, Object> map = LocalDatasource.FILE.match(list);
                    this.renameModel.clear();
                    this.renameModel.addAll(map.values(), map.keySet());
                });
            }));
            SwingUI.installAction((JComponent)this, 114, SwingUI.newAction("Edit Match", actionEvent -> {
                if (!this.namesList.getModel().isEmpty()) {
                    this.editSelectedMatch.actionPerformed((ActionEvent)actionEvent);
                    return;
                }
                SwingUI.withWaitCursor((Object)this, () -> {
                    List<File> list = ReadOnlyFile.of(this.renameModel.files());
                    Map<File, Object> map = LocalDatasource.XATTR.match(list);
                    LinkedHashSet<File> linkedHashSet = new LinkedHashSet<File>((Collection<File>)this.renameModel.files());
                    linkedHashSet.removeAll(map.keySet());
                    this.renameModel.clear();
                    this.renameModel.addAll(map.values(), map.keySet());
                    this.renameModel.files().addAll(linkedHashSet);
                });
            }));
            SwingUI.installAction((JComponent)this, 117, SwingUI.newAction("Toggle Match Details", this::toggleMatchDetailPanel));
            SwingUI.installAction(this.namesList.getListComponent(), 0, KeyStroke.getKeyStroke(32, 0), SwingUI.newAction("Toggle Match Details", this::toggleMatchDetailPanel));
            SwingUI.installAction(this.filesList.getListComponent(), 0, KeyStroke.getKeyStroke(32, 0), SwingUI.newAction("Toggle Match Details", this::toggleMatchDetailPanel));
            SwingUI.installAction((JComponent)this, 118, SwingUI.newAction("Copy Debug Information", this::snapshot));
            SwingUI.installAction((JComponent)this, 78, 128, this.editSelectedName);
            SwingUI.installAction((JComponent)this, 77, 128, this.editSelectedMatch);
            SwingUI.installAction((JComponent)this, 79, 128, this.openSelectedFile);
            SwingUI.installAction((JComponent)this, 76, 128, this.revealSelectedFile);
        });
        this.setLayout((LayoutManager)new MigLayout("fill, insets dialog, gapx 10px", "[fill][align center, pref!][fill]", "align 33%"));
        this.add((Component)new LoadingOverlayPane(this.filesList), "grow, sizegroupx list");
        jButton.setMargin(new Insets(3, 14, 2, 14));
        jButton2.setMargin(new Insets(6, 11, 2, 11));
        this.add((Component)jButton, "split 2, flowy, sizegroupx button");
        this.add((Component)jButton2, "gapy 30px, sizegroupx button");
        this.add((Component)new LoadingOverlayPane(this.namesList), "grow, sizegroupx list");
    }

    private void restoreState() {
        this.matchAction.setMatchMode(persistentPreferredMatchMode.getValue());
        this.renameAction.configurePostProcess((Apply[])Stream.of(persistentPostProcessActions.getValue(), persistentPostProcessScripts).flatMap(Collection::stream).toArray(Apply[]::new));
    }

    private void restoreFormatter() {
        for (Mode mode : Mode.values()) {
            MatchFormatterType matchFormatterType = mode.getFormatterType();
            String string = mode.persistentFormat().getValue();
            this.renameModel.useFormatter(matchFormatterType, string == null ? matchFormatterType : new ExpressionFormatter(string, matchFormatterType));
        }
        this.renameModel.useFormatter(MatchFormatterType.STRING, MatchFormatterType.STRING);
        this.renameModel.useFormatter(MatchFormatterType.FILE_VFS, MatchFormatterType.FILE_VFS);
    }

    private void resetPrototypeCellSize() {
        this.filesList.getPrototypeCellSize().reset();
        this.namesList.getPrototypeCellSize().reset();
        this.revalidate();
        this.repaint();
    }

    private void toggleMatchDetailPanel(ActionEvent actionEvent) {
        if (this.matchDetailPanel == null) {
            SwingUI.withWaitCursor((Object)this, () -> {
                this.matchDetailPanel = new MatchDetailPanel(this.renameModel, this.namesList.getSelectionModel());
                this.add((Component)this.matchDetailPanel, "dock north, hmax 450px, hidemode 3");
                this.matchDetailPanel.setVisible(false);
                this.matchDetailPanel.addComponentListener(SwingUI.componentShown(componentEvent -> this.matchDetailPanel.hook()));
                this.matchDetailPanel.addComponentListener(SwingUI.componentHidden(componentEvent -> this.matchDetailPanel.unhook()));
            });
        }
        if (actionEvent.getSource() == this.namesList.getListComponent()) {
            this.matchDetailPanel.setTabIndex(1);
        }
        if (actionEvent.getSource() == this.filesList.getListComponent()) {
            this.matchDetailPanel.setTabIndex(2);
        }
        this.matchDetailPanel.setVisible(!this.matchDetailPanel.isVisible());
        this.revalidate();
    }

    private void showPresetsPopup(ActionEvent actionEvent) {
        List<Preset[]> list = UserPresets.USER_PRESETS.getPresetGroups();
        ActionPopup actionPopup = new ActionPopup("Presets", ResourceManager.getIcon("action.script"));
        for (Preset[] presetArray : list) {
            actionPopup.addGroup((Action[])Stream.of(presetArray).map(preset -> new ApplyPresetAction((Preset)preset)).toArray(Action[]::new));
        }
        if (actionPopup.count() == 0 || SwingUI.isShiftOrAltDown(actionEvent)) {
            actionPopup.addGroup((Action[])UserPresets.DEFAULT_PRESETS.list().map(preset -> new ApplyPresetAction((Preset)preset)).toArray(Action[]::new));
        }
        actionPopup.addGroup(SwingUI.newAction("Edit Preset", ResourceManager.getIcon("script.add"), actionEvent2 -> this.showEditPresetsPopup(list, actionEvent)));
        SwingUI.showDropDown((JPopupMenu)actionPopup, actionEvent);
    }

    private void showEditPresetsPopup(List<Preset[]> list, ActionEvent actionEvent2) {
        ActionPopup actionPopup = new ActionPopup("Edit Preset", ResourceManager.getIcon("script.add"));
        for (Preset[] presetArray : list) {
            actionPopup.addGroup((Action[])Stream.of(presetArray).map(preset -> SwingUI.newAction(preset.getName(), ResourceManager.getIcon("script.edit"), actionEvent -> this.showPresetEditor((Preset)preset, SwingUI.getWindow(this)))).toArray(Action[]::new));
        }
        if (actionPopup.count() <= 30 || SwingUI.isShiftOrAltDown(actionEvent2)) {
            actionPopup.addGroup((Action[])UserPresets.DEFAULT_PRESETS.list().map(preset -> SwingUI.newAction(preset.getName(), ResourceManager.getIcon("script.add"), actionEvent -> this.showPresetEditor((Preset)preset, SwingUI.getWindow(this)))).toArray(Action[]::new));
        }
        actionPopup.addGroup(SwingUI.newAction("New Preset", ResourceManager.getIcon("script.add"), actionEvent -> this.showPresetEditor(null, SwingUI.getWindow(this))));
        SwingUI.showDropDown((JPopupMenu)actionPopup, actionEvent2);
    }

    private ActionPopup createFetchPopup() {
        ActionPopup actionPopup = new ActionPopup("Fetch & Match Data", ResourceManager.getIcon("action.fetch"));
        actionPopup.addDescription("Episode Mode:");
        for (EpisodeListProvider datasource : WebServices.getEpisodeListProviders()) {
            actionPopup.add(new ModelAutoCompleteAction(datasource));
        }
        actionPopup.addSeparator();
        actionPopup.addDescription("Movie Mode:");
        for (Datasource datasource : WebServices.getMovieLookupServices()) {
            actionPopup.add(new ModelAutoCompleteAction(datasource));
        }
        actionPopup.addSeparator();
        actionPopup.addDescription("Music Mode:");
        for (Datasource datasource : WebServices.getMusicLookupServices()) {
            actionPopup.add(new ModelAutoCompleteAction(datasource));
        }
        actionPopup.addSeparator();
        actionPopup.addDescription("Smart Mode:");
        for (Datasource datasource : SmartMode.values()) {
            actionPopup.add(new ModelAutoCompleteAction(datasource));
        }
        actionPopup.addSeparator();
        actionPopup.addDescription("Options:");
        actionPopup.add(SwingUI.newAction("Edit Format", ResourceManager.getIcon("action.format"), actionEvent -> {
            if (this.renameModel.hasComplement(0) && MatchType.getType(this.renameModel.getMatch(0)).canFormat()) {
                this.showFormatEditor(this.renameModel.getMatch(0), false);
            } else {
                this.showFormatEditor(null, false);
            }
        }));
        actionPopup.add(SwingUI.newAction("Preferences", ResourceManager.getIcon("action.preferences"), actionEvent -> {
            PreferencesPanel preferencesPanel2 = new PreferencesPanel();
            preferencesPanel2.setMatchMode(persistentPreferredMatchMode.getValue());
            preferencesPanel2.setOrder(persistentPreferredEpisodeOrder.getValue());
            preferencesPanel2.setLanguage(persistentPreferredLanguage.getValue());
            GlassOptionPane.showConfigurationDialog(preferencesPanel2, null, "Match Preferences", ResourceManager.getIcon("action.preferences"), this, preferencesPanel -> {
                persistentPreferredMatchMode.setValue(preferencesPanel.getMatchMode());
                persistentPreferredEpisodeOrder.setValue(preferencesPanel.getOrder());
                persistentPreferredLanguage.setValue(preferencesPanel.getLanguage());
                this.matchAction.setMatchMode(preferencesPanel.getMatchMode());
            });
        }));
        actionPopup.addPopupMenuListener(SwingUI.popupMenuWillBecomeVisible(popupMenuEvent -> SwingUtilities.invokeLater(this.filesList.getSelectionModel()::clearSelection)));
        return actionPopup;
    }

    private ActionPopup createFetchPopup(int[] nArray) {
        int[] nArray2 = IntStream.of(nArray).filter(n -> n < this.filesList.getModel().size()).sorted().toArray();
        if (nArray2.length == 0) {
            return this.createFetchPopup();
        }
        ActionPopup actionPopup = new ActionPopup("Select Match", ResourceManager.getIcon("action.fetch"));
        actionPopup.addDescription("Episode Mode:");
        for (EpisodeListProvider datasource : WebServices.getEpisodeListProviders()) {
            actionPopup.add(new SelectionAutoCompleteAction(datasource, nArray2));
        }
        actionPopup.addSeparator();
        actionPopup.addDescription("Movie Mode:");
        for (Datasource datasource : WebServices.getMovieLookupServices()) {
            actionPopup.add(new SelectionAutoCompleteAction(datasource, nArray2));
        }
        actionPopup.addSeparator();
        actionPopup.addDescription("Music Mode:");
        for (Datasource datasource : WebServices.getMusicLookupServices()) {
            actionPopup.add(new SelectionAutoCompleteAction(datasource, nArray2));
        }
        actionPopup.addSeparator();
        actionPopup.addDescription("Smart Mode:");
        for (Datasource datasource : SmartMode.values()) {
            actionPopup.add(new SelectionAutoCompleteAction(datasource, nArray2));
        }
        actionPopup.setStatus((String)(nArray2.length == 1 ? "1 selected item" : nArray2.length + " selected items"));
        return actionPopup;
    }

    private ActionPopup createEditFilePopup(int n, File file, Object object) {
        String string = file.isDirectory() ? "Folder" : "File";
        MatchType matchType = MatchType.getType(object);
        ActionPopup actionPopup = new ActionPopup((String)(matchType.canEdit() ? matchType + " " + string : string), matchType.canEdit() ? MatchType.getIcon(object) : MatchType.getIcon(file));
        actionPopup.add(SwingUI.newAction("Open", actionEvent -> SwingUI.withWaitCursor((Object)this, () -> UserInteraction.open(file))));
        actionPopup.add(SwingUI.newAction("Reveal", actionEvent -> SwingUI.withWaitCursor((Object)this, () -> UserInteraction.reveal(file))));
        actionPopup.addSeparator();
        actionPopup.add(SwingUI.newAction("Rename", actionEvent -> {
            try {
                String extension = FileUtilities.getExtension(file);
                String string2 = extension == null ? file.getName() : FileUtilities.getName(file);
                String string3 = SwingUI.showInputDialog("Please enter a new file name:", string2, "Rename", ResourceManager.getIcon("action.match.rename"), this);
                if (string3 != null && !string3.equals(string2)) {
                    File file2 = FileUtilities.rename(file, (String)(extension == null ? string3 : string3 + "." + extension));
                    this.renameModel.files().set(n, ReadOnlyFile.of(file2));
                }
            }
            catch (Exception exception) {
                Logging.log.warning(Logging.cause(exception));
            }
        }));
        actionPopup.add(SwingUI.newAction("Move to Trash", actionEvent -> SwingUI.withWaitCursor((Object)this, () -> {
            if (UserInteraction.delete(file)) {
                this.renameModel.removeMatch(n);
            }
        })));
        if (matchType.canEdit()) {
            actionPopup.addSeparator();
            actionPopup.add(SwingUI.newAction("Set Attributes", ResourceManager.getIcon("action.properties"), actionEvent -> SwingUI.withWaitCursor((Object)this, () -> MetaObjectDialog.showSelectDialog(object, file, null, "Set Attributes", ResourceManager.getIcon("action.properties"), this, value -> {
                XattrMetaInfo.xattr.setMetaInfo(file, value, null);
                this.renameModel.files().set(n, ReadOnlyFile.of(file));
            }))));
        }
        return actionPopup;
    }

    private ActionPopup createEditMatchPopup(String string, Icon icon) {
        ActionPopup actionPopup = new ActionPopup(string, icon);
        actionPopup.add(this.editSelectedFormat);
        actionPopup.add(this.editSelectedMatch);
        actionPopup.add(this.editSelectedName);
        actionPopup.addSeparator();
        actionPopup.add(SwingUI.newAction("Inspect", ResourceManager.getIcon("action.properties"), this::toggleMatchDetailPanel));
        return actionPopup;
    }

    private ActionPopup createPastePopup() {
        ActionPopup actionPopup = new ActionPopup("Load Names", ResourceManager.getIcon("action.paste"));
        actionPopup.add(new LoadAction("Load from Directory Index", ResourceManager.getIcon("action.load"), () -> {
            NamesListTransferablePolicy namesListTransferablePolicy = (NamesListTransferablePolicy)this.namesList.getTransferablePolicy();
            return namesListTransferablePolicy.withDirectoryMode(true);
        }));
        actionPopup.add(new LoadAction("Load from Text Content", ResourceManager.getIcon("action.load"), () -> {
            NamesListTransferablePolicy namesListTransferablePolicy = (NamesListTransferablePolicy)this.namesList.getTransferablePolicy();
            return namesListTransferablePolicy.withDirectoryMode(false);
        }));
        actionPopup.addSeparator();
        actionPopup.add(SwingUI.newAction("Paste from Clipboard", ResourceManager.getIcon("action.paste"), actionEvent -> {
            try {
                this.namesList.getTransferablePolicy().importDataFromSystemClipboard((EventObject)actionEvent);
            }
            catch (Exception exception) {
                Logging.trace(exception);
            }
        }));
        return actionPopup;
    }

    private ActionPopup createSettingsPopup() {
        ActionPopup actionPopup = new ActionPopup("Settings", ResourceManager.getIcon("action.settings"));
        actionPopup.addDescription("Extension:");
        actionPopup.add(new SetRenameMode(false, "Preserve", ResourceManager.getIcon("action.extension.preserve")));
        actionPopup.add(new SetRenameMode(true, "Override", ResourceManager.getIcon("action.extension.override")));
        actionPopup.addSeparator();
        actionPopup.addDescription("Action:");
        for (StandardRenameAction standardRenameAction : Preset.getSupportedActions()) {
            actionPopup.add(new SetRenameAction(standardRenameAction));
        }
        actionPopup.addSeparator();
        actionPopup.addDescription("Options:");
        actionPopup.add(SwingUI.newAction("Post Process", ResourceManager.getIcon("script.palette"), actionEvent -> PostProcessConfigurationDialog.showConfigurationDialog(persistentPostProcessActions.getValue(), persistentPostProcessScripts, this, postProcessConfigurationDialog -> {
            this.renameAction.configurePostProcess((Apply[])Stream.of(postProcessConfigurationDialog.getSelectedActions(), postProcessConfigurationDialog.getSelectedScripts()).flatMap(Collection::stream).toArray(Apply[]::new));
            persistentPostProcessActions.setValue(postProcessConfigurationDialog.getSelectedActions());
            persistentPostProcessScripts.set(postProcessConfigurationDialog.getSelectedScripts());
        })));
        return actionPopup;
    }

    private void showFormatEditor(Match<Object, File> match, boolean bl) {
        MediaBindingBean mediaBindingBean = match == null ? null : new MediaBindingBean(match.getValue(), match.getCandidate(), this.renameModel.getMatchContext(match));
        Mode mode2 = match == null ? persistentLastFormatState.getValue() : Mode.getMode(match.getValue());
        String string2 = match == null ? null : this.renameModel.getFormatExpression(match);
        FormatDialog.open(this, mode2, bl, string2, mediaBindingBean, bl, (mode, string) -> {
            MatchFormatterType matchFormatterType = mode.getFormatterType();
            this.renameModel.useFormatter(matchFormatterType, new ExpressionFormatter((String)string, matchFormatterType));
            mode.persistentFormat().setValue((String)string);
            if (mediaBindingBean == null) {
                persistentLastFormatState.setValue((Mode)((Object)mode));
            }
            this.resetPrototypeCellSize();
        });
    }

    private void showPresetEditor(Preset preset, Window window) {
        try {
            PresetEditor presetEditor = new PresetEditor(window);
            if (preset != null) {
                presetEditor.setPreset(preset);
            }
            presetEditor.setLocation(SwingUI.getOffsetLocation(presetEditor));
            presetEditor.setVisible(true);
            switch (presetEditor.getResult()) {
                case SET: {
                    presetEditor.getPreset().ifPresent(preset3 -> {
                        UserPresets.save(preset3);
                        KeyStroke keyStroke = preset == null ? null : preset.getKeyStroke();
                        KeyStroke keyStroke2 = preset3.getKeyStroke();
                        if (keyStroke != null && !keyStroke.equals(keyStroke2)) {
                            SwingUI.installAction((JComponent)this, keyStroke, null);
                        }
                        if (keyStroke2 != null) {
                            SwingUI.installAction((JComponent)this, keyStroke2, (Action)new ApplyPresetAction((Preset)preset3));
                            if (!keyStroke2.equals(keyStroke)) {
                                UserPresets.USER_PRESETS.list().forEach(preset2 -> {
                                    if (keyStroke2.equals(preset2.getKeyStroke()) && !preset3.getKey().equals(preset2.getKey())) {
                                        UserPresets.save(preset2.deleteKeyStroke());
                                    }
                                });
                            }
                        }
                    });
                    break;
                }
                case DELETE: {
                    if (preset == null) break;
                    UserPresets.delete(preset);
                    if (preset.getKeyStroke() == null) break;
                    SwingUI.installAction((JComponent)this, preset.getKeyStroke(), null);
                    break;
                }
            }
        }
        catch (Exception exception) {
            Logging.log.log(Level.WARNING, exception, Logging.cause(exception));
        }
    }

    private void withFileSelection(BiConsumer<Integer, File> biConsumer) {
        Match match;
        int n = this.filesList.getSelectionModel().getLeadSelectionIndex();
        if (this.filesList.getModel().isEmpty()) {
            Logging.log.info("No file to select. Please <Load> files first.");
            return;
        }
        if (n < 0) {
            Logging.log.info("No file selected. Please select a file first.");
            return;
        }
        if (this.filesList.getSelectedIndices().length > 1) {
            this.filesList.setSelectedIndices(n);
        }
        if ((match = this.renameModel.getMatch(n)) != null && match.getCandidate() != null) {
            biConsumer.accept(n, (File)match.getCandidate());
        }
    }

    private void withMatchSelection(BiConsumer<Integer, Match<Object, File>> biConsumer) {
        Match match;
        int n = this.namesList.getSelectionModel().getLeadSelectionIndex();
        if (this.namesList.getModel().isEmpty()) {
            Logging.log.info("No match to select. Please <Load> files and <Fetch Data> first.");
            return;
        }
        if (n < 0) {
            Logging.log.info("No match selected. Please select a match first.");
            return;
        }
        if (this.namesList.getSelectedIndices().length > 1) {
            this.namesList.setSelectedIndices(n);
        }
        if ((match = this.renameModel.getMatch(n)) != null && match.getValue() != null) {
            biConsumer.accept(n, match);
        }
    }

    private void snapshot(ActionEvent actionEvent) {
        SwingUI.withWaitCursor((Object)actionEvent, () -> {
            try {
                if (this.renameModel.size() > 0) {
                    UserInteraction.copy(this.renameModel.exportContent());
                } else {
                    this.renameModel.importContent(UserInteraction.paste());
                    Logging.log.info((String)(this.renameModel.size() == 1 ? "1 match has been pasted from the clipboard." : this.renameModel.size() + " matches have been pasted from the clipboard."));
                }
            }
            catch (Exception exception) {
                Logging.log.warning(Logging.cause(exception));
            }
        });
    }

    @Subscribe
    public void handle(Transferable transferable) throws Exception {
        if (this.filesList.getTransferablePolicy().importData(transferable, TransferablePolicy.TransferAction.PUT)) {
            return;
        }
        if (this.namesList.getTransferablePolicy().importData(transferable, TransferablePolicy.TransferAction.PUT)) {
            return;
        }
    }

    private void setLoading(JComponent jComponent, boolean bl) {
        jComponent.firePropertyChange("loading", !bl, bl);
    }

    private void resetMatcher() {
        if (this.workerList.size() > 0) {
            this.workerList.forEach((object, set) -> set.addAll(AutoSelectionMode.cancel()));
            this.workerList.clear();
        }
    }

    private class ModelAutoCompleteAction
    extends AutoCompleteAction {
        public ModelAutoCompleteAction(Datasource datasource) {
            super(datasource);
        }

        @Override
        protected void publish(Collection<Match<Object, File>> collection, Set<AutoSelectionMode> set, Collection<File> collection2, MatchMode matchMode, SortOrder sortOrder, Locale locale, AutoDetectionMode autoDetectionMode, Function<Object, Object> function, Component component) {
            super.publish(collection, set, collection2, matchMode, sortOrder, locale, autoDetectionMode, function, component);
            if ((this.db instanceof EpisodeListProvider || this.db instanceof MovieLookupService || this.db == SmartMode.Automatic) && !set.contains((Object)AutoSelectionMode.Cancel)) {
                if (collection.isEmpty() && !collection2.isEmpty()) {
                    if (matchMode == MatchMode.Strict && autoDetectionMode == AutoDetectionMode.Auto) {
                        GlassOptionPane.showSuggestionDialog("Failed to match files strictly.", "<html>Use <b>Match Mode: Opportunistic</b> instead?<html>", "Match Mode: Strict", matchMode.getIcon(), component, () -> SwingUI.invokeLater(250, () -> {
                            persistentPreferredMatchMode.setValue(MatchMode.Opportunistic);
                            RenamePanel.this.matchAction.setMatchMode(MatchMode.Opportunistic);
                            this.start(collection2, MatchMode.Opportunistic, sortOrder, locale, AutoDetectionMode.Auto, function, component);
                        }));
                        return;
                    }
                    if (matchMode == MatchMode.Opportunistic && autoDetectionMode == AutoDetectionMode.Auto && !set.contains((Object)AutoSelectionMode.Input)) {
                        GlassOptionPane.showSuggestionDialog("Failed to match files automatically.", "<html>Use <b>manual selection</b> instead?<html>", "Match Mode: Opportunistic", matchMode.getIcon(), component, () -> SwingUI.invokeLater(250, () -> this.start(collection2, MatchMode.Opportunistic, sortOrder, locale, AutoDetectionMode.Input, function, component)));
                        return;
                    }
                }
                if (collection.size() >= 8 && collection.size() <= 2000 && sortOrder != SortOrder.Absolute && matchMode == MatchMode.Opportunistic && autoDetectionMode == AutoDetectionMode.Auto && (this.db == WebServices.TheTVDB || this.db == WebServices.TheMovieDB_TV)) {
                    FormattedFuture.evaluatorPool().execute(() -> {
                        int n = MediaDetection.countSequentiallyNumberedEpisodes(collection.stream().map(Match::getCandidate).filter(Objects::nonNull).map(File::getName)::iterator);
                        if ((double)n > (double)collection.size() * 0.8) {
                            SwingUI.invokeLater(1500, () -> {
                                long l = RenamePanel.this.renameModel.names().stream().filter(formattedFuture -> formattedFuture.getMatchProbablity() < 1.0f).count();
                                if ((double)l > (double)RenamePanel.this.renameModel.names().size() * 0.4) {
                                    GlassOptionPane.showSuggestionDialog("<html><b>" + sortOrder + " Order</b> does not seem to match the files at hand.</html>", "<html>Try <b>Absolute Order</b> instead?<html>", "Match Order: " + sortOrder, this.db.getIcon(), component, () -> SwingUI.invokeLater(50, () -> {
                                        List<File> list = RenamePanel.this.renameModel.files().stream().collect(Collectors.toList());
                                        RenamePanel.this.renameModel.clear();
                                        RenamePanel.this.renameModel.files().addAll(list);
                                        this.start(list, MatchMode.Opportunistic, SortOrder.Absolute, locale, AutoDetectionMode.Auto, object -> {
                                            try {
                                                return EpisodeUtilities.reorderEpisode((Episode)object, sortOrder);
                                            }
                                            catch (Exception exception) {
                                                Logging.debug.warning(Logging.cause("Failed to map Absolute Order to " + sortOrder + " Order", exception));
                                                return object;
                                            }
                                        }, component);
                                    }));
                                }
                            });
                        }
                    });
                    return;
                }
            }
        }
    }

    private class SelectionAutoCompleteAction
    extends AutoCompleteAction {
        protected final int[] selection;

        public SelectionAutoCompleteAction(Datasource datasource, int[] nArray) {
            super(datasource);
            this.selection = nArray;
        }

        @Override
        protected List<File> getFiles() {
            return RenamePanel.this.filesList.getSelectedValues(this.selection);
        }

        @Override
        protected MatchMode getMatchMode() {
            return MatchMode.Opportunistic;
        }

        @Override
        protected AutoDetectionMode getAutoDetectionMode(ActionEvent actionEvent) {
            return SwingUI.isShiftOrAltDown(actionEvent) ? AutoDetectionMode.Input : AutoDetectionMode.Select;
        }

        @Override
        protected void publish(Collection<Match<Object, File>> collection, Set<AutoSelectionMode> set, Collection<File> collection2, MatchMode matchMode, SortOrder sortOrder, Locale locale, AutoDetectionMode autoDetectionMode, Function<Object, Object> function, Component component) {
            int n;
            int n2;
            if (collection.isEmpty()) {
                if (set.contains((Object)AutoSelectionMode.Input) && !set.contains((Object)AutoSelectionMode.Cancel)) {
                    Logging.log.info("No match found.");
                }
                return;
            }
            Map map = collection.stream().collect(Collectors.toMap(Match::getCandidate, Match::getValue, (object, object2) -> object, LinkedHashMap::new));
            ArrayList arrayList = new ArrayList();
            RenamePanel.this.filesList.getSelectionModel().clearSelection();
            for (n2 = this.selection.length - 1; n2 >= 0 && !map.isEmpty(); --n2) {
                File file;
                Object v;
                n = this.selection[n2];
                if (n >= RenamePanel.this.renameModel.files().size() || (v = map.remove(file = (File)RenamePanel.this.renameModel.files().get(n))) == null) continue;
                if (RenamePanel.this.renameModel.hasComplement(n)) {
                    RenamePanel.this.renameModel.set(n, v);
                    RenamePanel.this.filesList.getSelectionModel().addSelectionInterval(n, n);
                    continue;
                }
                RenamePanel.this.renameModel.removeMatch(n);
                arrayList.add(Match.of(v, file));
            }
            for (n2 = arrayList.size() - 1; n2 >= 0; --n2) {
                n = RenamePanel.this.renameModel.insertMatch((Match)arrayList.get(n2));
                RenamePanel.this.filesList.getSelectionModel().addSelectionInterval(n, n);
                RenamePanel.this.filesList.getListComponent().ensureIndexIsVisible(n);
            }
        }
    }

    private class SetRenameMode
    extends AbstractAction {
        private final boolean activate;

        private SetRenameMode(boolean bl, String string, Icon icon) {
            super(string, icon);
            this.activate = bl;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (RenamePanel.this.renameModel.files().isEmpty()) {
                Logging.log.info("No files have been selected. Please <Load> files first.");
                return;
            }
            RenamePanel.this.renameModel.setPreserveExtension(!this.activate);
            RenamePanel.this.resetPrototypeCellSize();
        }
    }

    private class SetRenameAction
    extends AbstractAction {
        private final StandardRenameAction action;

        public SetRenameAction(StandardRenameAction standardRenameAction) {
            super(standardRenameAction.getDisplayName(), ResourceManager.getIcon("rename.action." + standardRenameAction.name().toLowerCase(Locale.ROOT)));
            this.action = standardRenameAction;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            RenamePanel.this.renameAction.configure(this.action);
        }
    }

    private class ApplyPresetAction
    extends AutoCompleteAction
    implements ContextAction {
        protected final Preset preset;

        public ApplyPresetAction(Preset preset) {
            super(preset.getDatasource());
            this.putValue("Name", preset.getName());
            this.preset = preset;
        }

        @Override
        protected boolean requireInputFiles() {
            return true;
        }

        @Override
        protected List<File> getFiles() {
            List list;
            List<File> list2;
            File file = this.preset.getInputFolder();
            String string = this.preset.getIncludeFilterExpression();
            String string2 = this.preset.getFileOrderExpression();
            List<File> list3 = list2 = file == null ? super.getFiles() : Collections.emptyList();
            if (file == null && (list2 == null || string == null && string2 == null)) {
                return list2;
            }
            if (file != null) {
                if (!file.exists()) {
                    Logging.log.warning(Logging.message("Folder does not exist", file));
                    return null;
                }
                if (Settings.isMacSandbox() && !MacAppUtilities.askUnlockFolders(SwingUI.getWindow(RenamePanel.this), Collections.singleton(file))) {
                    return null;
                }
            }
            try {
                if (list2.isEmpty()) {
                    RenamePanel.this.renameModel.clear();
                }
                RenamePanel.this.setLoading(RenamePanel.this.filesList, true);
                Set<AutoSelectionMode> set = AutoSelectionMode.newSet();
                RenamePanel.this.workerList.put(this, set);
                list = SwingUI.onSecondaryLoop(() -> {
                    List<File> files = list2;
                    if (string != null) {
                        ExpressionFileFilter expressionFileFilter = new ExpressionFileFilter(string, XattrMetaInfo.xattr::getMetaInfo);
                        files = file != null ? ReadOnlyFile.find(file, f -> f.isFile() && expressionFileFilter.accept(f), f -> set.contains((Object)AutoSelectionMode.Cancel) ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE) : FileUtilities.filter(files, expressionFileFilter);
                    } else if (file != null) {
                        files = ReadOnlyFile.find(file, FileUtilities.FILES, f -> set.contains((Object)AutoSelectionMode.Cancel) ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE);
                    }
                    if (files.size() > 1 && string2 != null) {
                        files.sort(new ExpressionFileComparator(string2));
                    }
                    return files;
                });
                if (RenamePanel.this.workerList.remove(this) == null || set.contains((Object)AutoSelectionMode.Cancel)) {
                    List<File> list4 = null;
                    return list4;
                }
                if (list.isEmpty()) {
                    if (list2.isEmpty()) {
                        Logging.log.info("No files have been selected from the input folder.");
                    } else {
                        Logging.log.info("No files in <Original Files> match your <Includes> filter.");
                    }
                    List<File> list5 = null;
                    return list5;
                }
                RenamePanel.this.renameModel.clear();
                RenamePanel.this.renameModel.files().addAll((Collection)list);
                List list6 = list;
                return list6;
            }
            catch (Exception exception) {
                if (Logging.isCancellation(exception)) {
                    list = null;
                    return list;
                }
                Logging.log.warning(Logging.cause(exception));
            }
            finally {
                RenamePanel.this.setLoading(RenamePanel.this.filesList, false);
            }
            return null;
        }

        @Override
        protected MatchMode getMatchMode() {
            return this.preset.getMatchMode() != null ? this.preset.getMatchMode() : super.getMatchMode();
        }

        @Override
        protected SortOrder getSortOrder() {
            return this.preset.getSortOrder() != null ? this.preset.getSortOrder() : super.getSortOrder();
        }

        @Override
        protected Locale getLocale() {
            return this.preset.getLanguage() != null ? this.preset.getLanguage().getLocale() : super.getLocale();
        }

        @Override
        protected void configureFormatter() {
            String string = this.preset.getFormatExpression();
            Datasource datasource = this.preset.getDatasource();
            if (string == null || datasource == null) {
                RenamePanel.this.restoreFormatter();
                return;
            }
            if (datasource == LocalDatasource.XATTR || datasource instanceof SmartMode) {
                RenamePanel.this.renameModel.useFormatter(MatchFormatterType.EPISODE, new ExpressionFormatter(string, MatchFormatterType.EPISODE));
                RenamePanel.this.renameModel.useFormatter(MatchFormatterType.MOVIE, new ExpressionFormatter(string, MatchFormatterType.MOVIE));
                RenamePanel.this.renameModel.useFormatter(MatchFormatterType.MUSIC, new ExpressionFormatter(string, MatchFormatterType.MUSIC));
                return;
            }
            MatchFormatterType matchFormatterType = Mode.getMode(datasource).getFormatterType();
            RenamePanel.this.renameModel.useFormatter(matchFormatterType, new ExpressionFormatter(string, matchFormatterType));
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (this.preset.getRenameAction() != null) {
                RenamePanel.this.renameAction.configure(this.preset.getRenameAction());
            }
            super.actionPerformed(actionEvent);
        }

        @Override
        public void contextActionPerformed(MouseEvent mouseEvent) {
            RenamePanel.this.showPresetEditor(this.preset, SwingUI.getWindow(RenamePanel.this));
        }
    }

    private abstract class AutoCompleteAction
    extends AbstractAction {
        protected final Datasource db;

        public AutoCompleteAction(Datasource datasource) {
            super(datasource.getName(), datasource.getIcon());
            this.db = datasource;
        }

        protected AutoCompleteMatcher matcher() {
            if (this.db instanceof MovieLookupService) {
                return new MovieMatcher((MovieLookupService)this.db);
            }
            if (this.db instanceof EpisodeListProvider) {
                return new EpisodeListMatcher((EpisodeListProvider)this.db, this.db == WebServices.AniDB);
            }
            if (this.db instanceof MusicLookupService) {
                return new MusicMatcher((MusicLookupService)this.db);
            }
            if (this.db instanceof SmartMode) {
                return ((SmartMode)this.db).newMatcher();
            }
            if (this.db instanceof LocalDatasource) {
                return new LocalFileMatcher((LocalDatasource)this.db);
            }
            throw new IllegalStateException("Invalid datasource: " + this.db);
        }

        protected boolean requireInputFiles() {
            return this.db instanceof SmartMode || this.db instanceof MusicLookupService;
        }

        protected List<File> getFiles() {
            if (this.requireInputFiles() && RenamePanel.this.renameModel.files().isEmpty()) {
                Logging.log.info("No files have been selected. Please <Load> files first.");
                return null;
            }
            RenamePanel.this.renameModel.values().clear();
            return new ArrayList<File>((Collection<File>)RenamePanel.this.renameModel.files());
        }

        protected MatchMode getMatchMode() {
            return persistentPreferredMatchMode.getValue();
        }

        protected SortOrder getSortOrder() {
            return persistentPreferredEpisodeOrder.getValue();
        }

        protected Locale getLocale() {
            return persistentPreferredLanguage.getValue().getLocale();
        }

        protected AutoDetectionMode getAutoDetectionMode(ActionEvent actionEvent) {
            return SwingUI.isShiftOrAltDown(actionEvent) ? AutoDetectionMode.Input : AutoDetectionMode.Auto;
        }

        protected void configureFormatter() {
            RenamePanel.this.restoreFormatter();
        }

        protected void publish(Collection<Match<Object, File>> collection, Set<AutoSelectionMode> set, Collection<File> collection2, MatchMode matchMode, SortOrder sortOrder, Locale locale, AutoDetectionMode autoDetectionMode, Function<Object, Object> function, Component component) {
            RenamePanel.this.renameModel.clear();
            this.configureFormatter();
            RenamePanel.this.resetPrototypeCellSize();
            RenamePanel.this.renameModel.addAll(collection);
            RenamePanel.this.renameModel.files().addAll(collection2);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            RenamePanel.this.resetMatcher();
            List<File> list = this.getFiles();
            if (list == null) {
                return;
            }
            MatchMode matchMode = this.getMatchMode();
            SortOrder sortOrder = this.getSortOrder();
            Locale locale = this.getLocale();
            AutoDetectionMode autoDetectionMode = this.getAutoDetectionMode(actionEvent);
            Window window = SwingUI.getWindow(RenamePanel.this);
            if (Settings.isMacSandbox() && !MacAppUtilities.askUnlockFolders(window, list)) {
                return;
            }
            this.start(list, matchMode, sortOrder, locale, autoDetectionMode, null, window);
        }

        protected void start(Collection<File> collection, MatchMode matchMode, SortOrder sortOrder, Locale locale, AutoDetectionMode autoDetectionMode, Function<Object, Object> function, Component component) {
            Set<AutoSelectionMode> set = AutoSelectionMode.newSet();
            AutoCompleteMatcher autoCompleteMatcher = this.matcher();
            RenamePanel.this.workerList.put(autoCompleteMatcher, set);
            RenamePanel.this.setLoading(RenamePanel.this.namesList, true);
            LinkedHashSet<File> linkedHashSet = new LinkedHashSet<File>(collection);
            SwingUI.onSwingWorker(() -> {
                ArrayList arrayList = new ArrayList();
                FormattedFuture.evaluatorPool().execute(() -> {
                    try {
                        Class<?> clazz = this.getClass();
                        synchronized (clazz) {
                            while ((double)this.getClass().getProtectionDomain().getCodeSource().getLocation().openConnection().getContentLength() < 4000000.0) {
                                this.getClass().wait();
                            }
                        }
                    }
                    catch (Exception exception) {
                        Logging.trace(exception);
                    }
                });
                for (Match<File, ?> match : autoCompleteMatcher.match(linkedHashSet, matchMode, sortOrder, locale, autoDetectionMode, set, component)) {
                    File file = match.getValue();
                    Object object = match.getCandidate();
                    if (function != null) {
                        object = function.apply(object);
                    }
                    arrayList.add(Match.of(object, file));
                    linkedHashSet.remove(match.getValue());
                }
                return arrayList;
            }, list -> {
                if (RenamePanel.this.workerList.remove(autoCompleteMatcher) != null) {
                    this.publish((Collection<Match<Object, File>>)list, set, (Collection<File>)linkedHashSet, matchMode, sortOrder, locale, autoDetectionMode, function, component);
                }
            }, exception -> {
                CancellationException cancellationException = Logging.findCause(exception, CancellationException.class);
                if (cancellationException != null) {
                    return;
                }
                InvalidResponseException invalidResponseException = Logging.findCause(exception, InvalidResponseException.class);
                if (invalidResponseException != null) {
                    Logging.log.warning(invalidResponseException::getMessage);
                    return;
                }
                InvalidInputException invalidInputException = Logging.findCause(exception, InvalidInputException.class);
                if (invalidInputException != null) {
                    Logging.log.info(invalidInputException::getMessage);
                    return;
                }
                Logging.log.log(Level.WARNING, (Throwable)exception, Logging.cause(exception));
            }, () -> RenamePanel.this.setLoading(RenamePanel.this.namesList, false));
        }
    }
}

