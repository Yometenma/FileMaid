package net.filemaid.util.ui;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;

public class EmptySelectionModel
implements ListSelectionModel {
    @Override
    public void addListSelectionListener(ListSelectionListener listSelectionListener) {
    }

    @Override
    public void addSelectionInterval(int n, int n2) {
    }

    @Override
    public void clearSelection() {
    }

    @Override
    public int getAnchorSelectionIndex() {
        return -1;
    }

    @Override
    public int getLeadSelectionIndex() {
        return -1;
    }

    @Override
    public int getMaxSelectionIndex() {
        return -1;
    }

    @Override
    public int getMinSelectionIndex() {
        return -1;
    }

    @Override
    public int getSelectionMode() {
        return -1;
    }

    @Override
    public boolean getValueIsAdjusting() {
        return false;
    }

    @Override
    public void insertIndexInterval(int n, int n2, boolean bl) {
    }

    @Override
    public boolean isSelectedIndex(int n) {
        return false;
    }

    @Override
    public boolean isSelectionEmpty() {
        return true;
    }

    @Override
    public void removeIndexInterval(int n, int n2) {
    }

    @Override
    public void removeListSelectionListener(ListSelectionListener listSelectionListener) {
    }

    @Override
    public void removeSelectionInterval(int n, int n2) {
    }

    @Override
    public void setAnchorSelectionIndex(int n) {
    }

    @Override
    public void setLeadSelectionIndex(int n) {
    }

    @Override
    public void setSelectionInterval(int n, int n2) {
    }

    @Override
    public void setSelectionMode(int n) {
    }

    @Override
    public void setValueIsAdjusting(boolean bl) {
    }
}

