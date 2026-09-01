package net.filemaid.ui.rename;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Window;
import java.io.File;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.ui.BaseDialog;
import net.filemaid.ui.rename.CharacterHighlightPainter;
import net.filemaid.ui.rename.HighlightListCellRenderer;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

class ValidateDialog
extends BaseDialog {
    private JList list;
    private File[] model;
    private boolean cancel = true;
    private final Action validateAction = SwingUI.newAction("Validate", ResourceManager.getIcon("dialog.continue"), actionEvent -> this.validateModel());
    private final Action continueAction = SwingUI.newAction("Continue", ResourceManager.getIcon("dialog.continue.invalid"), actionEvent -> this.finish(false));
    private final Action cancelAction = SwingUI.newAction("Cancel", ResourceManager.getIcon("dialog.cancel"), actionEvent -> this.finish(true));

    public ValidateDialog(Window window, Collection<File> collection) {
        super(window, "Invalid Characters");
        this.model = collection.toArray(new File[0]);
        this.list = new JList<File>(this.model);
        this.list.setEnabled(false);
        this.list.setCellRenderer(new IllegalCharactersListCellRenderer());
        JComponent jComponent = (JComponent)this.getContentPane();
        jComponent.setLayout((LayoutManager)new MigLayout("insets dialog, nogrid, fill", "", "[fill][pref!]"));
        jComponent.add((Component)new JScrollPane(this.list), "grow, wrap");
        jComponent.add((Component)SwingUI.newButton(this.cancelAction), "tag left");
        jComponent.add((Component)SwingUI.newButton(this.validateAction), "tag next");
        jComponent.add((Component)SwingUI.newButton(this.continueAction), "tag ok");
        SwingUI.installAction(jComponent, 27, this.cancelAction);
        this.setDefaultCloseOperation(2);
        this.setMinimumSize(new Dimension(365, 280));
        this.pack();
        if (!FileUtilities.UNIX) {
            this.setDelayedEnabled(this.continueAction, 3000);
        }
        SwingUtilities.invokeLater(jComponent.getComponent(2)::requestFocusInWindow);
    }

    public void setDelayedEnabled(Action action, int n) {
        action.setEnabled(false);
        this.addWindowListener(SwingUI.windowOpened(windowEvent -> SwingUI.invokeLater(n, () -> action.setEnabled(true))));
    }

    public List<File> getModel() {
        return Collections.unmodifiableList(Arrays.asList(this.model));
    }

    public void validateModel() {
        for (int i = 0; i < this.model.length; ++i) {
            this.model[i] = FileUtilities.validateFilePath(this.model[i]);
        }
        this.list.repaint();
        this.validateAction.setEnabled(false);
        this.continueAction.setEnabled(true);
        this.continueAction.putValue("SmallIcon", ResourceManager.getIcon("dialog.continue"));
    }

    public boolean cancel() {
        return this.cancel;
    }

    private void finish(boolean bl) {
        this.cancel = bl;
        this.setVisible(false);
    }

    public static boolean validate(Component component, Map<File, File> map) {
        return ValidateDialog.validate(component, MapList.view(map));
    }

    public static boolean validate(Component component, List<File> list) {
        IndexView<File> indexView = ValidateDialog.getInvalidFilePathIndex(list);
        if (indexView.isEmpty()) {
            return true;
        }
        ValidateDialog validateDialog = new ValidateDialog(SwingUI.getWindow(component), indexView);
        validateDialog.setLocation(SwingUI.getOffsetLocation(validateDialog));
        validateDialog.setVisible(true);
        if (validateDialog.cancel()) {
            return false;
        }
        List<File> list2 = validateDialog.getModel();
        for (int i = 0; i < indexView.size(); ++i) {
            indexView.set(i, list2.get(i));
        }
        return true;
    }

    private static IndexView<File> getInvalidFilePathIndex(List<File> list) {
        IndexView<File> indexView = new IndexView<File>(list);
        HashMap<File, Boolean> hashMap = new HashMap<File, Boolean>();
        for (int i = 0; i < list.size(); ++i) {
            File file = list.get(i);
            if (FileUtilities.isInvalidFilePathComponent(file)) {
                indexView.addIndex(i);
                continue;
            }
            File file3 = file.getParentFile();
            if (file3 == null || !FileUtilities.listPath(file3).stream().anyMatch(path -> hashMap.computeIfAbsent(path, p -> FileUtilities.isInvalidFilePathComponent(p)))) continue;
            indexView.addIndex(i);
        }
        return indexView;
    }

    private static class IllegalCharactersListCellRenderer
    extends HighlightListCellRenderer {
        public IllegalCharactersListCellRenderer() {
            super(FileUtilities.ILLEGAL_CHARACTERS, new CharacterHighlightPainter(new Color(16728576), new Color(16716288)), 4);
        }

        @Override
        protected void updateHighlighter() {
            this.textComponent.getHighlighter().removeAllHighlights();
            Matcher matcher = this.pattern.matcher(this.textComponent.getText());
            File file = new File(this.textComponent.getText());
            for (File file2 : FileUtilities.listPath(file)) {
                if (!FileUtilities.isInvalidFilePathComponent(file2)) continue;
                int n = file2.getPath().length();
                matcher.region(n - file2.getName().length(), n);
                while (matcher.find()) {
                    try {
                        this.textComponent.getHighlighter().addHighlight(matcher.start(0), matcher.end(0), this.highlightPainter);
                    }
                    catch (Exception exception) {
                        Logging.trace(exception);
                    }
                }
            }
        }
    }

    private static class MapList<E>
    extends AbstractList<E> {
        private List<Map.Entry<?, E>> values;

        private MapList(Map<?, E> map) {
            this.values = map.entrySet().stream().collect(Collectors.toList());
        }

        @Override
        public E get(int n) {
            return this.values.get(n).getValue();
        }

        @Override
        public E set(int n, E e) {
            return this.values.get(n).setValue(e);
        }

        @Override
        public int size() {
            return this.values.size();
        }

        public static <E> List<E> view(Map<?, E> map) {
            return new MapList<E>(map);
        }
    }

    private static class IndexView<E>
    extends AbstractList<E> {
        private List<Integer> mapping = new ArrayList<Integer>();
        private List<E> source;

        public IndexView(List<E> list) {
            this.source = list;
        }

        public boolean addIndex(int n) {
            return this.mapping.add(n);
        }

        @Override
        public E get(int n) {
            int n2 = this.mapping.get(n);
            return n2 >= 0 ? (E)this.source.get(n2) : null;
        }

        @Override
        public E set(int n, E e) {
            return this.source.set(this.mapping.get(n), e);
        }

        @Override
        public int size() {
            return this.mapping.size();
        }
    }
}

