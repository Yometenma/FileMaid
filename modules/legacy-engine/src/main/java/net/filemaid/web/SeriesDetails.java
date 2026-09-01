package net.filemaid.web;

import java.io.Serializable;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import net.filemaid.web.Datasource;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.SortOrder;
import net.filemaid.web.WebRequest;

public class SeriesDetails
extends SeriesInfo
implements Serializable {
    protected String overview;
    protected String poster;
    protected String originalName;
    protected String originalLanguage;
    protected String[] country;
    protected int[] seasons;
    protected Double popularity;
    protected SimpleDate endDate;
    protected Map<String, String> certifications;
    protected String[] keywords;
    protected String slug;
    protected Integer imdbId;
    protected String[] airsDays;
    protected String airsTime;
    protected Instant lastUpdated;

    public SeriesDetails() {
    }

    public SeriesDetails(SeriesDetails seriesDetails) {
        super(seriesDetails);
        this.overview = seriesDetails.overview;
        this.poster = seriesDetails.poster;
        this.originalName = seriesDetails.originalName;
        this.originalLanguage = seriesDetails.originalLanguage;
        this.country = seriesDetails.country;
        this.seasons = seriesDetails.seasons;
        this.popularity = seriesDetails.popularity;
        this.endDate = seriesDetails.endDate;
        this.certifications = seriesDetails.certifications;
        this.slug = seriesDetails.slug;
        this.imdbId = seriesDetails.imdbId;
        this.overview = seriesDetails.overview;
        this.airsDays = seriesDetails.airsDays;
        this.airsTime = seriesDetails.airsTime;
        this.lastUpdated = seriesDetails.lastUpdated;
    }

    public SeriesDetails(Datasource datasource, SortOrder sortOrder, Locale locale, Integer n, String string) {
        super(datasource, sortOrder, locale, n, string);
    }

    public String getOverview() {
        return this.overview;
    }

    public void setOverview(String string) {
        this.overview = string;
    }

    public void setPoster(URL uRL) {
        this.poster = uRL == null ? null : uRL.toExternalForm();
    }

    public URL getPoster() {
        return WebRequest.parseURL(this.poster);
    }

    public String getOriginalName() {
        return this.originalName;
    }

    public void setOriginalName(String string) {
        this.originalName = string;
    }

    public String getOriginalLanguage() {
        return this.originalLanguage;
    }

    public void setOriginalLanguage(String string) {
        this.originalLanguage = string;
    }

    public List<String> getCountry() {
        return this.country == null || this.country.length == 0 ? Collections.emptyList() : Collections.unmodifiableList(Arrays.asList(this.country));
    }

    public void setCountry(String[] stringArray) {
        this.country = stringArray;
    }

    public int[] getSeasons() {
        return (int[])this.seasons.clone();
    }

    public void setSeasons(int[] nArray) {
        this.seasons = nArray;
    }

    public Double getPopularity() {
        return this.popularity;
    }

    public void setPopularity(Double d) {
        this.popularity = d;
    }

    public SimpleDate getEndDate() {
        return this.endDate;
    }

    public void setEndDate(SimpleDate simpleDate) {
        this.endDate = simpleDate;
    }

    public Map<String, String> getCertifications() {
        return this.certifications == null || this.certifications.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(this.certifications);
    }

    public void setCertifications(Map<String, String> map) {
        this.certifications = map;
    }

    public List<String> getKeywords() {
        return this.keywords == null || this.keywords.length == 0 ? Collections.emptyList() : Collections.unmodifiableList(Arrays.asList(this.keywords));
    }

    public void setKeywords(String[] stringArray) {
        this.keywords = stringArray;
    }

    public String getSlug() {
        return this.slug;
    }

    public void setSlug(String string) {
        this.slug = string;
    }

    public Integer getImdbId() {
        return this.imdbId;
    }

    public void setImdbId(Integer n) {
        this.imdbId = n;
    }

    public List<String> getAirsDays() {
        return this.airsDays == null || this.airsDays.length == 0 ? Collections.emptyList() : Collections.unmodifiableList(Arrays.asList(this.airsDays));
    }

    public void setAirsDays(String[] stringArray) {
        this.airsDays = stringArray;
    }

    public String getAirsTime() {
        return this.airsTime;
    }

    public void setAirsTime(String string) {
        this.airsTime = string;
    }

    public Instant getLastUpdated() {
        return this.lastUpdated;
    }

    public void setLastUpdated(Instant instant) {
        this.lastUpdated = instant;
    }

    public boolean hasEnded() {
        return "Ended".equalsIgnoreCase(this.status);
    }

    public boolean isContinuing() {
        return "Continuing".equalsIgnoreCase(this.status);
    }

    public boolean isRecent() {
        if (this.endDate != null && this.endDate.toLocalDate().until(LocalDate.now()).toTotalMonths() < 2L) {
            return true;
        }
        return this.startDate != null && this.startDate.toLocalDate().plusYears(this.seasons == null ? 0L : (long)(this.seasons.length - 1)).until(LocalDate.now()).toTotalMonths() < 6L;
    }

    public boolean isAiringToday() {
        if (this.airsDays != null && Arrays.stream(this.airsDays).anyMatch("Weekdays"::equalsIgnoreCase)) {
            return true;
        }
        return this.airsDays != null && Stream.of(LocalDate.now(), LocalDate.now().minusDays(1L)).anyMatch(localDate -> Arrays.stream(this.airsDays).anyMatch(localDate.getDayOfWeek().name()::equalsIgnoreCase));
    }

    @Override
    public SeriesDetails clone() {
        return new SeriesDetails(this);
    }
}

