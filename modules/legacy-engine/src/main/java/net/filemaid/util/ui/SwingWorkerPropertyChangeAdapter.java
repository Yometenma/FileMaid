package net.filemaid.util.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.SwingWorker;

public abstract class SwingWorkerPropertyChangeAdapter
implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        if (propertyChangeEvent.getPropertyName().equals("progress")) {
            this.progress(propertyChangeEvent);
        } else if (propertyChangeEvent.getPropertyName().equals("state")) {
            this.state(propertyChangeEvent);
        } else {
            this.event(propertyChangeEvent.getPropertyName(), propertyChangeEvent.getOldValue(), propertyChangeEvent.getNewValue());
        }
    }

    protected void state(PropertyChangeEvent propertyChangeEvent) {
        switch ((SwingWorker.StateValue)((Object)propertyChangeEvent.getNewValue())) {
            case STARTED: {
                this.started(propertyChangeEvent);
                break;
            }
            case DONE: {
                this.done(propertyChangeEvent);
                break;
            }
        }
    }

    protected void progress(PropertyChangeEvent propertyChangeEvent) {
    }

    protected void started(PropertyChangeEvent propertyChangeEvent) {
    }

    protected void done(PropertyChangeEvent propertyChangeEvent) {
    }

    protected void event(String string, Object object, Object object2) {
    }
}

