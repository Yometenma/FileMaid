package net.filemaid.ui.sfv;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import net.filemaid.CategoryFileFilter;
import net.filemaid.hash.HashType;
import net.filemaid.hash.VerificationFileWriter;
import net.filemaid.ui.sfv.ChecksumCell;
import net.filemaid.ui.sfv.ChecksumRow;
import net.filemaid.ui.sfv.ChecksumTableModel;
import net.filemaid.ui.transfer.TextFileExportHandler;
import net.filemaid.util.FileUtilities;

class ChecksumTableExportHandler
extends TextFileExportHandler {
    private final ChecksumTableModel model;

    public ChecksumTableExportHandler(ChecksumTableModel checksumTableModel) {
        this.model = checksumTableModel;
    }

    @Override
    public boolean canExport() {
        return this.model.getRowCount() > 0 && this.defaultColumn() != null;
    }

    @Override
    public void export(PrintWriter printWriter, boolean bl) {
        this.export(new VerificationFileWriter(printWriter, this.model.getHashType().getFormat(), StandardCharsets.UTF_8), this.defaultColumn(), this.model.getHashType());
    }

    @Override
    public String getDefaultFileName() {
        return this.getDefaultFileName(this.defaultColumn());
    }

    protected File defaultColumn() {
        for (File file : this.model.getChecksumColumns()) {
            if (!file.isDirectory()) continue;
            return file;
        }
        return null;
    }

    public boolean canExport(File file) {
        return this.model.getRowCount() > 0 && this.model.getChecksumColumns().contains(file);
    }

    public void export(File file, File file2) throws IOException {
        try (VerificationFileWriter verificationFileWriter = new VerificationFileWriter(file, this.model.getHashType().getFormat(), StandardCharsets.UTF_8);){
            this.export(verificationFileWriter, file2, this.model.getHashType());
        }
    }

    public void export(VerificationFileWriter verificationFileWriter, File file, HashType hashType) {
        for (ChecksumRow checksumRow : this.model.rows()) {
            ChecksumCell checksumCell = checksumRow.getChecksum(file);
            if (checksumCell == null) {
                return;
            }
            String string = checksumCell.getChecksum(hashType);
            if (string == null) {
                return;
            }
            verificationFileWriter.write(checksumCell.getName(), string);
        }
    }

    public String getDefaultFileName(File file) {
        return FileUtilities.getName(file) + "." + this.getFileFilter().extension();
    }

    @Override
    public CategoryFileFilter getFileFilter() {
        return this.model.getHashType().getFilter();
    }
}

