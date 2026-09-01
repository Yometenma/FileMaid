package net.filemaid.ui.transfer;

import java.io.File;
import net.filemaid.CategoryFileFilter;

public interface FileExportHandler {
    public boolean canExport();

    public void export(File var1) throws Exception;

    public String getDefaultFileName();

    public CategoryFileFilter getFileFilter();
}

