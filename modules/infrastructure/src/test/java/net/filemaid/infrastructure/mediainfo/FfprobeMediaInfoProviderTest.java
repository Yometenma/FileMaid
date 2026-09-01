package net.filemaid.infrastructure.mediainfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.filemaid.core.model.MediaInfo;
import org.junit.jupiter.api.Test;

class FfprobeMediaInfoProviderTest {

    private static final String SAMPLE_JSON = """
            {
              "streams": [
                { "codec_type": "video", "codec_name": "h264", "profile": "High",
                  "width": 1920, "height": 1080, "avg_frame_rate": "30000/1001" },
                { "codec_type": "audio", "codec_name": "aac", "tags": { "language": "jpn" } },
                { "codec_type": "subtitle", "codec_name": "ass", "tags": { "language": "chi" } }
              ],
              "format": {
                "filename": "D:/media/Show/Show - S01E01.mkv",
                "size": "123456789",
                "duration": "1440.5",
                "bit_rate": "685000",
                "tags": { "title": "Episode 1" }
              }
            }
            """;

    private final FfprobeMediaInfoProvider provider = new FfprobeMediaInfoProvider("ffprobe");

    @Test
    void parsesVideoAudioSubtitleAndFormat() throws Exception {
        MediaInfo info = provider.parse(SAMPLE_JSON, "Show - S01E01.mkv");

        assertEquals("Show - S01E01.mkv", info.fileName());
        assertEquals(123456789L, info.fileSize());
        assertEquals("h264", info.videoCodec());
        assertEquals("High", info.videoProfile());
        assertEquals(1920, info.width());
        assertEquals(1080, info.height());
        assertEquals("1080p", info.resolution());
        assertEquals(30000.0 / 1001.0, info.frameRate(), 1e-6);
        assertEquals("aac", info.audioCodec());
        assertEquals("jpn", info.audioLanguage());
        assertEquals("ass", info.subtitleCodec());
        assertEquals("chi", info.subtitleLanguage());
        assertEquals(1440.5, info.durationSeconds(), 1e-9);
        assertEquals(685000.0, info.bitRate(), 1e-9);
        assertEquals("Episode 1", info.title());
    }

    @Test
    void returnsNullsForMissingFields() throws Exception {
        MediaInfo info = provider.parse("{ \"streams\": [], \"format\": {} }", "x.mkv");
        assertNull(info.videoCodec());
        assertNull(info.resolution());
        assertNull(info.fileSize());
        assertNull(info.durationSeconds());
    }

    @Test
    void handlesFrameRateAsPlainNumber() throws Exception {
        MediaInfo info = provider.parse(
                "{ \"streams\": [ { \"codec_type\": \"video\", \"avg_frame_rate\": \"25\" } ], "
                        + "\"format\": { \"filename\": \"x.mkv\" } }",
                "x.mkv");
        assertEquals(25.0, info.frameRate(), 1e-9);
    }

    @Test
    void resolutionFallsBackToRawHeight() throws Exception {
        MediaInfo info = provider.parse(
                "{ \"streams\": [ { \"codec_type\": \"video\", \"height\": 300 } ], \"format\": {} }",
                "x.mkv");
        assertEquals("300p", info.resolution());
    }
}
