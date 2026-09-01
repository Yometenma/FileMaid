package net.filemaid.ui.rename;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.LayoutManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.ThumbnailServices;
import net.filemaid.UserInteraction;
import net.filemaid.WebServices;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.similarity.Normalization;
import net.filemaid.similarity.SeasonEpisodeMatcher;
import net.filemaid.ui.FileBotList;
import net.filemaid.ui.SelectButtonTextField;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.ToolTip;
import net.filemaid.ui.rename.BlankThumbnail;
import net.filemaid.ui.rename.FormatExpressionTextArea;
import net.filemaid.ui.rename.MatchType;
import net.filemaid.ui.rename.RenameListCellRenderer;
import net.filemaid.ui.rename.TextColorizer;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.DefaultFancyListCellRenderer;
import net.filemaid.util.ui.GlassOptionPane;
import net.filemaid.util.ui.LabelProvider;
import net.filemaid.util.ui.LazyDocumentListener;
import net.filemaid.util.ui.LazyThumbnailListModel;
import net.filemaid.util.ui.LoadingOverlayPane;
import net.filemaid.util.ui.PrototypeCellSize;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.AudioTrack;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieLookupService;
import net.filemaid.web.MultiEpisode;
import net.filemaid.web.MusicLookupService;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SortOrder;
import net.filemaid.web.ThumbnailProvider;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public abstract class MetaObjectDialog<T>
extends JPanel {
    protected final T object;
    protected final File file;

    public MetaObjectDialog(T t, File file) {
        super((LayoutManager)new MigLayout("insets 0, nogrid, fill"));
        this.object = t;
        this.file = file;
    }

    public abstract T getSelection();

    protected void submit() {
        this.processKeyEvent(new KeyEvent(this, 401, 0L, 0, 10, '\uffff'));
    }

    protected JLabel createHeader() {
        if (this.file == null) {
            JLabel jLabel = new JLabel();
            jLabel.setVisible(false);
            return jLabel;
        }
        File file = this.getRelativeFile();
        JLabel jLabel = new JLabel(TextColorizer.colorizeFilePath(file), this.getHeaderIcon(), 2);
        jLabel.addMouseListener(SwingUI.mouseClicked(mouseEvent -> UserInteraction.copy(file.getPath())));
        jLabel.setCursor(Cursor.getPredefinedCursor(12));
        return jLabel;
    }

    protected Icon getHeaderIcon() {
        return MatchType.getIcon(this.object);
    }

    protected File getRelativeFile() {
        try {
            File file = MediaFileUtilities.getStructurePathTail(this.file);
            if (file != null) {
                return file;
            }
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
        return FileUtilities.getRelativePathTail(this.file, 3);
    }

    public abstract Component getFocusComponent();

    @Override
    public boolean requestFocusInWindow() {
        return this.getFocusComponent().requestFocusInWindow();
    }

    public static GlassOptionPane showSelectDialog(Object object, File file, JComponent jComponent, String string, Icon icon, Component component, Consumer<Object> consumer) {
        MetaObjectDialog metaObjectDialog2 = MetaObjectDialog.forObject(object, file);
        if (metaObjectDialog2 == null) {
            return null;
        }
        return GlassOptionPane.showConfigurationDialog(metaObjectDialog2, jComponent, string, icon, component, metaObjectDialog -> Optional.ofNullable(metaObjectDialog.getSelection()).ifPresent(consumer));
    }

    public static MetaObjectDialog forObject(Object object, File file) {
        if (object instanceof Episode) {
            return new EpisodeChooser((Episode)object, file);
        }
        if (object instanceof Movie) {
            return new MovieChooser((Movie)object, file);
        }
        if (object instanceof AudioTrack) {
            return new AudioTrackChooser((AudioTrack)object, file);
        }
        if (object instanceof File) {
            return new FileChooser((File)object, file);
        }
        if (object instanceof String) {
            return new StringChooser((String)object, file);
        }
        return null;
    }

    public static class EpisodeChooser
    extends SearchListChooser<Episode> {
        public EpisodeChooser(Episode episode, File file) {
            super(episode, file);
        }

        @Override
        protected void setModel(List<Episode> list, Episode episode) {
            if (this.file != null) {
                this.status.setText("");
                this.status.setIcon(null);
                this.status.setVisible(false);
                list.stream().map(Episode::getSeriesInfo).filter(Objects::nonNull).findFirst().ifPresent(seriesInfo -> {
                    List<SeasonEpisodeMatcher.SxE> list2 = MediaDetection.parseEpisodeNumber(this.file, true);
                    if (list2 != null) {
                        list2.stream().filter(sxE -> sxE.season > 0 && sxE.episode > 0).findFirst().ifPresent(sxE -> {
                            Episode episode3 = list.stream().filter(ep -> ep.getSeason() != null && ep.getEpisode() != null && sxE.season == ep.getSeason() && sxE.episode == ep.getEpisode()).findFirst().orElse(null);
                            if (episode3 == null) {
                                this.status.setText(String.format("<html><p><b>%s</b> does <u>not</u> exist in <b>%s Order</b>.</p></html>", sxE, seriesInfo.getOrder()));
                                this.status.setIcon(ResourceManager.getIcon("status.warning"));
                                this.status.setVisible(true);
                            } else if (episode == null) {
                                SwingUtilities.invokeLater(() -> this.setSelectedValue(list, episode3));
                            }
                        });
                    }
                });
            }
            super.setModel(list, episode);
        }

        public SortOrder getSortOrder() {
            return Optional.ofNullable((Episode)this.object).map(Episode::getSeriesInfo).map(SeriesInfo::getOrder).map(SortOrder::forName).orElse(SortOrder.Airdate);
        }

        @Override
        public Locale getLocale() {
            return Optional.ofNullable((Episode)this.object).map(Episode::getSeriesInfo).map(SeriesInfo::getLanguage).orElse(Locale.ENGLISH);
        }

        @Override
        protected Datasource[] getDatasources() {
            return WebServices.getEpisodeListProviders();
        }

        @Override
        protected ListModel<Episode> createListModel(List<Episode> list) {
            return new DefaultEventListModel(GlazedLists.eventList(list));
        }

        @Override
        protected Episode getSelection(List<Episode> list) {
            return list.size() == 1 ? list.get(0) : new MultiEpisode(list);
        }

        @Override
        protected Stream<Episode> getSelection(Episode episode) {
            return EpisodeUtilities.streamMultiEpisode(episode);
        }

        @Override
        protected int getSelectionMode() {
            return 2;
        }

        @Override
        protected Stream<String> getSearchIndex() {
            for (EpisodeListProvider episodeListProvider : WebServices.getEpisodeListProviders()) {
                if (!EpisodeUtilities.isInstance((Datasource)episodeListProvider, (Episode)this.object)) continue;
                try {
                    return episodeListProvider.getIndex().stream().map(SearchResult::getName);
                }
                catch (Exception exception) {
                    Logging.trace(exception);
                }
            }
            return Stream.empty();
        }

        @Override
        protected EpisodeListProvider getInitialDatasource() {
            return WebServices.getEpisodeListProvider(((Episode)this.object).getSeriesInfo().getDatabase());
        }

        @Override
        protected String getInitialQuery() {
            return ((Episode)this.object).getSeriesName();
        }

        @Override
        protected List<Episode> getInitialModel() throws Exception {
            return this.getInitialDatasource().getEpisodeList(((Episode)this.object).getSeriesInfo().getId(), this.getSortOrder(), this.getLocale());
        }

        @Override
        protected List<Episode> search(Datasource datasource, String string) throws Exception {
            EpisodeListProvider episodeListProvider = (EpisodeListProvider)datasource;
            List<SearchResult> list = episodeListProvider.lookup(string, this.getLocale());
            if (list.isEmpty()) {
                return Collections.emptyList();
            }
            return episodeListProvider.getEpisodeList(list.get(0), this.getSortOrder(), this.getLocale());
        }
    }

    public static class MovieChooser
    extends SearchListChooser<Movie> {
        public MovieChooser(Movie movie, File file) {
            super(movie, file);
        }

        @Override
        public Locale getLocale() {
            return Optional.ofNullable((Movie)this.object).map(Movie::getLanguage).orElse(Locale.US);
        }

        @Override
        protected MovieLookupService getInitialDatasource() {
            return this.getDatasource((Movie)this.object);
        }

        private MovieLookupService getDatasource(Movie movie) {
            return movie.getTmdbId() > 0 ? WebServices.TheMovieDB : WebServices.OMDb;
        }

        @Override
        protected String getInitialQuery() {
            return ((Movie)this.object).getName();
        }

        @Override
        protected List<Movie> getInitialModel() throws Exception {
            return this.getInitialDatasource().lookupMovie(this.getInitialQuery(), this.getLocale());
        }

        @Override
        protected Datasource[] getDatasources() {
            return WebServices.getMovieLookupServices();
        }

        @Override
        protected ListCellRenderer createCellRenderer() {
            DefaultFancyListCellRenderer defaultFancyListCellRenderer = new DefaultFancyListCellRenderer(4){

                @Override
                public Component getListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
                    super.getListCellRendererComponent(jList, object, n, bl, bl2);
                    if (jList.getModel() instanceof LazyThumbnailListModel) {
                        LazyThumbnailListModel lazyThumbnailListModel = (LazyThumbnailListModel)jList.getModel();
                        if (jList.isValid()) {
                            this.setIcon(lazyThumbnailListModel.getIcon(n));
                        } else {
                            this.setIcon(lazyThumbnailListModel.getPreviewIcon(n));
                        }
                    } else {
                        this.setIcon(BlankThumbnail.BLANK_POSTER);
                    }
                    ToolTip.HTML.setToolTip(this, object);
                    return this;
                }

                @Override
                public String getToolTipText(MouseEvent mouseEvent) {
                    return ToolTip.HTML.getToolTip(this);
                }
            };
            defaultFancyListCellRenderer.setHighlightingEnabled(false);
            return defaultFancyListCellRenderer;
        }

        @Override
        protected ListModel<Movie> createListModel(List<Movie> list) {
            return new LazyThumbnailListModel<Movie>(list.toArray(), this::preview, this::thumbnail);
        }

        private Icon preview(Movie movie) {
            return BlankThumbnail.BLANK_POSTER;
        }

        private CompletableFuture<Icon> thumbnail(Movie movie) {
            if (movie.getTmdbId() > 0) {
                return ThumbnailServices.TheMovieDB.requestThumbnail(movie.getTmdbId(), ThumbnailProvider.ResolutionVariant.fromScaleFactor(this.list));
            }
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected Movie getSelection(List<Movie> list) {
            Movie movie = list.get(0);
            return SwingUI.withWaitCursor((Object)this, () -> MediaDetection.getLocalizedMovie(this.getDatasource(movie), movie, this.getLocale())).orElse(movie);
        }

        @Override
        protected Stream<Movie> getSelection(Movie movie) {
            return Stream.of(movie).map(Movie::new);
        }

        @Override
        protected int getSelectionMode() {
            return 0;
        }

        @Override
        protected Stream<String> getSearchIndex() {
            for (MovieLookupService movieLookupService : WebServices.getMovieLookupServices()) {
                try {
                    return movieLookupService.getIndex().stream().map(SearchResult::getName);
                }
                catch (Exception exception) {
                    Logging.trace(exception);
                }
            }
            return Stream.empty();
        }

        @Override
        protected List<Movie> search(Datasource datasource, String string) throws Exception {
            MovieLookupService movieLookupService = (MovieLookupService)datasource;
            return movieLookupService.lookupMovie(string, this.getLocale());
        }
    }

    public static class AudioTrackChooser
    extends MetaObjectDialog<AudioTrack> {
        protected final JTable table = new JTable(new AudioTrackTableModel(new AudioTrack[0]));

        public AudioTrackChooser(AudioTrack audioTrack, File file) {
            super(audioTrack, file);
            this.table.setAutoCreateRowSorter(true);
            this.table.setAutoCreateColumnsFromModel(true);
            this.table.setFillsViewportHeight(true);
            this.table.setAutoResizeMode(2);
            this.table.setSelectionMode(0);
            this.table.setBackground(ThemeSupport.getPanelBackground());
            this.table.setGridColor(ThemeSupport.getColor(0xEEEEEE));
            this.table.setRowHeight(25);
            JPanel jPanel = new JPanel((LayoutManager)new MigLayout("insets 0, nogrid, fill"));
            jPanel.add((Component)this.createHeader(), "gap 15px, wmax 700px, growx, wrap 4px, hidemode 2");
            jPanel.add((Component)SwingUI.createScrollPaneGroup("", this.table), "grow, push");
            this.add((Component)jPanel, "dock center");
            try {
                this.setModel(this.search(file), audioTrack);
            }
            catch (Exception exception) {
                Logging.trace(exception);
            }
            this.table.addMouseListener(SwingUI.mouseDoubleClicked(mouseEvent -> {
                AudioTrack selectedTrack = this.getSelection();
                if (selectedTrack != null) {
                    this.submit();
                }
            }));
        }

        private List<AudioTrack> search(File file) throws Exception {
            ArrayList<AudioTrack> arrayList = new ArrayList<AudioTrack>();
            for (MusicLookupService musicLookupService : WebServices.getMusicLookupServices()) {
                arrayList.addAll(musicLookupService.lookup(file));
            }
            return arrayList;
        }

        private void setModel(List<AudioTrack> list, AudioTrack audioTrack) {
            this.table.setModel(new AudioTrackTableModel(list));
            this.table.getColumnModel().getColumn(0).setMaxWidth(30);
            this.table.getColumnModel().getColumn(this.table.getColumnCount() - 1).setMaxWidth(45);
            int n = list.indexOf(audioTrack);
            if (n >= 0) {
                this.table.getSelectionModel().setSelectionInterval(n, n);
            }
        }

        @Override
        public AudioTrack getSelection() {
            AudioTrackTableModel audioTrackTableModel = (AudioTrackTableModel)this.table.getModel();
            if (this.table.getSelectedRow() < audioTrackTableModel.getRowCount()) {
                return audioTrackTableModel.get(this.table.convertRowIndexToModel(this.table.getSelectedRow()));
            }
            return null;
        }

        @Override
        public Component getFocusComponent() {
            return this.table;
        }

        private static class AudioTrackTableModel
        extends AbstractTableModel {
            private AudioTrack[] rows;

            public AudioTrackTableModel(AudioTrack ... audioTrackArray) {
                this.rows = audioTrackArray;
            }

            public AudioTrackTableModel(Collection<AudioTrack> collection) {
                this.rows = collection.toArray(new AudioTrack[0]);
            }

            public AudioTrack get(int n) {
                return this.rows[n];
            }

            @Override
            public int getRowCount() {
                return this.rows.length;
            }

            @Override
            public int getColumnCount() {
                return 5;
            }

            @Override
            public String getColumnName(int n) {
                switch (n) {
                    case 1: {
                        return "Artist";
                    }
                    case 2: {
                        return "Title";
                    }
                    case 3: {
                        return "Album";
                    }
                    case 4: {
                        return "Track";
                    }
                }
                return null;
            }

            @Override
            public Object getValueAt(int n, int n2) {
                AudioTrack audioTrack = this.get(n);
                switch (n2) {
                    case 0: {
                        return MatchType.getIcon(audioTrack);
                    }
                    case 1: {
                        return audioTrack.getAlbumArtist() != null ? audioTrack.getAlbumArtist() : audioTrack.getArtist();
                    }
                    case 2: {
                        return audioTrack.getTrackTitle() != null ? audioTrack.getTrackTitle() : audioTrack.getTitle();
                    }
                    case 3: {
                        return audioTrack.getAlbum();
                    }
                    case 4: {
                        return Stream.of(audioTrack.getTrack(), audioTrack.getTrackCount()).filter(Objects::nonNull).map(Objects::toString).collect(Collectors.joining("/"));
                    }
                }
                return null;
            }

            @Override
            public Class<?> getColumnClass(int n) {
                switch (n) {
                    case 0: {
                        return Icon.class;
                    }
                }
                return String.class;
            }
        }
    }

    public static class FileChooser
    extends MetaObjectDialog<File> {
        protected final FileBotList<File> list = new FileBotList();

        public FileChooser(File file, File file2) {
            super(file, file2);
            this.list.getListComponent().setSelectionMode(0);
            this.list.getListComponent().setCellRenderer(RenameListCellRenderer.create(this.list.getModel(), true));
            JPanel jPanel = new JPanel((LayoutManager)new MigLayout("insets 0, nogrid, fill"));
            jPanel.add((Component)this.createHeader(), "gap 15px, wmax 700px, growx, wrap 2px, hidemode 2");
            jPanel.add(this.list, "grow, push, wmin 400px, hmin 200px");
            this.add((Component)jPanel, "dock center");
            this.setModel(this.getInitialModel(), file);
            PrototypeCellSize.fixedCellSize(this.list.getListComponent());
            this.list.getListComponent().addMouseListener(SwingUI.mouseDoubleClicked(mouseEvent -> {
                File selectedFile = this.getSelection();
                if (selectedFile != null) {
                    this.submit();
                }
            }));
        }

        @Override
        public File getSelection() {
            return this.list.getListComponent().getSelectedValue();
        }

        @Override
        public Component getFocusComponent() {
            return this.list.getListComponent();
        }

        protected List<File> getInitialModel() {
            return FileUtilities.getChildren(((File)this.object).getParentFile()).stream().filter(file -> ((File)this.object).isFile() == file.isFile() && !file.isHidden()).sorted(FileUtilities.HUMAN_NAME_ORDER).collect(Collectors.toList());
        }

        protected void setModel(List<File> list, File file) {
            int n;
            this.list.getModel().clear();
            this.list.getModel().addAll(list);
            if (file != null && (n = list.indexOf(file)) >= 0) {
                this.list.getListComponent().setSelectedIndex(n);
                this.list.getListComponent().ensureIndexIsVisible(n);
            }
        }
    }

    public static class StringChooser
    extends MetaObjectDialog<String> {
        protected final FormatExpressionTextArea editor = new FormatExpressionTextArea(new RSyntaxDocument("text/plain"), false);

        public StringChooser(String string, File file) {
            super(string, file);
            this.editor.setText(string);
            this.editor.setRows(4);
            this.editor.setColumns(Math.max(40, Math.min(80, string.length() + 10)));
            RTextScrollPane rTextScrollPane = new RTextScrollPane((RTextArea)this.editor, false);
            rTextScrollPane.setLineNumbersEnabled(false);
            rTextScrollPane.setFoldIndicatorEnabled(false);
            rTextScrollPane.setIconRowHeaderEnabled(false);
            rTextScrollPane.setVerticalScrollBarPolicy(21);
            rTextScrollPane.setHorizontalScrollBarPolicy(30);
            rTextScrollPane.setBackground(this.editor.getBackground());
            rTextScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
            rTextScrollPane.setOpaque(true);
            JPanel jPanel = new JPanel((LayoutManager)new MigLayout("insets 0, nogrid, fill"));
            jPanel.add((Component)this.createHeader(), "growx, wmax 700px, wrap 4px, hidemode 2");
            jPanel.add((Component)rTextScrollPane, "grow, push");
            this.add((Component)jPanel, "dock center");
            SwingUI.installAction((JComponent)((Object)this.editor), 0, KeyStroke.getKeyStroke(10, 0), SwingUI.newAction("Submit", actionEvent -> this.submit()));
        }

        @Override
        public String getSelection() {
            return Optional.of(Normalization.replaceSpace(this.editor.getText(), " ")).filter(string -> !string.isEmpty()).orElse(null);
        }

        @Override
        public Component getFocusComponent() {
            return this.editor;
        }
    }

    public static abstract class SearchListChooser<T>
    extends MetaObjectDialog<T> {
        private Future<?> worker;
        protected final SelectButtonTextField<Datasource> searchTextField = new SelectButtonTextField();
        protected final FileBotList<T> list;
        protected final JLabel status;

        public SearchListChooser(T t, File file) {
            super(t, file);
            this.searchTextField.getSelectButton().setModel(Arrays.asList(this.getDatasources()));
            this.searchTextField.getSelectButton().setLabelProvider(LabelProvider.via(Datasource::getName, Datasource::getIcon));
            BasicEventList basicEventList = new BasicEventList();
            AutoCompleteSupport autoCompleteSupport = AutoCompleteSupport.install(this.searchTextField.getEditor(), (EventList)basicEventList);
            autoCompleteSupport.setTextMatchingStrategy(TextMatcherEditor.IDENTICAL_STRATEGY);
            autoCompleteSupport.setFilterMode(0);
            autoCompleteSupport.setCorrectsCase(true);
            autoCompleteSupport.setStrict(false);
            SwingUI.onSwingWorker(() -> this.getSearchIndex().collect(Collectors.toCollection(() -> new TreeSet<String>(String.CASE_INSENSITIVE_ORDER))), arg_0 -> SearchListChooser.lambda$new$2((EventList)basicEventList, arg_0));
            this.list = new FileBotList();
            this.list.getListComponent().setSelectionMode(this.getSelectionMode());
            this.list.getListComponent().setCellRenderer(this.createCellRenderer());
            this.status = new JLabel();
            this.status.setVisible(false);
            this.status.setBorder(BorderFactory.createEmptyBorder(3, 1, 3, 1));
            JPanel jPanel = new JPanel((LayoutManager)new MigLayout("insets 0, nogrid, fill"));
            jPanel.add(this.searchTextField, "align center, hmin 35px, wrap 20px");
            jPanel.add((Component)this.createHeader(), "gap 15px, wmax 700px, growx, wrap 3px, hidemode 2");
            jPanel.add(this.list, "grow, push, wmin 400px, hmin 200px");
            this.add((Component)new LoadingOverlayPane(jPanel, this, "0px", "0px"), "dock center");
            this.add((Component)this.status, "dock south, gap 15px, hidemode 3");
            try {
                this.searchTextField.getSelectButton().setSelectedValue(this.getInitialDatasource());
                this.searchTextField.setText(this.getInitialQuery());
                this.setModel(this.getInitialModel(), t);
            }
            catch (Exception exception) {
                Logging.trace(exception);
            }
            this.searchTextField.getEditorDocument().addDocumentListener(new LazyDocumentListener(documentEvent -> this.search()));
            this.searchTextField.getSelectButton().getSelectionModel().addChangeListener(changeEvent -> this.search());
            this.list.getListComponent().addMouseListener(SwingUI.mouseDoubleClicked(mouseEvent -> this.submit()));
        }

        protected void search() {
            this.firePropertyChange("loading", false, true);
            Datasource datasource = this.searchTextField.getSelectButton().getSelectedValue();
            String string = this.searchTextField.getText().trim();
            if (this.worker != null && !this.worker.isDone()) {
                this.worker.cancel(true);
            }
            this.worker = SwingUI.onSwingWorker(() -> {
                if (datasource == this.getInitialDatasource() && string.equalsIgnoreCase(this.getInitialQuery())) {
                    return this.getInitialModel();
                }
                return this.search(datasource, string);
            }, list -> this.setModel((List<T>)list, null), exception -> Logging.trace(exception), () -> this.firePropertyChange("loading", true, false));
        }

        protected void setModel(List<T> list, T t) {
            this.list.getListComponent().setModel(this.createListModel(list));
            PrototypeCellSize.fixedCellSize(this.list.getListComponent());
            if (t != null) {
                this.setSelectedValue(list, t);
            }
            this.revalidate();
        }

        protected void setSelectedValue(List<T> list, T t) {
            int[] nArray = this.getSelection(t).mapToInt(list::indexOf).filter(n -> n >= 0).sorted().distinct().toArray();
            if (nArray.length > 0) {
                this.list.getListComponent().setSelectedIndices(nArray);
                this.list.getListComponent().ensureIndexIsVisible(nArray[0]);
            }
        }

        @Override
        public T getSelection() {
            List<T> list = this.list.getListComponent().getSelectedValuesList();
            return list.isEmpty() ? null : (T)this.getSelection(list);
        }

        @Override
        public Component getFocusComponent() {
            return this.list.getListComponent();
        }

        @Override
        protected Icon getHeaderIcon() {
            return ResourceManager.getIcon("search.generic");
        }

        protected ListCellRenderer createCellRenderer() {
            DefaultFancyListCellRenderer defaultFancyListCellRenderer = new DefaultFancyListCellRenderer();
            defaultFancyListCellRenderer.setHighlightingEnabled(false);
            return defaultFancyListCellRenderer;
        }

        protected abstract ListModel<T> createListModel(List<T> var1);

        protected abstract T getSelection(List<T> var1);

        protected abstract Stream<T> getSelection(T var1);

        protected abstract int getSelectionMode();

        protected abstract Stream<String> getSearchIndex();

        protected abstract Datasource getInitialDatasource();

        protected abstract String getInitialQuery();

        protected abstract List<T> getInitialModel() throws Exception;

        protected abstract Datasource[] getDatasources();

        protected abstract List<T> search(Datasource var1, String var2) throws Exception;

        private static /* synthetic */ void lambda$new$2(EventList eventList, TreeSet treeSet) {
            eventList.clear();
            eventList.addAll((Collection)treeSet);
        }
    }
}

