package net.filemaid.web;

import java.io.Serializable;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SeriesDetails;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SimpleDate;

public class SearchResultDetails
extends SearchResult
implements Serializable {
    protected SimpleDate firstAired;
    protected String overview;
    protected String originalName;
    protected String originalLanguage;
    protected String[] country;
    protected Double popularity;
    protected String slug;
    protected String network;
    protected String status;

    public SearchResultDetails() {
    }

    public SearchResultDetails(SearchResult searchResult, SearchResultDetails searchResultDetails) {
        super(searchResult);
        this.firstAired = searchResultDetails.firstAired;
        this.overview = searchResultDetails.overview;
        this.originalName = searchResultDetails.originalName;
        this.originalLanguage = searchResultDetails.originalLanguage;
        this.country = searchResultDetails.country;
        this.popularity = searchResultDetails.popularity;
        this.slug = searchResultDetails.slug;
        this.network = searchResultDetails.network;
        this.status = searchResultDetails.status;
    }

    public SearchResultDetails(int n, String string, String[] stringArray, SimpleDate simpleDate, String string2, String string3, String string4, String[] stringArray2, Double d, String string5, String string6, String string7) {
        super(n, string, stringArray);
        this.firstAired = simpleDate;
        this.overview = string2;
        this.originalName = string3;
        this.originalLanguage = string4;
        this.country = stringArray2;
        this.popularity = d;
        this.slug = string5;
        this.network = string6;
        this.status = string7;
    }

    public SimpleDate getFirstAired() {
        return this.firstAired;
    }

    public String getOverview() {
        return this.overview;
    }

    public String getOriginalName() {
        return this.originalName;
    }

    public String getOriginalLanguage() {
        return this.originalLanguage;
    }

    public String[] getCountry() {
        return this.country;
    }

    public Double getPopularity() {
        return this.popularity;
    }

    public String getSlug() {
        return this.slug;
    }

    public String getNetwork() {
        return this.network;
    }

    public String getStatus() {
        return this.status;
    }

    @Override
    public SearchResultDetails clone() {
        return new SearchResultDetails(this, this);
    }

    public String getNameWithYear() {
        String string = this.toString();
        String string2 = this.getFirstAired() != null ? " (" + this.getFirstAired().getYear() + ")" : "";
        return string.endsWith(string2) ? string : string + string2;
    }

    public static SearchResultDetails from(SeriesInfo seriesInfo) {
        if (seriesInfo == null) {
            return null;
        }
        if (seriesInfo instanceof SeriesDetails) {
            SeriesDetails seriesDetails = (SeriesDetails)seriesInfo;
            return new SearchResultDetails(seriesInfo.getId(), seriesInfo.getName(), seriesInfo.getAliasNames().toArray(EMPTY_STRING_ARRAY), seriesInfo.getStartDate(), seriesDetails.getOverview(), seriesDetails.getOriginalName(), seriesDetails.getOriginalLanguage(), seriesDetails.getCountry().toArray(EMPTY_STRING_ARRAY), seriesDetails.getPopularity(), seriesDetails.getSlug(), seriesInfo.getNetwork(), seriesInfo.getStatus());
        }
        return new SearchResultDetails(seriesInfo.getId(), seriesInfo.getName(), seriesInfo.getAliasNames().toArray(EMPTY_STRING_ARRAY), seriesInfo.getStartDate(), null, null, null, null, null, null, seriesInfo.getNetwork(), seriesInfo.getStatus());
    }
}

