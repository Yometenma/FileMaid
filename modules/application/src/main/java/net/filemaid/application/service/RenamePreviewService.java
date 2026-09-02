package net.filemaid.application.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import net.filemaid.application.port.MediaNameParser;
import net.filemaid.application.port.NamingTemplateEngine;
import net.filemaid.core.model.MediaInfo;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.MetadataSelection;
import net.filemaid.core.model.MetadataType;
import net.filemaid.core.model.ParsedMediaName;
import net.filemaid.core.model.RenamePreview;

public final class RenamePreviewService {
    private final MediaNameParser parser;
    private final NamingTemplateEngine naming;
    private final ProbeMediaInfoService probeService;

    public RenamePreviewService(MediaNameParser parser, NamingTemplateEngine naming, ProbeMediaInfoService probeService) {
        this.parser = parser;
        this.naming = naming;
        this.probeService = probeService;
    }

    public List<RenamePreview> preview(List<String> relativePaths) {
        return preview(null, relativePaths, List.of());
    }

    public List<RenamePreview> preview(List<String> relativePaths, List<MetadataSelection> selections) {
        return preview(null, relativePaths, selections);
    }

    /** When {@code rootId} is provided, media info is probed for video files and passed to the naming template. */
    public List<RenamePreview> preview(String rootId, List<String> relativePaths, List<MetadataSelection> selections) {
        return preview(rootId, relativePaths, selections, ignored -> { }, () -> false);
    }

    public List<RenamePreview> preview(String rootId, List<String> relativePaths, List<MetadataSelection> selections,
            IntConsumer progress, BooleanSupplier cancelled) {
        if (relativePaths == null || relativePaths.isEmpty()) throw new IllegalArgumentException("At least one relative path is required");
        if (relativePaths.size() > 1_000) throw new IllegalArgumentException("A preview may contain at most 1000 paths");
        List<MetadataSelection> safeSelections = selections == null ? List.of() : List.copyOf(selections);
        List<RenamePreview> previews = new ArrayList<>(relativePaths.size());
        for (int index = 0; index < relativePaths.size(); index++) {
            if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
                throw new java.util.concurrent.CancellationException("预览任务已取消");
            }
            String path = relativePaths.get(index);
            previews.add(previewOne(rootId, path, selectionFor(path, safeSelections)));
            progress.accept((index + 1) * 100 / relativePaths.size());
        }
        return markTargetConflicts(previews);
    }

    private List<RenamePreview> markTargetConflicts(List<RenamePreview> previews) {
        Map<String, Long> counts = previews.stream()
                .map(RenamePreview::target)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return previews.stream().map(preview -> {
            if (counts.getOrDefault(preview.target(), 0L) > 1) {
                List<String> warnings = new ArrayList<>(preview.warnings());
                warnings.add("目标路径与其它文件冲突（重名）");
                return new RenamePreview(preview.source(), preview.target(), preview.media(), preview.metadata(), preview.mediaInfo(), warnings);
            }
            return preview;
        }).toList();
    }

    private MetadataSelection selectionFor(String source, List<MetadataSelection> selections) {
        String normalized = normalize(source);
        return selections.stream().filter(item -> normalize(item.source()).equals(normalized)).findFirst().orElse(null);
    }

    private RenamePreview previewOne(String rootId, String source, MetadataSelection selection) {
        Path sourcePath = Path.of(source).normalize();
        if (sourcePath.isAbsolute() || sourcePath.startsWith("..")) throw new IllegalArgumentException("Preview paths must stay relative to a storage root");
        ParsedMediaName media = parser.parse(sourcePath.getFileName().toString());
        if (selection != null) media = applySelection(media, selection);
        List<String> warnings = new ArrayList<>();
        MediaInfo mediaInfo = probeMediaInfo(rootId, source, media);
        String target = naming.format(media, mediaInfo);
        boolean confident = (media.type() == MediaType.EPISODE && media.season() != null && !media.episodes().isEmpty())
                || (media.type() == MediaType.MOVIE && media.year() != null);
        if (!confident) warnings.add("The file name could not be classified with enough confidence");
        return new RenamePreview(normalize(source), target, media, selection, mediaInfo, warnings);
    }

    private MediaInfo probeMediaInfo(String rootId, String source, ParsedMediaName media) {
        if (probeService == null || rootId == null || rootId.isBlank()) return null;
        if (media.type() != MediaType.EPISODE && media.type() != MediaType.MOVIE) return null;
        try {
            return probeService.probe(rootId, source).orElse(null);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private ParsedMediaName applySelection(ParsedMediaName parsed, MetadataSelection selection) {
        MediaType expected = selection.type() == MetadataType.MOVIE ? MediaType.MOVIE : MediaType.EPISODE;
        if (parsed.type() != expected && parsed.type() != MediaType.UNKNOWN) {
            throw new IllegalArgumentException("Selected metadata type does not match the parsed media type");
        }
        return new ParsedMediaName(parsed.originalName(), selection.title(), expected,
                selection.year() != null ? selection.year() : parsed.year(), parsed.season(), parsed.episodes(),
                parsed.extension(), 1.0, "confirmed:" + selection.provider() + ":" + selection.id());
    }

    private String normalize(String source) {
        Path path = Path.of(source).normalize();
        if (path.isAbsolute() || path.startsWith("..")) throw new IllegalArgumentException("Preview paths must stay relative to a storage root");
        return path.toString().replace('\\', '/');
    }
}
