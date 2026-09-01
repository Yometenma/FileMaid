package net.filemaid.ui.rename;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.LayoutManager;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.Logging;
import net.filemaid.MemoryCache;
import net.filemaid.ResourceManager;
import net.filemaid.UserData;
import net.filemaid.UserInteraction;
import net.filemaid.WebServices;
import net.filemaid.format.BitRate;
import net.filemaid.format.FileSize;
import net.filemaid.format.MediaBindingBean;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.media.MetaAttributes;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.similarity.Match;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.rename.FormatExpressionTextArea;
import net.filemaid.ui.rename.FormattedFuture;
import net.filemaid.ui.rename.MatchType;
import net.filemaid.ui.rename.MediaInfoDialog;
import net.filemaid.ui.rename.RenameModel;
import net.filemaid.util.DateTimeUtilities;
import net.filemaid.util.ReadOnlyFile;
import net.filemaid.util.ui.LoadingOverlayPane;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeDetails;
import net.filemaid.web.EpisodeFormat;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.Link;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieDetails;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.imgscalr.Scalr;

public class MatchDetailPanel
extends JPanel
implements ListSelectionListener,
ListEventListener<Object> {
    public static final int TAB_INDEX_PATH = 0;
    public static final int TAB_INDEX_MATCH_DETAILS = 1;
    public static final int TAB_INDEX_MEDIA_DETAILS = 2;
    private final RenameModel model;
    private final ListSelectionModel selection;
    private MediaBindingBean sample;
    private final DetailField sourceFile = new DetailField("Media File:", SwingUI.newAction("Open MediaInfo", ResourceManager.getIcon("action.properties"), actionEvent -> Optional.ofNullable(this.sample).map(MediaBindingBean::getFileObject).ifPresent(file -> MediaInfoDialog.show(file, SwingUI.getWindow(this)))));
    private final DetailField sourceObject = new DetailField("Xattr Metadata Object:", SwingUI.newAction("Open External Link", actionEvent -> Optional.ofNullable(this.sample).map(MediaBindingBean::getFileObject).map(XattrMetaInfo.xattr::getMetaInfo).map(MatchType::getLink).ifPresent(UserInteraction::browse)));
    private final DetailField targetObject = new DetailField("Match Object:", SwingUI.newAction("Open External Link", actionEvent -> Optional.ofNullable(this.sample).map(MediaBindingBean::getInfoObject).map(MatchType::getLink).ifPresent(UserInteraction::browse)));
    private final DetailField targetPath = new DetailField("Target Path:");
    private final DetailField destinationFile = new DetailField("Destination File:");
    private final JTabbedPane container = new JTabbedPane();
    private final FormatExpressionTextArea jsonTextArea = new FormatExpressionTextArea(new RSyntaxDocument("text/json"), false);
    private final DescriptionField descriptionField = new DescriptionField();
    private final ImageField imageField = new ImageField();
    private final JTable mediaTable = new JTable(new MediaTableModel());
    private static final int THUMBNAIL_WIDTH = 320;
    private static final int THUMBNAIL_HEIGHT = 180;
    private final MemoryCache<Object, Icon> imageCache = MemoryCache.weak();
    private final Refresh<MediaTableModel> mediaTableModel = new Refresh<MediaTableModel>(this.mediaTable::setModel, this.mediaTable);
    private final Refresh<Object> descriptionModel = new Refresh<Object>(this.descriptionField::setValue, this.descriptionField);
    private final Refresh<Icon> imageModel = new Refresh<Icon>(this.imageField::setImage, this.descriptionField);

    public MatchDetailPanel(RenameModel renameModel, ListSelectionModel listSelectionModel) {
        this.model = renameModel;
        this.selection = listSelectionModel;
        this.setLayout((LayoutManager)new MigLayout("nogrid, novisualpadding, fillx"));
        JPanel jPanel = new JPanel((LayoutManager)new MigLayout("nogrid, novisualpadding, fillx"));
        jPanel.setOpaque(false);
        jPanel.add((Component)this.sourceFile, "growx, wrap paragraph");
        jPanel.add((Component)this.sourceObject, "growx, wrap paragraph, hidemode 3");
        jPanel.add((Component)this.targetObject, "growx, wrap paragraph");
        jPanel.add((Component)this.targetPath, "growx, wrap paragraph");
        jPanel.add((Component)this.destinationFile, "growx, wrap paragraph");
        JPanel jPanel2 = new JPanel((LayoutManager)new MigLayout("nogrid, fill"));
        jPanel2.setOpaque(false);
        this.jsonTextArea.setBracketMatchingEnabled(false);
        jPanel2.add((Component)this.imageField, "hidemode 3, pos 1al 1al");
        jPanel2.add((Component)SwingUI.createScrollPaneGroup("JSON", (Component)((Object)this.jsonTextArea)), "grow, sg 1");
        jPanel2.add((Component)this.descriptionField, "hidemode 3, growx, align 0 0, sg 1");
        JPanel jPanel3 = new JPanel((LayoutManager)new MigLayout("nogrid, fill"));
        JScrollPane jScrollPane = new JScrollPane(this.mediaTable);
        this.mediaTable.setFillsViewportHeight(true);
        this.mediaTable.setBackground(ThemeSupport.getPanelBackground());
        this.mediaTable.setGridColor(ThemeSupport.getColor(0xEEEEEE));
        this.mediaTable.setRowHeight(25);
        jPanel3.add((Component)new LoadingOverlayPane(jScrollPane, this.mediaTable, "25px", "30px"), "grow");
        this.container.setFocusable(false);
        this.container.addTab("Match", jPanel);
        this.container.addTab("Object", new LoadingOverlayPane(jPanel2, this.descriptionField));
        this.container.addTab("Media", ResourceManager.getIcon("action.properties"), jPanel3);
        this.add((Component)this.container, "grow");
        UserData.forPackage(MatchDetailPanel.class).restoreTabbedPane("inspect", this.container);
        this.setMatchIndex(-1);
        this.container.addChangeListener(changeEvent -> this.refresh());
    }

    public void setValue(File file, Object object, String string, File file2) {
        this.sample = new MediaBindingBean(object, file);
        MatchType matchType = MatchType.getType(object);
        Icon icon = MatchType.getIcon(object);
        this.container.setTitleAt(1, matchType.toString());
        this.container.setIconAt(1, icon);
        this.sourceFile.setValue(file);
        Object object2 = file != null ? XattrMetaInfo.xattr.getMetaInfo(file) : null;
        this.sourceObject.setVisible(object2 != null);
        this.sourceObject.setValue(object2);
        this.sourceObject.setIcon(MatchType.getIcon(object2), MatchType.getType(object2).canLink());
        this.targetObject.setValue(object);
        this.targetObject.setIcon(icon, matchType.canLink());
        this.targetPath.setValue(string);
        this.destinationFile.setValue(file2);
        this.jsonTextArea.setText(object != null ? this.toJson(object).toString() : "undefined");
        this.jsonTextArea.setEnabled(object != null);
        this.jsonTextArea.setEditable(false);
        this.jsonTextArea.setCaretPosition(0);
        if (this.container.getSelectedIndex() == 2) {
            this.refreshMediaDetails(file, file2);
        }
        if (this.container.getSelectedIndex() == 1) {
            this.refreshMatchDetails(object);
        }
    }

    private void refreshMediaDetails(File ... fileArray) {
        this.mediaTableModel.refresh(() -> MediaTableModel.read(fileArray));
    }

    private void refreshMatchDetails(Object object) {
        this.descriptionModel.refresh(() -> {
            try {
                Serializable serializable;
                if (object instanceof Episode) {
                    serializable = (Episode)object;
                    for (EpisodeListProvider episodeListProvider : WebServices.getEpisodeListProviders()) {
                        if (!EpisodeUtilities.isInstance((Datasource)episodeListProvider, (Episode)serializable)) continue;
                        EpisodeDetails episodeDetails = episodeListProvider.getEpisodeInfo((Episode)serializable, ((Episode)serializable).getSeriesInfo().getLanguage());
                        this.imageModel.refresh(() -> this.lambda$refreshMatchDetails$6(episodeDetails, (Episode)object));
                        return episodeDetails;
                    }
                }
                if (object instanceof Movie) {
                    serializable = (Movie)object;
                    MovieDetails movieDetails = WebServices.TheMovieDB.getMovieInfo((Movie)serializable, ((Movie)serializable).getLanguage(), true);
                    this.imageModel.refresh(() -> this.lambda$refreshMatchDetails$7(movieDetails, (Movie)object));
                    return movieDetails;
                }
            }
            catch (Exception exception) {
                this.imageModel.refresh(() -> null);
                return exception;
            }
            this.imageModel.refresh(() -> null);
            return null;
        });
    }

    private Object toJson(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof File) {
            object = ReadOnlyFile.asRegularFile((File)object);
        }
        return MetaAttributes.toJson(object, true);
    }

    public void setTabIndex(int n) {
        this.container.setSelectedIndex(n);
    }

    public void setMatchIndex(int n) {
        if (n >= 0) {
            if (n < this.model.names().size()) {
                FormattedFuture formattedFuture = (FormattedFuture)this.model.names().get(n);
                this.setValue(formattedFuture.getMatch().getCandidate(), formattedFuture.getMatch().getValue(), formattedFuture.getTargetPath(), formattedFuture.getDestinationFile());
                return;
            }
            if (n < this.model.size()) {
                Match match = this.model.getMatch(n);
                this.setValue((File)match.getCandidate(), match.getValue(), null, null);
                return;
            }
        }
        this.setValue(null, null, null, null);
    }

    private void refresh() {
        this.setMatchIndex(this.selection.getLeadSelectionIndex());
    }

    public void hook() {
        this.refresh();
        this.selection.addListSelectionListener(this);
        this.model.files().addListEventListener((ListEventListener)this);
        this.model.names().addListEventListener((ListEventListener)this);
    }

    public void unhook() {
        this.setMatchIndex(-1);
        this.selection.removeListSelectionListener(this);
        this.model.files().removeListEventListener((ListEventListener)this);
        this.model.names().removeListEventListener((ListEventListener)this);
        this.imageCache.invalidateAll();
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        if (listSelectionEvent.getValueIsAdjusting()) {
            return;
        }
        this.refresh();
    }

    public void listChanged(ListEvent<Object> listEvent) {
        while (listEvent.next()) {
            if (listEvent.getIndex() != this.selection.getLeadSelectionIndex()) continue;
            SwingUI.invokeLater(50, () -> this.refresh());
            return;
        }
    }

    private Icon cacheImage(Object object, URL uRL) throws Exception {
        if (uRL == null) {
            return null;
        }
        Icon icon = this.imageCache.getIfPresent(object);
        if (icon == null) {
            icon = this.fetchImage(uRL);
            this.imageCache.put(object, icon);
        }
        return icon;
    }

    private Icon fetchImage(URL uRL) throws Exception {
        byte[] byArray = Cache.getConcurrentCache("url", CacheType.Monthly).url(uRL).get();
        if (byArray == null || byArray.length == 0) {
            return null;
        }
        BufferedImage bufferedImage = ResourceManager.readImage(byArray);
        if (bufferedImage == null) {
            return null;
        }
        int n = (int)(320.0 * ResourceManager.PRIMARY_SCALE_FACTOR);
        int n2 = (int)(180.0 * ResourceManager.PRIMARY_SCALE_FACTOR);
        if (bufferedImage.getWidth() > n || bufferedImage.getHeight() > n2) {
            bufferedImage = Scalr.resize((BufferedImage)bufferedImage, (Scalr.Method)Scalr.Method.QUALITY, (Scalr.Mode)Scalr.Mode.FIT_TO_HEIGHT, (int)n, (int)n2, (BufferedImageOp[])new BufferedImageOp[0]);
        }
        return new ImageIcon(ResourceManager.getMultiResolutionImage(bufferedImage, ResourceManager.PRIMARY_SCALE_FACTOR));
    }

    private /* synthetic */ Icon lambda$refreshMatchDetails$7(MovieDetails movieDetails, Movie movie) throws Exception {
        return movieDetails == null ? null : this.cacheImage(movie, movieDetails.getPoster());
    }

    private /* synthetic */ Icon lambda$refreshMatchDetails$6(EpisodeDetails episodeDetails, Episode episode) throws Exception {
        return episodeDetails == null ? null : this.cacheImage(episode, episodeDetails.getImage());
    }

    private static class DetailField
    extends JPanel {
        private JTextField text;
        private JButton button;

        public DetailField(String string) {
            this.setLayout((LayoutManager)new MigLayout("insets 0, nogrid, fill"));
            this.setOpaque(false);
            this.add((Component)new JLabel(string), "wrap 2px");
            this.text = new JTextField();
            this.add((Component)this.text, "hmin 20px, growx");
        }

        public DetailField(String string, Action action) {
            this(string);
            this.button = SwingUI.createImageButton(action);
            this.add(this.button);
        }

        public void setValue(Object object) {
            this.text.setText(object != null ? object.toString() : "undefined");
            this.text.setEnabled(object != null);
            this.text.setEditable(false);
            this.text.setCaretPosition(0);
            if (this.button != null) {
                this.button.setEnabled(object != null);
            }
        }

        public void setIcon(Icon icon, boolean bl) {
            if (this.button != null) {
                this.button.setIcon(icon);
                this.button.setEnabled(bl);
            }
        }
    }

    private static class DescriptionField
    extends JLabel {
        private String link;

        public DescriptionField() {
            super(null, null, 2);
            this.setVisible(false);
            this.addMouseListener(SwingUI.mouseClicked(mouseEvent -> {
                if (this.link != null) {
                    UserInteraction.browse(this.link);
                }
            }));
        }

        public void setLink(String string) {
            this.link = string;
            this.setCursor(Cursor.getPredefinedCursor(string == null ? 0 : 12));
        }

        public void setValue(Object object) {
            if (object instanceof EpisodeDetails) {
                EpisodeDetails episodeDetails = (EpisodeDetails)object;
                StringBuilder stringBuilder = new StringBuilder(256);
                stringBuilder.append("<html>");
                if (episodeDetails.getTitle() != null) {
                    this.header(stringBuilder, episodeDetails.getTitle());
                } else {
                    this.header(stringBuilder, EpisodeFormat.DEFAULT.format(episodeDetails));
                }
                if (episodeDetails.getOverview() != null) {
                    this.paragraph(stringBuilder, episodeDetails.getOverview());
                } else {
                    this.paragraph(stringBuilder, "Summary is not available.");
                }
                stringBuilder.append("<br/>");
                this.paragraph(stringBuilder, "Writer", episodeDetails.getWriters());
                this.paragraph(stringBuilder, "Director", episodeDetails.getDirectors());
                stringBuilder.append("</html>");
                this.setText(stringBuilder.toString());
                this.setLink(episodeDetails.getPage());
                this.setVisible(true);
                return;
            }
            if (object instanceof MovieDetails) {
                MovieDetails movieDetails = (MovieDetails)object;
                StringBuilder stringBuilder = new StringBuilder(256);
                stringBuilder.append("<html>");
                if (movieDetails.getOriginalName() != null) {
                    this.header(stringBuilder, movieDetails.getOriginalName());
                } else {
                    this.header(stringBuilder, movieDetails.getName());
                }
                if (movieDetails.getOverview() != null) {
                    this.paragraph(stringBuilder, movieDetails.getOverview());
                } else {
                    this.paragraph(stringBuilder, "Summary is not available.");
                }
                stringBuilder.append("<br/>");
                this.paragraph(stringBuilder, "Director", movieDetails.getDirectors());
                this.paragraph(stringBuilder, "Release Date", movieDetails.getReleased());
                this.paragraph(stringBuilder, "Country", movieDetails.getProductionCountries());
                this.paragraph(stringBuilder, "Certification", movieDetails.getCertification());
                this.paragraph(stringBuilder, "Collection", movieDetails.getCollection());
                stringBuilder.append("</html>");
                this.setText(stringBuilder.toString());
                this.setLink(Link.TheMovieDB.getURL(movieDetails));
                this.setVisible(true);
                return;
            }
            if (object instanceof Exception) {
                Exception exception = (Exception)object;
                StringBuilder stringBuilder = new StringBuilder(256);
                stringBuilder.append("<html>");
                this.paragraph(stringBuilder, Logging.cause(exception).toString());
                stringBuilder.append("</html>");
                this.setText(stringBuilder.toString());
                this.setLink(null);
                this.setVisible(true);
                return;
            }
            this.setText(null);
            this.setLink(null);
            this.setVisible(false);
        }

        private StringBuilder header(StringBuilder stringBuilder, String string) {
            return string == null || string.isEmpty() ? stringBuilder : stringBuilder.append("<h3>").append(SwingUI.escapeHTML(string)).append("</h3>");
        }

        private StringBuilder paragraph(StringBuilder stringBuilder, String string) {
            return string == null || string.isEmpty() ? stringBuilder : stringBuilder.append("<p>").append(SwingUI.escapeHTML(string)).append("</p>");
        }

        private StringBuilder paragraph(StringBuilder stringBuilder, String string, String string2) {
            return string2 == null || string2.isEmpty() ? stringBuilder : stringBuilder.append("<p style='margin:3px'>").append(string).append(": ").append(SwingUI.escapeHTML(string2)).append("</p>");
        }

        private StringBuilder paragraph(StringBuilder stringBuilder, String string, Object object) {
            return object == null ? stringBuilder : this.paragraph(stringBuilder, string, object.toString());
        }

        private StringBuilder paragraph(StringBuilder stringBuilder, String string, List<String> list) {
            return list == null || list.isEmpty() ? stringBuilder : this.paragraph(stringBuilder, string, list.size() > 5 ? list.stream().limit(5L).collect(Collectors.joining(", ", "", ", ...")) : list.stream().collect(Collectors.joining(", ")));
        }
    }

    private static class ImageField
    extends JPanel {
        private JLabel imageLabel = new JLabel(null, null, 0);

        public ImageField() {
            this.setVisible(false);
            this.setLayout(new BorderLayout());
            this.setOpaque(false);
            this.setBorder(SwingUI.shadow());
            this.add((Component)this.imageLabel, "Center");
        }

        public void setImage(Icon icon) {
            if (icon == null) {
                this.imageLabel.setIcon(null);
                this.setVisible(false);
            } else {
                this.imageLabel.setIcon(icon);
                this.setVisible(true);
            }
        }
    }

    private static class MediaTableModel
    extends AbstractTableModel {
        private final File[] files;
        private final MediaCharacteristics[] properties;
        private final Object[] xattr;
        private static final String[] COLUMN_NAMES = new String[]{"Original File", "Destination File"};
        private static final String[] PROPERTY_NAMES = new String[]{"File Name", "File Size", "Resolution", "Video Codec", "Audio Codec", "Audio Languages", "Subtitle Languages", "Duration", "Bitrate", "Creation Time", "Media Tags", "Extended Attributes"};

        public MediaTableModel() {
            this.files = new File[COLUMN_NAMES.length];
            this.properties = new MediaCharacteristics[COLUMN_NAMES.length];
            this.xattr = new Object[COLUMN_NAMES.length];
        }

        public MediaTableModel(File[] fileArray, MediaCharacteristics[] mediaCharacteristicsArray, Object[] objectArray) {
            this.files = fileArray;
            this.properties = mediaCharacteristicsArray;
            this.xattr = objectArray;
        }

        @Override
        public int getColumnCount() {
            return 1 + this.files.length;
        }

        @Override
        public String getColumnName(int n) {
            return n == 0 ? "" : COLUMN_NAMES[n - 1];
        }

        @Override
        public int getRowCount() {
            return PROPERTY_NAMES.length;
        }

        public String getRowName(int n) {
            return PROPERTY_NAMES[n];
        }

        public Optional<Object> getProperty(String string, int n) {
            switch (string) {
                case "File Name": {
                    return MediaTableModel.optional(this.files, n).map(File::getName);
                }
                case "File Size": {
                    return MediaTableModel.optional(this.files, n).map(File::length).map(FileSize::new);
                }
                case "Resolution": {
                    return MediaTableModel.optional(this.properties, n).map(mediaCharacteristics -> mediaCharacteristics.getWidth() + "x" + mediaCharacteristics.getHeight());
                }
                case "Video Codec": {
                    return MediaTableModel.optional(this.properties, n).map(mediaCharacteristics -> mediaCharacteristics.getVideoCodec() + " (" + mediaCharacteristics.getVideoProfile() + ")");
                }
                case "Audio Codec": {
                    return MediaTableModel.optional(this.properties, n).map(MediaCharacteristics::getAudioCodec);
                }
                case "Audio Languages": {
                    return MediaTableModel.optional(this.properties, n).map(MediaCharacteristics::getAudioLanguage);
                }
                case "Subtitle Languages": {
                    return MediaTableModel.optional(this.properties, n).map(MediaCharacteristics::getSubtitleLanguage);
                }
                case "Duration": {
                    return MediaTableModel.optional(this.properties, n).map(MediaCharacteristics::getDuration).map(duration -> DateTimeUtilities.format(duration, "H:mm", Locale.US));
                }
                case "Bitrate": {
                    return MediaTableModel.optional(this.properties, n).map(MediaCharacteristics::getBitRate).map(BitRate::new);
                }
                case "Creation Time": {
                    return MediaTableModel.optional(this.properties, n).map(MediaCharacteristics::getCreationTime).map(instant -> DateTimeUtilities.format(instant, "uuuu-MM-dd HH:mm:ss", Locale.US));
                }
                case "Media Tags": {
                    return MediaTableModel.optional(this.properties, n).map(MediaCharacteristics::getMediaTags);
                }
                case "Extended Attributes": {
                    return MediaTableModel.optional(this.xattr, n);
                }
            }
            throw new IllegalArgumentException(string);
        }

        @Override
        public Object getValueAt(int n, int n2) {
            String string = this.getRowName(n);
            return n2 == 0 ? string : this.getProperty(string, n2 - 1).map(Object::toString).orElse(null);
        }

        private static <T> Optional<T> optional(T[] TArray, int n) {
            return Optional.ofNullable(TArray[n]);
        }

        public static MediaTableModel read(File ... fileArray) {
            File[] fileArray2 = (File[])Stream.of(fileArray).filter(Objects::nonNull).map(ReadOnlyFile::asRegularFile).filter(File::exists).map(ReadOnlyFile::of).toArray(File[]::new);
            MediaCharacteristics[] mediaCharacteristicsArray = (MediaCharacteristics[])Stream.of(fileArray2).map(file -> CachedMediaCharacteristics.getMediaCharacteristics(file).orElse(null)).toArray(MediaCharacteristics[]::new);
            Object[] objectArray = Stream.of(fileArray2).map(XattrMetaInfo.xattr::getMetaInfo).toArray(Object[]::new);
            return fileArray2.length == 0 ? new MediaTableModel() : new MediaTableModel(fileArray2, mediaCharacteristicsArray, objectArray);
        }
    }

    private static class Refresh<T> {
        private final Consumer<T> consumer;
        private final JComponent propertyChangeSource;
        private Future<T> future;

        public Refresh(Consumer<T> consumer, JComponent jComponent) {
            this.consumer = consumer;
            this.propertyChangeSource = jComponent;
        }

        public void refresh(SwingUI.BackgroundSupplier<T> backgroundSupplier) {
            if (this.future != null && !this.future.isDone()) {
                this.future.cancel(false);
            }
            this.propertyChangeSource.firePropertyChange("loading", false, true);
            this.future = SwingUI.onSwingWorker(backgroundSupplier, this.consumer, exception -> Logging.log.warning(Logging.cause(exception)), () -> this.propertyChangeSource.firePropertyChange("loading", true, false));
        }
    }
}

