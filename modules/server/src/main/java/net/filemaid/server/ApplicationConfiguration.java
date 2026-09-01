package net.filemaid.server;

import java.util.List;
import net.filemaid.application.port.MediaScanner;
import net.filemaid.application.port.MediaNameParser;
import net.filemaid.application.port.MediaInfoProvider;
import net.filemaid.application.port.MetadataProvider;
import net.filemaid.application.port.NamingTemplateEngine;
import net.filemaid.application.port.SimilarityRanker;
import net.filemaid.application.port.MatchDecisionRepository;
import net.filemaid.application.port.OperationHistoryRepository;
import net.filemaid.application.port.SettingsRepository;
import net.filemaid.application.port.StorageBrowser;
import net.filemaid.application.port.MediaPostProcessor;
import net.filemaid.application.port.UserAccountRepository;
import net.filemaid.application.service.ParseMediaNameService;
import net.filemaid.application.service.ProbeMediaInfoService;
import net.filemaid.application.service.RenamePreviewService;
import net.filemaid.application.service.ScanMediaService;
import net.filemaid.application.service.StoragePathPolicy;
import net.filemaid.application.service.SearchMetadataService;
import net.filemaid.application.service.AnalyzeMediaGroupsService;
import net.filemaid.application.service.BuildRenamePlanService;
import net.filemaid.application.service.ExecuteRenamePlanService;
import net.filemaid.application.service.MatchDecisionService;
import net.filemaid.application.service.MatchMetadataService;
import net.filemaid.application.service.OperationHistoryService;
import net.filemaid.application.service.UndoService;
import net.filemaid.application.service.ValidateRenamePlanService;
import net.filemaid.application.service.SettingsService;
import net.filemaid.application.service.BrowseStorageService;
import net.filemaid.application.service.PostProcessMediaService;
import net.filemaid.core.model.StorageRoot;
import net.filemaid.infrastructure.filesystem.LocalMediaScanner;
import net.filemaid.infrastructure.filesystem.LocalStorageBrowser;
import net.filemaid.infrastructure.mediainfo.FfprobeMediaInfoProvider;
import net.filemaid.infrastructure.parser.RegexMediaNameParser;
import net.filemaid.infrastructure.metadata.AnidbHttpMetadataProvider;
import net.filemaid.infrastructure.metadata.BuiltinSimilarityRanker;
import net.filemaid.infrastructure.metadata.OmdbHttpMetadataProvider;
import net.filemaid.infrastructure.metadata.TmdbHttpMetadataProvider;
import net.filemaid.infrastructure.metadata.TvMazeHttpMetadataProvider;
import net.filemaid.infrastructure.metadata.TvdbHttpMetadataProvider;
import net.filemaid.infrastructure.naming.SafeNamingTemplateEngine;
import net.filemaid.infrastructure.persistence.SqliteMatchDecisionRepository;
import net.filemaid.infrastructure.persistence.SqliteOperationHistoryRepository;
import net.filemaid.infrastructure.persistence.SqliteSettingsRepository;
import net.filemaid.infrastructure.postprocess.LocalMediaPostProcessor;
import net.filemaid.infrastructure.persistence.SqliteUserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    @Bean MediaScanner mediaScanner() { return new LocalMediaScanner(); }
    @Bean StorageBrowser storageBrowser() { return new LocalStorageBrowser(); }
    @Bean StoragePathPolicy storagePathPolicy() { return new StoragePathPolicy(); }
    @Bean BrowseStorageService browseStorageService(FileMaidProperties properties, StoragePathPolicy policy, StorageBrowser browser) {
        return new BrowseStorageService(storageRoots(properties), policy, browser);
    }
    @Bean MediaNameParser mediaNameParser() { return new RegexMediaNameParser(); }
    @Bean ParseMediaNameService parseMediaNameService(MediaNameParser parser) { return new ParseMediaNameService(parser); }
    @Bean NamingTemplateEngine namingTemplateEngine(FileMaidProperties properties, SettingsService settings) { return new RuntimeNamingTemplateEngine(properties, settings); }
    @Bean RenamePreviewService renamePreviewService(MediaNameParser parser, NamingTemplateEngine naming, ProbeMediaInfoService probeService) { return new RenamePreviewService(parser, naming, probeService); }
    @Bean BuildRenamePlanService buildRenamePlanService(RenamePreviewService previewService) { return new BuildRenamePlanService(previewService); }
    @Bean ValidateRenamePlanService validateRenamePlanService(FileMaidProperties properties, StoragePathPolicy pathPolicy) {
        return new ValidateRenamePlanService(storageRoots(properties), pathPolicy);
    }
    @Bean OperationHistoryRepository operationHistoryRepository(FileMaidProperties properties) { return new SqliteOperationHistoryRepository(properties.dbPath()); }
    @Bean OperationHistoryService operationHistoryService(OperationHistoryRepository repository) { return new OperationHistoryService(repository); }
    @Bean UndoService undoService(FileMaidProperties properties, StoragePathPolicy pathPolicy, OperationHistoryRepository history) {
        return new UndoService(storageRoots(properties), pathPolicy, history);
    }
    @Bean ExecuteRenamePlanService executeRenamePlanService(FileMaidProperties properties, StoragePathPolicy pathPolicy, OperationHistoryRepository history) {
        return new ExecuteRenamePlanService(storageRoots(properties), pathPolicy, history);
    }
    @Bean SettingsRepository settingsRepository(FileMaidProperties properties) { return new SqliteSettingsRepository(properties.dbPath()); }
    @Bean UserAccountRepository userAccountRepository(FileMaidProperties properties) { return new SqliteUserAccountRepository(properties.dbPath()); }
    @Bean SettingsService settingsService(SettingsRepository repository) { return new SettingsService(repository); }
    @Bean MediaPostProcessor mediaPostProcessor() { return new LocalMediaPostProcessor(); }
    @Bean PostProcessMediaService postProcessMediaService(FileMaidProperties properties, StoragePathPolicy policy, MediaPostProcessor processor, OperationHistoryRepository history) {
        return new PostProcessMediaService(storageRoots(properties), policy, processor, history);
    }
    @Bean SearchMetadataService searchMetadataService(List<MetadataProvider> providers, SettingsService settings) { return new SearchMetadataService(providers, settings::languagePriority); }
    @Bean SimilarityRanker similarityRanker() { return new BuiltinSimilarityRanker(); }
    @Bean MatchMetadataService matchMetadataService(MediaNameParser parser, SearchMetadataService searchService, SimilarityRanker ranker) { return new MatchMetadataService(parser, searchService, ranker); }
    @Bean MatchDecisionRepository matchDecisionRepository(FileMaidProperties properties) { return new SqliteMatchDecisionRepository(properties.dbPath()); }
    @Bean MatchDecisionService matchDecisionService(MatchDecisionRepository repository) { return new MatchDecisionService(repository); }
    @Bean AnalyzeMediaGroupsService analyzeMediaGroupsService(MediaNameParser parser) { return new AnalyzeMediaGroupsService(parser); }
    @Bean MediaInfoProvider mediaInfoProvider(FileMaidProperties properties) { return new FfprobeMediaInfoProvider(properties.probe().ffprobePath()); }
    @Bean ProbeMediaInfoService probeMediaInfoService(FileMaidProperties properties, StoragePathPolicy pathPolicy, MediaInfoProvider provider) {
        return new ProbeMediaInfoService(storageRoots(properties), pathPolicy, provider);
    }
    @Bean ScanMediaService scanMediaService(FileMaidProperties properties, MediaScanner scanner, StoragePathPolicy pathPolicy) {
        return new ScanMediaService(storageRoots(properties), scanner, pathPolicy);
    }

    @Bean MetadataProvider tmdbMetadataProvider(FileMaidProperties properties, SettingsService settings) { return new RuntimeMetadataProvider("tmdb", properties, settings); }
    @Bean MetadataProvider tvdbMetadataProvider(FileMaidProperties properties, SettingsService settings) { return new RuntimeMetadataProvider("tvdb", properties, settings); }
    @Bean MetadataProvider omdbMetadataProvider(FileMaidProperties properties, SettingsService settings) { return new RuntimeMetadataProvider("omdb", properties, settings); }
    @Bean MetadataProvider tvmazeMetadataProvider(FileMaidProperties properties, SettingsService settings) { return new RuntimeMetadataProvider("tvmaze", properties, settings); }
    @Bean MetadataProvider anidbMetadataProvider(FileMaidProperties properties, SettingsService settings) { return new RuntimeMetadataProvider("anidb", properties, settings); }

    private List<StorageRoot> storageRoots(FileMaidProperties properties) {
        return properties.roots().stream()
                .map(root -> new StorageRoot(root.id(), root.path(), root.writable()))
                .toList();
    }
}
