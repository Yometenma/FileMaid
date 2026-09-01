package net.filemaid.ui.subtitle.upload;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import net.filemaid.Language;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.WebServices;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.ui.BaseDialog;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.subtitle.upload.FileEditor;
import net.filemaid.ui.subtitle.upload.FileRenderer;
import net.filemaid.ui.subtitle.upload.LanguageEditor;
import net.filemaid.ui.subtitle.upload.LanguageRenderer;
import net.filemaid.ui.subtitle.upload.MovieEditor;
import net.filemaid.ui.subtitle.upload.MovieRenderer;
import net.filemaid.ui.subtitle.upload.Status;
import net.filemaid.ui.subtitle.upload.StatusRenderer;
import net.filemaid.ui.subtitle.upload.SubtitleGroup;
import net.filemaid.ui.subtitle.upload.SubtitleMapping;
import net.filemaid.ui.subtitle.upload.SubtitleMappingTableModel;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.EmptySelectionModel;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.Movie;
import net.filemaid.web.SearchResult;
import net.filemaid.web.Series;
import net.filemaid.web.SubtitleLookupService;
import net.filemaid.web.XDB;
import net.miginfocom.swing.MigLayout;

public class SubtitleUploadDialog
extends BaseDialog {
    private final JTable subtitleMappingTable;
    private final WebServices.OpenSubtitlesClient database;
    private ExecutorService checkExecutorService = Executors.newSingleThreadExecutor();
    private ExecutorService uploadExecutorService;
    private static final Pattern CDI_PATTERN = Pattern.compile("(?<!\\p{Alnum})CD\\D?(?<i>[1-9])(?!\\p{Digit})", 258);

    public SubtitleUploadDialog(WebServices.OpenSubtitlesClient openSubtitlesClient, Window window) {
        super(window, "Upload Subtitles");
        this.database = openSubtitlesClient;
        this.subtitleMappingTable = this.createTable();
        JComponent jComponent = (JComponent)this.getContentPane();
        jComponent.setLayout((LayoutManager)new MigLayout("fill, insets dialog, nogrid, novisualpadding", "", "[fill][pref!]"));
        jComponent.add((Component)new JScrollPane(this.subtitleMappingTable), "grow, wrap");
        jComponent.add((Component)SwingUI.newButton("Upload", ResourceManager.getIcon("dialog.continue"), this::doUpload), "tag ok");
        jComponent.add((Component)SwingUI.newButton("Close", ResourceManager.getIcon("dialog.cancel"), this::doClose), "tag cancel");
    }

    protected JTable createTable() {
        JTable jTable = new JTable(new SubtitleMappingTableModel());
        jTable.setDefaultRenderer(Movie.class, new MovieRenderer(this.database.getIcon()));
        jTable.setDefaultRenderer(File.class, new FileRenderer());
        jTable.setDefaultRenderer(Language.class, new LanguageRenderer());
        jTable.setDefaultRenderer(Status.class, new StatusRenderer());
        jTable.setRowHeight(28);
        jTable.setIntercellSpacing(new Dimension(5, 5));
        jTable.setBackground(ThemeSupport.getPanelBackground());
        jTable.setAutoCreateRowSorter(true);
        jTable.setFillsViewportHeight(true);
        jTable.setSelectionModel(new EmptySelectionModel());
        jTable.setDefaultEditor(Movie.class, new MovieEditor(this.database));
        jTable.setDefaultEditor(File.class, new FileEditor());
        jTable.setDefaultEditor(Language.class, new LanguageEditor());
        return jTable;
    }

    public void setUploadPlan(Map<File, File> map) {
        ArrayList<SubtitleMapping> arrayList = new ArrayList<SubtitleMapping>(map.size());
        for (Map.Entry<File, File> entry : map.entrySet()) {
            File file = entry.getKey();
            File file2 = entry.getValue();
            Locale locale = MediaFileUtilities.guessLanguageFromSuffix(file);
            Language language = Language.getLanguage(locale);
            arrayList.add(new SubtitleMapping(file, file2, language));
        }
        this.subtitleMappingTable.setModel(new SubtitleMappingTableModel(arrayList).onCheckPending(this::startChecking));
    }

    public void startChecking() {
        for (SubtitleMapping subtitleMapping : ((SubtitleMappingTableModel)this.subtitleMappingTable.getModel()).getData()) {
            if (!subtitleMapping.isCheckReady()) continue;
            this.checkExecutorService.submit(() -> this.runCheck(subtitleMapping));
        }
    }

    private int getCD(SubtitleMapping subtitleMapping) {
        int n = Integer.MIN_VALUE;
        for (File file : new File[]{subtitleMapping.getSubtitle(), subtitleMapping.getVideo()}) {
            Matcher matcher = CDI_PATTERN.matcher(file.getName());
            while (matcher.find()) {
                n = Integer.parseInt(matcher.group("i"));
            }
        }
        return n;
    }

    private List<SubtitleGroup> getUploadGroups(SubtitleMapping[] subtitleMappingArray) {
        return Stream.of(subtitleMappingArray).filter(SubtitleMapping::isGroupReady).collect(Collectors.groupingBy(SubtitleMapping::getGroup, LinkedHashMap::new, Collectors.toList())).values().stream().flatMap(this::groupRunsByCD).filter(SubtitleGroup::isUploadReady).collect(Collectors.toList());
    }

    private Stream<SubtitleGroup> groupRunsByCD(List<SubtitleMapping> list) {
        list.sort(Comparator.comparing(SubtitleMapping::getVideo));
        return this.groupRuns(list, (subtitleMapping, subtitleMapping2) -> this.getCD((SubtitleMapping)subtitleMapping) + 1 == this.getCD((SubtitleMapping)subtitleMapping2)).map(SubtitleGroup::new);
    }

    private <T> Stream<List<T>> groupRuns(List<T> list, BiPredicate<T, T> biPredicate) {
        int[] nArray = IntStream.rangeClosed(0, list.size()).filter(n -> n == 0 || n == list.size() || !biPredicate.test(list.get(n - 1), list.get(n))).toArray();
        return IntStream.range(0, nArray.length - 1).mapToObj(n -> list.subList(nArray[n], nArray[n + 1]));
    }

    private void runCheck(SubtitleMapping subtitleMapping) {
        try {
            block18: {
                if (subtitleMapping.getIdentity() == null && subtitleMapping.getVideo() != null) {
                    subtitleMapping.setState(Status.Checking);
                    SubtitleLookupService.CheckResult checkResult = this.database.checkSubtitle(subtitleMapping.getVideo(), subtitleMapping.getSubtitle());
                    if (checkResult.exists) {
                        subtitleMapping.setLanguage(Language.getLanguage(checkResult.language));
                    }
                }
                if (subtitleMapping.getLanguage() == null) {
                    subtitleMapping.setState(Status.Identifying);
                    try {
                        Locale locale = this.database.detectLanguage(FileUtilities.readFile(subtitleMapping.getSubtitle()));
                        subtitleMapping.setLanguage(Language.getLanguage(locale));
                    }
                    catch (Exception exception) {
                        Logging.debug.warning(Logging.cause("Failed to auto-detect language", exception));
                    }
                }
                if (subtitleMapping.getIdentity() == null && subtitleMapping.getVideo() != null) {
                    subtitleMapping.setState(Status.Identifying);
                    try {
                        if (MediaDetection.isEpisode(subtitleMapping.getVideo().getPath(), true)) {
                            for (Series series : MediaDetection.detectSeries(Collections.singleton(subtitleMapping.getVideo()), false, Locale.US)) {
                                Integer n = this.getID(series);
                                if (n == null || n <= 0) continue;
                                subtitleMapping.setIdentity(this.database.getMovieDescriptor(Movie.IMDB(n), Locale.US));
                                break block18;
                            }
                            break block18;
                        }
                        for (Movie movie : MediaDetection.detectMovie(subtitleMapping.getVideo(), this.database, Locale.US, true, false)) {
                            if (movie.getImdbId() < 1 && movie.getTmdbId() > 0) {
                                movie = WebServices.TheMovieDB.getMovieDescriptor(movie, Locale.US);
                            }
                            if (movie == null || movie.getImdbId() <= 0) continue;
                            subtitleMapping.setIdentity(movie);
                            break;
                        }
                    }
                    catch (Exception exception) {
                        Logging.debug.warning(Logging.cause("Failed to auto-detect movie", exception));
                    }
                }
            }
            if (subtitleMapping.getVideo() == null) {
                subtitleMapping.setState(Status.IllegalInput);
            } else if (subtitleMapping.getIdentity() == null || subtitleMapping.getLanguage() == null) {
                subtitleMapping.setState(Status.IdentificationRequired);
            } else {
                subtitleMapping.setState(Status.UploadReady);
            }
        }
        catch (Throwable throwable) {
            Logging.log.warning(Logging.cause(throwable));
            subtitleMapping.setState(Status.CheckFailed);
        }
    }

    private Integer getID(Series series) throws Exception {
        Integer n = series.getExternalId(XDB.IMDb);
        if (n != null) {
            return n;
        }
        Integer n2 = series.getExternalId(XDB.TheMovieDB);
        if (n2 == null) {
            n2 = WebServices.TheMovieDB_TV.search(series.getName(), Locale.US).stream().map(SearchResult::getId).findFirst().orElse(null);
        }
        return XDB.TheMovieDB.getExternalId(n2, XDB.IMDb);
    }

    private void runUpload(SubtitleGroup subtitleGroup) {
        try {
            subtitleGroup.setState(Status.Uploading);
            this.database.uploadSubtitle(subtitleGroup.getIdentity(), subtitleGroup.getLanguage().getLocale(), subtitleGroup.getVideoFiles(), subtitleGroup.getSubtitleFiles());
            subtitleGroup.setState(Status.UploadComplete);
        }
        catch (FileAlreadyExistsException fileAlreadyExistsException) {
            Logging.debug.info(fileAlreadyExistsException::getMessage);
            subtitleGroup.setState(Status.AlreadyExists);
        }
        catch (Throwable throwable) {
            Logging.log.warning(Logging.cause(throwable));
            subtitleGroup.setState(Status.UploadFailed);
        }
    }

    public void doUpload(ActionEvent actionEvent) {
        if (this.subtitleMappingTable.getCellEditor() != null) {
            this.subtitleMappingTable.getCellEditor().stopCellEditing();
        }
        if (this.uploadExecutorService != null && !this.uploadExecutorService.isTerminated()) {
            return;
        }
        SubtitleMapping[] subtitleMappingArray = ((SubtitleMappingTableModel)this.subtitleMappingTable.getModel()).getData();
        List<SubtitleGroup> list = this.getUploadGroups(subtitleMappingArray);
        if (list.isEmpty()) {
            return;
        }
        this.uploadExecutorService = Executors.newSingleThreadExecutor();
        for (SubtitleGroup subtitleGroup : list) {
            this.uploadExecutorService.submit(() -> this.runUpload(subtitleGroup));
        }
        this.uploadExecutorService.shutdown();
    }

    public void doClose(ActionEvent actionEvent) {
        if (this.checkExecutorService != null) {
            this.checkExecutorService.shutdownNow();
        }
        if (this.uploadExecutorService != null) {
            this.uploadExecutorService.shutdownNow();
        }
        this.setVisible(false);
        this.dispose();
    }
}

