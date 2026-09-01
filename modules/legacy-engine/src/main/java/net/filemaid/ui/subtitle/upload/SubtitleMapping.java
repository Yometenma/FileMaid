package net.filemaid.ui.subtitle.upload;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import net.filemaid.Language;
import net.filemaid.ui.subtitle.upload.Status;
import net.filemaid.util.ui.AbstractBean;
import net.filemaid.web.Movie;

class SubtitleMapping
extends AbstractBean {
    private Movie identity;
    private File video;
    private File subtitle;
    private Language language;
    private Status status;

    public SubtitleMapping(File file, File file2, Language language) {
        this.subtitle = file;
        this.video = file2;
        this.language = language;
        this.status = file2 == null || file == null ? Status.IllegalInput : Status.CheckPending;
    }

    public boolean isCheckReady() {
        return this.subtitle != null && this.status == Status.CheckPending;
    }

    public boolean isUploadReady() {
        return this.identity != null && this.subtitle != null && this.video != null && this.language != null && this.status == Status.UploadReady;
    }

    public boolean isGroupReady() {
        return this.identity != null && this.language != null;
    }

    public Object getGroup() {
        return Arrays.asList(this.identity.getId(), this.language.getCode());
    }

    public Object getIdentity() {
        return this.identity;
    }

    public File getSubtitle() {
        return this.subtitle;
    }

    public File getVideo() {
        return this.video;
    }

    public Language getLanguage() {
        return this.language;
    }

    public Status getStatus() {
        return this.status;
    }

    public void setVideo(File file) {
        this.video = file;
        this.firePropertyChange("video", null, this.video);
    }

    public void setIdentity(Movie movie) {
        this.identity = movie;
        this.firePropertyChange("identity", null, this.identity);
    }

    public void setLanguage(Language language) {
        this.language = language;
        this.firePropertyChange("language", null, this.language);
    }

    public void setState(Status status) {
        this.status = status;
        this.firePropertyChange("status", null, (Object)this.status);
    }

    public String toString() {
        return Arrays.asList(new Serializable[]{this.identity, this.video, this.subtitle, this.language, this.status}).toString();
    }
}

