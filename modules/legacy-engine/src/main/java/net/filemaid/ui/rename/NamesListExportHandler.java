package net.filemaid.ui.rename;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import net.filemaid.similarity.Match;
import net.filemaid.ui.rename.FormattedFuture;
import net.filemaid.ui.rename.RenameList;
import net.filemaid.ui.transfer.ArrayTransferable;
import net.filemaid.ui.transfer.CompositeTranserable;
import net.filemaid.ui.transfer.DefaultClipboardHandler;
import net.filemaid.util.StringUtilities;
import net.filemaid.web.Episode;
import net.filemaid.web.Movie;

public class NamesListExportHandler
extends DefaultClipboardHandler {
    private final RenameList<FormattedFuture> namesList;

    public NamesListExportHandler(RenameList<FormattedFuture> renameList) {
        this.namesList = renameList;
    }

    @Override
    public void exportToClipboard(JComponent jComponent, Clipboard clipboard, int n) throws IllegalStateException {
        List list = this.namesList.getListComponent().getSelectedValuesList().stream().map(FormattedFuture::getMatch).map(Match::getValue).collect(Collectors.toList());
        ArrayList<Transferable> arrayList = new ArrayList<Transferable>(3);
        Episode[] episodeArray = (Episode[])list.stream().filter(Episode.class::isInstance).map(Episode.class::cast).toArray(Episode[]::new);
        if (episodeArray.length > 0) {
            arrayList.add(new ArrayTransferable<Episode>(episodeArray));
        }
        Movie[] movieArray = (Movie[])list.stream().filter(Movie.class::isInstance).map(Movie.class::cast).toArray(Movie[]::new);
        if (movieArray.length > 0) {
            arrayList.add(new ArrayTransferable<Movie>(movieArray));
        }
        arrayList.add(new StringSelection(StringUtilities.join(list, (CharSequence)System.lineSeparator())));
        clipboard.setContents(new CompositeTranserable(arrayList), null);
    }
}

