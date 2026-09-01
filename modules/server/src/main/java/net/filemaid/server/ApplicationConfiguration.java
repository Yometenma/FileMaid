package net.filemaid.server;

import java.util.List;
import net.filemaid.application.port.MediaScanner;
import net.filemaid.application.port.MediaNameParser;
import net.filemaid.application.port.MediaInfoProvider;
import net.filemaid.application.port.MetadataProvider;
import net.filemaid.application.port.NamingTemplateEngine;
import net.filemaid.application.port.SimilarityRanker;
import net.filemaid.application.service.ParseMediaNameService;
import net.filemaid.application.service.ProbeMediaInfoService;
import net.filemaid.application.service.RenamePreviewService;
import net.filemaid.application.service.ScanMediaService;
import net.filemaid.application.service.StoragePathPolicy;
import net.filemaid.application.service.SearchMetadataService;
import net.filemaid.application.service.AnalyzeMediaGroupsService;
import net.filemaid.application.service.BuildRenamePlanService;
import net.filemaid.application.service.ExecuteRenamePlanService;
import net.filemaid.application.port.MatchDecisionRepository;
import net.filemaid.application.service.MatchDecisionService;
import net.filemaid.application.service.MatchMetadataService;
import net.filemaid.application.service.ValidateRenamePlanService;
import net.filemaid.core.model.StorageRoot;
import net.filemaid.infrastructure.filesystem.LocalMediaScanner;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    @Bean MediaScanner mediaScanner() { return new LocalMediaScanner(); }
    @Bean StoragePathPolicy storagePathPolicy() { return new StoragePathPolicy(); }
    @Bean MediaNameParser mediaNameParser() { return new RegexMediaNameParser(); }
    @Bean ParseMediaNameService parseMediaNameService(MediaNameParser parser) { return new ParseMediaNameService(parser); }
    @Bean NamingTemplateEngine namingTemplateEngine(FileMaidProperties properties) {
        var naming = properties.naming();
        return new SafeNamingTemplateEngine(naming.series(), naming.movie(), naming.unknown());
    }
    @Bean RenamePreviewService renamePreviewService(MediaNameParser parser, NamingTemplateEngine naming, ProbeMediaInfoService probeService) { return new RenamePreviewService(parser, naming, probeService); }
    @Bean BuildRenamePlanService buildRenamePlanService(RenamePreviewService previewService) { return new BuildRenamePlanService(previewService); }
    @Bean ValidateRenamePlanService validateRenamePlanService(FileMaidProperties properties, StoragePathPolicy pathPolicy) {
        List<StorageRoot> roots = properties.roots().stream()
                .map(root -> new StorageRoot(root.id(), root.path(), root.writable()))
                .toList();
        return new ValidateRenamePlanService(roots, pathPolicy);
    }
    @Bean ExecuteRenamePlanService executeRenamePlanService(FileMaidProperties properties, StoragePathPolicy pathPolicy) {
        List<StorageRoot> roots = properties.roots().stream()
                .map(root -> new StorageRoot(root.id(), root.path(), root.writable()))
                .toList();
        return new ExecuteRenamePlanService(roots, pathPolicy);
    }
    @Bean SearchMetadataService searchMetadataService(List<MetadataProvider> providers) { return new SearchMetadataService(providers); }
    @Bean SimilarityRanker similarityRanker() { return new BuiltinSimilarityRanker(); }
    @Bean MatchMetadataService matchMetadataService(MediaNameParser parser, SearchMetadataService searchService, SimilarityRanker ranker) { return new MatchMetadataService(parser, searchService, ranker); }
    @Bean MatchDecisionRepository matchDecisionRepository(FileMaidProperties properties) { return new SqliteMatchDecisionRepository(properties.dbPath()); }
    @Bean MatchDecisionService matchDecisionService(MatchDecisionRepository repository) { return new MatchDecisionService(repository); }
    @Bean AnalyzeMediaGroupsService analyzeMediaGroupsService(MediaNameParser parser) { return new AnalyzeMediaGroupsService(parser); }
    @Bean MediaInfoProvider mediaInfoProvider(FileMaidProperties properties) { return new FfprobeMediaInfoProvider(properties.probe().ffprobePath()); }
    @Bean ProbeMediaInfoService probeMediaInfoService(FileMaidProperties properties, StoragePathPolicy pathPolicy, MediaInfoProvider provider) {
        List<StorageRoot> roots = properties.roots().stream()
                .map(root -> new StorageRoot(root.id(), root.path(), root.writable()))
                .toList();
        return new ProbeMediaInfoService(roots, pathPolicy, provider);
    }

    @Bean
    ScanMediaService scanMediaService(FileMaidProperties properties, MediaScanner scanner, StoragePathPolicy pathPolicy) {
        List<StorageRoot> roots = properties.roots().stream()
                .map(root -> new StorageRoot(root.id(), root.path(), root.writable()))
                .toList();
        return new ScanMediaService(roots, scanner, pathPolicy);
    }

    @Bean MetadataProvider tmdbMetadataProvider(FileMaidProperties properties) { return new TmdbHttpMetadataProvider(properties.metadata().tmdbApiKey()); }
    @Bean MetadataProvider tvdbMetadataProvider(FileMaidProperties properties) { return new TvdbHttpMetadataProvider(properties.metadata().tvdbApiKey(), properties.metadata().tvdbPin()); }
    @Bean MetadataProvider omdbMetadataProvider(FileMaidProperties properties) { return new OmdbHttpMetadataProvider(properties.metadata().omdbApiKey()); }
    @Bean MetadataProvider tvmazeMetadataProvider(FileMaidProperties properties) { return new TvMazeHttpMetadataProvider(properties.metadata().tvmazeEnabled()); }
    @Bean MetadataProvider anidbMetadataProvider(FileMaidProperties properties) { return new AnidbHttpMetadataProvider(properties.metadata().anidbEnabled()); }
}
