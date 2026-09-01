package net.filemaid.ui.sfv;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import javax.swing.SwingWorker;
import javax.swing.event.SwingPropertyChangeSupport;
import net.filemaid.Logging;
import net.filemaid.hash.HashType;
import net.filemaid.ui.sfv.ChecksumComputationTask;

class ChecksumCell {
    private final String name;
    private final File root;
    private Map<HashType, String> hashes;
    private ChecksumComputationTask task;
    private Throwable error;
    private final PropertyChangeListener taskListener = new PropertyChangeListener(){

        @Override
        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            if ("state".equals(propertyChangeEvent.getPropertyName())) {
                if (propertyChangeEvent.getNewValue() == SwingWorker.StateValue.DONE) {
                    this.done(propertyChangeEvent);
                }
                ChecksumCell.this.pcs.firePropertyChange("state", null, (Object)ChecksumCell.this.getState());
            } else {
                ChecksumCell.this.pcs.firePropertyChange(propertyChangeEvent.getPropertyName(), propertyChangeEvent.getOldValue(), propertyChangeEvent.getNewValue());
            }
        }

        protected void done(PropertyChangeEvent propertyChangeEvent) {
            try {
                ChecksumCell.this.hashes.putAll((Map)ChecksumCell.this.task.get());
            }
            catch (Exception exception) {
                ChecksumCell.this.error = Logging.findCause(exception, CancellationException.class) == null ? Logging.getRootCause(exception) : null;
            }
            finally {
                ChecksumCell.this.task = null;
            }
        }
    };
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this, true);

    public ChecksumCell(String string, File file, Map<HashType, String> map) {
        this.name = string;
        this.root = file;
        this.hashes = map;
    }

    public ChecksumCell(String string, File file, ChecksumComputationTask checksumComputationTask) {
        this.name = string;
        this.root = file;
        this.hashes = new EnumMap<HashType, String>(HashType.class);
        this.task = checksumComputationTask;
        checksumComputationTask.addPropertyChangeListener(this.taskListener);
    }

    public String getName() {
        return this.name;
    }

    public File getRoot() {
        return this.root;
    }

    public String getChecksum(HashType hashType) {
        return this.hashes.get((Object)hashType);
    }

    public void putTask(ChecksumComputationTask checksumComputationTask) {
        if (this.task != null) {
            this.task.removePropertyChangeListener(this.taskListener);
            this.task.cancel(true);
        }
        this.task = checksumComputationTask;
        this.error = null;
        if (this.task != null) {
            this.task.addPropertyChangeListener(this.taskListener);
        }
        this.pcs.firePropertyChange("state", null, (Object)this.getState());
    }

    public ChecksumComputationTask getTask() {
        return this.task;
    }

    public Throwable getError() {
        return this.error;
    }

    public State getState() {
        if (this.task != null) {
            switch (this.task.getState()) {
                case PENDING: {
                    return State.PENDING;
                }
            }
            return State.PROGRESS;
        }
        if (this.error != null) {
            return State.ERROR;
        }
        return State.READY;
    }

    public void dispose() {
        for (PropertyChangeListener propertyChangeListener : this.pcs.getPropertyChangeListeners()) {
            this.pcs.removePropertyChangeListener(propertyChangeListener);
        }
        if (this.task != null) {
            this.task.removePropertyChangeListener(this.taskListener);
            this.task.cancel(true);
        }
        this.hashes = null;
        this.error = null;
        this.task = null;
        this.pcs = null;
    }

    public String toString() {
        return this.name + " " + this.hashes;
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.pcs.addPropertyChangeListener(propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.pcs.removePropertyChangeListener(propertyChangeListener);
    }

    public static enum State {
        PENDING,
        PROGRESS,
        READY,
        ERROR;

    }
}

