package net.filemaid.web;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.filemaid.web.Datasource;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.SortOrder;

public class SeriesInfo
implements Serializable {
    public static final String TYPE_SERIES = "TV Series";
    public static final String TYPE_ANIME = "Anime";
    protected String database;
    protected String order;
    protected String language;
    protected String type;
    protected Integer id;
    protected String name;
    protected String[] aliasNames;
    protected String certification;
    protected SimpleDate startDate;
    protected String[] genres;
    protected String[] spokenLanguages;
    protected String network;
    protected Double rating;
    protected Integer ratingCount;
    protected Integer runtime;
    protected String status;

    public SeriesInfo() {
    }

    public SeriesInfo(SeriesInfo seriesInfo) {
        this.database = seriesInfo.database;
        this.order = seriesInfo.order;
        this.language = seriesInfo.language;
        this.type = seriesInfo.type;
        this.id = seriesInfo.id;
        this.name = seriesInfo.name;
        this.aliasNames = seriesInfo.aliasNames == null ? null : (String[])seriesInfo.aliasNames.clone();
        this.certification = seriesInfo.certification;
        this.startDate = seriesInfo.startDate == null ? null : seriesInfo.startDate.clone();
        this.genres = seriesInfo.genres == null ? null : (String[])seriesInfo.genres.clone();
        this.spokenLanguages = seriesInfo.spokenLanguages == null ? null : (String[])seriesInfo.spokenLanguages.clone();
        this.network = seriesInfo.network;
        this.rating = seriesInfo.rating;
        this.ratingCount = seriesInfo.ratingCount;
        this.runtime = seriesInfo.runtime;
        this.status = seriesInfo.status;
    }

    public SeriesInfo(Datasource datasource, SortOrder sortOrder, Locale locale, Integer n, String string) {
        this.database = datasource == null ? null : datasource.getIdentifier();
        this.order = sortOrder == null ? null : sortOrder.name();
        this.language = locale == null ? null : locale.toLanguageTag();
        this.id = n;
        this.type = string;
    }

    public void setDatabase(String string) {
        this.database = string;
    }

    public String getDatabase() {
        return this.database;
    }

    public void setOrder(String string) {
        this.order = string;
    }

    public String getOrder() {
        return this.order;
    }

    public Locale getLanguage() {
        return this.language == null ? null : Locale.forLanguageTag(this.language);
    }

    public void setLanguage(String string) {
        this.language = string;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String string) {
        this.type = string;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer n) {
        this.id = n;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String string) {
        this.name = string;
    }

    public List<String> getAliasNames() {
        return this.aliasNames == null ? Collections.emptyList() : Collections.unmodifiableList(Arrays.asList(this.aliasNames));
    }

    public void setAliasNames(String ... stringArray) {
        this.aliasNames = (String[])stringArray.clone();
    }

    public String getCertification() {
        return this.certification;
    }

    public void setCertification(String string) {
        this.certification = string;
    }

    public SimpleDate getStartDate() {
        return this.startDate;
    }

    public void setStartDate(SimpleDate simpleDate) {
        this.startDate = simpleDate;
    }

    public List<String> getGenres() {
        return this.genres == null ? Collections.emptyList() : Collections.unmodifiableList(Arrays.asList(this.genres));
    }

    public void setGenres(String ... stringArray) {
        this.genres = (String[])stringArray.clone();
    }

    public List<String> getSpokenLanguages() {
        return this.spokenLanguages == null ? Collections.emptyList() : Collections.unmodifiableList(Arrays.asList(this.spokenLanguages));
    }

    public void setSpokenLanguages(String ... stringArray) {
        this.spokenLanguages = (String[])stringArray.clone();
    }

    public String getNetwork() {
        return this.network;
    }

    public void setNetwork(String string) {
        this.network = string;
    }

    public Double getRating() {
        return this.rating;
    }

    public void setRating(Double d) {
        this.rating = d;
    }

    public Integer getRatingCount() {
        return this.ratingCount;
    }

    public void setRatingCount(Integer n) {
        this.ratingCount = n;
    }

    public Integer getRuntime() {
        return this.runtime;
    }

    public void setRuntime(Integer n) {
        this.runtime = n;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String string) {
        this.status = string;
    }

    public int hashCode() {
        return this.id == null ? 0 : this.id;
    }

    public boolean equals(Object object) {
        if (object instanceof SeriesInfo) {
            SeriesInfo seriesInfo = (SeriesInfo)object;
            return Objects.equals(this.id, seriesInfo.id) && Objects.equals(this.database, seriesInfo.database);
        }
        return false;
    }

    public SeriesInfo clone() {
        return new SeriesInfo(this);
    }

    public String toString() {
        return this.database + "::" + this.id;
    }
}

