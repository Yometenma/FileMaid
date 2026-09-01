package net.filemaid.media;

import java.time.Duration;
import java.time.Instant;

public interface MediaCharacteristics
extends AutoCloseable {
    public String getFileName();

    public Long getFileSize();

    public String getVideoCodec();

    public String getVideoProfile();

    public String getAudioCodec();

    public String getAudioLanguage();

    public String getSubtitleCodec();

    public String getSubtitleLanguage();

    public Duration getDuration();

    public Integer getWidth();

    public Integer getHeight();

    public Double getBitRate();

    public Double getFrameRate();

    public String getTitle();

    public Instant getCreationTime();

    public Object getMediaTags();
}

