package net.filemaid.cli;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.bundle.LanternaThemes;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.CheckBoxList;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.LayoutData;
import com.googlecode.lanterna.gui2.LayoutManager;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Panels;
import com.googlecode.lanterna.gui2.Separator;
import com.googlecode.lanterna.gui2.TextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.WindowManager;
import com.googlecode.lanterna.gui2.dialogs.ListSelectDialog;
import com.googlecode.lanterna.gui2.dialogs.ListSelectDialogBuilder;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorAutoCloseTrigger;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.filemaid.HistorySpooler;
import net.filemaid.RenameAction;
import net.filemaid.Settings;
import net.filemaid.cli.CmdlineException;
import net.filemaid.cli.CmdlineOperations;
import net.filemaid.cli.ConflictAction;
import net.filemaid.cli.ExecCommand;
import net.filemaid.format.ExpressionFileFormat;
import net.filemaid.media.MediaDetection;
import net.filemaid.postprocess.Apply;
import net.filemaid.similarity.Match;
import net.filemaid.util.SystemProperty;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SearchResultDetails;
import net.filemaid.web.Series;

public class CmdlineOperationsTextUI
extends CmdlineOperations {
    protected final Theme theme = SystemProperty.get("net.filemaid.cli.theme", Theme::forName, Theme.BusinessMachine);
    protected final boolean emulator = SystemProperty.get("net.filemaid.cli.emulator", Boolean::parseBoolean, System.console() == null && !GraphicsEnvironment.isHeadless());
    protected final Screen screen = new TerminalScreen(this.createTerminal());
    protected final MultiWindowTextGUI ui = new MultiWindowTextGUI(this.screen, (WindowManager)new DefaultWindowManager(), (Component)new EmptySpace((TextColor)TextColor.ANSI.DEFAULT));

    public CmdlineOperationsTextUI() throws Exception {
        this.theme.setTheme((TextGUI)this.ui);
    }

    protected Terminal createTerminal() throws Exception {
        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
        if (this.emulator) {
            defaultTerminalFactory.setPreferTerminalEmulator(true);
            defaultTerminalFactory.setTerminalEmulatorTitle(Settings.getWindowTitle());
            defaultTerminalFactory.setTerminalEmulatorFrameAutoCloseTrigger(TerminalEmulatorAutoCloseTrigger.CloseOnEscape);
        }
        try {
            return defaultTerminalFactory.createTerminal();
        }
        catch (Exception exception) {
            throw new CmdlineException("Bad Terminal", exception.getMessage(), exception);
        }
    }

    public synchronized <T> T onScreen(Supplier<T> supplier) throws Exception {
        try {
            this.screen.startScreen();
            T t = supplier.get();
            return t;
        }
        finally {
            this.screen.stopScreen();
        }
    }

    @Override
    public List<File> renameAll(Map<File, File> map, RenameAction renameAction, ConflictAction conflictAction, List<Match<File, ?>> list, Apply[] applyArray, ExecCommand execCommand) throws Exception {
        if (map.isEmpty()) {
            return super.renameAll(map, renameAction, conflictAction, list, applyArray, execCommand);
        }
        Map<File, File> map2 = this.showFileMapInputDialog(map, 0, (file, file2) -> file.exists() && !file2.exists(), renameAction + " / " + conflictAction);
        if (map2.isEmpty()) {
            return Collections.emptyList();
        }
        return super.renameAll(map2, renameAction, conflictAction, list, applyArray, execCommand);
    }

    @Override
    protected Collection<SearchResult> lookupSeries(EpisodeListProvider episodeListProvider, boolean bl, Collection<Series> collection, Locale locale, int n) throws Exception {
        List<Series> list = collection.stream().limit(1L).collect(Collectors.toList());
        if (collection.size() > 1) {
            list = this.showInputDialog(collection, 0, Series::toString, series -> series.getScore() == Integer.MAX_VALUE, "Select Series", true);
        }
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<SearchResult> linkedHashSet = new LinkedHashSet<SearchResult>();
        for (Series series2 : list) {
            List<SearchResult> list2;
            SearchResult searchResult = episodeListProvider.id(series2);
            if (searchResult != null) {
                linkedHashSet.add(searchResult);
                continue;
            }
            String string = series2.getName();
            List<SearchResult> list3 = list2 = bl ? episodeListProvider.lookup(string, locale) : episodeListProvider.search(string, locale);
            if (list2.size() > 1) {
                list2 = this.selectSearchResult(string, list2, !bl, true, n);
            }
            linkedHashSet.addAll(list2);
        }
        return linkedHashSet;
    }

    @Override
    protected <T extends SearchResult> List<T> selectSearchResult(String string, Collection<T> collection, boolean bl, boolean bl2, int n) throws Exception {
        List<T> list = MediaDetection.getProbableMatches(bl ? string : null, collection, bl2, false);
        if (list.size() <= 1) {
            return list;
        }
        List<ListItem<SearchResult>> list2 = list.stream().map(searchResult -> {
            if (searchResult instanceof SearchResultDetails) {
                return new ListItem<SearchResult>((SearchResult)searchResult, ((SearchResultDetails)searchResult).getNameWithYear());
            }
            return new ListItem<SearchResult>((SearchResult)searchResult, searchResult.toString());
        }).collect(Collectors.toList());
        ListItem<SearchResult> listItem = this.showInputDialog(list2, "Multiple Options", "Select best match for '" + string + "'");
        if (listItem == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList((T)listItem.getValue());
    }

    @Override
    protected Map<File, File> selectHistory(Collection<File> collection, FileFilter fileFilter, ExpressionFileFormat expressionFileFormat, File file3) throws Exception {
        boolean bl = collection.isEmpty() && fileFilter == null;
        Map<File, File> map = bl ? this.getRevertMap(HistorySpooler.HISTORY.getCompleteHistory(), File::exists, expressionFileFormat, file3) : super.selectHistory(collection, fileFilter, expressionFileFormat, file3);
        String string = !bl ? "Revert Selection" : "Revert History";
        BiPredicate<File, File> biPredicate = (file, file2) -> !bl && !file.exists();
        int n = !bl ? 0 : map.size() - 1;
        return this.showFileMapInputDialog(map, n, biPredicate, string);
    }

    public <T> T showInputDialog(Collection<T> collection, String string, String string2) throws Exception {
        return (T)this.onScreen(() -> {
            ListSelectDialogBuilder listSelectDialogBuilder = new ListSelectDialogBuilder();
            listSelectDialogBuilder.setTitle(string);
            listSelectDialogBuilder.setDescription(string2);
            collection.forEach(arg_0 -> ((ListSelectDialogBuilder)listSelectDialogBuilder).addListItem(arg_0));
            return ((ListSelectDialog)listSelectDialogBuilder.build()).showDialog((WindowBasedTextGUI)this.ui);
        });
    }

    public <T> List<T> showInputDialog(Collection<T> collection, int n, Function<T, String> function, Predicate<T> predicate, String string, final boolean bl) throws Exception {
        return this.onScreen(() -> {
            final ArrayList arrayList = new ArrayList(collection.size());
            final BasicWindow basicWindow = new BasicWindow();
            basicWindow.setTitle(string);
            basicWindow.setHints(Arrays.asList(Window.Hint.MODAL, Window.Hint.CENTERED, Window.Hint.FIT_TERMINAL_WINDOW));
            CheckBoxList checkBoxList = new CheckBoxList<ListItem<T>>(){

                public synchronized Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
                    int n;
                    if (bl && this.isFocused() && keyStroke.getKeyType() == KeyType.Enter && (n = this.getSelectedIndex()) >= 0) {
                        List<ListItem<T>> list = this.getCheckedItems();
                        if (list.size() == 0 || this.isChecked(n).booleanValue() && list.size() == 1) {
                            arrayList.add(((ListItem)this.getItemAt(n)).getValue());
                            basicWindow.close();
                        } else {
                            for (ListItem<T> listItem : list) {
                                this.setChecked(listItem, false);
                            }
                            this.setChecked((ListItem)this.getItemAt(n), true);
                        }
                        return Interactable.Result.HANDLED;
                    }
                    return super.handleKeyStroke(keyStroke);
                }
            };
            for (T item : collection) {
                checkBoxList.addItem(new ListItem<T>(item, function.apply(item)), predicate.test(item));
            }
            checkBoxList.setSelectedIndex(n);
            Button button = new Button("Select", () -> CmdlineOperationsTextUI.collectCheckedItems(checkBoxList, arrayList, basicWindow));
            Button button2 = new Button("Cancel", () -> basicWindow.close());
            Panel panel = new Panel();
            panel.setLayoutManager((LayoutManager)new GridLayout(1));
            panel.addComponent(new Separator(Direction.HORIZONTAL).setLayoutData(GridLayout.createLayoutData((GridLayout.Alignment)GridLayout.Alignment.FILL, (GridLayout.Alignment)GridLayout.Alignment.CENTER, (boolean)true, (boolean)false)));
            panel.addComponent(Panels.grid((int)2, (Component[])new Component[]{button, button2}).setLayoutData(GridLayout.createLayoutData((GridLayout.Alignment)GridLayout.Alignment.END, (GridLayout.Alignment)GridLayout.Alignment.CENTER, (boolean)false, (boolean)false)));
            Panel panel2 = new Panel();
            panel2.setLayoutManager((LayoutManager)new BorderLayout());
            panel2.addComponent(checkBoxList.setLayoutData((LayoutData)BorderLayout.Location.CENTER));
            panel2.addComponent(panel.setLayoutData((LayoutData)BorderLayout.Location.BOTTOM));
            basicWindow.setComponent((Component)panel2);
            this.ui.addWindowAndWait((Window)basicWindow);
            return arrayList;
        });
    }

    public Map<File, File> showFileMapInputDialog(Map<File, File> map, int n, BiPredicate<File, File> biPredicate, String string) throws Exception {
        if (map.isEmpty()) {
            return Collections.emptyMap();
        }
        int n2 = map.keySet().stream().mapToInt(file -> file.getName().length()).max().orElse(0);
        String string2 = "%-" + n2 + "s\t=>\t%s";
        List<Map.Entry<File, File>> list = this.showInputDialog(map.entrySet(), n, entry -> String.format(string2, ((File)entry.getKey()).getName(), ((File)entry.getValue()).getName()), entry -> biPredicate.test((File)entry.getKey(), (File)entry.getValue()), string, false);
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (file, file2) -> file, LinkedHashMap::new));
    }

    private static void collectCheckedItems(CheckBoxList<ListItem<?>> checkBoxList, List<Object> list, BasicWindow basicWindow) {
        for (ListItem<?> listItem : checkBoxList.getCheckedItems()) {
            list.add(listItem.getValue());
        }
        basicWindow.close();
    }

    public static enum Theme {
        Default,
        BigSnake,
        Blaster,
        BusinessMachine,
        Conqueror,
        Defrost;


        public void setTheme(TextGUI textGUI) {
            textGUI.setTheme(LanternaThemes.getRegisteredTheme((String)this.name().toLowerCase(Locale.ROOT)));
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

    protected static class ListItem<T> {
        private final T value;
        private final String label;

        public ListItem(T t, String string) {
            this.value = t;
            this.label = string;
        }

        public T getValue() {
            return this.value;
        }

        public String toString() {
            return this.label;
        }
    }
}

