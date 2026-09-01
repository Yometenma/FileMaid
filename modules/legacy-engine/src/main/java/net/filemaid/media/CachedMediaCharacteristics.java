package net.filemaid.media;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.Logging;
import net.filemaid.MemoryCache;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.media.MediaCharacteristicsParser;
import net.filemaid.util.FileKey;

public class CachedMediaCharacteristics {
    private static final MediaCharacteristicsParser parser = MediaCharacteristicsParser.getDefault();
    private static final MemoryCache<FileKey, Optional<MediaCharacteristics>> cache = MemoryCache.forMinutes();

    public static Optional<MediaCharacteristics> getMediaCharacteristics(File file) {
        if (!parser.acceptVideoFile(file)) {
            return Optional.empty();
        }
        return cache.get(FileKey.of(file), fileKey -> {
            try (MediaCharacteristics mediaCharacteristics = parser.open(fileKey.getFile());){
                Value value = new Value(fileKey.getFile(), mediaCharacteristics);
                if (value.isValid()) {
                    Optional<MediaCharacteristics> optional = Optional.of(value);
                    return optional;
                }
                Logging.debug.warning(Logging.message("Invalid media file", value));
                return Optional.empty();
            }
            catch (Throwable throwable) {
                Logging.debug.warning(Logging.cause("Failed to read media characteristics", fileKey, throwable));
            }
            return Optional.empty();
        });
    }

    public static <T> Optional<T> getMediaCharacteristics(File file, Function<MediaCharacteristics, T> function) {
        return CachedMediaCharacteristics.getMediaCharacteristics(file).map(mediaCharacteristics -> {
            try {
                return function.apply((MediaCharacteristics)mediaCharacteristics);
            }
            catch (Throwable throwable) {
                Logging.debug.warning(Logging.cause("Failed to parse media characteristics", mediaCharacteristics, throwable));
                return null;
            }
        });
    }

    public static void applyMediaCharacteristics(File file, Consumer<MediaCharacteristics> consumer) {
        CachedMediaCharacteristics.getMediaCharacteristics(file).ifPresent(mediaCharacteristics -> {
            try {
                consumer.accept((MediaCharacteristics)mediaCharacteristics);
            }
            catch (Throwable throwable) {
                Logging.debug.warning(Logging.cause("Failed to apply media characteristics", mediaCharacteristics, throwable));
            }
        });
    }

    public static MediaCharacteristicsParser getMediaCharacteristicsParser() {
        return parser;
    }

    private static class Value
    implements MediaCharacteristics {
        private final String name;
        private final Long size;
        private final String videoCodec;
        private final String videoProfile;
        private final String audioCodec;
        private final String audioLanguage;
        private final String subtitleCodec;
        private final String subtitleLanguage;
        private final Duration duration;
        private final Integer width;
        private final Integer height;
        private final Double bitRate;
        private final Double frameRate;
        private final String title;
        private final Instant creationTime;
        private final Object mediaTags;

        public Value(File file, MediaCharacteristics mediaCharacteristics) {
            this.name = file.getName();
            this.size = file.length();
            this.videoCodec = this.get(mediaCharacteristics::getVideoCodec, "VideoCodec");
            this.videoProfile = this.get(mediaCharacteristics::getVideoProfile, "VideoProfile");
            this.audioCodec = this.get(mediaCharacteristics::getAudioCodec, "AudioCodec");
            this.audioLanguage = this.get(mediaCharacteristics::getAudioLanguage, "AudioLanguage");
            this.subtitleCodec = this.get(mediaCharacteristics::getSubtitleCodec, "SubtitleCodec");
            this.subtitleLanguage = this.get(mediaCharacteristics::getSubtitleLanguage, "SubtitleLanguage");
            this.duration = this.get(mediaCharacteristics::getDuration, "Duration");
            this.width = this.get(mediaCharacteristics::getWidth, "Width");
            this.height = this.get(mediaCharacteristics::getHeight, "Height");
            this.bitRate = this.get(mediaCharacteristics::getBitRate, "BitRate");
            this.frameRate = this.get(mediaCharacteristics::getFrameRate, "FrameRate");
            this.title = this.get(mediaCharacteristics::getTitle, "Title");
            this.creationTime = this.get(mediaCharacteristics::getCreationTime, "CreationTime");
            this.mediaTags = this.get(mediaCharacteristics::getMediaTags, "MediaTags");
        }

        private <T> T get(Supplier<T> supplier, String string) {
            try {
                T t = supplier.get();
                if (t instanceof CharSequence && t.toString().isEmpty()) {
                    return null;
                }
                return t;
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("Failed to parse media property", string, this.name, exception));
                return null;
            }
        }

        @Override
        public String getFileName() {
            return this.name;
        }

        @Override
        public Long getFileSize() {
            return this.size;
        }

        @Override
        public String getVideoCodec() {
            return this.videoCodec;
        }

        @Override
        public String getVideoProfile() {
            return this.videoProfile;
        }

        @Override
        public String getAudioCodec() {
            return this.audioCodec;
        }

        @Override
        public String getAudioLanguage() {
            return this.audioLanguage;
        }

        @Override
        public String getSubtitleCodec() {
            return this.subtitleCodec;
        }

        @Override
        public String getSubtitleLanguage() {
            return this.subtitleLanguage;
        }

        @Override
        public Duration getDuration() {
            return this.duration;
        }

        @Override
        public Integer getWidth() {
            return this.width;
        }

        @Override
        public Integer getHeight() {
            return this.height;
        }

        @Override
        public Double getBitRate() {
            return this.bitRate;
        }

        @Override
        public Double getFrameRate() {
            return this.frameRate;
        }

        @Override
        public String getTitle() {
            return this.title;
        }

        @Override
        public Instant getCreationTime() {
            return this.creationTime;
        }

        @Override
        public Object getMediaTags() {
            return this.mediaTags;
        }

        @Override
        public void close() {
        }

        public Stream<Object> values() {
            return Stream.of(this.name, this.size, this.videoCodec, this.videoProfile, this.audioCodec, this.audioLanguage, this.subtitleCodec, this.subtitleLanguage, this.duration, this.width, this.height, this.bitRate, this.frameRate, this.title, this.creationTime, this.mediaTags);
        }

        public boolean isValid() {
            return this.values().skip(2L).anyMatch(Objects::nonNull);
        }

        public String toString() {
            return this.values().filter(Objects::nonNull).map(Objects::toString).collect(Collectors.joining(" | ", "[", "]"));
        }
    }
}

