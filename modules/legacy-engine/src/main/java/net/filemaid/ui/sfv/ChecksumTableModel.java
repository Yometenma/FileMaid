package net.filemaid.ui.sfv;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;
import net.filemaid.hash.HashType;
import net.filemaid.ui.sfv.ChecksumCell;
import net.filemaid.ui.sfv.ChecksumRow;
import net.filemaid.util.FileUtilities;

class ChecksumTableModel
extends AbstractTableModel {
    private final IndexedMap<String, ChecksumRow> rows = new IndexedMap<String, ChecksumRow>(){

        @Override
        public String key(ChecksumRow checksumRow) {
            return checksumRow.getName();
        }
    };
    private final List<File> checksumColumns = new ArrayList<File>(4);
    public static final String HASH_TYPE_PROPERTY = "hashType";
    private HashType hashType = HashType.SFV;
    private final PropertyChangeListener stateListener = propertyChangeEvent -> {
        int n = this.getRowIndex((ChecksumRow)propertyChangeEvent.getSource());
        if (n >= 0) {
            this.fireTableRowsUpdated(n, n);
        }
    };
    private final PropertyChangeListener progressListener = propertyChangeEvent -> {
        ChecksumCell checksumCell = (ChecksumCell)propertyChangeEvent.getSource();
        int n = this.getRowIndex(checksumCell);
        int n2 = this.getColumnIndex(checksumCell);
        if (n >= 0 && n2 >= 0) {
            this.fireTableCellUpdated(n, n2);
        }
    };
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    ChecksumTableModel() {
    }

    @Override
    public String getColumnName(int n) {
        switch (n) {
            case 0: {
                return "State";
            }
            case 1: {
                return "Name";
            }
        }
        return FileUtilities.getFolderName(this.getColumnRoot(n));
    }

    @Override
    public Class<?> getColumnClass(int n) {
        switch (n) {
            case 0: {
                return ChecksumRow.State.class;
            }
            case 1: {
                return String.class;
            }
        }
        return ChecksumCell.class;
    }

    protected int getColumnIndex(ChecksumCell checksumCell) {
        int n = this.checksumColumns.indexOf(checksumCell.getRoot());
        if (n < 0) {
            return -1;
        }
        return n + 2;
    }

    public File getColumnRoot(int n) {
        return this.checksumColumns.get(n - 2);
    }

    public boolean isVerificationColumn(int n) {
        return n >= 2 && this.getColumnRoot(n).isFile();
    }

    public List<File> getChecksumColumns() {
        return Collections.unmodifiableList(this.checksumColumns);
    }

    @Override
    public int getColumnCount() {
        return this.checksumColumns.size() + 2;
    }

    protected int getRowIndex(ChecksumRow checksumRow) {
        return this.rows.getIndexByKey(checksumRow.getName());
    }

    protected int getRowIndex(ChecksumCell checksumCell) {
        return this.rows.getIndexByKey(checksumCell.getName());
    }

    public List<ChecksumRow> rows() {
        return Collections.unmodifiableList(this.rows);
    }

    @Override
    public int getRowCount() {
        return this.rows.size();
    }

    public void setHashType(HashType hashType) {
        HashType hashType2 = this.hashType;
        this.hashType = hashType;
        this.fireTableDataChanged();
        this.pcs.firePropertyChange(HASH_TYPE_PROPERTY, (Object)hashType2, (Object)hashType);
    }

    public HashType getHashType() {
        return this.hashType;
    }

    @Override
    public Object getValueAt(int n, int n2) {
        ChecksumRow checksumRow = this.rows.get(n);
        switch (n2) {
            case 0: {
                return checksumRow.getState();
            }
            case 1: {
                return checksumRow.getName();
            }
        }
        ChecksumCell checksumCell = checksumRow.getChecksum(this.getColumnRoot(n2));
        if (checksumCell == null) {
            return null;
        }
        switch (checksumCell.getState()) {
            case READY: {
                return checksumCell.getChecksum(this.hashType);
            }
            case ERROR: {
                return checksumCell.getError();
            }
        }
        return checksumCell.getTask();
    }

    public void addAll(Collection<ChecksumCell> collection) {
        int n;
        ArrayList<ChecksumCell> arrayList = new ArrayList<ChecksumCell>();
        int n2 = this.getRowCount();
        int n3 = this.getColumnCount();
        for (ChecksumCell checksumCell : collection) {
            ChecksumRow checksumRow;
            n = this.getRowIndex(checksumCell);
            if (n >= 0) {
                checksumRow = this.rows.get(n);
            } else {
                checksumRow = new ChecksumRow(checksumCell.getName());
                checksumRow.addPropertyChangeListener(this.stateListener);
                this.rows.add(checksumRow);
            }
            ChecksumCell checksumCell2 = checksumRow.put(checksumCell);
            if (checksumCell2 != null) {
                checksumCell2.dispose();
                arrayList.add(checksumCell);
            }
            checksumCell.addPropertyChangeListener(this.progressListener);
            if (this.checksumColumns.contains(checksumCell.getRoot())) continue;
            this.checksumColumns.add(checksumCell.getRoot());
        }
        if (n3 != this.getColumnCount()) {
            this.fireTableStructureChanged();
            return;
        }
        for (ChecksumCell checksumCell : arrayList) {
            n = this.getRowIndex(checksumCell);
            this.fireTableRowsUpdated(n, n);
        }
        if (n2 != this.getRowCount()) {
            this.fireTableRowsInserted(n2, this.getRowCount() - 1);
        }
    }

    public void remove(int ... nArray) {
        Arrays.sort(nArray);
        for (int n : nArray) {
            this.rows.get(n).dispose();
        }
        this.rows.removeAll(nArray);
        this.fireTableRowsDeleted(nArray[0], nArray[nArray.length - 1]);
    }

    public void clear() {
        this.checksumColumns.clear();
        this.rows.clear();
        this.fireTableStructureChanged();
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.pcs.addPropertyChangeListener(propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.pcs.removePropertyChangeListener(propertyChangeListener);
    }

    protected static abstract class IndexedMap<K, V>
    extends AbstractList<V> {
        private final Map<K, Integer> indexMap = new HashMap<K, Integer>(64);
        private final List<V> list = new ArrayList<V>(64);

        protected IndexedMap() {
        }

        public abstract K key(V var1);

        @Override
        public V get(int n) {
            return this.list.get(n);
        }

        public int getIndexByKey(K k) {
            Integer n = this.indexMap.get(k);
            if (n == null) {
                return -1;
            }
            return n;
        }

        @Override
        public boolean add(V v) {
            K k = this.key(v);
            Integer n = this.indexMap.get(k);
            if (n == null && this.list.add(v)) {
                this.indexMap.put(k, this.lastIndexOf(v));
                return true;
            }
            return false;
        }

        public void removeAll(int ... nArray) {
            Arrays.sort(nArray);
            for (int i = nArray.length - 1; i >= 0; --i) {
                V v = this.list.remove(nArray[i]);
                this.indexMap.remove(this.key(v));
            }
            this.updateIndexMap();
        }

        private void updateIndexMap() {
            for (int i = 0; i < this.list.size(); ++i) {
                this.indexMap.put(this.key(this.list.get(i)), i);
            }
        }

        @Override
        public int size() {
            return this.list.size();
        }

        @Override
        public void clear() {
            this.list.clear();
            this.indexMap.clear();
        }
    }
}

