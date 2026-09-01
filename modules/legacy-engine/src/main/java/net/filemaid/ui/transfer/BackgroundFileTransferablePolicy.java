package net.filemaid.ui.transfer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.SwingPropertyChangeSupport;
import net.filemaid.Logging;
import net.filemaid.Parallelism;
import net.filemaid.ui.transfer.FileTransferablePolicy;
import net.filemaid.ui.transfer.TransferablePolicy;
import net.filemaid.util.ReadOnlyFile;

public abstract class BackgroundFileTransferablePolicy<V>
extends FileTransferablePolicy {
    public static final String LOADING_PROPERTY = "loading";
    private final ThreadLocal<BackgroundWorker> threadLocalWorker;
    private final List<BackgroundWorker> workerList;
    private final PropertyChangeSupport propertyChangeSupport;

    public BackgroundFileTransferablePolicy() {
        this.threadLocalWorker = new ThreadLocal();
        this.workerList = new LinkedList<BackgroundWorker>();
        this.propertyChangeSupport = new SwingPropertyChangeSupport(this, true);
    }

    protected BackgroundFileTransferablePolicy(BackgroundFileTransferablePolicy backgroundFileTransferablePolicy) {
        this.threadLocalWorker = backgroundFileTransferablePolicy.threadLocalWorker;
        this.workerList = backgroundFileTransferablePolicy.workerList;
        this.propertyChangeSupport = backgroundFileTransferablePolicy.propertyChangeSupport;
    }

    @Override
    public void handleFileTransferable(List<File> list, TransferablePolicy.TransferAction transferAction) throws Exception {
        if (transferAction != TransferablePolicy.TransferAction.ADD) {
            this.reset();
            this.clear();
        }
        if (list.size() > 0) {
            this.submit(list, transferAction);
        }
    }

    protected void submit(List<File> list, TransferablePolicy.TransferAction transferAction) {
        new BackgroundWorker(list, transferAction).execute();
    }

    public void reset() {
        List<BackgroundWorker> list = this.workerList;
        synchronized (list) {
            if (this.workerList.size() > 0) {
                for (Object object : this.workerList.toArray()) {
                    ((BackgroundWorker)object).cancel(true);
                }
            }
        }
    }

    protected abstract void process(List<V> var1);

    protected void process(Exception exception) {
        Logging.log.log(Level.WARNING, exception, Logging.cause(exception));
    }

    protected final void publish(V[] VArray) {
        this.currentWorker().offer(VArray);
    }

    protected final void publish(Exception exception) {
        SwingUtilities.invokeLater(() -> this.process(exception));
    }

    protected List<File> walkFileTree(Collection<File> collection, FileFilter fileFilter, int n) {
        BackgroundWorker backgroundWorker = this.currentWorker();
        List<File> list = ReadOnlyFile.of(collection);
        try {
            List<File> list2 = list.stream().filter(File::isDirectory).collect(Collectors.toCollection(ArrayList::new));
            for (int i = 0; !list2.isEmpty() && i < n; ++i) {
                List<List> list3 = Parallelism.commonPool().map(list2, file2 -> Arrays.stream(file2.listFiles(file -> backgroundWorker.accept(file) && fileFilter.accept(file) && file.isDirectory())).collect(Collectors.toList()));
                list2.clear();
                list3.forEach(list2::addAll);
            }
        }
        catch (Exception exception) {
            if (Logging.isCancellation(exception)) {
                return Collections.emptyList();
            }
            this.publish(exception);
        }
        return list;
    }

    protected BackgroundWorker currentWorker() {
        BackgroundWorker backgroundWorker = this.threadLocalWorker.get();
        if (backgroundWorker != null) {
            return backgroundWorker;
        }
        throw new IllegalThreadStateException("Illegal access thread: " + Thread.currentThread());
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);
    }

    protected class BackgroundWorker
    extends SwingWorker<Object, V>
    implements FileFilter {
        private final List<File> files;
        private final TransferablePolicy.TransferAction action;

        public BackgroundWorker(List<File> list, TransferablePolicy.TransferAction transferAction) {
            this.files = list;
            this.action = transferAction;
            List<BackgroundWorker> list2 = BackgroundFileTransferablePolicy.this.workerList;
            synchronized (list2) {
                if (BackgroundFileTransferablePolicy.this.workerList.add(this) && BackgroundFileTransferablePolicy.this.workerList.size() == 1) {
                    BackgroundFileTransferablePolicy.this.propertyChangeSupport.firePropertyChange(BackgroundFileTransferablePolicy.LOADING_PROPERTY, false, true);
                }
            }
        }

        @Override
        protected Object doInBackground() throws Exception {
            BackgroundFileTransferablePolicy.this.threadLocalWorker.set(this);
            try {
                BackgroundFileTransferablePolicy.this.load(this.files, this.action);
            }
            finally {
                BackgroundFileTransferablePolicy.this.threadLocalWorker.remove();
            }
            return null;
        }

        @Override
        public boolean accept(File file) {
            if (this.isCancelled()) {
                throw new CancellationException("Background worker has been cancelled");
            }
            return true;
        }

        public void offer(V[] VArray) {
            if (!this.isCancelled()) {
                this.publish(VArray);
            }
        }

        @Override
        protected void process(List<V> list) {
            if (!this.isCancelled()) {
                BackgroundFileTransferablePolicy.this.process(list);
            }
        }

        @Override
        protected void done() {
            if (!this.isCancelled()) {
                try {
                    this.get();
                }
                catch (Exception exception) {
                    BackgroundFileTransferablePolicy.this.process(exception);
                }
            }
            List<BackgroundWorker> list = BackgroundFileTransferablePolicy.this.workerList;
            synchronized (list) {
                if (BackgroundFileTransferablePolicy.this.workerList.remove(this) && BackgroundFileTransferablePolicy.this.workerList.isEmpty()) {
                    BackgroundFileTransferablePolicy.this.propertyChangeSupport.firePropertyChange(BackgroundFileTransferablePolicy.LOADING_PROPERTY, true, false);
                }
            }
        }
    }
}

