package net.filemaid.web;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import net.filemaid.web.OpenSubtitlesRestClient;
import net.filemaid.web.SubtitleDescriptor;

public class OpenSubtitlesRestSubtitleDescriptor
implements SubtitleDescriptor {
    private final Map<SubtitleProperty, Object> attributes;
    private final Map<FeatureProperty, Object> featureDetails;
    private final int file_id;
    private final int cd_number;
    private final String file_name;
    private final OpenSubtitlesRestClient client;

    public OpenSubtitlesRestSubtitleDescriptor(Map<SubtitleProperty, Object> map, Map<FeatureProperty, Object> map2, int n, int n2, String string, OpenSubtitlesRestClient openSubtitlesRestClient) {
        this.attributes = map;
        this.featureDetails = map2;
        this.file_id = n;
        this.cd_number = n2;
        this.file_name = string;
        this.client = openSubtitlesRestClient;
    }

    public Map<SubtitleProperty, Object> getAttributes() {
        return this.attributes;
    }

    public Map<FeatureProperty, Object> getFeatureDetails() {
        return this.featureDetails;
    }

    @Override
    public String getPath() {
        StringBuilder stringBuilder = new StringBuilder(64);
        Object object = this.featureDetails.get((Object)FeatureProperty.movie_name);
        if (object instanceof String) {
            stringBuilder.append(object);
            stringBuilder.append('/');
        }
        Object object2 = this.featureDetails.get((Object)FeatureProperty.season_number);
        Object object3 = this.featureDetails.get((Object)FeatureProperty.episode_number);
        if (object2 instanceof Number && object3 instanceof Number) {
            stringBuilder.append(String.format(Locale.ROOT, "S%02dE%02d", object2, object3));
            stringBuilder.append('/');
        }
        stringBuilder.append(this.file_name);
        if (this.cd_number > 1) {
            stringBuilder.append(".CD").append(this.cd_number);
        }
        stringBuilder.append(".").append(this.attributes.get((Object)SubtitleProperty.language));
        if (this.isHI()) {
            stringBuilder.append("-HI");
        }
        stringBuilder.append(".").append(this.getType());
        return stringBuilder.toString();
    }

    @Override
    public String getName() {
        return this.file_name;
    }

    @Override
    public String getLanguageName() {
        return (String)this.attributes.get((Object)SubtitleProperty.language);
    }

    @Override
    public boolean isForced() {
        return Boolean.TRUE.equals(this.attributes.get((Object)SubtitleProperty.foreign_parts_only));
    }

    @Override
    public boolean isHI() {
        return Boolean.TRUE.equals(this.attributes.get((Object)SubtitleProperty.hearing_impaired));
    }

    @Override
    public String getType() {
        return "srt";
    }

    @Override
    public long getLength() {
        return -1L;
    }

    public float getFPS() {
        Number number = (Number)this.attributes.get((Object)SubtitleProperty.fps);
        if (number != null) {
            return number.floatValue();
        }
        return -1.0f;
    }

    @Override
    public ByteBuffer fetch() throws Exception {
        return this.client.download(this.file_id, this.getType());
    }

    public int hashCode() {
        return this.file_id;
    }

    public boolean equals(Object object) {
        if (object instanceof OpenSubtitlesRestSubtitleDescriptor) {
            OpenSubtitlesRestSubtitleDescriptor openSubtitlesRestSubtitleDescriptor = (OpenSubtitlesRestSubtitleDescriptor)object;
            return this.file_id == openSubtitlesRestSubtitleDescriptor.file_id;
        }
        return false;
    }

    public String toString() {
        return this.file_name;
    }

    @Override
    public File toFile() {
        return new File(this.getPath());
    }

    public static enum FeatureProperty {
        feature_id,
        feature_type,
        year,
        title,
        movie_name,
        imdb_id,
        tmdb_id,
        season_number,
        episode_number,
        parent_imdb_id,
        parent_title,
        parent_tmdb_id,
        parent_feature_id;

    }

    public static enum SubtitleProperty {
        subtitle_id,
        language,
        download_count,
        new_download_count,
        hearing_impaired,
        hd,
        fps,
        votes,
        ratings,
        from_trusted,
        foreign_parts_only,
        upload_date,
        ai_translated,
        machine_translated,
        release,
        comments,
        legacy_subtitle_id,
        legacy_uploader_id,
        url,
        moviehash_match;

    }
}

