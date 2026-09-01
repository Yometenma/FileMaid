package net.filemaid.ui.sfv;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.event.SwingPropertyChangeSupport;
import net.filemaid.hash.HashType;
import net.filemaid.hash.VerificationUtilities;
import net.filemaid.ui.sfv.ChecksumCell;

class ChecksumRow {
    private String name;
    private Map<String, ChecksumCell> hashes = new HashMap<String, ChecksumCell>(4);
    private State state = State.UNKNOWN;
    private String embeddedChecksum;
    private final PropertyChangeListener updateStateListener = propertyChangeEvent -> {
        if ("state".equals(propertyChangeEvent.getPropertyName())) {
            this.setState(this.getState(this.hashes.values()));
        }
    };
    private SwingPropertyChangeSupport pcs = new SwingPropertyChangeSupport(this, true);

    public ChecksumRow(String string) {
        this.name = string;
        this.embeddedChecksum = VerificationUtilities.getEmbeddedChecksum(string);
    }

    public String getName() {
        return this.name;
    }

    public State getState() {
        return this.state;
    }

    protected void setState(State state) {
        State state2 = this.state;
        this.state = state;
        this.pcs.firePropertyChange("state", (Object)state2, (Object)state);
    }

    public ChecksumCell getChecksum(File file) {
        return this.hashes.get(file.getAbsolutePath());
    }

    public Collection<ChecksumCell> values() {
        return Collections.unmodifiableCollection(this.hashes.values());
    }

    public ChecksumCell put(ChecksumCell checksumCell) {
        ChecksumCell checksumCell2 = this.hashes.put(checksumCell.getRoot().getAbsolutePath(), checksumCell);
        this.state = this.getState(this.hashes.values());
        checksumCell.addPropertyChangeListener(this.updateStateListener);
        return checksumCell2;
    }

    public void dispose() {
        for (PropertyChangeListener propertyChangeListener : this.pcs.getPropertyChangeListeners()) {
            this.pcs.removePropertyChangeListener(propertyChangeListener);
        }
        for (ChecksumCell checksumCell : this.hashes.values()) {
            checksumCell.dispose();
        }
        this.name = null;
        this.embeddedChecksum = null;
        this.hashes = null;
        this.state = null;
        this.pcs = null;
    }

    protected State getState(Collection<ChecksumCell> collection) {
        for (ChecksumCell object2 : collection) {
            if (object2.getState() == ChecksumCell.State.ERROR) {
                return State.ERROR;
            }
            if (object2.getState() == ChecksumCell.State.READY) continue;
            return State.UNKNOWN;
        }
        HashSet hashSet = new HashSet(2);
        EnumSet<State> enumSet = EnumSet.noneOf(State.class);
        for (HashType hashType : HashType.values()) {
            hashSet.clear();
            for (ChecksumCell checksumCell : collection) {
                String string = checksumCell.getChecksum(hashType);
                if (string == null) continue;
                hashSet.add(string.toLowerCase(Locale.ROOT));
            }
            enumSet.add(this.getVerdict(hashSet));
        }
        return Collections.max(enumSet);
    }

    protected State getVerdict(Set<String> set) {
        String string;
        if (set.size() < 1) {
            return State.UNKNOWN;
        }
        if (set.size() > 1) {
            return State.ERROR;
        }
        if (this.embeddedChecksum != null && (string = set.iterator().next()).length() == this.embeddedChecksum.length() && !string.equalsIgnoreCase(this.embeddedChecksum)) {
            return State.WARNING;
        }
        return State.OK;
    }

    public String toString() {
        return this.state + " " + this.name + " " + this.hashes;
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.pcs.addPropertyChangeListener(propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.pcs.removePropertyChangeListener(propertyChangeListener);
    }

    public static enum State {
        UNKNOWN,
        OK,
        WARNING,
        ERROR;

    }
}

