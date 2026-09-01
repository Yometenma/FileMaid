package net.filemaid.util.prefs;

import java.io.File;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import net.filemaid.Logging;
import net.filemaid.util.prefs.PropertyFileBackingStore;

public class FilePreferences
extends AbstractPreferences {
    private final PropertyFileBackingStore backingStore;
    private final boolean autoSync;

    public FilePreferences(File file, boolean bl) {
        super(null, "");
        this.backingStore = new PropertyFileBackingStore(file);
        this.autoSync = bl;
        this.syncSpi();
        Runtime.getRuntime().addShutdownHook(new Thread(this::flush, "FilePreferencesShutdownHook"));
    }

    protected FilePreferences(FilePreferences filePreferences, String string) {
        super(filePreferences, string);
        this.backingStore = filePreferences.backingStore;
        this.autoSync = filePreferences.autoSync;
    }

    public PropertyFileBackingStore getBackingStore() {
        return this.backingStore;
    }

    protected String getNodeKey() {
        return this.absolutePath().substring(1);
    }

    @Override
    protected FilePreferences childSpi(String string) {
        return new FilePreferences(this, string);
    }

    @Override
    protected void putSpi(String string, String string2) {
        if (this.autoSync) {
            this.syncSpi();
        }
        this.backingStore.setValue(this.getNodeKey(), string, string2);
        if (this.autoSync) {
            this.flushSpi();
        }
    }

    @Override
    protected String getSpi(String string) {
        if (this.autoSync) {
            this.syncSpi();
        }
        return this.backingStore.getValue(this.getNodeKey(), string);
    }

    @Override
    protected void removeSpi(String string) {
        if (this.autoSync) {
            this.syncSpi();
        }
        this.backingStore.removeValue(this.getNodeKey(), string);
        if (this.autoSync) {
            this.flushSpi();
        }
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        if (this.autoSync) {
            this.syncSpi();
        }
        this.backingStore.removeNode(this.getNodeKey());
        if (this.autoSync) {
            this.flushSpi();
        }
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
        if (this.autoSync) {
            this.syncSpi();
        }
        return this.backingStore.getKeys(this.getNodeKey()).toArray(new String[0]);
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        if (this.autoSync) {
            this.syncSpi();
        }
        return this.backingStore.getChildren(this.getNodeKey()).toArray(new String[0]);
    }

    @Override
    public void sync() {
        this.syncSpi();
    }

    @Override
    protected void syncSpi() {
        try {
            this.backingStore.sync();
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Failed to sync preferences", exception));
        }
    }

    @Override
    public void flush() {
        this.syncSpi();
        this.flushSpi();
    }

    @Override
    protected void flushSpi() {
        try {
            this.backingStore.flush();
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Failed to flush preferences", exception));
        }
    }

    @Override
    public String toString() {
        return "User Preferences: " + this.backingStore;
    }
}

