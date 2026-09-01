package net.filemaid.web;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.web.SimpleDate;

public class AudioTrack
implements Serializable {
    protected String database;
    protected String artist;
    protected String title;
    protected String album;
    protected String albumArtist;
    protected String trackTitle;
    protected String genre;
    protected SimpleDate albumReleaseDate;
    protected Integer mediumIndex;
    protected Integer mediumCount;
    protected Integer trackIndex;
    protected Integer trackCount;
    protected String mbid;

    public AudioTrack() {
    }

    public AudioTrack(AudioTrack audioTrack) {
        this.artist = audioTrack.artist;
        this.title = audioTrack.title;
        this.album = audioTrack.album;
        this.albumArtist = audioTrack.albumArtist;
        this.trackTitle = audioTrack.trackTitle;
        this.genre = audioTrack.genre;
        this.albumReleaseDate = audioTrack.albumReleaseDate;
        this.mediumIndex = audioTrack.mediumIndex;
        this.mediumCount = audioTrack.mediumCount;
        this.trackIndex = audioTrack.trackIndex;
        this.trackCount = audioTrack.trackCount;
        this.mbid = audioTrack.mbid;
        this.database = audioTrack.database;
    }

    public AudioTrack(String string, String string2, String string3, String string4) {
        this.artist = string;
        this.title = string2;
        this.album = string3;
        this.database = string4;
    }

    public AudioTrack(String string, String string2, String string3, String string4, String string5, String string6, SimpleDate simpleDate, Integer n, Integer n2, Integer n3, Integer n4, String string7, String string8) {
        this.artist = string;
        this.title = string2;
        this.album = string3;
        this.albumArtist = string4;
        this.trackTitle = string5;
        this.genre = string6;
        this.albumReleaseDate = simpleDate;
        this.mediumIndex = n;
        this.mediumCount = n2;
        this.trackIndex = n3;
        this.trackCount = n4;
        this.mbid = string7;
        this.database = string8;
    }

    public String getArtist() {
        return this.artist;
    }

    public String getTitle() {
        return this.title;
    }

    public String getAlbum() {
        return this.album;
    }

    public String getAlbumArtist() {
        return this.albumArtist;
    }

    public String getTrackTitle() {
        return this.trackTitle;
    }

    public String getGenre() {
        return this.genre;
    }

    public SimpleDate getAlbumReleaseDate() {
        return this.albumReleaseDate;
    }

    public Integer getMedium() {
        return this.mediumIndex;
    }

    public Integer getMediumCount() {
        return this.mediumCount;
    }

    public Integer getTrack() {
        return this.trackIndex;
    }

    public Integer getTrackCount() {
        return this.trackCount;
    }

    public String getMBID() {
        return this.mbid;
    }

    public String getDatabase() {
        return this.database;
    }

    public AudioTrack clone() {
        return new AudioTrack(this);
    }

    public String toString() {
        return Stream.of(this.album, this.artist, this.title).filter(Objects::nonNull).map(Objects::toString).collect(Collectors.joining(" - "));
    }

    public int hashCode() {
        return Objects.hash(this.mbid, this.album, this.artist, this.title);
    }

    public boolean equals(Object object) {
        if (object instanceof AudioTrack) {
            AudioTrack audioTrack = (AudioTrack)object;
            return Objects.equals(this.mbid, audioTrack.mbid) && Objects.equals(this.album, audioTrack.album) && Objects.equals(this.artist, audioTrack.artist) && Objects.equals(this.title, audioTrack.title);
        }
        return super.equals(object);
    }
}

