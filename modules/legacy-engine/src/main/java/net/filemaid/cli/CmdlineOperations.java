package net.filemaid.cli;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.filemaid.Cache;
import net.filemaid.History;
import net.filemaid.HistorySpooler;
import net.filemaid.Language;
import net.filemaid.LicenseError;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.RenameAction;
import net.filemaid.Settings;
import net.filemaid.StandardRenameAction;
import net.filemaid.UserFiles;
import net.filemaid.WebServices;
import net.filemaid.archive.Archive;
import net.filemaid.cli.CmdlineException;
import net.filemaid.cli.CmdlineInterface;
import net.filemaid.cli.ConfigurationException;
import net.filemaid.cli.ConflictAction;
import net.filemaid.cli.ExecCommand;
import net.filemaid.format.ExpressionFileFormat;
import net.filemaid.format.ExpressionFilter;
import net.filemaid.format.ExpressionFormat;
import net.filemaid.format.ExpressionMapper;
import net.filemaid.format.MediaBindingBean;
import net.filemaid.format.QueryExpression;
import net.filemaid.hash.HashType;
import net.filemaid.hash.VerificationFileReader;
import net.filemaid.hash.VerificationFileWriter;
import net.filemaid.hash.VerificationUtilities;
import net.filemaid.media.AutoDetection;
import net.filemaid.media.LocalDatasource;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.media.MediaInfoTable;
import net.filemaid.media.XattrMetaInfo;
import net.filemaid.postprocess.Apply;
import net.filemaid.postprocess.FeedbackLogger;
import net.filemaid.similarity.CommonSequenceMatcher;
import net.filemaid.similarity.DerivateCollection;
import net.filemaid.similarity.EpisodeMatcher;
import net.filemaid.similarity.EpisodeMetrics;
import net.filemaid.similarity.Match;
import net.filemaid.subtitle.SubtitleFormat;
import net.filemaid.subtitle.SubtitleNaming;
import net.filemaid.subtitle.SubtitleUtilities;
import net.filemaid.util.ByteBufferOutputStream;
import net.filemaid.util.EntryList;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.FunctionList;
import net.filemaid.util.ZipUtilities;
import net.filemaid.vfs.FileInfo;
import net.filemaid.vfs.MemoryFile;
import net.filemaid.vfs.SimpleFileInfo;
import net.filemaid.web.AudioTrack;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeFormat;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.LookupException;
import net.filemaid.web.MappedEpisode;
import net.filemaid.web.Movie;
import net.filemaid.web.MovieLookupService;
import net.filemaid.web.MoviePart;
import net.filemaid.web.MusicLookupService;
import net.filemaid.web.SearchResult;
import net.filemaid.web.Series;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SortOrder;
import net.filemaid.web.SubtitleDescriptor;
import net.filemaid.web.SubtitleLookupService;
import net.filemaid.web.SubtitleProvider;

public class CmdlineOperations
implements CmdlineInterface {
    @Override
    public List<File> rename(Collection<File> collection, Datasource datasource, QueryExpression queryExpression, SortOrder sortOrder, Locale locale, ExpressionFilter expressionFilter, ExpressionMapper expressionMapper, boolean bl, ExpressionFileFormat expressionFileFormat, File file, RenameAction renameAction, ConflictAction conflictAction, Apply[] applyArray, ExecCommand execCommand) throws Exception {
        if (collection.isEmpty()) {
            throw new CmdlineException("No input files");
        }
        if (datasource instanceof LocalDatasource) {
            return this.renameFiles(collection, renameAction, conflictAction, file, expressionFileFormat, (LocalDatasource)datasource, expressionFilter, bl, applyArray, execCommand);
        }
        if (datasource instanceof MusicLookupService) {
            return this.renameMusic(collection, renameAction, conflictAction, file, expressionFileFormat, new MusicLookupService[]{(MusicLookupService)datasource}, applyArray, execCommand);
        }
        List<File> list = FileUtilities.filter(collection, FileUtilities.not(MediaFileUtilities.EXTRA_FILES));
        if (list.isEmpty()) {
            throw new CmdlineException("No input files", collection);
        }
        if (datasource instanceof MovieLookupService) {
            return this.renameMovie(list, renameAction, conflictAction, file, expressionFileFormat, (MovieLookupService)datasource, queryExpression, expressionFilter, locale, bl, applyArray, execCommand);
        }
        if (datasource instanceof EpisodeListProvider) {
            return this.renameSeries(list, renameAction, conflictAction, file, expressionFileFormat, (EpisodeListProvider)datasource, queryExpression, sortOrder, expressionFilter, expressionMapper, locale, bl, applyArray, execCommand);
        }
        if (datasource == null && queryExpression != null) {
            return this.renameSeries(list, renameAction, conflictAction, file, expressionFileFormat, WebServices.getDefaultSeriesDB(), queryExpression, sortOrder, expressionFilter, expressionMapper, locale, bl, applyArray, execCommand);
        }
        Logging.log.config("Classify media files");
        List<File> list3 = FileUtilities.filter(list, MediaTypes.VIDEO_FILES, MediaTypes.SUBTITLE_FILES, MediaTypes.AUDIO_FILES, MediaTypes.NFO_FILES, FileUtilities.FOLDERS);
        if (list3.isEmpty()) {
            throw new CmdlineException("No media files", list);
        }
        Map<AutoDetection.Group, List<File>> map = new AutoDetection(list3, locale).group();
        ArrayList<File> arrayList = new ArrayList<File>();
        if (map.size() == 1) {
            Logging.help("* Consider specifying --db TheMovieDB::TV to force Episode Mode");
            Logging.help("* Consider specifying --db TheMovieDB to force Movie Mode");
        }
        map.forEach((group, list2) -> {
            try {
                AutoDetection.Type[] typeArray = group.types();
                if (typeArray.length == 0) {
                    throw new IllegalStateException("Failed to classify media files: no episode match, no movie match");
                }
                if (typeArray.length != 1) {
                    throw new IllegalStateException("Failed to unambiguously classify media files: " + Arrays.stream(typeArray).map(Objects::toString).collect(Collectors.joining(" | ")));
                }
                switch (typeArray[0]) {
                    case Movie: {
                        arrayList.addAll(this.renameMovie((Collection<File>)list2, renameAction, conflictAction, file, expressionFileFormat, WebServices.getDefaultMovieDB(), queryExpression, expressionFilter, locale, bl, applyArray, execCommand));
                        break;
                    }
                    case Series: {
                        arrayList.addAll(this.renameSeries((Collection<File>)list2, renameAction, conflictAction, file, expressionFileFormat, WebServices.getDefaultSeriesDB(), queryExpression, sortOrder, expressionFilter, expressionMapper, locale, bl, applyArray, execCommand));
                        break;
                    }
                    case Anime: {
                        arrayList.addAll(this.renameSeries((Collection<File>)list2, renameAction, conflictAction, file, expressionFileFormat, WebServices.getDefaultAnimeDB(), queryExpression, SortOrder.Absolute, expressionFilter, expressionMapper, locale, bl, applyArray, execCommand));
                        break;
                    }
                    case Music: {
                        arrayList.addAll(this.renameMusic((Collection<File>)list2, renameAction, conflictAction, file, expressionFileFormat, WebServices.getDefaultMusicDB(), applyArray, execCommand));
                    }
                }
            }
            catch (Exception exception) {
                Logging.debug.severe(Logging.format("Failed to process group: %s %s", group, list2));
                Logging.debug.severe(Logging.cause(exception));
            }
        });
        if (arrayList.isEmpty() || renameAction == StandardRenameAction.TEST) {
            return Collections.emptyList();
        }
        return arrayList;
    }

    @Override
    public List<File> renameLinear(List<File> list, Datasource datasource, QueryExpression queryExpression, SortOrder sortOrder, Locale locale, ExpressionFilter expressionFilter, ExpressionMapper expressionMapper, ExpressionFileFormat expressionFileFormat, File file, RenameAction renameAction, ConflictAction conflictAction, Apply[] applyArray, ExecCommand execCommand) throws Exception {
        if (queryExpression == null) {
            throw new CmdlineException("Please specify the --q query option");
        }
        ArrayList arrayList = new ArrayList();
        for (Map.Entry<String, List<File>> entry : queryExpression.group(list).entrySet()) {
            List<?> list2 = this.fetchList(datasource, entry.getKey(), sortOrder, locale, expressionFilter, expressionMapper, true);
            for (int i = 0; i < list2.size() && i < entry.getValue().size(); ++i) {
                arrayList.add(Match.of(entry.getValue().get(i), list2.get(i)));
            }
        }
        return this.renameAll(this.getRenameMap(arrayList, expressionFileFormat, file), renameAction, conflictAction, arrayList, applyArray, execCommand);
    }

    @Override
    public List<File> rename(Map<File, File> map, RenameAction renameAction, ConflictAction conflictAction) throws Exception {
        return this.renameAll(map, renameAction, conflictAction, null, null, null);
    }

    public List<File> renameSeries(Collection<File> collection, RenameAction renameAction, ConflictAction conflictAction, File file2, ExpressionFileFormat expressionFileFormat, EpisodeListProvider episodeListProvider, QueryExpression queryExpression, SortOrder sortOrder, ExpressionFilter expressionFilter, ExpressionMapper expressionMapper, Locale locale, boolean bl, Apply[] applyArray, ExecCommand execCommand) throws Exception {
        Series series;
        Logging.log.config(Logging.format("Rename episodes using [%s] with [%s Order]", new Object[]{episodeListProvider.getName(), episodeListProvider.vetoRequestParameter(sortOrder)}));
        LinkedHashSet<File> linkedHashSet = new LinkedHashSet<File>(FileUtilities.filter(collection, MediaTypes.VIDEO_FILES));
        LinkedHashSet<File> linkedHashSet2 = new LinkedHashSet<File>(FileUtilities.filter(collection, FileUtilities.FOLDERS));
        DerivateCollection derivateCollection = DerivateCollection.derive(collection, linkedHashSet, linkedHashSet2);
        ArrayList<File> arrayList = new ArrayList<File>();
        arrayList.addAll(linkedHashSet);
        arrayList.addAll(linkedHashSet2);
        arrayList.addAll(FileUtilities.filter(derivateCollection.orphans(), MediaTypes.SUBTITLE_FILES));
        if (arrayList.isEmpty()) {
            throw new CmdlineException("No media files", collection);
        }
        List<Match<File, ?>> arrayList2 = new ArrayList<>();
        ArrayList<Match<Series, Collection>> arrayList3 = new ArrayList<Match<Series, Collection>>();
        Map<String, List<File>> map = queryExpression == null ? Collections.<String, List<File>>singletonMap((String) null, arrayList) : queryExpression.group(arrayList);
        for (Map.Entry<String, List<File>> object : map.entrySet()) {
            series = Series.QUERY(object.getKey());
            for (Map.Entry<Set<File>, Set<Series>> entry : MediaDetection.mapSeriesNamesByFiles(object.getValue(), locale, episodeListProvider == WebServices.AniDB).entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    arrayList3.add(Match.of(series, entry.getKey()));
                    continue;
                }
                Series seriesRef = series;
                FileUtilities.mapByFolder(entry.getKey()).forEach((file, list2) -> arrayList3.add(Match.of(seriesRef, list2)));
            }
        }
        for (Match match : arrayList3) {
            List<Episode> list;
            List<Episode> var26_32;
            SeriesInfo[] seriesInfoArray;
            EpisodeMetrics episodeMetrics = new EpisodeMetrics();
            series = (Series)match.getValue();
            Collection<File> collection2 = (Collection<File>)match.getCandidate();
            if (series == null) {
                this.printXattrMetadata(collection2);
                Set<Series> seriesSet = MediaDetection.detectSeries(collection2, episodeListProvider == WebServices.AniDB, locale);
                if (seriesSet.isEmpty()) {
                    Logging.log.warning(Logging.message("Failed to detect query for files", collection2));
                    continue;
                }
                var26_32 = this.fetchEpisodeSet(episodeListProvider, false, seriesSet, sortOrder, locale, bl, 5);
            } else {
                var26_32 = this.fetchEpisodeSet(episodeListProvider, true, Collections.singleton(series), sortOrder, locale, false, 1);
            }
            if (var26_32.isEmpty() || (list = this.applyExpressionFilter(var26_32, expressionFilter)).isEmpty()) continue;
            if (bl && (seriesInfoArray = (SeriesInfo[])list.stream().map(Episode::getSeriesInfo).distinct().toArray(SeriesInfo[]::new)).length > 1) {
                collection2.forEach(file -> Logging.log.warning(Logging.message("Multiple options for ambiguous file path", file, Arrays.stream(seriesInfoArray).map(SeriesInfo::getName).collect(Collectors.toList()))));
                Logging.help("* Consider using -non-strict to enable advanced auto-selection");
                Logging.help(Logging.format("* Consider using --filter \"id in [%s]\" or --q \"%s\" to select one specific series", seriesInfoArray[0].getId(), seriesInfoArray[0].getName()));
                if (System.console() == null) continue;
                Logging.help("* Consider using --mode interactive to enable interactive mode");
                continue;
            }
            List<Episode> list5 = this.applyEpisodeExpressionMapper(list, expressionMapper, MappedEpisode::map);
            for (List<File> list6 : MediaFileUtilities.mapByMediaExtension(collection2).values()) {
                for (Match<File, Episode> match2 : this.matchEpisodes(list6, list5, bl, episodeMetrics)) {
                    File file3 = match2.getValue();
                    Episode episode = match2.getCandidate();
                    if (episode instanceof MappedEpisode) {
                        Episode episode2 = ((MappedEpisode)episode).getMapping();
                        Episode episode3 = ((MappedEpisode)episode).getOriginal();
                        Logging.log.fine(Logging.format("Reverse Map [%s] to [%s]", episode2, episode3));
                        episode = episode3;
                    }
                    derivateCollection.matches(file3, episode::clone).forEach(arrayList2::add);
                }
            }
        }
        return this.renameAll(this.getRenameMap(arrayList2, expressionFileFormat, file2), renameAction, conflictAction, arrayList2, applyArray, execCommand);
    }

    private List<Match<File, Episode>> matchEpisodes(Collection<File> collection, Collection<Episode> collection2, boolean bl, EpisodeMetrics episodeMetrics) throws Exception {
        EpisodeMatcher episodeMatcher = new EpisodeMatcher(collection, collection2, bl, episodeMetrics.matchSequence());
        List<Match<File, Episode>> list = episodeMatcher.match();
        episodeMatcher.remainingValues().forEach(file -> Logging.log.warning(Logging.message("No matching episode", file)));
        if (!bl) {
            return list;
        }
        ArrayList<Match<File, Episode>> arrayList = new ArrayList<Match<File, Episode>>(list.size());
        list.forEach(match -> {
            Episode episode;
            File file = (File)match.getValue();
            if (MediaDetection.isEpisodeNumberMatch(file, episode = (Episode)match.getCandidate())) {
                arrayList.add((Match<File, Episode>)match);
            } else {
                Logging.log.finest(Logging.format("Episode numbers do not strictly match: [%s] <=> [%s]", episode, file));
            }
        });
        if (arrayList.isEmpty()) {
            Logging.help("* Consider using -non-strict to enable advanced auto-selection");
        }
        return arrayList;
    }

    private List<Episode> fetchEpisodeSet(EpisodeListProvider episodeListProvider, boolean bl, Collection<Series> collection, SortOrder sortOrder, Locale locale, boolean bl2, int n) throws Exception {
        LinkedHashSet<Episode> linkedHashSet = new LinkedHashSet<Episode>();
        for (SearchResult searchResult : this.lookupSeries(episodeListProvider, bl, collection, locale, n)) {
            try {
                Logging.log.finest(Logging.format("Fetching episode data for [%s]", searchResult));
                List<Episode> list = episodeListProvider.getEpisodeList(searchResult, sortOrder, locale);
                linkedHashSet.addAll(list);
                Logging.help(() -> linkedHashSet.stream().filter(episode -> episode.getEpisode() != null).collect(Collectors.groupingBy(Episode::getSeriesInfo)).entrySet().stream().map(entry -> "* " + ((SeriesInfo)entry.getKey()).getName() + " [" + ((SeriesInfo)entry.getKey()).getId() + "] | " + ((List)entry.getValue()).size() + " episodes | " + EpisodeFormat.DEFAULT.formatMultiRangeNumbers((Iterable)entry.getValue(), "%01dx", "%02d", "x", "-", " .. ")).collect(Collectors.joining("\n")));
            }
            catch (LookupException lookupException) {
                Logging.help(() -> "* " + lookupException.getMessage());
            }
            catch (IOException iOException) {
                throw new CmdlineException("Failed to fetch episode data", Logging.format("%s%n\u2514 %s", searchResult, Logging.cause(iOException)), iOException);
            }
        }
        if (linkedHashSet.isEmpty()) {
            Logging.log.warning(Logging.message("No episode data", collection));
        }
        return linkedHashSet.stream().collect(Collectors.toList());
    }

    protected Collection<SearchResult> lookupSeries(EpisodeListProvider episodeListProvider, boolean bl, Collection<Series> collection, Locale locale, int n) throws Exception {
        LinkedHashSet<SearchResult> linkedHashSet = new LinkedHashSet<SearchResult>();
        LinkedHashSet<String> linkedHashSet2 = new LinkedHashSet<String>();
        for (Series series : collection) {
            SearchResult id = episodeListProvider.id(series);
            if (id != null) {
                linkedHashSet.add(id);
                continue;
            }
            String name = series.getName();
            if (name.isEmpty()) continue;
            linkedHashSet2.add(name);
        }
        Logging.log.config(Logging.format("Lookup via %s %s", linkedHashSet, linkedHashSet2));
        for (String string : linkedHashSet2) {
            List<SearchResult> list = bl ? episodeListProvider.lookup(string, locale) : episodeListProvider.search(string, locale);
            if (linkedHashSet.stream().map(SearchResult::getName).anyMatch(string::equalsIgnoreCase) || list.isEmpty()) continue;
            linkedHashSet.addAll(this.selectSearchResult(string, list, !bl, true, n));
        }
        return linkedHashSet;
    }

    public List<File> renameMovie(Collection<File> collection, RenameAction renameAction, ConflictAction conflictAction, File file, ExpressionFileFormat expressionFileFormat, MovieLookupService movieLookupService, QueryExpression queryExpression, ExpressionFilter expressionFilter, Locale locale, boolean bl, Apply[] applyArray, ExecCommand execCommand) throws Exception {
        Logging.log.config(Logging.format("Rename movies using [%s]", movieLookupService.getName()));
        List<File> list = FileUtilities.filter(collection, MediaTypes.VIDEO_FILES);
        List<File> list2 = FileUtilities.filter(collection, MediaTypes.NFO_FILES);
        List<File> list3 = FileUtilities.filter(collection, FileUtilities.FOLDERS);
        DerivateCollection derivateCollection = DerivateCollection.derive(collection, list, list2, list3);
        ArrayList<File> arrayList = new ArrayList<File>();
        arrayList.addAll(list);
        arrayList.addAll(list2);
        arrayList.addAll(list3);
        arrayList.addAll(FileUtilities.filter(derivateCollection.orphans(), MediaTypes.SUBTITLE_FILES));
        if (arrayList.isEmpty()) {
            throw new CmdlineException("No media files", collection);
        }
        LinkedHashMap<File, Movie> linkedHashMap = new LinkedHashMap<File, Movie>();
        if (queryExpression == null) {
            LinkedHashSet<File> nfoFiles = new LinkedHashSet<File>(list2);
            for (File file2 : FileUtilities.mapByFolder(list).keySet()) {
                nfoFiles.addAll(FileUtilities.getChildren(file2, MediaTypes.NFO_FILES));
            }
            for (File file3 : list3) {
                nfoFiles.addAll(FileUtilities.getChildren(file3, MediaTypes.NFO_FILES));
            }
            for (File file4 : nfoFiles) {
                Movie movie = MediaDetection.getLocalizedMovie(movieLookupService, MediaDetection.grepMovie(file4), locale);
                if (movie == null) continue;
                if (list2.contains(file4)) {
                    linkedHashMap.put(file4, movie);
                }
                if (MediaFileUtilities.isDiskFolder(file4.getParentFile())) {
                    for (File file5 : list3) {
                        if (!file4.getParentFile().equals(file5)) continue;
                        linkedHashMap.put(file5, movie);
                    }
                    continue;
                }
                String name = MediaDetection.stripReleaseInfo(FileUtilities.getName(file4)).toLowerCase();
                if (name.length() <= 0) continue;
                for (File file6 : FileUtilities.filter(list, FileUtilities.newParentFilter(file4.getParentFile()))) {
                    if (!MediaDetection.stripReleaseInfo(FileUtilities.getName(file6)).toLowerCase().startsWith(name)) continue;
                    linkedHashMap.put(file6, movie);
                }
            }
        } else {
            for (Map.Entry<String, List<File>> entry : queryExpression.group(arrayList).entrySet()) {
                Logging.log.fine(Logging.format("Looking up movie by query [%s]", entry.getKey()));
                List<Movie> list4 = movieLookupService.lookupMovie(entry.getKey(), locale);
                List<Movie> filtered = this.applyExpressionFilter(list4, expressionFilter);
                if (filtered.isEmpty()) {
                    throw new CmdlineException("Failed to find a matching movie", list4);
                }
                Movie movie = this.selectMovie(entry.getKey(), filtered);
                if (movie == null) continue;
                movie = MediaDetection.getLocalizedMovie(movieLookupService, movie, locale);
                for (File file6 : entry.getValue()) {
                    linkedHashMap.put(file6, movie);
                }
            }
        }
        HashMap<Movie, Set<File>> hashMap = new HashMap<Movie, Set<File>>();
        for (File file7 : arrayList) {
            Movie movie = linkedHashMap.get(file7);
            if (movie == null) {
                Logging.log.fine(Logging.format("Auto-detect movie from context [%s]", file7));
                this.printXattrMetadata(Collections.singleton(file7));
                List<Movie> candidates = MediaDetection.detectMovieWithYear(file7, movieLookupService, locale, bl);
                if (bl && candidates == null) {
                    Logging.log.warning(Logging.message("Name (Year) movie pattern not found", file7));
                    Logging.help("* Consider using -non-strict to enable opportunistic matching");
                    continue;
                }
                if ((candidates = this.applyExpressionFilter(candidates, expressionFilter)).isEmpty()) {
                    Logging.log.warning(Logging.message("Movie not found", file7));
                    continue;
                }
                List<Movie> list5 = MediaDetection.matchMovieByFileFolderName(file7, candidates);
                if (bl && list5.isEmpty()) {
                    Logging.log.warning(Logging.message("Name (Year) movie pattern does not strictly match", file7, candidates));
                    Logging.help("* Consider using -non-strict to enable opportunistic matching");
                    continue;
                }
                if (list5.size() > 0) {
                    candidates = list5;
                }
                movie = this.selectMovie(MediaDetection.checkMovieStripReleaseInfo(file7, bl), candidates);
                if (movie == null) {
                    Logging.help("* Consider using --q \"Name (Year)\" or --q \"id\" to lookup a specific movie");
                    continue;
                }
                movie = MediaDetection.getLocalizedMovie(movieLookupService, movie, locale);
            }
            if (movie == null) continue;
            hashMap.computeIfAbsent(movie, m -> new LinkedHashSet<File>()).add(file7);
        }
        List<Match<File, ?>> arrayList2 = new ArrayList<>();
        hashMap.forEach((movie, set) -> MediaFileUtilities.groupByMediaCharacteristics(set).forEach(group -> {
            for (int i = 0; i < group.size(); ++i) {
                Movie movie2 = group.size() == 1 ? movie : new MoviePart(movie, i + 1, group.size());
                derivateCollection.matches(group.get(i), movie2::clone).forEach(arrayList2::add);
            }
        }));
        return this.renameAll(this.getRenameMap(arrayList2, expressionFileFormat, file), renameAction, conflictAction, arrayList2, applyArray, execCommand);
    }

    protected Movie selectMovie(String string, Collection<Movie> collection) throws Exception {
        if ((string = MediaDetection.getUniqueQueryKey(string)) != null) {
            Iterator<Movie> iterator = collection.iterator();
            while (iterator.hasNext()) {
                Movie movie = iterator.next();
                String string2 = MediaDetection.getUniqueQueryKey(movie.getNameWithYear());
                if (!string.startsWith(string2)) continue;
                return movie;
            }
        }
        List<Movie> list = this.selectSearchResult(string, collection, false, false, 1);
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    public List<File> renameMusic(Collection<File> collection, RenameAction renameAction, ConflictAction conflictAction, File file, ExpressionFileFormat expressionFileFormat, MusicLookupService[] musicLookupServiceArray, Apply[] applyArray, ExecCommand execCommand) throws Exception {
        List<File> list = FileUtilities.filter(collection, MediaTypes.AUDIO_FILES, MediaTypes.VIDEO_FILES);
        if (list.isEmpty()) {
            throw new CmdlineException("No audio files", collection);
        }
        DerivateCollection derivateCollection = DerivateCollection.derive(collection, list);
        ArrayList arrayList = new ArrayList();
        for (MusicLookupService musicLookupService : musicLookupServiceArray) {
            Logging.log.config(Logging.format("Rename music using [%s]", musicLookupService.getName()));
            ArrayList<File> arrayList2 = new ArrayList<File>();
            for (File file2 : list) {
                List<AudioTrack> list2 = musicLookupService.lookup(file2);
                if (list2.size() > 0) {
                    derivateCollection.matches(file2, list2.get(0)::clone).forEach(arrayList::add);
                    continue;
                }
                Logging.log.warning(Logging.message(musicLookupService.getName(), "Failed to identify music file", file2));
                arrayList2.add(file2);
            }
            if (arrayList2.isEmpty()) break;
            list = arrayList2;
        }
        return this.renameAll(this.getRenameMap(arrayList, expressionFileFormat, file), renameAction, conflictAction, arrayList, applyArray, execCommand);
    }

    public List<File> renameFiles(Collection<File> collection, RenameAction renameAction, ConflictAction conflictAction, File file2, ExpressionFileFormat expressionFileFormat, LocalDatasource localDatasource, ExpressionFilter expressionFilter, boolean bl, Apply[] applyArray, ExecCommand execCommand) throws Exception {
        Logging.log.config(Logging.format("Rename files using [%s]", localDatasource.getName()));
        ArrayList arrayList = new ArrayList();
        Map<File, Object> map = localDatasource.match(collection);
        if (expressionFilter != null) {
            map.forEach((file, object) -> {
                try {
                    if (expressionFilter.matches(new MediaBindingBean(object, (File)file, map))) {
                        Logging.log.finest(Logging.format("Include [%s]", file));
                        arrayList.add(Match.of(file, object));
                    }
                }
                catch (Exception exception) {
                    Logging.debug.finest(Logging.cause("Filter", file, exception));
                }
            });
        } else {
            map.forEach((file, object) -> arrayList.add(Match.of(file, object)));
        }
        return this.renameAll(this.getRenameMap(arrayList, expressionFileFormat, file2), renameAction, conflictAction, arrayList, applyArray, execCommand);
    }

    private Map<File, Object> getContext(List<Match<File, ?>> list) {
        List<File> list2 = FunctionList.of(list, Match::getValue);
        List<Object> list3 = FunctionList.of(list, Match::getCandidate);
        return EntryList.of(list2, list3);
    }

    private File getDestinationFile(File file, String string, File file2) {
        String string2 = FileUtilities.getExtension(file);
        File file3 = new File((String)(string2 != null && !FileUtilities.hasExtension(string, string2) ? string + "." + string2.toLowerCase(Locale.ROOT) : string));
        if (file2 != null && !file3.isAbsolute()) {
            file3 = new File(file2, file3.getPath());
        }
        if (string2 != null && FileUtilities.getExtension(file3.getName()) == null) {
            Logging.log.warning(Logging.message("Ignore invalid target file name generated by erroneous format expression", file3));
            file3 = new File(file3.getParentFile(), file.getName());
        }
        if (file2 == null && !file3.isAbsolute() && file3.getParentFile() != null) {
            Logging.help("* Consider using --output to specify an absolute output folder");
            Logging.help("* Consider using {drive} or {folder} in your format to generate absolute output file paths");
        }
        if (Settings.isUnixFS() || !FileUtilities.isInvalidFilePath(file3)) {
            return file3;
        }
        Logging.log.config(Logging.message("Stripping invalid characters from the output file path", file3));
        return FileUtilities.validateFilePath(file3);
    }

    private Map<File, File> getRenameMap(List<Match<File, ?>> list, ExpressionFileFormat expressionFileFormat, File file) throws Exception {
        if (expressionFileFormat != null && expressionFileFormat.isConstant()) {
            throw new CmdlineException("Invalid format expression: " + expressionFileFormat.getExpression());
        }
        LinkedHashMap<File, File> linkedHashMap = new LinkedHashMap<File, File>();
        Map<File, Object> map = this.getContext(list);
        for (Match<File, ?> match : list) {
            File file2 = match.getValue();
            Object obj = match.getCandidate();
            String string = this.formatMatch(expressionFileFormat, obj, file2, map);
            linkedHashMap.put(file2, this.getDestinationFile(file2, string, file));
        }
        return linkedHashMap;
    }

    private String formatMatch(ExpressionFileFormat expressionFileFormat, Object object, File file, Map<File, Object> map) {
        if (expressionFileFormat != null) {
            return expressionFileFormat.format(new MediaBindingBean(object, file, map));
        }
        if (object instanceof File) {
            File file2 = (File)object;
            return FileUtilities.getName(file2);
        }
        return FileUtilities.validateFileName(object.toString());
    }

    protected List<File> renameAll(Map<File, File> map, RenameAction renameAction, ConflictAction conflictAction, List<Match<File, ?>> list, Apply[] applyArray, ExecCommand execCommand) throws Exception {
        if (map.isEmpty()) {
            throw new CmdlineException("Failed to identify or process any files");
        }
        if (renameAction != StandardRenameAction.TEST || System.console() == null) {
            try {
                Settings.LICENSE.check();
            }
            catch (LicenseError licenseError) {
                Logging.log.severe(Logging.format("Whoopsies! --action %s requires a valid license.", renameAction));
                if (renameAction != StandardRenameAction.TEST) {
                    Logging.help("* Consider using --action TEST");
                } else {
                    Logging.help("* Consider using --mode interactive");
                }
                throw licenseError;
            }
        }
        Cache.DISK_STORE.flush();
        LinkedHashMap<File, File> linkedHashMap = new LinkedHashMap<File, File>();
        try {
            for (Map.Entry<File, File> entry : map.entrySet()) {
                File file = entry.getKey();
                File file2 = entry.getValue();
                try {
                    file2 = renameAction.resolve(file, file2);
                }
                catch (Exception exception) {
                    Logging.help(Logging.cause(exception));
                    Logging.log.severe(Logging.format("[%s] from [%s] to [%s] failed due to I/O error: %s", renameAction, file, file2, Logging.cause(exception)));
                    continue;
                }
                if (FileUtilities.existsNoFollowLinks(file2) && !FileUtilities.sameFile(file, file2)) {
                    if (list != null && renameAction != StandardRenameAction.TEST && file.length() > 0L && FileUtilities.equalsLastModified(file, file2, 2000) && FileUtilities.equalsFileContent(file, file2)) {
                        Logging.log.severe(Logging.format("Failed to process [%s] because [%s] is an exact copy and already exists [Last-Modified: %tc]", file, file2, file2.lastModified()));
                        continue;
                    }
                    try {
                        file2 = conflictAction.conflict(file, file2);
                    }
                    catch (Exception exception) {
                        throw new CmdlineException("[" + conflictAction + "] " + Logging.cause(exception));
                    }
                    if (file2 == null) {
                        Logging.log.warning(Logging.format("[%s] Skipped [%s] because [%s] already exists", conflictAction, entry.getKey(), entry.getValue()));
                        continue;
                    }
                    if (FileUtilities.existsNoFollowLinks(file2) && !FileUtilities.sameFile(file, file2)) {
                        Logging.log.warning(Logging.format("[%s] Delete [%s]", conflictAction, file2));
                        if (renameAction != StandardRenameAction.TEST) {
                            try {
                                UserFiles.trash(file2);
                            }
                            catch (Exception exception) {
                                Logging.log.severe(Logging.format("[%s] Failed to process [%s] because [%s] already exists and cannot be deleted: %s", conflictAction, file, file2, Logging.cause(exception)));
                                continue;
                            }
                        }
                    }
                }
                if (renameAction.canRename(file, file2)) {
                    try {
                        Logging.log.info(Logging.format("[%s] from [%s] to [%s]", renameAction, file, file2));
                        file2 = renameAction.rename(file, file2);
                        linkedHashMap.put(file, file2);
                    }
                    catch (Exception exception) {
                        Logging.help(Logging.cause(exception));
                        Logging.log.severe(Logging.format("[%s] from [%s] to [%s] failed due to I/O error: %s", renameAction, file, file2, Logging.cause(exception)));
                    }
                    continue;
                }
                Logging.log.warning(Logging.format("[%s] Skipped [%s] because [%s] already exists", renameAction, file, file2));
            }
            if (renameAction != StandardRenameAction.TEST) {
                HistorySpooler.HISTORY.append(linkedHashMap);
                this.applyPostProcess(linkedHashMap, renameAction, list, applyArray, execCommand);
                Cache.DISK_STORE.flush();
            }
        }
        catch (Throwable throwable) {
            if (renameAction != StandardRenameAction.TEST) {
                HistorySpooler.HISTORY.append(linkedHashMap);
                this.applyPostProcess(linkedHashMap, renameAction, list, applyArray, execCommand);
                Cache.DISK_STORE.flush();
            }
            Logging.log.finest(Logging.format("Processed %,d %s", linkedHashMap.size(), linkedHashMap.size() == 1 ? "file" : "files"));
            throw throwable;
        }
        Logging.log.finest(Logging.format("Processed %,d %s", linkedHashMap.size(), linkedHashMap.size() == 1 ? "file" : "files"));
        if (renameAction == StandardRenameAction.TEST) {
            return Collections.emptyList();
        }
        return linkedHashMap.values().stream().collect(Collectors.toList());
    }

    protected void applyPostProcess(Map<File, File> map, RenameAction renameAction, List<Match<File, ?>> list, Apply[] applyArray, ExecCommand execCommand) {
        LinkedHashMap<File, Match<File, ?>> linkedHashMap = new LinkedHashMap<>();
        if (list != null) {
            for (Match match2 : list) {
                File file2;
                File object;
                if (match2.getCandidate() == null || (object = map.get(file2 = (File)match2.getValue())) == null || !object.exists()) continue;
                linkedHashMap.put(object, match2);
            }
        }
        linkedHashMap.forEach((file, match) -> {
            File file2 = (File)match.getValue();
            long l = file2.isFile() ? file2.lastModified() : file.lastModified();
            XattrMetaInfo.xattr.setMetaInfo((File)file, match.getCandidate(), file2.getName());
            file.setLastModified(l);
            if (MediaTypes.VIDEO_FILES.accept(file2)) {
                MediaInfoTable.copy(file2, file);
            }
        });
        if (applyArray != null) {
            for (Apply apply : applyArray) {
                try {
                    apply.apply(linkedHashMap, renameAction, new FeedbackLogger(apply));
                }
                catch (Exception exception) {
                    Logging.trace(apply, exception);
                }
            }
        }
        if (execCommand != null) {
            execCommand.execute(linkedHashMap.entrySet().stream().map(entry -> new MediaBindingBean(((Match)entry.getValue()).getCandidate(), (File)entry.getKey()))).sum();
        }
    }

    protected File nextAvailableIndexedName(File file2) {
        File file3 = file2.getParentFile();
        String string = FileUtilities.getName(file2);
        String string2 = FileUtilities.getExtension(file2);
        return IntStream.range(1, 100).mapToObj(n -> new File(file3, string + "." + n + "." + string2)).filter(file -> !file.exists()).findFirst().orElseThrow(IndexOutOfBoundsException::new);
    }

    protected void printXattrMetadata(Collection<File> collection) {
        if (Logging.help()) {
            for (File file : collection) {
                Object object = XattrMetaInfo.xattr.getMetaInfo(file);
                if (object == null) continue;
                Logging.help(Logging.format("[XATTR] %s (%s)", object, file));
            }
        }
    }

    @Override
    public List<File> getSubtitles(Collection<File> collection, QueryExpression queryExpression, Language language, SubtitleFormat subtitleFormat, Charset charset, SubtitleNaming subtitleNaming, boolean bl) throws Exception {
        Map<File, File> map;
        Map<File, List<SubtitleDescriptor>> exception;
        List<File> list = FileUtilities.filter(FileUtilities.filter(collection, FileUtilities.not(MediaFileUtilities.EXTRA_FILES)), MediaTypes.VIDEO_FILES);
        if (list.isEmpty()) {
            throw new CmdlineException("No video files", collection);
        }
        LinkedHashSet<File> linkedHashSet = new LinkedHashSet<File>(list);
        ArrayList<File> arrayList = new ArrayList<File>();
        Logging.log.finest(Logging.format("Get [%s] subtitles for %,d %s", language.getName(), linkedHashSet.size(), linkedHashSet.size() == 1 ? "file" : "files"));
        for (SubtitleLookupService datasource : WebServices.getSubtitleLookupServices(language.getLocale())) {
            if (linkedHashSet.isEmpty()) break;
            if (datasource.requireLogin()) {
                throw new ConfigurationException(datasource.getName() + " does not support anonymous login");
            }
            try {
                Logging.log.fine("Looking up subtitles by hash via " + datasource.getName());
                exception = SubtitleUtilities.lookupSubtitlesByHash(datasource, linkedHashSet, language.getLocale(), false, bl);
                map = this.downloadSubtitleBatch(datasource, exception, subtitleFormat, charset, subtitleNaming);
                linkedHashSet.removeAll(map.keySet());
                arrayList.addAll(map.values());
            }
            catch (Exception exception2) {
                Logging.log.warning(Logging.cause("Lookup by hash failed", exception2));
            }
        }
        if (bl) {
            linkedHashSet.forEach(file -> Logging.log.warning(Logging.format("No subtitles: %s (%s)", file, language.getName())));
            return arrayList;
        }
        for (SubtitleProvider datasource : WebServices.getSubtitleProviders(language.getLocale())) {
            if (linkedHashSet.isEmpty()) break;
            if (datasource.requireLogin()) {
                throw new CmdlineException("Please enter your login details by calling `filebot -script fn:configure`");
            }
            try {
                Logging.log.fine("Looking up subtitles by name via " + datasource.getName());
                exception = SubtitleUtilities.findSubtitlesByName((SubtitleProvider)datasource, linkedHashSet, language.getLocale(), queryExpression, false, bl);
                map = this.downloadSubtitleBatch(datasource, exception, subtitleFormat, charset, subtitleNaming);
                linkedHashSet.removeAll(map.keySet());
                arrayList.addAll(map.values());
            }
            catch (Exception exception3) {
                Logging.log.warning(Logging.cause("Search by name failed", exception3));
            }
        }
        linkedHashSet.forEach(file -> Logging.log.warning(Logging.format("No subtitles: %s (%s)", file, language.getName())));
        return arrayList;
    }

    @Override
    public List<File> getMissingSubtitles(Collection<File> collection, QueryExpression queryExpression, final Language language, SubtitleFormat subtitleFormat, Charset charset, final SubtitleNaming subtitleNaming, boolean bl) throws Exception {
        List<File> list = FileUtilities.filter(FileUtilities.filter(collection, MediaTypes.VIDEO_FILES), new FileFilter(){
            private Map<File, List<File>> cache = new HashMap<File, List<File>>();

            public boolean matchesLanguageCode(File file) {
                Language language2 = Language.getLanguage(MediaDetection.releaseInfo.getSubtitleLanguageTag(FileUtilities.getName(file)));
                if (language2 != null) {
                    return language2.matches(language);
                }
                return false;
            }

            @Override
            public boolean accept(File file3) {
                if (!file3.isFile()) {
                    return false;
                }
                List<File> list = this.cache.computeIfAbsent(file3.getParentFile(), file -> FileUtilities.getChildren(file, MediaTypes.SUBTITLE_FILES));
                if (subtitleNaming == SubtitleNaming.ORIGINAL) {
                    return list.size() == 0;
                }
                return list.stream().allMatch(file2 -> {
                    if (MediaFileUtilities.isDerived(file2, file3)) {
                        return subtitleNaming != SubtitleNaming.MATCH_VIDEO && !this.matchesLanguageCode((File)file2);
                    }
                    return true;
                });
            }
        });
        if (list.isEmpty()) {
            Logging.log.info("No missing subtitles");
            return null;
        }
        return this.getSubtitles(list, queryExpression, language, subtitleFormat, charset, subtitleNaming, bl);
    }

    private Map<File, File> downloadSubtitleBatch(Datasource datasource, Map<File, List<SubtitleDescriptor>> map, SubtitleFormat subtitleFormat, Charset charset, SubtitleNaming subtitleNaming) {
        LinkedHashMap<File, File> linkedHashMap = new LinkedHashMap<File, File>();
        map.forEach((file, list) -> {
            if (list.size() > 0) {
                SubtitleDescriptor subtitleDescriptor = (SubtitleDescriptor)list.get(0);
                try {
                    linkedHashMap.put((File)file, this.downloadSubtitle(datasource, subtitleDescriptor, (File)file, subtitleFormat, charset, subtitleNaming));
                }
                catch (Exception exception) {
                    Logging.log.warning(Logging.cause("Failed to download subtitle file", subtitleDescriptor, exception));
                }
            }
        });
        return linkedHashMap;
    }

    private File downloadSubtitle(Datasource datasource, SubtitleDescriptor subtitleDescriptor, File file, SubtitleFormat subtitleFormat, Charset charset, SubtitleNaming subtitleNaming) throws Exception {
        Logging.log.config(Logging.format("Fetching [%s] subtitles [%s] from [%s]", subtitleDescriptor.getLanguageName(), subtitleDescriptor, datasource.getName()));
        MemoryFile memoryFile = SubtitleUtilities.fetchSubtitle(subtitleDescriptor);
        String string = FileUtilities.getExtension(memoryFile.getName());
        ByteBuffer byteBuffer = memoryFile.getData();
        if (subtitleFormat != null || charset != null) {
            if (subtitleFormat != null) {
                string = subtitleFormat.getFilter().extension();
            }
            if (charset == null) {
                charset = StandardCharsets.UTF_8;
            }
            Logging.log.finest(Logging.format("Export [%s] as [%s]", memoryFile.getName(), Stream.of(new Comparable[]{subtitleFormat, charset}).filter(Objects::nonNull).map(Objects::toString).collect(Collectors.joining(" / "))));
            byteBuffer = SubtitleUtilities.exportSubtitles(memoryFile, subtitleFormat, charset);
        }
        File file2 = new File(file.getParentFile(), subtitleNaming.format(file, subtitleDescriptor, string));
        Logging.log.info(Logging.format("Writing [%s] to [%s]", memoryFile.getName(), file2.getName()));
        FileUtilities.writeFile(byteBuffer, file2);
        return file2;
    }

    protected <T> List<T> applyExpressionFilter(List<T> list, ExpressionFilter expressionFilter) {
        if (expressionFilter == null || list.isEmpty()) {
            return list;
        }
        Logging.log.fine(Logging.formatSingleLine("Apply filter [%s] on [%s] %s", expressionFilter.getExpression(), list.size(), list.size() == 1 ? "option" : "options"));
        EntryList entryList = new EntryList(null, list);
        List list2 = list.stream().filter(object -> {
            try {
                if (expressionFilter.matches(new MediaBindingBean(object, null, entryList))) {
                    Logging.log.finest(Logging.format("Include [%s]", object));
                    return true;
                }
            }
            catch (Exception exception) {
                Logging.debug.finest(Logging.cause("Filter", object, exception));
            }
            return false;
        }).collect(Collectors.toList());
        if (list2.isEmpty()) {
            list.forEach(object -> Logging.help(Logging.format("Exclude [%s]", object)));
        }
        Logging.log.fine(Logging.format("[%s] %s remaining", list2.size(), list2.size() == 1 ? "option" : "options"));
        if (list2.isEmpty()) {
            Logging.help("* Consider using --filter \"true\" to disable your filter if your filter has unexpectedly excluded all potential matches");
        }
        return list2;
    }

    protected List<Episode> applyEpisodeExpressionMapper(List<Episode> list, ExpressionMapper expressionMapper, BiFunction<Episode, Episode, Episode> biFunction) {
        if (expressionMapper == null || list.isEmpty()) {
            return list;
        }
        Logging.log.fine(Logging.formatSingleLine("Apply mapper [%s] on [%s] episodes", expressionMapper.getExpression(), list.size()));
        EntryList entryList = new EntryList(null, list);
        return list.stream().flatMap(episode3 -> {
            try {
                return MappedEpisode.generate(episode3, expressionMapper.map(new MediaBindingBean(episode3, null, entryList), Episode[].class), (episode, episode2) -> {
                    Logging.log.finest(Logging.format("Map [%s] to [%s]", episode, episode2));
                    return (Episode)biFunction.apply((Episode)episode, (Episode)episode2);
                });
            }
            catch (Exception exception) {
                Logging.log.severe(Logging.format("Map [%s] to [...] failed: %s", episode3, Logging.cause(exception)));
                return Stream.empty();
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected <T extends SearchResult> List<T> selectSearchResult(String string, Collection<T> collection, boolean bl, boolean bl2, int n) throws Exception {
        List<T> list = MediaDetection.getProbableMatches(bl ? string : null, collection, bl2, false);
        if (list.isEmpty()) {
            if (collection.size() == 1) {
                return collection.stream().collect(Collectors.toList());
            }
            if (bl) {
                list = MediaDetection.sortBySeriesMatchSimilarity(collection, string);
            }
        }
        return list.stream().limit(n).collect(Collectors.toList());
    }

    @Override
    public File compute(Collection<File> collection, HashType hashType, File file2, Charset charset) throws Exception {
        File[][] fileArray;
        if (file2 != null && !hashType.getFilter().accept(file2)) {
            Logging.help(Logging.format("* Consider specifying a %s file (%s) as --output file", hashType.getFilter().getDescription(), hashType.getFilter()));
        }
        if ((fileArray = (File[][])collection.stream().map(file -> FileUtilities.listPath(file.getParentFile()).toArray(new File[0])).toArray(n -> new File[n][])).length == 0) {
            throw new CmdlineException("No files", collection);
        }
        CommonSequenceMatcher commonSequenceMatcher = new CommonSequenceMatcher(0, true);
        File[] fileArray2 = commonSequenceMatcher.matchFirstCommonSequence(fileArray);
        if (fileArray2 == null) {
            throw new CmdlineException("All files must be on the same filesystem", collection);
        }
        File file3 = fileArray2[fileArray2.length - 1];
        if (file2 == null) {
            file2 = new File(file3, file3.getName() + "." + hashType.getFilter().extension());
        } else if (!file2.isAbsolute()) {
            file2 = new File(file3, file2.getPath());
        }
        Logging.log.fine(Logging.format("Compute %s for %,d files [%s]", hashType.getAlgorithm(), collection.size(), file2));
        this.compute(file3, collection, file2, hashType, charset);
        return file2;
    }

    @Override
    public void check(Collection<File> collection) throws Exception {
        for (File file : FileUtilities.filter(collection, MediaTypes.VERIFICATION_FILES)) {
            if (this.check(file, file.getParentFile())) continue;
            throw new CmdlineException("Failed to verify file integrity", file);
        }
    }

    private boolean check(File file, File file2) throws Exception {
        ArrayList<String> arrayList;
        ArrayList<String> arrayList2;
        HashType hashType = VerificationUtilities.getHashType(file);
        if (hashType == null) {
            throw new CmdlineException("Format not supported", file);
        }
        Logging.log.fine(Logging.format("Verify %s file [%s]", hashType.getAlgorithm(), file));
        arrayList2 = new ArrayList<String>();
        arrayList = new ArrayList<String>();
        VerificationFileReader verificationFileReader = hashType.newReader(file);
        try {
            while (verificationFileReader.hasNext()) {
                Map.Entry<File, String> entry = verificationFileReader.next();
                String string = entry.getKey().getPath();
                File file3 = new File(file2, string);
                String string2 = entry.getValue();
                if (!file3.exists()) {
                    Logging.log.warning(Logging.message("Missing file", string));
                    arrayList.add(string);
                    continue;
                }
                try {
                    String string3 = VerificationUtilities.computeHash(file3, hashType);
                    Logging.log.info(Logging.format("%s %s", string3, string));
                    if (string2.equalsIgnoreCase(string3)) continue;
                    Logging.log.severe(Logging.format("Corrupt file: %s [%s \u2260 %s]", string, string3.toLowerCase(Locale.ROOT), string2.toLowerCase(Locale.ROOT)));
                    arrayList2.add(string);
                }
                catch (Exception exception) {
                    Logging.log.warning(Logging.cause("Corrupt file", exception));
                    arrayList2.add(string);
                }
            }
        }
        finally {
            if (verificationFileReader != null) {
                verificationFileReader.close();
            }
        }
        if (arrayList2.size() == 0 && arrayList.size() == 0) {
            Logging.log.fine("OK");
            return true;
        }
        if (arrayList2.size() > 0) {
            Logging.log.warning(Logging.format("%s corrupt %s", arrayList2.size(), arrayList2.size() == 1 ? "file" : "files"));
        }
        if (arrayList.size() > 0) {
            Logging.log.warning(Logging.format("%s missing %s", arrayList.size(), arrayList.size() == 1 ? "file" : "files"));
        }
        return false;
    }

    private void compute(File file, Collection<File> collection, File file2, HashType hashType, Charset charset) throws IOException, Exception {
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
        try (VerificationFileWriter verificationFileWriter = new VerificationFileWriter(byteBufferOutputStream, hashType.getFormat(), charset != null ? charset : StandardCharsets.UTF_8);){
            for (File file3 : collection) {
                if (file3.isHidden() || !file3.isFile() || MediaTypes.VERIFICATION_FILES.accept(file3)) continue;
                String string = FileUtilities.normalizePathSeparators(file3.getPath().substring(file.getPath().length() + 1));
                String string2 = VerificationUtilities.computeHash(file3, hashType);
                Logging.log.info(Logging.format("%s %s", string2, string));
                verificationFileWriter.write(string, string2);
            }
        }
        FileUtilities.writeFile(byteBufferOutputStream.getByteBuffer(), file2);
    }

    private List<Episode> fetchEpisodeList(EpisodeListProvider episodeListProvider, String string, ExpressionFilter expressionFilter, ExpressionMapper expressionMapper, SortOrder sortOrder, Locale locale, boolean bl) throws Exception {
        List<Episode> arrayList = new ArrayList<Episode>();
        List<SearchResult> list = episodeListProvider.lookup(string, locale);
        if (list.isEmpty()) {
            throw new CmdlineException("No search results");
        }
        for (SearchResult searchResult : this.selectSearchResult(string, list, false, false, bl ? 1 : 5)) {
            try {
                arrayList.addAll(episodeListProvider.getEpisodeList(searchResult, sortOrder, locale));
            }
            catch (LookupException lookupException) {
                Logging.debug.warning(Logging.cause("Invalid series", searchResult, lookupException));
            }
        }
        if (arrayList.isEmpty()) {
            throw new CmdlineException("No episodes found");
        }
        arrayList = this.applyExpressionFilter(arrayList, expressionFilter);
        arrayList = this.applyEpisodeExpressionMapper(arrayList, expressionMapper, (episode, episode2) -> MappedEpisode.unmap(episode2, MappedEpisode::getMapping));
        return arrayList;
    }

    private List<Movie> fetchMovieList(MovieLookupService movieLookupService, String string, ExpressionFilter expressionFilter, Locale locale, boolean bl) throws Exception {
        List<Movie> arrayList = new ArrayList<Movie>();
        List<Movie> list = movieLookupService.lookupMovie(string, locale);
        if (list.isEmpty()) {
            throw new CmdlineException("No search results");
        }
        for (Movie movie : this.selectSearchResult(string, list, false, false, bl ? 1 : 5)) {
            try {
                arrayList.add(movieLookupService.getMovieDescriptor(movie, locale));
            }
            catch (LookupException lookupException) {
                Logging.debug.warning(Logging.cause("Invalid movie", movie, lookupException));
            }
        }
        if (arrayList.isEmpty()) {
            throw new CmdlineException("No movies found");
        }
        arrayList = this.applyExpressionFilter(arrayList, expressionFilter);
        return arrayList;
    }

    private List<?> fetchList(Datasource datasource, String string, SortOrder sortOrder, Locale locale, ExpressionFilter expressionFilter, ExpressionMapper expressionMapper, boolean bl) throws Exception {
        if (string == null || string.isEmpty()) {
            throw new CmdlineException("Please specify the --q query option");
        }
        if (datasource instanceof MovieLookupService) {
            return this.fetchMovieList((MovieLookupService)datasource, string, expressionFilter, locale, bl);
        }
        if (datasource instanceof EpisodeListProvider) {
            return this.fetchEpisodeList((EpisodeListProvider)datasource, string, expressionFilter, expressionMapper, sortOrder, locale, bl);
        }
        if (datasource == null) {
            return this.fetchEpisodeList(WebServices.getDefaultSeriesDB(), string, expressionFilter, expressionMapper, sortOrder, locale, bl);
        }
        throw new CmdlineException("-list does not support --db " + datasource.getIdentifier());
    }

    @Override
    public Stream<String> list(Datasource datasource, QueryExpression queryExpression, SortOrder sortOrder, Locale locale, ExpressionFilter expressionFilter, ExpressionMapper expressionMapper, ExpressionFormat expressionFormat, boolean bl) throws Exception {
        if (queryExpression == null) {
            throw new CmdlineException("Please specify the --q query option");
        }
        List<?> list = this.fetchList(datasource, queryExpression.value(), sortOrder, locale, expressionFilter, expressionMapper, bl);
        if (expressionFormat == null) {
            return list.stream().map(Object::toString);
        }
        EntryList entryList = new EntryList(null, list);
        return list.stream().map(object -> {
            try {
                return expressionFormat.format(new MediaBindingBean(object, null, entryList));
            }
            catch (Exception exception) {
                Logging.log.warning(exception::getMessage);
                return null;
            }
        }).filter(string -> string != null && !string.isEmpty());
    }

    @Override
    public Stream<String> getMediaInfo(Collection<File> collection, FileFilter fileFilter, ExpressionFormat expressionFormat) throws Exception {
        if (expressionFormat == null) {
            return this.getMediaInfo(collection, fileFilter, new ExpressionFormat("{f} [{resolution} {vc} {channels} {ac} {hours} {bitrate}]"));
        }
        return this.find(collection, fileFilter).map(mediaBindingBean -> {
            try {
                return expressionFormat.format(mediaBindingBean);
            }
            catch (Exception exception) {
                Logging.log.warning(exception::getMessage);
                return null;
            }
        }).filter(Objects::nonNull);
    }

    @Override
    public IntStream execute(Collection<File> collection, FileFilter fileFilter, ExpressionFormat expressionFormat, Apply[] applyArray, ExecCommand execCommand) {
        Stream<MediaBindingBean> stream = this.find(collection, fileFilter);
        if (expressionFormat != null) {
            stream = stream.peek(mediaBindingBean -> {
                try {
                    Logging.log.fine(expressionFormat.format(mediaBindingBean));
                }
                catch (Exception exception) {
                    Logging.log.warning(exception::getMessage);
                }
            });
        }
        if (applyArray == null) {
            return execCommand.execute(stream);
        }
        MediaBindingBean[] mediaBindingBeanArray = (MediaBindingBean[])stream.toArray(MediaBindingBean[]::new);
        LinkedHashMap<File, Match<File, ?>> linkedHashMap = new LinkedHashMap<File, Match<File, ?>>();
        for (MediaBindingBean mediaBindingBean2 : mediaBindingBeanArray) {
            Object object = mediaBindingBean2.getInfoObject();
            File file = mediaBindingBean2.getFileObject();
            File file2 = Optional.of(file).map(XattrMetaInfo.xattr::getOriginalName).map(File::new).orElse(null);
            linkedHashMap.put(file, Match.of(file2, object));
        }
        IntStream intStream = Stream.of(applyArray).mapToInt(apply -> {
            try {
                apply.apply(linkedHashMap, StandardRenameAction.DUPLICATE, new FeedbackLogger(apply));
                return 0;
            }
            catch (Exception exception) {
                Logging.trace(apply, exception);
                return 1;
            }
        });
        if (execCommand == null) {
            return intStream;
        }
        IntStream intStream2 = execCommand.execute(Stream.of(mediaBindingBeanArray));
        return IntStream.concat(intStream, intStream2);
    }

    private Stream<MediaBindingBean> find(Collection<File> collection, FileFilter fileFilter) {
        return collection.stream().filter(file -> file.exists() && (fileFilter == null || fileFilter.accept((File)file))).map(file -> new MediaBindingBean(XattrMetaInfo.xattr.getMetaInfo((File)file), (File)file));
    }

    @Override
    public List<File> revert(Collection<File> collection, FileFilter fileFilter, ExpressionFileFormat expressionFileFormat, File file3, RenameAction renameAction) throws Exception {
        ArrayList<File> arrayList = new ArrayList<File>();
        this.selectHistory(collection, fileFilter, expressionFileFormat, file3).forEach((file, file2) -> {
            Logging.log.info(Logging.format("Revert [%s] to [%s]", file2, file));
            if (renameAction == StandardRenameAction.TEST) {
                return;
            }
            try {
                XattrMetaInfo.xattr.clear((File)file2);
                arrayList.add(StandardRenameAction.revert(file2, file));
            }
            catch (Exception exception) {
                Logging.log.warning(Logging.cause("Failed to revert file", exception));
            }
        });
        Logging.log.finest(Logging.format("Reverted %,d %s", arrayList.size(), arrayList.size() == 1 ? "file" : "files"));
        return arrayList;
    }

    protected Map<File, File> selectHistory(Collection<File> collection, FileFilter fileFilter, ExpressionFileFormat expressionFileFormat, File file2) throws Exception {
        History history2 = HistorySpooler.HISTORY.getCompleteHistory();
        if (collection.isEmpty()) {
            if (fileFilter == null) {
                return history2.split(History.DATE_DESCENDING).map(history -> this.getRevertMap((History)history, File::exists, expressionFileFormat, file2)).filter(map -> !map.isEmpty()).findFirst().orElse(Collections.emptyMap());
            }
            return this.getRevertMap(history2, file -> file.exists() && fileFilter.accept(file), expressionFileFormat, file2);
        }
        return this.getRevertMap(history2, file -> FileUtilities.listPath(file).stream().anyMatch(collection::contains) && file.exists() && (fileFilter == null || fileFilter.accept(file)), expressionFileFormat, file2);
    }

    protected Map<File, File> getRevertMap(History history, FileFilter fileFilter, ExpressionFileFormat expressionFileFormat, File file) {
        LinkedHashMap<File, File> linkedHashMap = new LinkedHashMap<File, File>();
        HashSet hashSet = new HashSet();
        history.getRenameMap().forEach((file2, file3) -> {
            if (fileFilter.accept((File)file3)) {
                if (!hashSet.add(file3)) {
                    Logging.log.warning(Logging.format("Cannot revert [%s] to [%s] because [%s] interferes with another revert operation", file3, file2, file3));
                    return;
                }
                if (file != null) {
                    file2 = new File(file, file2.getName());
                }
                if (expressionFileFormat != null) {
                    file2 = this.getDestinationFile((File)file3, expressionFileFormat.format(new MediaBindingBean(XattrMetaInfo.xattr.getMetaInfo((File)file3), (File)file3)), file2.getParentFile());
                }
                if (!hashSet.add(file2) && !file3.equals(file2)) {
                    Logging.log.warning(Logging.format("Cannot revert [%s] to [%s] because [%s] interferes with another revert operation", file3, file2, file2));
                    return;
                }
                linkedHashMap.put((File)file2, (File)file3);
            }
        });
        return linkedHashMap;
    }

    @Override
    public List<File> extract(Collection<File> collection, File file, FileFilter fileFilter, boolean bl) throws Exception {
        List<File> list = FileUtilities.filter(collection, Archive::isArchive);
        ArrayList<File> arrayList = new ArrayList<File>();
        for (File file2 : list) {
            Archive archive = Archive.open(file2);
            try {
                File file3 = file != null && file.isAbsolute() ? file : new File(file2.getParentFile(), file != null ? file.getPath() : FileUtilities.getName(file2));
                Logging.log.info(Logging.format("Read archive [%s] and extract to [%s]", file2.getName(), file3));
                FileUtilities.createFolders(file3);
                List<SimpleFileInfo> list2 = archive.listFiles().stream().map(fileInfo -> new SimpleFileInfo(fileInfo.getPath(), fileInfo.getLength())).collect(Collectors.toList());
                if (list2.isEmpty()) {
                    Logging.log.warning(Logging.format("[%s] contains [%s] files", file2.getName(), list2.size()));
                    continue;
                }
                List<SimpleFileInfo> list3 = list2.stream().filter(fileInfo -> {
                    File file4 = new File(file3, fileInfo.getPath());
                    if (fileFilter != null && !fileFilter.accept(file4)) {
                        Logging.log.finest(Logging.format("Skip [%s]", fileInfo));
                        return false;
                    }
                    if (file4.exists() && file4.length() == fileInfo.getLength()) {
                        Logging.log.finest(Logging.format("Skip [%s] because [%s] already exists (%s)", fileInfo, file4, FileUtilities.formatSize(fileInfo.getLength())));
                        return false;
                    }
                    Logging.log.finest(Logging.format("Select [%s]", fileInfo));
                    return true;
                }).collect(Collectors.toList());
                if (list3.isEmpty()) {
                    Logging.log.warning(Logging.format("[%s] contains [%s] missing files", file2.getName(), list3.size()));
                    continue;
                }
                if (list2.size() == list3.size() || bl) {
                    Logging.log.finest("Extracting files " + list2);
                    archive.extract(file3);
                    list2.stream().map(fileInfo -> new File(file3, fileInfo.getPath())).forEach(arrayList::add);
                    continue;
                }
                Logging.log.finest("Extracting files " + list3);
                Set<File> set = list3.stream().map(FileInfo::toFile).collect(Collectors.toSet());
                archive.extract(file3, set::contains);
                list3.stream().map(fileInfo -> new File(file3, fileInfo.getPath())).forEach(arrayList::add);
            }
            finally {
                if (archive == null) continue;
                archive.close();
            }
        }
        return arrayList;
    }

    @Override
    public File zip(Collection<File> collection, File file2, FileFilter fileFilter) throws Exception {
        Cache.DISK_STORE.flush();
        if (file2 == null || !MediaTypes.ZIP.accept(file2)) {
            throw new CmdlineException("Please specify a --output *.zip file");
        }
        Logging.log.config(Logging.format("Create zip archive [%s]", file2.getName()));
        ZipUtilities.zip(collection, file -> {
            if (file.isFile() && !file.isHidden() && !MediaTypes.ZIP.accept(file) && (fileFilter == null || fileFilter.accept(file))) {
                Logging.log.finest(Logging.format("* %s (%s)", file, FileUtilities.formatSize(file.length())));
                return true;
            }
            return false;
        }, file2);
        Logging.log.info(ZipUtilities.summaryStatistics(file2));
        return file2;
    }
}

