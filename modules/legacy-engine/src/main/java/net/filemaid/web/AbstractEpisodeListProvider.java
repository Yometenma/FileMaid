package net.filemaid.web;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.util.RegularExpressions;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeDetails;
import net.filemaid.web.EpisodeListProvider;
import net.filemaid.web.SearchResult;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SortOrder;

public abstract class AbstractEpisodeListProvider
implements EpisodeListProvider {
    protected abstract List<SearchResult> fetchSearchResult(String var1, Locale var2) throws Exception;

    protected abstract SeriesData fetchSeriesData(SearchResult var1, SortOrder var2, Locale var3) throws Exception;

    @Override
    public List<SearchResult> search(String string, Locale locale) throws Exception {
        return this.getSearchCache(locale).computeIfAbsent(string.toLowerCase(locale), element -> this.fetchSearchResult(string, locale));
    }

    @Override
    public List<SearchResult> lookup(String string, Locale locale) throws Exception {
        if (string.startsWith("\"") && string.endsWith("\"")) {
            return this.search(string.substring(1, string.length() - 1), locale);
        }
        SearchResult searchResult = this.id(string);
        if (searchResult != null) {
            return Collections.singletonList(searchResult);
        }
        return this.search(string, locale);
    }

    @Override
    public SearchResult id(String string) throws Exception {
        if (RegularExpressions.DIGIT.matcher(string).matches()) {
            return new SearchResult(Integer.parseInt(string));
        }
        return null;
    }

    @Override
    public SortOrder vetoRequestParameter(SortOrder sortOrder) {
        return sortOrder == null ? SortOrder.Airdate : sortOrder;
    }

    @Override
    public Locale vetoRequestParameter(Locale locale) {
        return this.getLanguageCode(locale) == null ? Locale.ENGLISH : locale;
    }

    @Override
    public List<Episode> getEpisodeList(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception {
        return this.getSeriesData(searchResult, sortOrder, locale).getEpisodeList();
    }

    @Override
    public List<Episode> getEpisodeList(int n, SortOrder sortOrder, Locale locale) throws Exception {
        return this.getEpisodeList(new SearchResult(n), sortOrder, locale);
    }

    @Override
    public SeriesInfo getSeriesInfo(SearchResult searchResult, Locale locale) throws Exception {
        return this.getSeriesData(searchResult, null, locale).getSeriesInfo();
    }

    @Override
    public SeriesInfo getSeriesInfo(int n, Locale locale) throws Exception {
        return this.getSeriesInfo(new SearchResult(n), locale);
    }

    @Override
    public EpisodeDetails getEpisodeInfo(Episode episode, Locale locale) throws Exception {
        return null;
    }

    @Override
    public List<? extends SearchResult> getIndex() throws Exception {
        return Collections.emptyList();
    }

    protected SeriesData getSeriesData(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception {
        SortOrder sortOrder2 = this.vetoRequestParameter(sortOrder);
        Locale locale2 = this.vetoRequestParameter(locale);
        return this.getDataCache(sortOrder2, locale2).computeIfAbsent(searchResult.getId(), element -> this.fetchSeriesData(searchResult, sortOrder2, locale2));
    }

    protected String getLanguageCode(Locale locale) {
        String string;
        if (locale == null) {
            return null;
        }
        switch (string = locale.getLanguage()) {
            case "iw": {
                return "he";
            }
            case "in": {
                return "id";
            }
            case "": {
                return null;
            }
        }
        return string;
    }

    protected Cache getCache(String string) {
        return Cache.getCache(this.getName() + "_" + string, CacheType.Daily);
    }

    protected Cache.TypedCache<List<SearchResult>> getSearchCache(Locale locale) {
        return this.getCache("search_" + this.getLanguageCode(locale)).castList(SearchResult.class);
    }

    protected Cache.TypedCache<SeriesData> getDataCache(SortOrder sortOrder, Locale locale) {
        return this.getCache("data_" + sortOrder.ordinal() + "_" + this.getLanguageCode(locale)).cast(SeriesData.class);
    }

    protected static class SeriesData
    implements Serializable {
        public SeriesInfo seriesInfo;
        public Episode[] episodeList;

        public SeriesData() {
        }

        public SeriesData(SeriesInfo seriesInfo, List<Episode> list) {
            this.seriesInfo = seriesInfo;
            this.episodeList = list.toArray(new Episode[0]);
        }

        public SeriesInfo getSeriesInfo() {
            return this.seriesInfo.clone();
        }

        public List<Episode> getEpisodeList() {
            return Collections.unmodifiableList(Arrays.asList(this.episodeList));
        }
    }
}

