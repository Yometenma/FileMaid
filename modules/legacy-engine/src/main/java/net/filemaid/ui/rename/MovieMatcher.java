package net.filemaid.ui.rename;

import java.awt.Component;
import java.awt.Cursor;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import net.filemaid.InvalidInputException;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.Parallelism;
import net.filemaid.UserData;
import net.filemaid.UserInteraction;
import net.filemaid.WebServices;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.similarity.DerivateCollection;
import net.filemaid.similarity.Match;
import net.filemaid.similarity.NameSimilarityMetric;
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
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieLookupService;
import net.filemaid.web.MoviePart;
import net.filemaid.web.SortOrder;
import net.filemaid.web.ThumbnailProvider;

class MovieMatcher
implements AutoCompleteMatcher {
    private final MovieLookupService service;
    private final Map<String, Optional<Movie>> selectionMemory = new HashMap<String, Optional<Movie>>();
    private final Map<String, String> inputMemory = new HashMap<String, String>();

    public MovieMatcher(MovieLookupService movieLookupService) {
        this.service = movieLookupService;
    }

    @Override
    public List<Match<File, ?>> match(Collection<File> collection, MatchMode matchMode, SortOrder sortOrder, Locale locale, AutoDetectionMode autoDetectionMode, Set<AutoSelectionMode> set2, Component component) throws Exception {
        Collection<File> collection2;
        List<File> list;
        if (collection.isEmpty()) {
            return this.justFetchMovieInfo(locale, set2, component);
        }
        List<File> list2 = list = autoDetectionMode == AutoDetectionMode.Auto ? FileUtilities.filter(collection, FileUtilities.not(MediaFileUtilities.EXTRA_FILES)) : collection.stream().collect(Collectors.toList());
        if (list.isEmpty()) {
            Logging.log.info(Logging.format("%s movie files have been selected alongside %s companion %s.", list.size(), collection.size(), collection.size() == 1 ? "file" : "files"));
        }
        LinkedHashSet<File> linkedHashSet = new LinkedHashSet<File>(FileUtilities.filter(list, MediaTypes.VIDEO_FILES));
        LinkedHashSet<File> linkedHashSet2 = new LinkedHashSet<File>(FileUtilities.filter(list, MediaTypes.NFO_FILES));
        LinkedHashSet<File> linkedHashSet3 = new LinkedHashSet<File>(FileUtilities.filter(list, FileUtilities.FOLDERS));
        DerivateCollection derivateCollection = DerivateCollection.derive(list, linkedHashSet, linkedHashSet2, linkedHashSet3);
        List<File> list3 = FileUtilities.filter(derivateCollection.orphans(), MediaTypes.SUBTITLE_FILES);
        if (linkedHashSet.isEmpty() && list3.isEmpty() && linkedHashSet2.isEmpty() && linkedHashSet3.isEmpty()) {
            throw new InvalidInputException("No movie files have been selected. Please <Load> movie files.");
        }
        LinkedHashMap<File, Movie> linkedHashMap = new LinkedHashMap<>();
        if (autoDetectionMode != AutoDetectionMode.Select) {
            collection2 = new LinkedHashSet<File>(linkedHashSet2);
            for (File serializable2 : FileUtilities.mapByFolder(linkedHashSet).keySet()) {
                collection2.addAll(FileUtilities.getChildren(serializable2, MediaTypes.NFO_FILES));
            }
            for (File file2 : linkedHashSet3) {
                collection2.addAll(FileUtilities.getChildren(file2, MediaTypes.NFO_FILES));
            }
            Parallelism.commonPool().map(collection2, file -> this.lookupByNfo((File)file, (Set<File>)linkedHashSet2, (Set<File>)linkedHashSet, (Set<File>)linkedHashSet3, locale)).forEach(linkedHashMap::putAll);
        }
        collection2 = Stream.of(linkedHashSet, linkedHashSet2, linkedHashSet3, list3).flatMap(Collection::stream).filter(file -> linkedHashMap.get(file) == null).collect(Collectors.toList());
        Parallelism.commonPool().map(collection2, file -> {
            if (set2.contains((Object)AutoSelectionMode.Cancel)) {
                return null;
            }
            List<Movie> movies = autoDetectionMode != AutoDetectionMode.Select ? MediaDetection.detectMovieWithYear(file, this.service, locale, matchMode == MatchMode.Strict) : MediaDetection.detectMovie(file, this.service, locale, matchMode == MatchMode.Strict, true);
            Movie movie = this.grabMovie((File)file, (List<Movie>)(movies == null ? Collections.emptyList() : movies), locale, matchMode == MatchMode.Strict, autoDetectionMode, set2, component);
            if (movie == null) {
                return null;
            }
            Movie movie2 = MediaDetection.getLocalizedMovie(this.service, movie, locale);
            if (movie2 == null) {
                return movie;
            }
            return movie2;
        }, (file, movie) -> {
            if (movie != null) {
                linkedHashMap.put(file, movie);
            }
        });
        Map<Movie, TreeSet<File>> map = linkedHashMap.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue, LinkedHashMap::new, Collectors.mapping(Map.Entry::getKey, Collectors.toCollection(TreeSet::new))));
        ArrayList<Match<File, ?>> arrayList = new ArrayList<>();
        map.forEach((movie, set) -> MediaFileUtilities.groupByMediaCharacteristics(set).forEach(files -> {
            for (int i = 0; i < files.size(); ++i) {
                Movie movie2 = files.size() == 1 ? movie : new MoviePart(movie, i + 1, files.size());
                derivateCollection.matches(files.get(i), movie2::clone).forEach(arrayList::add);
            }
        }));
        arrayList.sort(Comparator.comparing((Match<File, ?> match) -> match.getValue(), OriginalOrder.of(collection)));
        return arrayList;
    }

    protected Map<File, Movie> lookupByNfo(File file, Set<File> set, Set<File> set2, Set<File> set3, Locale locale) throws Exception {
        Movie movie = MediaDetection.getLocalizedMovie(this.service, MediaDetection.grepMovie(file), locale);
        if (movie == null) {
            return Collections.emptyMap();
        }
        LinkedHashMap<File, Movie> linkedHashMap = new LinkedHashMap<File, Movie>();
        File file2 = file.getParentFile();
        if (set.contains(file)) {
            linkedHashMap.put(file, movie);
        }
        if (MediaFileUtilities.isDiskFolder(file2)) {
            for (File file3 : set3) {
                if (!file2.equals(file3)) continue;
                linkedHashMap.put(file3, movie);
            }
            return linkedHashMap;
        }
        String string = MediaDetection.stripReleaseInfo(FileUtilities.getName(file)).toLowerCase();
        for (File file4 : FileUtilities.filter(set2, FileUtilities.newParentFilter(file2))) {
            if (string.isEmpty() || !MediaDetection.stripReleaseInfo(FileUtilities.getName(file4)).toLowerCase().startsWith(string)) continue;
            linkedHashMap.put(file4, movie);
        }
        return linkedHashMap;
    }

    protected Movie grabMovie(File file, List<Movie> list, Locale locale, boolean bl, AutoDetectionMode autoDetectionMode, Set<AutoSelectionMode> set, Component component) throws Exception {
        if (autoDetectionMode == AutoDetectionMode.Input) {
            return this.queryMovie(file, list, locale, autoDetectionMode, set, component);
        }
        if (list.isEmpty() && set.isEmpty() && !bl) {
            return this.queryMovie(file, list, locale, autoDetectionMode, set, component);
        }
        Movie movie = this.selectMovie(file, null, list, bl, autoDetectionMode, set, component);
        if (movie == null && autoDetectionMode == AutoDetectionMode.Select && !set.contains((Object)AutoSelectionMode.Cancel)) {
            return this.queryMovie(file, list, locale, autoDetectionMode, set, component);
        }
        return movie;
    }

    private Movie queryMovie(File file, List<Movie> list, Locale locale, AutoDetectionMode autoDetectionMode, Set<AutoSelectionMode> set, Component component) throws Exception {
        String string = this.getQuerySuggestion(file, list);
        String string2 = MediaDetection.getUniqueQueryKey(string);
        Map<String, Optional<Movie>> map = this.selectionMemory;
        synchronized (map) {
            String string3;
            String string4 = string3 = string2 == null ? null : this.inputMemory.get(string2);
            if (string3 == null) {
                set.add(AutoSelectionMode.Input);
                string3 = SwingUI.showInputDialog(this.getQueryInputMessage("Please identify the following files:", "Enter movie name:", file), string, this.service.getName(), this.service.getIcon(), component);
                if (string3 != null) {
                    this.inputMemory.put(string2, string3);
                }
            }
            if (string3 != null && !string3.isEmpty()) {
                return this.selectMovie(file, string3, this.service.lookupMovie(string3, locale), false, autoDetectionMode, set, component);
            }
        }
        return null;
    }

    protected String getQuerySuggestion(File file, List<Movie> list) {
        String string;
        String string2 = string = list.isEmpty() ? null : list.get(0).getName();
        if (string == null && ((string = MediaDetection.stripReleaseInfo(FileUtilities.getName(file))) == null || string.isEmpty())) {
            string = FileUtilities.getName(file);
        }
        if (string != null && this.service.grepMovie(string) != null) {
            return "\"" + string + "\"";
        }
        return string;
    }

    protected String getQueryInputMessage(String string, String string2, File file) throws Exception {
        File file2;
        StringBuilder stringBuilder = new StringBuilder(512);
        stringBuilder.append("<html>");
        if (string != null) {
            stringBuilder.append(SwingUI.escapeHTML(string)).append("<br>");
        }
        File file3 = (file2 = MediaDetection.guessMovieFolder(file)) == null ? new File(file.getName()) : new File(file2.getName(), file.getName());
        TextColorizer textColorizer = new TextColorizer("<nobr>\u2022 ", "</nobr><br>");
        textColorizer.colorizePath(stringBuilder, file3, file.isFile());
        stringBuilder.append("<br>");
        if (string2 != null) {
            stringBuilder.append(SwingUI.escapeHTML(string2));
        }
        stringBuilder.append("</html>");
        return stringBuilder.toString();
    }

    protected Movie selectMovie(File file, String string, List<Movie> list, boolean bl, AutoDetectionMode autoDetectionMode, Set<AutoSelectionMode> set, Component component) throws Exception {
        Callable<Movie> arrayDeque;
        Optional<Movie> optional;
        String string2;
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() == 1 && autoDetectionMode != AutoDetectionMode.Select) {
            return list.get(0);
        }
        String string3 = string != null ? string : MediaDetection.checkMovieStripReleaseInfo(file, bl);
        File file2 = MediaDetection.guessMovieFolder(file);
        String string4 = string2 = string != null || file2 == null ? "" : MediaDetection.checkMovieStripReleaseInfo(file2, bl);
        if (autoDetectionMode == AutoDetectionMode.Auto) {
            if (string == null && string3.length() < 2 && string2.length() < 2) {
                return null;
            }
            for (Movie object22 : list) {
                String map = Normalization.normalizePunctuation(object22.toString()).toLowerCase();
                if (!string3.toLowerCase().startsWith(map) && !string2.toLowerCase().startsWith(map)) continue;
                return object22;
            }
            ArrayDeque<Movie> candidates = new ArrayDeque<>();
            NameSimilarityMetric nameSimilarityMetric = new NameSimilarityMetric();
            float f = 0.9f;
            Iterator<Movie> iterator = list.iterator();
            while (iterator.hasNext()) {
                Movie movie = iterator.next();
                float f2 = 0.0f;
                for (String string5 : new String[]{string3, string2}) {
                    for (String string6 : bl ? movie.getEffectiveNames() : movie.getEffectiveNamesWithoutYear()) {
                        if (f2 >= f) continue;
                        f2 = Math.max(f2, nameSimilarityMetric.getSimilarity(string5, string6));
                    }
                }
                if (!(f2 >= f)) continue;
                candidates.add(movie);
            }
            if (candidates.size() == 1) {
                return candidates.getFirst();
            }
            if (bl) {
                return null;
            }
        }
        arrayDeque = () -> {
            SelectDialog<Movie> selectDialog = new SelectDialog<Movie>(component, list, this.preview(), this.thumbnail(component), true, false, this.createDialogHeader(file));
            selectDialog.setTitle(this.service.getName());
            selectDialog.getMessageLabel().setText(SwingUI.formatHTML("<html>Select best match for \"<b>%s</b>\"</html>", string3.length() >= 2 || string2.length() <= 2 ? string3 : string2));
            selectDialog.getCancelAction().putValue("Name", "Skip");
            selectDialog.getCancelAction().putValue("ShortDescription", "Select none of the above");
            selectDialog.pack();
            UserData userData = UserData.forPackage(MovieMatcher.class).node("dialog.select.movie");
            userData.restoreWindowBounds(selectDialog, window -> SwingUI.getOffsetLocation(window));
            userData.restoreToggleButton(selectDialog.repeatToggle);
            selectDialog.setVisible(true);
            Movie movie = selectDialog.getSelectedValue();
            if (movie == null && selectDialog.getSelectedAction() == SelectDialog.CLOSE) {
                set.addAll(AutoSelectionMode.cancel());
            }
            if (selectDialog.repeatToggle.isSelected()) {
                set.add(movie != null ? AutoSelectionMode.Auto : AutoSelectionMode.Skip);
            }
            return movie;
        };
        String string7 = MediaDetection.getUniqueQueryKey(string2 + " " + string3);
        Map<String, Optional<Movie>> map = this.selectionMemory;
        synchronized (map) {
            if (string7 != null && (optional = this.selectionMemory.get(string7)) != null) {
                return optional.orElse(null);
            }
            if (autoDetectionMode != AutoDetectionMode.Select) {
                if (set.contains((Object)AutoSelectionMode.Auto)) {
                    return list.get(0);
                }
                if (set.contains((Object)AutoSelectionMode.Skip)) {
                    return null;
                }
            }
            WebServices.requestPool().async(() -> MediaDetection.getLocalizedMovie(this.service, (Movie)list.get(0), ((Movie)list.get(0)).getLanguage()));
            Movie movie = SwingUI.showInputDialog(arrayDeque);
            if (string7 != null) {
                this.selectionMemory.put(string7, Optional.ofNullable(movie));
            }
            return movie;
        }
    }

    protected JComponent createDialogHeader(File file) throws Exception {
        JLabel jLabel = new JLabel(this.getQueryInputMessage("Unable to uniquely identify some of the following files:", null, file));
        jLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(""), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
        jLabel.addMouseListener(SwingUI.mouseClicked(mouseEvent -> UserInteraction.reveal(file)));
        jLabel.setCursor(Cursor.getPredefinedCursor(12));
        return jLabel;
    }

    protected Function<Movie, Icon> preview() {
        return movie -> BlankThumbnail.BLANK_POSTER;
    }

    protected Function<Movie, CompletableFuture<Icon>> thumbnail(Component component) {
        if (this.service instanceof ThumbnailProvider) {
            ThumbnailProvider thumbnailProvider = (ThumbnailProvider)((Object)this.service);
            ThumbnailProvider.ResolutionVariant resolutionVariant = ThumbnailProvider.ResolutionVariant.fromScaleFactor(component);
            return movie -> thumbnailProvider.requestThumbnail(movie.getId(), resolutionVariant);
        }
        return null;
    }

    public List<Match<File, ?>> justFetchMovieInfo(Locale locale, Set<AutoSelectionMode> set, Component component) throws Exception {
        ArrayList arrayList = new ArrayList();
        String string = SwingUI.showInputDialog("Enter movie name:", "", this.service.getName(), this.service.getIcon(), component);
        if (string != null) {
            Parallelism.commonPool().map(this.service.lookupMovie(string, locale), movie -> {
                if (set.contains((Object)AutoSelectionMode.Cancel)) {
                    return null;
                }
                return MediaDetection.getLocalizedMovie(this.service, movie, locale);
            }, (movie, movie2) -> arrayList.add(Match.of(null, movie2 != null ? movie2 : movie)));
        }
        return arrayList;
    }
}

