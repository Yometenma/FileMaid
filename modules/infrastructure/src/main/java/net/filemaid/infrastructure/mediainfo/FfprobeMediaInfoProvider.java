package net.filemaid.infrastructure.mediainfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.filemaid.application.port.MediaInfoProvider;
import net.filemaid.core.model.MediaInfo;

/**
 * Reads media characteristics by invoking the local {@code ffprobe} executable
 * and parsing its JSON output. The executable path is configurable; the default
 * resolves from {@code PATH}.
 */
public final class FfprobeMediaInfoProvider implements MediaInfoProvider {
    private final String executable;
    private final ObjectMapper mapper = new ObjectMapper();

    public FfprobeMediaInfoProvider(String executable) {
        this.executable = executable == null || executable.isBlank() ? "ffprobe" : executable.trim();
    }

    @Override
    public String id() {
        return "ffprobe";
    }

    @Override
    public boolean available() {
        try {
            Process process = new ProcessBuilder(executable, "-version")
                    .redirectErrorStream(true)
                    .start();
            process.getInputStream().readAllBytes();
            return process.waitFor() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public Optional<MediaInfo> probe(Path absolutePath) throws IOException {
        if (absolutePath == null || !Files.isRegularFile(absolutePath)) return Optional.empty();
        String json = run(absolutePath);
        if (json == null || json.isBlank()) return Optional.empty();
        String fileName = absolutePath.getFileName() == null ? null : absolutePath.getFileName().toString();
        return Optional.of(parse(json, fileName));
    }

    private String run(Path absolutePath) throws IOException {
        Process process = new ProcessBuilder(executable,
                "-show_streams", "-show_format", "-print_format", "json",
                "-v", "error", absolutePath.toString())
                .redirectErrorStream(true)
                .start();
        try (var in = process.getInputStream()) {
            String output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            return exit == 0 ? output : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("ffprobe interrupted", e);
        }
    }

    MediaInfo parse(String json, String fileName) throws IOException {
        JsonNode root = mapper.readTree(json);
        JsonNode streams = root.path("streams");
        JsonNode format = root.path("format");

        String videoCodec = null;
        String videoProfile = null;
        Integer width = null;
        Integer height = null;
        Double frameRate = null;
        String audioCodec = null;
        String audioLanguage = null;
        String subtitleCodec = null;
        String subtitleLanguage = null;

        if (streams.isArray()) {
            for (JsonNode stream : streams) {
                String codecType = stream.path("codec_type").asText(null);
                if ("video".equals(codecType)) {
                    videoCodec = text(stream, "codec_name");
                    videoProfile = text(stream, "profile");
                    width = integer(stream, "width");
                    height = integer(stream, "height");
                    frameRate = fraction(text(stream, "avg_frame_rate"));
                } else if ("audio".equals(codecType)) {
                    audioCodec = text(stream, "codec_name");
                    audioLanguage = text(stream.path("tags"), "language");
                } else if ("subtitle".equals(codecType)) {
                    subtitleCodec = text(stream, "codec_name");
                    subtitleLanguage = text(stream.path("tags"), "language");
                }
            }
        }

        String tagTitle = format == null ? null : text(format.path("tags"), "title");
        return new MediaInfo(
                fileName,
                longOrNull(format, "size"),
                videoCodec,
                videoProfile,
                width,
                height,
                frameRate,
                audioCodec,
                audioLanguage,
                subtitleCodec,
                subtitleLanguage,
                doubleOrNull(format, "duration"),
                doubleOrNull(format, "bit_rate"),
                tagTitle);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field)) return null;
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private static Integer integer(JsonNode node, String field) {
        if (node == null || !node.has(field)) return null;
        JsonNode value = node.path(field);
        return value.isIntegralNumber() ? value.asInt() : null;
    }

    private static Long longOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field)) return null;
        JsonNode value = node.path(field);
        if (value.isNumber()) return value.asLong();
        if (value.isTextual()) {
            try { return Long.parseLong(value.asText().trim()); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private static Double doubleOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field)) return null;
        JsonNode value = node.path(field);
        if (value.isNumber()) return value.asDouble();
        if (value.isTextual()) {
            try { return Double.parseDouble(value.asText().trim()); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private static Double fraction(String value) {
        if (value == null || value.isBlank()) return null;
        int slash = value.indexOf('/');
        if (slash < 0) {
            try { return Double.parseDouble(value); } catch (NumberFormatException e) { return null; }
        }
        try {
            double numerator = Double.parseDouble(value.substring(0, slash));
            double denominator = Double.parseDouble(value.substring(slash + 1));
            return denominator == 0 ? null : numerator / denominator;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
