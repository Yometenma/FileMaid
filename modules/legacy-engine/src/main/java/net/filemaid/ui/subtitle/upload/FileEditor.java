package net.filemaid.ui.subtitle.upload;

import java.awt.Component;
import java.io.File;
import java.util.EventObject;
import java.util.List;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import net.filemaid.CategoryFileFilter;
import net.filemaid.MediaTypes;
import net.filemaid.UserFiles;
import net.filemaid.ui.subtitle.upload.Status;
import net.filemaid.ui.subtitle.upload.SubtitleMapping;
import net.filemaid.ui.subtitle.upload.SubtitleMappingTableModel;

class FileEditor
implements TableCellEditor {
    FileEditor() {
    }

    @Override
    public Component getTableCellEditorComponent(JTable jTable, Object object, boolean bl, int n, int n2) {
        SubtitleMappingTableModel subtitleMappingTableModel = (SubtitleMappingTableModel)jTable.getModel();
        SubtitleMapping subtitleMapping = subtitleMappingTableModel.getData()[jTable.convertRowIndexToModel(n)];
        List<File> list = UserFiles.showLoadDialogSelectFiles(false, false, subtitleMapping.getSubtitle().getParentFile(), new CategoryFileFilter("Video", MediaTypes.VIDEO_FILES), "Select Video File", new EventObject(jTable));
        if (list.size() > 0) {
            subtitleMapping.setVideo(list.get(0));
            subtitleMapping.setState(Status.CheckPending);
        }
        return null;
    }

    @Override
    public boolean stopCellEditing() {
        return true;
    }

    @Override
    public boolean shouldSelectCell(EventObject eventObject) {
        return false;
    }

    @Override
    public void removeCellEditorListener(CellEditorListener cellEditorListener) {
    }

    @Override
    public boolean isCellEditable(EventObject eventObject) {
        return true;
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public void cancelCellEditing() {
    }

    @Override
    public void addCellEditorListener(CellEditorListener cellEditorListener) {
    }
}

