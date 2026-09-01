package net.filemaid.application.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.filemaid.application.port.MediaPostProcessor;
import net.filemaid.application.port.OperationHistoryRepository;
import net.filemaid.core.model.MetadataSelection;
import net.filemaid.core.model.OperationResult;
import net.filemaid.core.model.RenameOperation;
import net.filemaid.core.model.StorageRoot;

public final class PostProcessMediaService {
    private final Map<String, StorageRoot> roots;
    private final StoragePathPolicy pathPolicy;
    private final MediaPostProcessor processor;
    private final OperationHistoryRepository history;
    private final SettingsService settings;
    public PostProcessMediaService(List<StorageRoot> roots, StoragePathPolicy pathPolicy, MediaPostProcessor processor, OperationHistoryRepository history) {
        this(roots, pathPolicy, processor, history, null);
    }
    public PostProcessMediaService(List<StorageRoot> roots, StoragePathPolicy pathPolicy, MediaPostProcessor processor, OperationHistoryRepository history, SettingsService settings) {
        this.roots=roots.stream().collect(Collectors.toUnmodifiableMap(StorageRoot::id, Function.identity())); this.pathPolicy=pathPolicy; this.processor=processor; this.history=history; this.settings=settings;
    }
    public List<OperationResult> process(String rootId, List<Item> items, boolean nfo, boolean artwork, String artworkType) {
        StorageRoot root=roots.get(rootId); if(root==null)throw new IllegalArgumentException("Unknown storage root: "+rootId); if(!root.writable())throw new IllegalStateException("Storage root is read-only: "+rootId);
        List<OperationResult> results=new ArrayList<>();
        for(Item item:items){Path media=pathPolicy.resolve(root,item.mediaPath());if(!Files.isRegularFile(media)){results.add(new OperationResult(item.mediaPath(),item.mediaPath(),RenameOperation.OperationType.NFO,false,"媒体文件不存在"));continue;}
            if(nfo&&item.metadata()!=null)results.add(run(item.mediaPath(),RenameOperation.OperationType.NFO,()->processor.writeKodiNfo(media,item.metadata()),root));
            if(artwork&&item.artworkUrl()!=null&&!item.artworkUrl().isBlank())results.add(run(item.mediaPath(),RenameOperation.OperationType.ARTWORK,()->processor.downloadArtwork(media,item.artworkUrl(),artworkType),root));
        }
        if(!results.isEmpty())history.append(results); cleanupHistory(); return results;
    }
    private void cleanupHistory() {
        int days = settings == null ? 0 : OperationHistoryService.parseDays(settings.value("files.historyRetentionDays", "90"));
        if (history != null && days > 0) history.deleteOlderThan(Instant.now().minus(days, ChronoUnit.DAYS));
    }
    private OperationResult run(String source,RenameOperation.OperationType type,Action action,StorageRoot root){try{Path target=action.run();return new OperationResult(source,root.path().relativize(target).toString().replace('\\','/'),type,true,null);}catch(Exception failure){return new OperationResult(source,source,type,false,failure.getMessage());}}
    public record Item(String mediaPath, MetadataSelection metadata, String artworkUrl) { }
    @FunctionalInterface private interface Action { Path run() throws Exception; }
}
