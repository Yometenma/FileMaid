package net.filemaid.ui.subtitle.upload;

import java.awt.Component;
import java.awt.Cursor;
import java.io.File;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import net.filemaid.Logging;
import net.filemaid.WebServices;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.ui.SelectDialog;
import net.filemaid.ui.rename.BlankThumbnail;
import net.filemaid.ui.subtitle.upload.Status;
import net.filemaid.ui.subtitle.upload.SubtitleMapping;
import net.filemaid.ui.subtitle.upload.SubtitleMappingTableModel;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.Movie;
import net.filemaid.web.SubtitleSearchResult;
import net.filemaid.web.ThumbnailProvider;

class MovieEditor
implements TableCellEditor {
    private WebServices.OpenSubtitlesClient database;

    public MovieEditor(WebServices.OpenSubtitlesClient openSubtitlesClient) {
        this.database = openSubtitlesClient;
    }

    private String guessQuery(SubtitleMapping subtitleMapping) {
        String string = FileUtilities.getName(subtitleMapping.getVideo() != null ? subtitleMapping.getVideo() : subtitleMapping.getSubtitle());
        String string2 = MediaDetection.getSeriesNameMatcher(true).matchByEpisodeIdentifier(string);
        if (string2 != null) {
            return MediaDetection.stripReleaseInfo(string2, true);
        }
        return MediaDetection.stripReleaseInfo(string, false);
    }

    private String getFileHint(SubtitleMapping subtitleMapping) {
        File file = subtitleMapping.getVideo() != null ? subtitleMapping.getVideo() : subtitleMapping.getSubtitle();
        try {
            return MediaFileUtilities.getStructurePathTail(file).getPath();
        }
        catch (Exception exception) {
            return file.getPath();
        }
    }

    private List<SubtitleSearchResult> runSearch(SubtitleMapping subtitleMapping, JTable jTable) throws Exception {
        String string = SwingUI.showInputDialog("Enter movie / series name:", this.guessQuery(subtitleMapping), this.getFileHint(subtitleMapping), this.database.getIcon(), jTable);
        if (string != null) {
            return this.database.searchIMDB(string);
        }
        return null;
    }

    private void runSelect(List<SubtitleSearchResult> list, SubtitleMapping subtitleMapping, JTable jTable) {
        if (list == null) {
            return;
        }
        if (list.isEmpty()) {
            Logging.log.warning(Logging.message(this.database.getName(), "No results"));
            return;
        }
        SelectDialog<Movie> selectDialog = new SelectDialog<Movie>(jTable, list, this.preview(), this.thumbnail(jTable), false, false, null);
        selectDialog.pack();
        selectDialog.setLocation(SwingUI.getOffsetLocation(selectDialog));
        selectDialog.setVisible(true);
        Movie movie = selectDialog.getSelectedValue();
        if (movie != null) {
            subtitleMapping.setIdentity(movie);
            if (subtitleMapping.getIdentity() != null && subtitleMapping.getLanguage() != null && subtitleMapping.getVideo() != null) {
                subtitleMapping.setState(Status.CheckPending);
            }
        }
    }

    protected Function<Movie, Icon> preview() {
        return movie -> BlankThumbnail.BLANK_POSTER;
    }

    protected Function<Movie, CompletableFuture<Icon>> thumbnail(Component component) {
        if (this.database instanceof ThumbnailProvider) {
            WebServices.OpenSubtitlesClient openSubtitlesClient = this.database;
            ThumbnailProvider.ResolutionVariant resolutionVariant = ThumbnailProvider.ResolutionVariant.fromScaleFactor(component);
            return movie -> openSubtitlesClient.requestThumbnail(movie.getId(), resolutionVariant);
        }
        return null;
    }

    @Override
    public Component getTableCellEditorComponent(JTable jTable, Object object, boolean bl, int n, int n2) {
        SwingUI.getWindow(jTable).setCursor(Cursor.getPredefinedCursor(3));
        SubtitleMappingTableModel subtitleMappingTableModel = (SubtitleMappingTableModel)jTable.getModel();
        SubtitleMapping subtitleMapping = subtitleMappingTableModel.getData()[jTable.convertRowIndexToModel(n)];
        SwingUI.onSwingWorker(() -> this.runSearch(subtitleMapping, jTable), list -> this.runSelect((List<SubtitleSearchResult>)list, subtitleMapping, jTable), exception -> Logging.debug.warning(Logging.cause(subtitleMapping, exception)), () -> SwingUI.getWindow(jTable).setCursor(Cursor.getPredefinedCursor(0)));
        return null;
    }

    @Override
    public boolean stopCellEditing() {
        return true;
    }

    @Override
    public boolean shouldSelectCell(EventObject eventObject) {
        return false;
    }

    @Override
    public void removeCellEditorListener(CellEditorListener cellEditorListener) {
    }

    @Override
    public boolean isCellEditable(EventObject eventObject) {
        return true;
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public void cancelCellEditing() {
    }

    @Override
    public void addCellEditorListener(CellEditorListener cellEditorListener) {
    }
}

