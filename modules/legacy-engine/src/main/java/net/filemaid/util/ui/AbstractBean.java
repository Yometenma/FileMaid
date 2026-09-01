package net.filemaid.util.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.event.SwingPropertyChangeSupport;

public abstract class AbstractBean {
    private final PropertyChangeSupport pcs = new SwingPropertyChangeSupport(this, true);

    protected void firePropertyChange(String string, Object object, Object object2) {
        this.pcs.firePropertyChange(string, object, object2);
    }

    protected void firePropertyChange(PropertyChangeEvent propertyChangeEvent) {
        this.pcs.firePropertyChange(propertyChangeEvent);
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.pcs.addPropertyChangeListener(propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.pcs.removePropertyChangeListener(propertyChangeListener);
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return this.pcs.getPropertyChangeListeners();
    }
}

