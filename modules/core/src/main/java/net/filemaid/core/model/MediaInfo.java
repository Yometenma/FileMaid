package net.filemaid.core.model;

import java.util.Objects;

/**
 * Structured media characteristics read from a video file (codec, resolution,
 * audio/subtitle tracks, duration, bit rate, ...). Fields that the probe could
 * not determine are {@code null}.
 */
public record MediaInfo(
        String fileName,
        Long fileSize,
        String videoCodec,
        String videoProfile,
        Integer width,
        Integer height,
        Double frameRate,
        String audioCodec,
        String audioLanguage,
        String subtitleCodec,
        String subtitleLanguage,
        Double durationSeconds,
        Double bitRate,
        String title) {

    public MediaInfo {
        Objects.requireNonNull(fileName, "fileName");
    }

    /** Standard vertical-resolution label, e.g. {@code 1080p}; null when height is unknown. */
    public String resolution() {
        if (height == null) return null;
        if (height >= 2160) return "2160p";
        if (height >= 1440) return "1440p";
        if (height >= 1080) return "1080p";
        if (height >= 720) return "720p";
        if (height >= 576) return "576p";
        if (height >= 480) return "480p";
        return height + "p";
    }
}
