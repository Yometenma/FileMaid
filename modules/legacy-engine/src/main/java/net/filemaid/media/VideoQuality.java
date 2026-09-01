package net.filemaid.media;

import java.io.File;
import java.util.Comparator;
import java.util.regex.Pattern;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.media.CachedMediaCharacteristics;
import net.filemaid.media.MediaCharacteristics;
import net.filemaid.media.MediaDetection;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.media.VideoFormat;
import net.filemaid.util.StringUtilities;

public class VideoQuality
implements Comparator<File> {
    public static final Comparator<File> DESCENDING_ORDER = new VideoQuality(VideoFormat.DEFAULT_GROUPS, MediaDetection.releaseInfo.getRepackPattern()).reversed();
    private final VideoFormat format;
    private final Pattern repack;
    private final Comparator<MediaCharacteristics> quality = Comparator.comparingInt(this::getFormatScore).thenComparing(VideoCodec::compare).thenComparingInt(this::getRepackScore).thenComparingDouble(this::getBitrateScore).thenComparingLong(MediaCharacteristics::getFileSize);

    public static boolean isBetter(File file, File file2) {
        return DESCENDING_ORDER.compare(file, file2) < 0;
    }

    public VideoQuality(VideoFormat videoFormat, Pattern pattern) {
        this.format = videoFormat;
        this.repack = pattern;
    }

    @Override
    public int compare(File file, File file2) {
        File file3 = MediaFileUtilities.findPrimaryFile(file, MediaTypes.VIDEO_FILES).orElse(file);
        File file4 = MediaFileUtilities.findPrimaryFile(file2, MediaTypes.VIDEO_FILES).orElse(file2);
        MediaCharacteristics mediaCharacteristics = CachedMediaCharacteristics.getMediaCharacteristics(file3).orElse(null);
        MediaCharacteristics mediaCharacteristics2 = CachedMediaCharacteristics.getMediaCharacteristics(file4).orElse(null);
        if (mediaCharacteristics != null && mediaCharacteristics2 != null) {
            return this.quality.compare(mediaCharacteristics, mediaCharacteristics2);
        }
        return MediaFileUtilities.FILE_SIZE_ORDER.compare(file3, file4);
    }

    private int getRepackScore(MediaCharacteristics mediaCharacteristics) {
        return StringUtilities.find(mediaCharacteristics.getFileName(), this.repack) ? 1 : 0;
    }

    private int getFormatScore(MediaCharacteristics mediaCharacteristics) {
        Integer n = mediaCharacteristics.getWidth();
        Integer n2 = mediaCharacteristics.getHeight();
        if (n != null && n2 != null) {
            try {
                return this.format.guessFormat(n, n2);
            }
            catch (Exception exception) {
                Logging.debug.severe(exception::getMessage);
            }
        }
        return 0;
    }

    private double getBitrateScore(MediaCharacteristics mediaCharacteristics) {
        Integer n = mediaCharacteristics.getWidth();
        Integer n2 = mediaCharacteristics.getHeight();
        Double d = mediaCharacteristics.getBitRate();
        if (n != null && n2 != null && d != null) {
            return (double)(n * n2) * d;
        }
        return 0.0;
    }

    public static enum VideoCodec {
        MPEG,
        AVC,
        HEVC,
        AV1;


        public static VideoCodec get(MediaCharacteristics mediaCharacteristics) {
            String string = mediaCharacteristics.getVideoCodec();
            if (string != null) {
                if (string.contains("AV1") || string.contains("av1")) {
                    return AV1;
                }
                if (string.contains("HEVC") || string.contains("hevc")) {
                    return HEVC;
                }
                if (string.contains("AVC") || string.contains("h264")) {
                    return AVC;
                }
                if (string.contains("MPEG") || string.contains("mpeg")) {
                    return MPEG;
                }
            }
            throw new IllegalArgumentException("Unknown Video Codec: " + string);
        }

        public static int compare(MediaCharacteristics mediaCharacteristics, MediaCharacteristics mediaCharacteristics2) {
            try {
                VideoCodec videoCodec = VideoCodec.get(mediaCharacteristics);
                VideoCodec videoCodec2 = VideoCodec.get(mediaCharacteristics2);
                return videoCodec.compareTo(videoCodec2);
            }
            catch (Exception exception) {
                Logging.debug.severe(exception::getMessage);
                return 0;
            }
        }
    }
}

