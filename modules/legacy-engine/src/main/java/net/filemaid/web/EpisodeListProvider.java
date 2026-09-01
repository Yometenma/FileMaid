package net.filemaid.web;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeDetails;
import net.filemaid.web.SearchResult;
import net.filemaid.web.Series;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SortOrder;

public interface EpisodeListProvider
extends Datasource {
    public boolean hasSeasonSupport();

    public List<SearchResult> search(String var1, Locale var2) throws Exception;

    public List<SearchResult> lookup(String var1, Locale var2) throws Exception;

    public SearchResult id(String var1) throws Exception;

    public SearchResult id(Series var1) throws Exception;

    public List<Episode> getEpisodeList(SearchResult var1, SortOrder var2, Locale var3) throws Exception;

    public List<Episode> getEpisodeList(int var1, SortOrder var2, Locale var3) throws Exception;

    public SeriesInfo getSeriesInfo(SearchResult var1, Locale var2) throws Exception;

    public SeriesInfo getSeriesInfo(int var1, Locale var2) throws Exception;

    public EpisodeDetails getEpisodeInfo(Episode var1, Locale var2) throws Exception;

    public List<? extends SearchResult> getIndex() throws Exception;

    public URI getEpisodeListLink(SearchResult var1);

    public SortOrder vetoRequestParameter(SortOrder var1);

    public Locale vetoRequestParameter(Locale var1);
}

