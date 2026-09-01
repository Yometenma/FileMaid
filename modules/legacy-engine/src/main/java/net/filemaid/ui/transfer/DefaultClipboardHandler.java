package net.filemaid.ui.transfer;

import java.awt.datatransfer.Clipboard;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import net.filemaid.UserInteraction;
import net.filemaid.ui.transfer.ClipboardHandler;
import net.filemaid.util.StringUtilities;

public class DefaultClipboardHandler
implements ClipboardHandler {
    @Override
    public void exportToClipboard(JComponent jComponent, Clipboard clipboard, int n) throws IllegalStateException {
        UserInteraction.copy(clipboard, this.export(jComponent));
    }

    protected String export(JComponent jComponent) {
        if (jComponent instanceof JList) {
            return this.export((JList)jComponent);
        }
        if (jComponent instanceof JTree) {
            return this.export((JTree)jComponent);
        }
        if (jComponent instanceof JTable) {
            return this.export((JTable)jComponent);
        }
        throw new IllegalArgumentException("JComponent not supported: " + jComponent);
    }

    protected String export(Stream<?> stream) {
        return StringUtilities.join(stream, (CharSequence)System.lineSeparator());
    }

    protected String export(JList jList) {
        return this.export(jList.getSelectedValuesList().stream());
    }

    protected String export(JTree jTree) {
        return this.export(Arrays.stream(jTree.getSelectionPaths()).map(TreePath::getLastPathComponent));
    }

    protected String export(JTable jTable) {
        return this.export(Arrays.stream(jTable.getSelectedRows()).map(n -> jTable.getRowSorter().convertRowIndexToModel(n)).mapToObj(n -> IntStream.range(0, jTable.getColumnCount()).mapToObj(n2 -> jTable.getModel().getValueAt(n, n2)).map(object -> Objects.toString(object, "")).collect(Collectors.joining("\t"))));
    }
}

