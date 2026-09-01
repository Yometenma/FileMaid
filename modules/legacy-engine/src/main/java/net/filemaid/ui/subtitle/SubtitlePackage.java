package net.filemaid.ui.subtitle;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingWorker;
import javax.swing.event.SwingPropertyChangeSupport;
import net.filemaid.Language;
import net.filemaid.MediaTypes;
import net.filemaid.util.FileUtilities;
import net.filemaid.vfs.ArchiveType;
import net.filemaid.vfs.MemoryFile;
import net.filemaid.web.SubtitleDescriptor;
import net.filemaid.web.SubtitleProvider;

public class SubtitlePackage {
    private final SubtitleProvider provider;
    private final SubtitleDescriptor subtitle;
    private final Language language;
    protected Download download;
    private final PropertyChangeSupport pcs = new SwingPropertyChangeSupport(this, true);

    protected SubtitlePackage() {
        this.provider = null;
        this.subtitle = null;
        this.language = null;
        this.download = null;
    }

    public SubtitlePackage(SubtitleProvider subtitleProvider, SubtitleDescriptor subtitleDescriptor) {
        this.provider = subtitleProvider;
        this.subtitle = subtitleDescriptor;
        this.language = Language.findLanguage(subtitleDescriptor.getLanguageName());
        this.download = new Download(subtitleDescriptor);
        this.download.addPropertyChangeListener(propertyChangeEvent -> {
            if (propertyChangeEvent.getPropertyName().equals("phase")) {
                this.pcs.firePropertyChange("download.phase", propertyChangeEvent.getOldValue(), propertyChangeEvent.getNewValue());
            }
        });
    }

    public SubtitleProvider getProvider() {
        return this.provider;
    }

    public String getName() {
        return this.subtitle.getName();
    }

    public Language getLanguage() {
        return this.language;
    }

    public boolean isDownload() {
        return this.download != null;
    }

    public Download getDownload() {
        return this.download;
    }

    public void reset() {
        this.download.cancel(false);
        Download download = this.download;
        this.download = new Download(this.subtitle);
        for (PropertyChangeListener propertyChangeListener : download.getPropertyChangeSupport().getPropertyChangeListeners()) {
            download.removePropertyChangeListener(propertyChangeListener);
            this.download.addPropertyChangeListener(propertyChangeListener);
        }
        this.pcs.firePropertyChange("download.phase", (Object)download.getPhase(), (Object)this.download.getPhase());
    }

    public String toString() {
        return this.getName();
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.pcs.addPropertyChangeListener(propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.pcs.removePropertyChangeListener(propertyChangeListener);
    }

    public static class Download
    extends SwingWorker<List<MemoryFile>, Void> {
        private final SubtitleDescriptor subtitle;
        private Phase current = Phase.PENDING;

        private Download(SubtitleDescriptor subtitleDescriptor) {
            this.subtitle = subtitleDescriptor;
        }

        public void start() {
            this.setPhase(Phase.WAITING);
            this.execute();
        }

        @Override
        protected List<MemoryFile> doInBackground() throws Exception {
            this.setPhase(Phase.DOWNLOADING);
            ByteBuffer byteBuffer = this.subtitle.fetch();
            if (this.isCancelled()) {
                return null;
            }
            this.setPhase(Phase.EXTRACTING);
            ArchiveType archiveType = ArchiveType.forName(this.subtitle.getType());
            if (archiveType == ArchiveType.UNKOWN) {
                return Collections.singletonList(new MemoryFile(this.subtitle.getPath(), byteBuffer));
            }
            List<MemoryFile> list = this.extract(archiveType, byteBuffer);
            if (list.isEmpty() && archiveType != ArchiveType.ZIP) {
                list = this.extract(ArchiveType.ZIP, byteBuffer);
            }
            if (list.isEmpty()) {
                throw new IOException("Cannot extract files from archive");
            }
            return list;
        }

        private List<MemoryFile> extract(ArchiveType archiveType, ByteBuffer byteBuffer) throws IOException {
            ArrayList<MemoryFile> arrayList = new ArrayList<MemoryFile>();
            for (MemoryFile memoryFile : archiveType.fromData(byteBuffer)) {
                if (MediaTypes.SUBTITLE_FILES.accept(memoryFile.getName())) {
                    arrayList.add(memoryFile);
                    continue;
                }
                ArchiveType archiveType2 = ArchiveType.forName(FileUtilities.getExtension(memoryFile.getName()));
                if (archiveType2 == ArchiveType.UNKOWN) continue;
                arrayList.addAll(this.extract(archiveType2, memoryFile.getData()));
            }
            return arrayList;
        }

        @Override
        protected void done() {
            this.setPhase(Phase.DONE);
        }

        private void setPhase(Phase phase) {
            Phase phase2 = this.current;
            this.current = phase;
            this.firePropertyChange("phase", (Object)phase2, (Object)phase);
        }

        public boolean isStarted() {
            return this.current != Phase.PENDING;
        }

        public Phase getPhase() {
            return this.current;
        }

        static enum Phase {
            PENDING,
            WAITING,
            DOWNLOADING,
            EXTRACTING,
            DONE;

        }
    }
}

