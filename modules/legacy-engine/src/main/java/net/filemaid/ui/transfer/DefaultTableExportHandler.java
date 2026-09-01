package net.filemaid.ui.transfer;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.JTable;
import net.filemaid.CategoryFileFilter;
import net.filemaid.ui.transfer.TextFileExportHandler;
import net.filemaid.util.ExtensionFileFilter;

public class DefaultTableExportHandler
extends TextFileExportHandler {
    private final JTable table;

    public DefaultTableExportHandler(JTable jTable) {
        this.table = jTable;
    }

    @Override
    public String getDefaultFileName() {
        return "table.tsv";
    }

    @Override
    public CategoryFileFilter getFileFilter() {
        return new CategoryFileFilter("TSV", new ExtensionFileFilter("tsv"));
    }

    @Override
    public boolean canExport() {
        return true;
    }

    @Override
    public void export(PrintWriter printWriter, boolean bl) {
        IntStream intStream = bl ? Arrays.stream(this.table.getSelectedRows()) : IntStream.range(0, this.table.getRowCount());
        intStream.map(n -> this.table.getRowSorter().convertRowIndexToModel(n)).mapToObj(n -> IntStream.range(0, this.table.getColumnCount()).mapToObj(n2 -> this.table.getModel().getValueAt(n, n2)).map(object -> Objects.toString(object, "")).collect(Collectors.joining("\t"))).forEach(printWriter::println);
    }
}

