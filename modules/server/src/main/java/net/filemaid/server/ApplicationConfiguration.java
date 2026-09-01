package net.filemaid.server;

import java.util.List;
import net.filemaid.application.port.MediaScanner;
import net.filemaid.application.port.MediaNameParser;
import net.filemaid.application.port.MediaInfoProvider;
import net.filemaid.application.port.MetadataProvider;
import net.filemaid.application.port.NamingTemplateEngine;
import net.filemaid.application.service.ParseMediaNameService;
import net.filemaid.application.service.ProbeMediaInfoService;
import net.filemaid.application.service.RenamePreviewService;
import net.filemaid.application.service.ScanMediaService;
import net.filemaid.application.service.StoragePathPolicy;
import net.filemaid.application.service.SearchMetadataService;
import net.filemaid.application.service.AnalyzeMediaGroupsService;
import net.filemaid.core.model.StorageRoot;
import net.filemaid.infrastructure.filesystem.LocalMediaScanner;
import net.filemaid.infrastructure.mediainfo.FfprobeMediaInfoProvider;
import net.filemaid.infrastructure.parser.LegacyMediaNameParserAdapter;
import net.filemaid.infrastructure.metadata.PreferredTmdbMetadataProvider;
import net.filemaid.infrastructure.naming.SafeNamingTemplateEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    @Bean MediaScanner mediaScanner() { return new LocalMediaScanner(); }
    @Bean StoragePathPolicy storagePathPolicy() { return new StoragePathPolicy(); }
    @Bean MediaNameParser mediaNameParser() { return new LegacyMediaNameParserAdapter(); }
    @Bean ParseMediaNameService parseMediaNameService(MediaNameParser parser) { return new ParseMediaNameService(parser); }
    @Bean NamingTemplateEngine namingTemplateEngine(FileMaidProperties properties) {
        var naming = properties.naming();
        return new SafeNamingTemplateEngine(naming.series(), naming.movie(), naming.unknown());
    }
    @Bean RenamePreviewService renamePreviewService(MediaNameParser parser, NamingTemplateEngine naming) { return new RenamePreviewService(parser, naming); }
    @Bean MetadataProvider metadataProvider(FileMaidProperties properties) { return new PreferredTmdbMetadataProvider(properties.metadata().tmdbApiKey()); }
    @Bean SearchMetadataService searchMetadataService(MetadataProvider provider) { return new SearchMetadataService(provider); }
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
}
