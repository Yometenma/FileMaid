package net.filemaid.ui.rename;

import java.awt.Component;
import java.awt.Cursor;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.InvalidInputException;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.Parallelism;
import net.filemaid.UserData;
import net.filemaid.UserInteraction;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.similarity.DerivateCollection;
import net.filemaid.similarity.EpisodeMatcher;
import net.filemaid.similarity.EpisodeMetrics;
import net.filemaid.similarity.Match;
import net.filemaid.similarity.Matcher;
import net.filemaid.similarity.Normalization;
import net.filemaid.ui.SelectDialog;
import net.filemaid.ui.rename.AutoCompleteMatcher;
import net.filemaid.ui.rename.AutoDetectionMode;
import net.filemaid.ui.rename.AutoSelectionMode;
import net.filemaid.ui.rename.BlankThumbnail;
import net.filemaid.ui.rename.MatchMode;
import net.filemaid.ui.rename.OriginalOrder;
import net.filemaid.ui.rename.TextColorizer;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.StringUtilities;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.LookupException;
import net.filemaid.web.SearchResult;
import net.filemaid.web.Series;
import net.filemaid.web.SortOrder;
import net.filemaid.web.ThumbnailProvider;

class EpisodeListMatcher
implements AutoCompleteMatcher {
    private final EpisodeListProvider provider;
    private final boolean anime;
    private final Cache.TypedCache<SearchResult> persistentSelectionMemory;
    private final Map<String, Optional<SearchResult>> selectionMemory = new HashMap<String, Optional<SearchResult>>();
    private final Map<String, List<String>> inputMemory = new HashMap<String, List<String>>();
    private final EpisodeMetrics metrics = new EpisodeMetrics();

    public EpisodeListMatcher(EpisodeListProvider episodeListProvider, boolean bl) {
        this.provider = episodeListProvider;
        this.anime = bl;
        this.persistentSelectionMemory = Cache.getCache("series_selection_" + episodeListProvider.getName(), CacheType.Persistent).cast(SearchResult.class);
    }

    @Override
    public List<Match<File, ?>> match(Collection<File> collection, MatchMode matchMode, SortOrder sortOrder, Locale locale, AutoDetectionMode autoDetectionMode, Set<AutoSelectionMode> set, Component component) throws Exception {
        List<File> list;
        if (collection.isEmpty()) {
            return this.justFetchEpisodeList(sortOrder, locale, set, component);
        }
        List<File> list2 = list = autoDetectionMode == AutoDetectionMode.Auto ? FileUtilities.filter(collection, FileUtilities.not(MediaFileUtilities.EXTRA_FILES)) : collection.stream().collect(Collectors.toList());
        if (list.isEmpty()) {
            Logging.log.info(Logging.format("%s episode files have been selected alongside %s companion %s.", list.size(), collection.size(), collection.size() == 1 ? "file" : "files"));
            return Collections.emptyList();
        }
        LinkedHashSet<File> linkedHashSet = new LinkedHashSet<File>(FileUtilities.filter(list, MediaTypes.VIDEO_FILES));
        LinkedHashSet<File> linkedHashSet2 = new LinkedHashSet<File>(FileUtilities.filter(list, FileUtilities.FOLDERS));
        DerivateCollection derivateCollection = DerivateCollection.derive(list, linkedHashSet, linkedHashSet2);
        ArrayList<File> arrayList = new ArrayList<File>();
        arrayList.addAll(linkedHashSet);
        arrayList.addAll(linkedHashSet2);
        arrayList.addAll(FileUtilities.filter(derivateCollection.orphans(), MediaTypes.SUBTITLE_FILES));
        arrayList.addAll(FileUtilities.filter(derivateCollection.orphans(), file -> MediaTypes.NFO_FILES.accept(file) && MediaDetection.isEpisode(file, true)));
        arrayList.addAll(FileUtilities.filter(derivateCollection.orphans(), file -> MediaTypes.IMAGE_FILES.accept(file) && MediaDetection.isEpisode(file, true)));
        if (arrayList.isEmpty()) {
            throw new InvalidInputException("No episode files have been selected. Please <Load> episode files.");
        }
        ArrayList<Match<File, ?>> arrayList2 = new ArrayList<>();
        for (List<Match<File, ?>> list3 : this.matchVideoFiles(arrayList, sortOrder, locale, matchMode == MatchMode.Strict, autoDetectionMode, set, component)) {
            for (Match<File, ?> match : list3) {
                File file2 = match.getValue();
                Episode episode = (Episode)match.getCandidate();
                derivateCollection.matches(file2, episode::clone).forEach(arrayList2::add);
            }
        }
        arrayList2.sort(Comparator.comparing((Match<File, ?> match) -> match.getValue(), OriginalOrder.of(collection)));
        return arrayList2;
    }

    protected List<List<Match<File, ?>>> matchVideoFiles(Collection<File> collection, SortOrder sortOrder, Locale locale, boolean bl, AutoDetectionMode autoDetectionMode, Set<AutoSelectionMode> set, Component component) throws Exception {
        if (bl) {
            List<File> list = collection.stream().filter(file -> MediaDetection.isEpisode(file, false)).collect(Collectors.toList());
            List<Map.Entry<Set<Series>, List<File>>> list2 = Parallelism.commonPool().group(list, file -> MediaDetection.detectSeries(Collections.singleton(file), this.anime, locale)).entrySet().stream().filter(entry -> !((Set)entry.getKey()).isEmpty()).collect(Collectors.toList());
            return Parallelism.commonPool().map(list2, entry -> this.matchEpisodeSet(entry.getValue(), entry.getKey(), sortOrder, locale, bl, autoDetectionMode, set, component));
        }
        List<Match<? extends List<File>, Set<Series>>> list = MediaDetection.mapSeriesNamesByFiles(collection, locale, this.anime).entrySet().stream().flatMap(entry -> {
            ArrayList<File> arrayList = new ArrayList<File>(entry.getKey());
            Set<Series> series = entry.getValue();
            if (series != null && !series.isEmpty()) {
                return Stream.of(Match.of(arrayList, series));
            }
            return FileUtilities.mapByFolder(arrayList).values().stream().map(files -> Match.of(files, series));
        }).collect(Collectors.toList());
        return Parallelism.commonPool().map(list, match -> this.matchEpisodeSet((List)match.getValue(), match.getCandidate(), sortOrder, locale, bl, autoDetectionMode, set, component));
    }

    protected List<Match<File, ?>> matchEpisodeSet(List<File> list, Set<Series> set, SortOrder sortOrder, Locale locale, boolean bl, AutoDetectionMode autoDetectionMode, Set<AutoSelectionMode> set2, Component component) throws Exception {
        List<String> list2;
        Object object;
        if (set2.contains((Object)AutoSelectionMode.Cancel)) {
            return Collections.emptyList();
        }
        Collection<Episode> collection = Collections.emptySet();
        if (autoDetectionMode != AutoDetectionMode.Input && set != null && !set.isEmpty()) {
            collection = this.fetchEpisodeSet(list, false, set, sortOrder, locale, bl, autoDetectionMode, set2, component);
        }
        if (collection.isEmpty() && (autoDetectionMode == AutoDetectionMode.Input || !bl)) {
            object = this.getQuerySuggestion(list, locale);
            String string2 = MediaDetection.getUniqueQueryKey((String)object);
            Map<String, List<String>> object2 = this.inputMemory;
            synchronized (object2) {
                List<String> list3 = list2 = string2 == null ? null : this.inputMemory.get(string2);
                if (list2 == null) {
                    if (set2.contains((Object)AutoSelectionMode.Cancel)) {
                        return Collections.emptyList();
                    }
                    set2.add(AutoSelectionMode.Input);
                    list2 = SwingUI.showMultiValueInputDialog(this.getQueryInputMessage("Please identify the following files:", "Enter series name:", list), (String)object, this.provider.getName(), this.provider.getIcon(), component);
                    if (string2 != null) {
                        this.inputMemory.put(string2, list2);
                        Map<String, Optional<SearchResult>> map = this.selectionMemory;
                        synchronized (map) {
                            list2.forEach(string -> this.selectionMemory.remove(MediaDetection.getUniqueQueryKey(string)));
                        }
                    }
                }
                collection = this.fetchEpisodeSet(list, true, this.query(list2), sortOrder, locale, bl, AutoDetectionMode.Input, set2, component);
            }
        }
        List<Match<File, ?>> matches = new ArrayList<>();
        if (collection.size() > 0) {
            collection = this.map(collection);
            for (List<File> list4 : MediaFileUtilities.mapByMediaExtension(list).values()) {
                EpisodeMatcher episodeMatcher = new EpisodeMatcher(list4, collection, bl, this.metrics.matchSequence());
                for (Match match : episodeMatcher.match()) {
                    File file = (File)match.getValue();
                    Episode episode = (Episode)match.getCandidate();
                    if (bl && !MediaDetection.isEpisodeNumberMatch(file, episode)) continue;
                    episode = this.unmap(episode);
                    matches.add(Match.of(file, episode));
                }
            }
        }
        return matches;
    }

    protected Collection<Episode> map(Collection<Episode> collection) {
        return collection;
    }

    protected Episode unmap(Episode episode) {
        return episode;
    }

    protected Set<Episode> fetchEpisodeSet(List<File> list, boolean bl, Set<Series> set, SortOrder sortOrder, Locale locale, boolean bl2, AutoDetectionMode autoDetectionMode, Set<AutoSelectionMode> set2, Component component) throws Exception {
        LinkedHashSet<Episode> linkedHashSet = new LinkedHashSet<Episode>();
        for (SearchResult searchResult : this.lookupSeries(list, bl, set, locale, bl2, autoDetectionMode, set2, component)) {
            try {
                linkedHashSet.addAll(this.provider.getEpisodeList(searchResult, sortOrder, locale));
            }
            catch (LookupException lookupException) {
                Logging.debug.warning(Logging.cause(lookupException));
            }
        }
        return linkedHashSet;
    }

    private Set<SearchResult> lookupSeries(List<File> list, boolean bl, Set<Series> set, Locale locale, boolean bl2, AutoDetectionMode autoDetectionMode, Set<AutoSelectionMode> set2, Component component) throws Exception {
        LinkedHashMap<String, SearchResult> linkedHashMap = new LinkedHashMap<>();
        for (Series series : set) {
            SearchResult object;
            SearchResult searchResult2;
            String string = series.getName();
            String string2 = MediaDetection.getUniqueQueryKey(string);
            SearchResult searchResult3 = searchResult2 = autoDetectionMode == AutoDetectionMode.Auto ? this.provider.id(series) : null;
            if (searchResult2 != null && series.getScore() == Integer.MAX_VALUE) {
                Logging.debug.fine(Logging.format("Select Series ID '%s' => %s", string2, searchResult2.getId()));
                linkedHashMap.put(string2, searchResult2);
                continue;
            }
            if (string2 != null && autoDetectionMode == AutoDetectionMode.Auto && (object = this.persistentSelectionMemory.get(string2)) != null) {
                Logging.debug.fine(Logging.format("Repeat Select Series '%s' => %s", string2, object));
                linkedHashMap.put(string2, object);
                continue;
            }
            if (searchResult2 != null) {
                Logging.debug.fine(Logging.format("Select Series ID '%s' => %s", string2, searchResult2.getId()));
                if (linkedHashMap.put(string2, searchResult2) == null) continue;
            }
            if (string2 == null) continue;
            List<SearchResult> results = bl ? this.provider.lookup(string, locale) : this.provider.search(string, locale);
            Logging.debug.fine(Logging.format("Search Series '%s' => %s", series, results));
            if (results.stream().allMatch(linkedHashMap.values()::contains)) continue;
            SearchResult searchResult4 = this.selectSearchResult(list, string, results, bl2, autoDetectionMode, set2, component);
            Logging.debug.fine(Logging.format("Select Series '%s' => %s", string2, searchResult4));
            if (set2.contains((Object)AutoSelectionMode.Cancel)) {
                return Collections.emptySet();
            }
            if (searchResult4 == null) continue;
            if (autoDetectionMode == AutoDetectionMode.Select) {
                return Collections.singleton(searchResult4);
            }
            List<String> list2 = searchResult4.getEffectiveNames();
            linkedHashMap.values().removeIf(searchResult -> searchResult.getEffectiveNames().stream().anyMatch(list2::contains));
            linkedHashMap.put(string2, searchResult4);
        }
        return linkedHashMap.values().stream().collect(Collectors.toCollection(LinkedHashSet::new));
    }

    protected SearchResult selectSearchResult(List<File> list, String string, List<SearchResult> list2, boolean bl, AutoDetectionMode autoDetectionMode, Set<AutoSelectionMode> set, Component component) throws Exception {
        Collection<SearchResult> list3;
        if (list2.isEmpty()) {
            return null;
        }
        if (list2.size() == 1 && autoDetectionMode != AutoDetectionMode.Select) {
            return list2.get(0);
        }
        List<SearchResult> list4 = MediaDetection.getProbableMatches(string, list2, true, true);
        if (list4.size() == 1 && autoDetectionMode == AutoDetectionMode.Auto) {
            return list4.get(0);
        }
        String string2 = MediaDetection.getUniqueQueryKey(string);
        Collection<SearchResult> collection = list3 = bl ? (Collection<SearchResult>)list4 : Stream.concat(list4.stream(), list2.stream()).collect(Collectors.toCollection(LinkedHashSet::new));
        if (list3.isEmpty()) {
            return null;
        }
        Callable<SearchResult> callable = () -> {
            SelectDialog<SearchResult> selectDialog = new SelectDialog<SearchResult>(component, list3, this.preview(), this.thumbnail(component), true, false, this.createDialogHeader(this.getFilesForQuery(list, string)));
            selectDialog.setTitle(this.provider.getName());
            selectDialog.getMessageLabel().setText(this.getQueryInputPrompt(string, list3, autoDetectionMode));
            selectDialog.getCancelAction().putValue("Name", "Skip");
            selectDialog.getCancelAction().putValue("ShortDescription", SwingUI.formatHTML("<html>Select none of the above. \"<b>%s</b>\" is not the series name.</html>", string));
            selectDialog.pack();
            UserData userData = UserData.forPackage(EpisodeListMatcher.class).node("dialog.select.series");
            userData.restoreWindowBounds(selectDialog, window -> SwingUI.getOffsetLocation(window));
            userData.restoreToggleButton(selectDialog.repeatToggle);
            selectDialog.setVisible(true);
            SearchResult searchResult = selectDialog.getSelectedValue();
            if (searchResult == null && selectDialog.getSelectedAction() == SelectDialog.CLOSE) {
                set.addAll(AutoSelectionMode.cancel());
            }
            if (selectDialog.repeatToggle.isSelected()) {
                if (searchResult != null && string2 != null) {
                    this.persistentSelectionMemory.put(string2, searchResult);
                }
                if (bl) {
                    set.add(searchResult != null ? AutoSelectionMode.Auto : AutoSelectionMode.Skip);
                }
            }
            return searchResult;
        };
        Map<String, Optional<SearchResult>> map = this.selectionMemory;
        synchronized (map) {
            Optional<SearchResult> object;
            if (string2 != null && (object = this.selectionMemory.get(string2)) != null) {
                return object.orElse(null);
            }
            if (autoDetectionMode != AutoDetectionMode.Select) {
                if (set.contains((Object)AutoSelectionMode.Auto)) {
                    return list2.iterator().next();
                }
                if (set.contains((Object)AutoSelectionMode.Skip)) {
                    return null;
                }
            }
            SearchResult searchResult = SwingUI.showInputDialog(callable);
            if (string2 != null) {
                this.selectionMemory.put(string2, Optional.ofNullable(searchResult));
            }
            return searchResult;
        }
    }

    protected Function<SearchResult, Icon> preview() {
        return searchResult -> BlankThumbnail.BLANK_POSTER;
    }

    protected Function<SearchResult, CompletableFuture<Icon>> thumbnail(Component component) {
        if (this.provider instanceof ThumbnailProvider) {
            ThumbnailProvider thumbnailProvider = (ThumbnailProvider)((Object)this.provider);
            ThumbnailProvider.ResolutionVariant resolutionVariant = ThumbnailProvider.ResolutionVariant.fromScaleFactor(component);
            return searchResult -> thumbnailProvider.requestThumbnail(searchResult.getId(), resolutionVariant);
        }
        return null;
    }

    protected List<File> getFilesForQuery(List<File> list, String string) {
        if (string == null || string.isEmpty()) {
            return list;
        }
        Pattern pattern = Pattern.compile(Normalization.normalizePunctuation(string).replaceAll("\\W+", ".+"), 258);
        List<File> list2 = list.stream().filter(file -> StringUtilities.find(file.getPath(), pattern)).collect(Collectors.toList());
        if (list2.size() > 0) {
            return list2;
        }
        return list;
    }

    protected JComponent createDialogHeader(List<File> list) throws Exception {
        String string = this.getQueryInputMessage("Unable to uniquely identify some of the following files:", null, list);
        if (string.isEmpty()) {
            return null;
        }
        JLabel jLabel = new JLabel(string);
        jLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(""), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        jLabel.setCursor(Cursor.getPredefinedCursor(12));
        jLabel.addMouseListener(SwingUI.mouseClicked(mouseEvent -> UserInteraction.revealFiles(list)));
        return jLabel;
    }

    protected String getQuerySuggestion(List<File> list, Locale locale) throws Exception {
        Set<Series> set = MediaDetection.detectSeries(list, this.anime, locale);
        Object[] objectArray = (String[])MediaDetection.mapByUniqueQueryKey(set, SearchResult::getName).values().stream().toArray(String[]::new);
        if (objectArray.length == 0) {
            objectArray = (String[])list.stream().map((File file) -> MediaDetection.stripReleaseInfo(FileUtilities.getName(file))).filter(string -> !string.isEmpty()).limit(1L).distinct().toArray(String[]::new);
        }
        for (int i = 0; i < objectArray.length; ++i) {
            if (this.provider.id((String)objectArray[i]) == null) continue;
            objectArray[i] = "\"" + (String)objectArray[i] + "\"";
        }
        return StringUtilities.join(objectArray, (CharSequence)" | ");
    }

    protected String getQueryInputMessage(String string, String string2, List<File> list) throws Exception {
        List<File> list2 = list.stream().sorted(MediaFileUtilities.FILE_SIZE_DESCENDING_ORDER).limit(4L).sorted(FileUtilities.HUMAN_NAME_ORDER).collect(Collectors.toList());
        if (list2.isEmpty()) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder(512);
        stringBuilder.append("<html>");
        if (string != null) {
            stringBuilder.append(SwingUI.escapeHTML(string)).append("<br>");
        }
        TextColorizer textColorizer = new TextColorizer("<nobr>\u2022 ", "</nobr><br>");
        for (File file : list2) {
            File file2 = MediaFileUtilities.getStructurePathTail(file);
            if (file2 == null) {
                file2 = FileUtilities.getRelativePathTail(file, 3);
            }
            textColorizer.colorizePath(stringBuilder, file2, true);
        }
        if (list2.size() < list.size()) {
            stringBuilder.append("\u2022 ").append("\u2026").append("<br>");
        }
        stringBuilder.append("<br>");
        if (string2 != null) {
            stringBuilder.append(SwingUI.escapeHTML(string2));
        }
        stringBuilder.append("</html>");
        return stringBuilder.toString();
    }

    protected String getQueryInputPrompt(String string, Collection<SearchResult> collection, AutoDetectionMode autoDetectionMode) throws Exception {
        StringBuilder stringBuilder = new StringBuilder(512);
        stringBuilder.append("<html>");
        stringBuilder.append("<p>Select best match for \"<b>").append(SwingUI.escapeHTML(string)).append("</b>\"</p>");
        if (autoDetectionMode == AutoDetectionMode.Auto && collection.size() >= 20) {
            stringBuilder.append("<p>Select <b>Skip</b> if \"<b>").append(SwingUI.escapeHTML(string)).append("</b>\" is <u>not</u> the series name").append("</p>");
        }
        stringBuilder.append("</html>");
        return stringBuilder.toString();
    }

    public List<Match<File, ?>> justFetchEpisodeList(SortOrder sortOrder, Locale locale, Set<AutoSelectionMode> set, Component component) throws Exception {
        List<String> list = SwingUI.showMultiValueInputDialog("Enter series name:", "", this.provider.getName(), this.provider.getIcon(), component);
        ArrayList arrayList = new ArrayList();
        for (Episode episode : this.fetchEpisodeSet(Collections.emptyList(), true, this.query(list), sortOrder, locale, false, AutoDetectionMode.Input, set, component)) {
            arrayList.add(Match.of(null, episode.clone()));
        }
        return arrayList;
    }

    protected Set<Series> query(Collection<String> collection) {
        if (collection == null || collection.isEmpty()) {
            return Collections.emptySet();
        }
        return collection.stream().distinct().map(Series::QUERY).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

