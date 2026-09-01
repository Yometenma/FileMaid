package net.filemaid.ui.subtitle.upload;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import net.filemaid.Language;
import net.filemaid.ui.subtitle.upload.Status;
import net.filemaid.ui.subtitle.upload.SubtitleMapping;

class SubtitleGroup {
    private final SubtitleMapping[] mapping;

    public SubtitleGroup(List<SubtitleMapping> list) {
        this.mapping = list.toArray(new SubtitleMapping[list.size()]);
    }

    public void setState(Status status) {
        for (SubtitleMapping subtitleMapping : this.mapping) {
            subtitleMapping.setState(status);
        }
    }

    public boolean isUploadReady() {
        return Arrays.stream(this.mapping).allMatch(SubtitleMapping::isUploadReady);
    }

    public Object getIdentity() {
        return this.mapping[0].getIdentity();
    }

    public Language getLanguage() {
        return this.mapping[0].getLanguage();
    }

    public File[] getVideoFiles() {
        return (File[])Arrays.stream(this.mapping).map(SubtitleMapping::getVideo).toArray(File[]::new);
    }

    public File[] getSubtitleFiles() {
        return (File[])Arrays.stream(this.mapping).map(SubtitleMapping::getSubtitle).toArray(File[]::new);
    }

    public String toString() {
        return Arrays.asList(this.getIdentity(), this.getLanguage(), Arrays.asList(this.getVideoFiles()), Arrays.asList(this.getSubtitleFiles())).toString();
    }
}

