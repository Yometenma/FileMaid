package net.filemaid.ui.subtitle.upload;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import net.filemaid.Language;
import net.filemaid.ui.subtitle.upload.Status;
import net.filemaid.ui.subtitle.upload.SubtitleMapping;
import net.filemaid.web.Movie;

class SubtitleMappingTableModel
extends AbstractTableModel {
    private SubtitleMapping[] data;
    private Runnable onCheckPending;

    public SubtitleMappingTableModel() {
        this.data = new SubtitleMapping[0];
    }

    public SubtitleMappingTableModel(Collection<SubtitleMapping> collection) {
        this.data = collection.toArray(new SubtitleMapping[collection.size()]);
        for (int i = 0; i < this.data.length; ++i) {
            this.data[i].addPropertyChangeListener(new UpdateRowListener(i));
        }
    }

    public SubtitleMappingTableModel onCheckPending(Runnable runnable) {
        this.onCheckPending = runnable;
        return this;
    }

    public SubtitleMapping[] getData() {
        return (SubtitleMapping[])this.data.clone();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int n) {
        switch (n) {
            case 0: {
                return "Movie / Series";
            }
            case 1: {
                return "Video File";
            }
            case 2: {
                return "Subtitle File";
            }
            case 3: {
                return "Language";
            }
            case 4: {
                return "Status";
            }
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return this.data.length;
    }

    @Override
    public Object getValueAt(int n, int n2) {
        switch (n2) {
            case 0: {
                return this.data[n].getIdentity();
            }
            case 1: {
                return this.data[n].getVideo();
            }
            case 2: {
                return this.data[n].getSubtitle();
            }
            case 3: {
                return this.data[n].getLanguage();
            }
            case 4: {
                return this.data[n].getStatus();
            }
        }
        return null;
    }

    @Override
    public void setValueAt(Object object, int n, int n2) {
        if (this.getColumnClass(n2) == Language.class && object instanceof Language) {
            this.data[n].setLanguage((Language)object);
            if (this.data[n].getStatus() == Status.IdentificationRequired) {
                this.data[n].setState(Status.CheckPending);
            }
        }
    }

    @Override
    public boolean isCellEditable(int n, int n2) {
        return (n2 == 0 || n2 == 1 || n2 == 3) && EnumSet.of(Status.IdentificationRequired, Status.UploadReady, Status.IllegalInput).contains((Object)this.data[n].getStatus());
    }

    @Override
    public Class<?> getColumnClass(int n) {
        switch (n) {
            case 0: {
                return Movie.class;
            }
            case 1: {
                return File.class;
            }
            case 2: {
                return File.class;
            }
            case 3: {
                return Language.class;
            }
            case 4: {
                return Status.class;
            }
        }
        return null;
    }

    private class UpdateRowListener
    implements PropertyChangeListener {
        private final int index;

        public UpdateRowListener(int n) {
            this.index = n;
        }

        @Override
        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            SubtitleMappingTableModel.this.fireTableRowsUpdated(this.index, this.index);
            if (propertyChangeEvent.getNewValue().equals((Object)Status.CheckPending)) {
                SwingUtilities.invokeLater(SubtitleMappingTableModel.this.onCheckPending);
            }
        }
    }
}

