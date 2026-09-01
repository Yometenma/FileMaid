package net.filemaid.application.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.filemaid.application.port.MediaNameParser;
import net.filemaid.application.port.NamingTemplateEngine;
import net.filemaid.core.model.MediaType;
import net.filemaid.core.model.MetadataSelection;
import net.filemaid.core.model.MetadataType;
import net.filemaid.core.model.ParsedMediaName;
import net.filemaid.core.model.RenamePreview;

public final class RenamePreviewService {
    private final MediaNameParser parser;
    private final NamingTemplateEngine naming;

    public RenamePreviewService(MediaNameParser parser, NamingTemplateEngine naming) {
        this.parser = parser;
        this.naming = naming;
    }

    public List<RenamePreview> preview(List<String> relativePaths) {
        return preview(relativePaths, List.of());
    }

    public List<RenamePreview> preview(List<String> relativePaths, List<MetadataSelection> selections) {
        if (relativePaths == null || relativePaths.isEmpty()) throw new IllegalArgumentException("At least one relative path is required");
        if (relativePaths.size() > 1_000) throw new IllegalArgumentException("A preview may contain at most 1000 paths");
        List<MetadataSelection> safeSelections = selections == null ? List.of() : List.copyOf(selections);
        return relativePaths.stream().map(path -> previewOne(path, selectionFor(path, safeSelections))).toList();
    }

    private MetadataSelection selectionFor(String source, List<MetadataSelection> selections) {
        String normalized = normalize(source);
        return selections.stream().filter(item -> normalize(item.source()).equals(normalized)).findFirst().orElse(null);
    }

    private RenamePreview previewOne(String source, MetadataSelection selection) {
        Path sourcePath = Path.of(source).normalize();
        if (sourcePath.isAbsolute() || sourcePath.startsWith("..")) throw new IllegalArgumentException("Preview paths must stay relative to a storage root");
        ParsedMediaName media = parser.parse(sourcePath.getFileName().toString());
        if (selection != null) media = applySelection(media, selection);
        List<String> warnings = new ArrayList<>();
        String target;
        if (media.type() == MediaType.EPISODE && media.season() != null && !media.episodes().isEmpty()) {
            target = naming.format(media);
        } else if (media.type() == MediaType.MOVIE && media.year() != null) {
            target = naming.format(media);
        } else {
            target = naming.format(media);
            warnings.add("The file name could not be classified with enough confidence");
        }
        return new RenamePreview(normalize(source), target, media, selection, warnings);
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
