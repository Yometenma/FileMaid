package net.filemaid.ui.episodelist;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import javax.swing.JComponent;
import net.filemaid.Logging;
import net.filemaid.ui.FileBotList;
import net.filemaid.ui.FileBotListExportHandler;
import net.filemaid.ui.transfer.ArrayTransferable;
import net.filemaid.ui.transfer.ClipboardHandler;
import net.filemaid.ui.transfer.CompositeTranserable;
import net.filemaid.util.StringUtilities;
import net.filemaid.web.Episode;

class EpisodeListExportHandler
extends FileBotListExportHandler<Episode>
implements ClipboardHandler {
    public EpisodeListExportHandler(FileBotList<Episode> fileBotList) {
        super(fileBotList);
    }

    @Override
    public Transferable createTransferable(JComponent jComponent) {
        ArrayTransferable<Episode> arrayTransferable = this.exportEpisodeSelection();
        Transferable transferable = super.createTransferable(jComponent);
        return new CompositeTranserable(arrayTransferable, transferable);
    }

    @Override
    public void exportToClipboard(JComponent jComponent, Clipboard clipboard, int n) throws IllegalStateException {
        ArrayTransferable<Episode> arrayTransferable = this.exportEpisodeSelection();
        StringSelection stringSelection = new StringSelection(StringUtilities.join(arrayTransferable.getArray(), (CharSequence)System.lineSeparator()));
        clipboard.setContents(new CompositeTranserable(arrayTransferable, stringSelection), null);
        Logging.log.info((String)(arrayTransferable.size() == 1 ? "1 episode has been copied to the clipboard." : arrayTransferable.size() + " episodes have been copied to the clipboard."));
    }

    public ArrayTransferable<Episode> exportEpisodeSelection() {
        Episode[] episodeArray = (Episode[])this.list.getListComponent().getSelectedValuesList().stream().map(Episode.class::cast).toArray(Episode[]::new);
        if (episodeArray.length == 0) {
            episodeArray = (Episode[])this.list.getModel().stream().map(Episode.class::cast).toArray(Episode[]::new);
        }
        return new ArrayTransferable<Episode>(episodeArray);
    }
}

