package net.filemaid.web;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import net.filemaid.web.Datasource;
import net.filemaid.web.SubtitleDescriptor;
import net.filemaid.web.SubtitleSearchResult;

public interface SubtitleProvider
extends Datasource {
    public List<SubtitleSearchResult> search(String var1) throws Exception;

    public List<SubtitleSearchResult> guess(String var1) throws Exception;

    public List<SubtitleDescriptor> getSubtitleList(SubtitleSearchResult var1, int[][] var2, Locale var3) throws Exception;

    public URI getSubtitleListLink(SubtitleSearchResult var1, Locale var2);

    public URI getLink();

    public boolean requireLogin();

    public List<SubtitleSearchResult> getIndex() throws Exception;
}

