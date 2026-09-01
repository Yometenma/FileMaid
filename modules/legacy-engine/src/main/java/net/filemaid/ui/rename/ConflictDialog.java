package net.filemaid.ui.rename;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.UserFiles;
import net.filemaid.UserInteraction;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.media.VideoQuality;
import net.filemaid.ui.BaseDialog;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.DefaultFancyListCellRenderer;
import net.filemaid.util.ui.GlassProgressMonitor;
import net.filemaid.util.ui.HorizontalRule;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

class ConflictDialog
extends BaseDialog {
    private EventList<Conflict> model = new BasicEventList();
    private boolean cancel = true;

    public ConflictDialog(Window window, List<Conflict> list) {
        super(window, "Conflicts");
        this.model.addAll(list);
        JList jList = new JList(new DefaultEventListModel(this.model));
        jList.setCellRenderer(new ConflictCellRenderer());
        jList.setSelectionMode(0);
        JButton jButton = SwingUI.newButton("Skip", ResourceManager.getIcon("dialog.continue"), this::ok);
        JButton jButton2 = SwingUI.newButton("Cancel", ResourceManager.getIcon("dialog.cancel"), this::cancel);
        JButton jButton3 = SwingUI.newButton("Continue", ResourceManager.getIcon("dialog.continue.invalid"), this::overwrite);
        JComponent jComponent = (JComponent)this.getContentPane();
        jComponent.setLayout((LayoutManager)new MigLayout("insets dialog, nogrid, fill", "", "[fill][pref!]"));
        jComponent.add((Component)new JScrollPane(jList), "grow, wrap");
        jComponent.add((Component)jButton2, "tag left");
        jComponent.add((Component)jButton3, "tag next");
        jComponent.add((Component)jButton, "tag ok");
        jButton3.setVisible(list.stream().anyMatch(conflict -> conflict.overwrite));
        jList.addMouseListener(SwingUI.mouseDoubleClicked(mouseEvent -> {
            Conflict conflict = (Conflict)jList.getSelectedValue();
            if (conflict != null) {
                List<File> files = Stream.of(conflict.source, conflict.destination).filter(File::exists).distinct().collect(Collectors.toList());
                UserInteraction.revealFiles(files);
            }
        }));
        this.setBackground(ThemeSupport.getPanelBackground());
        this.setDefaultCloseOperation(2);
        this.setResizable(true);
        this.setMinimumSize(new Dimension(340, 280));
        this.setSize(new Dimension(560, 380));
        SwingUI.installAction(jComponent, 27, SwingUI.newAction("Cancel", this::cancel));
        SwingUtilities.invokeLater(jButton::requestFocusInWindow);
    }

    public boolean cancel() {
        return this.cancel;
    }

    public List<Conflict> getConflicts() {
        return this.model;
    }

    private void overwrite(ActionEvent actionEvent) {
        SwingUI.withWaitCursor((Object)actionEvent, () -> {
            List list = this.model.stream().map(conflict -> {
                if (!conflict.overwrite) {
                    return conflict;
                }
                try {
                    if (FileUtilities.sameFile(conflict.source, conflict.destination)) {
                        String string = ConflictDialog.formatDetails("Destination path %s already exists and cannot be deleted.", "%s and %s are the same file.", conflict.destination, conflict.source, conflict.destination);
                        return new Conflict(conflict.source, conflict.destination, Collections.singletonMap(Conflict.Kind.FILE_EXISTS_DELETE_FAILED, string), false);
                    }
                    if (conflict.is(Conflict.Kind.FILE_EXISTS) && !SwingUI.isShiftOrAltDown(actionEvent) && VideoQuality.isBetter(conflict.destination, conflict.source)) {
                        String string = ConflictDialog.formatDetails("Destination path %s already exists and is higher quality.", "Press %s again to overwrite %s (%s) with %s (%s) regardless.", conflict.destination, "Continue", conflict.destination, conflict.destination.length(), conflict.source, conflict.source.length());
                        return new Conflict(conflict.source, conflict.destination, Collections.singletonMap(Conflict.Kind.FILE_EXISTS_IS_BETTER, string), true);
                    }
                    UserFiles.trash(conflict.destination);
                }
                catch (Exception exception) {
                    String string = ConflictDialog.formatDetails("Destination path %s already exists and cannot be deleted.", Logging.cause(exception).toString().replace("%", "%%"), conflict.destination);
                    return new Conflict(conflict.source, conflict.destination, Collections.singletonMap(Conflict.Kind.FILE_EXISTS_DELETE_FAILED, string), false);
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            this.model.clear();
            this.model.addAll(list);
            if (list.isEmpty()) {
                this.ok(actionEvent);
            }
        });
    }

    public void ok(ActionEvent actionEvent) {
        this.cancel = false;
        this.setVisible(false);
    }

    public void cancel(ActionEvent actionEvent) {
        this.cancel = true;
        this.setVisible(false);
    }

    private static Map<File, List<List<File>>> getSourceGroupsForDestination(Map<File, File> map, Comparator<File> comparator) {
        HashMap<File, List<List<File>>> hashMap = new HashMap<File, List<List<File>>>(map.size());
        map.entrySet().stream().collect(Collectors.groupingBy(entry -> FileUtilities.resolveSibling((File)entry.getKey(), (File)entry.getValue()), Collectors.mapping(entry -> (File)entry.getKey(), Collectors.toList()))).forEach((file, list2) -> {
            List<List<File>> list3 = MediaFileUtilities.groupByMediaCharacteristics(list2);
            list3.sort(Comparator.comparing(list -> (File)list.stream().max(comparator).get(), comparator));
            hashMap.put((File)file, list3);
        });
        return hashMap;
    }

    private static String formatDetails(String string2, String string3, Object ... objectArray) {
        StringBuilder stringBuilder = new StringBuilder(64).append("<html><p style='padding:3px 6px'>");
        stringBuilder.append(String.format(String.join((CharSequence)"<br>", string2, string3), Arrays.stream(objectArray).map(object -> {
            if (object instanceof File) {
                File file = (File)object;
                return file.getName();
            }
            if (object instanceof File[]) {
                File[] fileArray = (File[])object;
                return Arrays.stream(fileArray).filter(Objects::nonNull).map(File::getName).collect(Collectors.joining(" | ", "[", "]"));
            }
            if (object instanceof Long) {
                return FileUtilities.formatSize((Long)object);
            }
            return String.valueOf(object);
        }).map(string -> ConflictDialog.formatDetailsVariable(string)).toArray()));
        return stringBuilder.append("</p></html>").toString();
    }

    private static String formatDetailsVariable(String string) {
        return "<nobr><span style='color:#32D515'>" + string + "</span></nobr>";
    }

    public static boolean check(Window window, Map<File, File> map) throws Exception {
        ConflictWorker conflictWorker = new ConflictWorker(map);
        Conflict[] conflictArray = GlassProgressMonitor.runTask(conflictWorker, window);
        if (conflictArray == null) {
            return false;
        }
        if (conflictArray.length == 0) {
            return true;
        }
        ConflictDialog conflictDialog = new ConflictDialog(window, Arrays.asList(conflictArray));
        conflictDialog.setLocation(SwingUI.getOffsetLocation(conflictDialog));
        conflictDialog.setVisible(true);
        if (conflictDialog.cancel()) {
            return false;
        }
        for (Conflict conflict : conflictDialog.getConflicts()) {
            map.remove(conflict.source);
        }
        return true;
    }

    private static class ConflictCellRenderer
    extends DefaultFancyListCellRenderer {
        public ConflictCellRenderer() {
            this.setHighlightingEnabled(false);
            HorizontalRule.south(this, 2, ThemeSupport.getColor(0xEEEEEE), ThemeSupport.getPanelBackground());
        }

        @Override
        protected void configureListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
            Conflict conflict = (Conflict)object;
            super.configureListCellRendererComponent(jList, conflict.getDetails(), n, bl, bl2);
            this.setIcon(ResourceManager.getIcon("status.warning"));
            this.setToolTipText(this.formatToolTip(conflict));
            this.setBorderPainted(n < jList.getModel().getSize() - 1);
        }

        private String formatToolTip(Conflict conflict) {
            StringBuilder stringBuilder = new StringBuilder(64).append("<html>");
            this.appendTooltipParagraph(stringBuilder, "Conflict", conflict.issues.keySet().stream().map(Objects::toString).collect(Collectors.joining(" | ")));
            this.appendTooltipParagraph(stringBuilder, "Source", conflict.source.getPath());
            this.appendTooltipParagraph(stringBuilder, "Destination", conflict.destination.getPath());
            return stringBuilder.append("</html>").toString();
        }

        private StringBuilder appendTooltipParagraph(StringBuilder stringBuilder, String string, Object object) {
            return stringBuilder.append("<p style='width:350px; margin:3px'><b>").append(string).append(":</b><br>").append(SwingUI.escapeHTML(object.toString())).append("</p>");
        }
    }

    private static class ConflictWorker
    implements GlassProgressMonitor.ProgressWorker<Conflict[]> {
        private final Map<File, File> renameMap;

        public ConflictWorker(Map<File, File> map) {
            this.renameMap = map;
        }

        @Override
        public String getName() {
            return "Checking for conflicts...";
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public String getDescription() {
            return "Preparing...";
        }

        @Override
        public boolean isIndeterminate() {
            return true;
        }

        @Override
        public Conflict[] call(Consumer<String> consumer, Consumer<String> consumer2, BiConsumer<Integer, Integer> biConsumer, Supplier<Boolean> supplier) throws Exception {
            ArrayList<Conflict> arrayList = new ArrayList<Conflict>();
            Map<File, List<List<File>>> map = ConflictDialog.getSourceGroupsForDestination(this.renameMap, VideoQuality.DESCENDING_ORDER);
            int n = 0;
            for (Map.Entry<File, File> entry : this.renameMap.entrySet()) {
                Object object;
                if (supplier.get().booleanValue()) {
                    return null;
                }
                biConsumer.accept(n++, this.renameMap.size());
                consumer2.accept(entry.getValue().getPath());
                File file = entry.getKey();
                File file2 = FileUtilities.resolveSibling(file, entry.getValue());
                EnumMap<Conflict.Kind, String> enumMap = new EnumMap<Conflict.Kind, String>(Conflict.Kind.class);
                if (!file.exists()) {
                    object = ConflictDialog.formatDetails("Source file path [%s] does not exist.", "This file will be skipped since it has been moved or deleted already.", file);
                    enumMap.put(Conflict.Kind.FILE_NOT_FOUND, (String)object);
                }
                if (FileUtilities.getExtension(file2.getName()) == null && file.isFile()) {
                    object = ConflictDialog.formatDetails("Destination file path [%s] has no file extension.", "Please edit your custom format to generate valid file paths.", file2);
                    enumMap.put(Conflict.Kind.MISSING_EXTENSION, (String)object);
                }
                List<List<File>> groups = map.get(file2);
                if (groups.size() > 1 || groups.get(0).size() > 1) {
                    groups.stream().flatMap(Collection::stream).findFirst().ifPresent(file3 -> {
                        if (file.equals(file3)) {
                            return;
                        }
                        File[] fileArray = new File[]{file, file3};
                        String string = ConflictDialog.formatDetails("Multiple files map to the same destination path %s \u2794 %s.", "The highest-quality file %s was chosen over %s.", fileArray, file2, file3, file);
                        enumMap.put(Conflict.Kind.LOW_QUALITY_DUPLICATE, string);
                    });
                }
                if (!FileUtilities.sameFile(file, file2) || !FileUtilities.sameFile(file.getParentFile(), file2.getParentFile())) {
                    String string;
                    File file4 = this.renameMap.get(file2);
                    if (file4 != null) {
                        string = ConflictDialog.formatDetails("Cyclic mapping between %s \u2794 %s and %s \u2794 %s.", "Please edit your custom format to generate unique file paths.", file, file2, file2, file4);
                        enumMap.put(Conflict.Kind.CYCLIC, string);
                    }
                    if (file2.exists()) {
                        string = ConflictDialog.formatDetails("Destination path %s already exists.", "Press %s to replace %s with %s.", file2, "Continue", file2, file);
                        enumMap.put(Conflict.Kind.FILE_EXISTS, string);
                    }
                }
                if (enumMap.size() <= 0) continue;
                boolean bl = enumMap.containsKey((Object)Conflict.Kind.FILE_EXISTS) && enumMap.size() == 1;
                arrayList.add(new Conflict(file, file2, enumMap, bl));
            }
            return (Conflict[])arrayList.stream().sorted(Conflict.SEVERITY_ORDER).toArray(Conflict[]::new);
        }
    }

    public static class Conflict {
        public static final Comparator<Conflict> SEVERITY_ORDER = Comparator.comparing(Conflict::getKind, Comparator.<Conflict.Kind>comparingInt(Enum::ordinal).reversed()).thenComparing(Conflict::getDetails);
        public final File source;
        public final File destination;
        public final Map<Kind, String> issues;
        public final boolean overwrite;

        public Conflict(File file, File file2, Map<Kind, String> map, boolean bl) {
            this.source = file;
            this.destination = file2;
            this.issues = map;
            this.overwrite = bl;
        }

        public boolean is(Kind kind) {
            return this.issues.keySet().contains((Object)kind);
        }

        public Kind getKind() {
            return this.issues.keySet().iterator().next();
        }

        public String getDetails() {
            return this.issues.values().iterator().next();
        }

        public String toString() {
            return this.issues.toString();
        }

        public static enum Kind {
            FILE_NOT_FOUND,
            MISSING_EXTENSION,
            CYCLIC,
            LOW_QUALITY_DUPLICATE,
            FILE_EXISTS,
            FILE_EXISTS_IS_BETTER,
            FILE_EXISTS_DELETE_FAILED;


            public String toString() {
                switch (this) {
                    case FILE_NOT_FOUND: {
                        return "Source file does not exist";
                    }
                    case MISSING_EXTENSION: {
                        return "Missing file extension";
                    }
                    case CYCLIC: {
                        return "Destination file is a source file";
                    }
                    case LOW_QUALITY_DUPLICATE: {
                        return "Destination file already chosen";
                    }
                    case FILE_EXISTS: {
                        return "Destination file already exists";
                    }
                    case FILE_EXISTS_IS_BETTER: {
                        return "Destination file already exists and is higher quality";
                    }
                    case FILE_EXISTS_DELETE_FAILED: {
                        return "Destination file already exists and cannot be deleted";
                    }
                }
                return null;
            }
        }
    }
}

