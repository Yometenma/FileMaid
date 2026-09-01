package net.filemaid.web;

import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.ResourceManager;
import net.filemaid.util.JsonUtilities;
import net.filemaid.web.AbstractEpisodeListProvider;
import net.filemaid.web.Episode;
import net.filemaid.web.Movie;
import net.filemaid.web.SearchResult;
import net.filemaid.web.Series;
import net.filemaid.web.SeriesInfo;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.SortOrder;
import net.filemaid.web.WebRequest;
import net.filemaid.web.XDB;

public class TVMazeClient
extends AbstractEpisodeListProvider {
    @Override
    public String getIdentifier() {
        return "TVmaze";
    }

    @Override
    public Icon getIcon() {
        return ResourceManager.getIcon("search.tvmaze");
    }

    @Override
    public boolean hasSeasonSupport() {
        return true;
    }

    @Override
    public SortOrder vetoRequestParameter(SortOrder sortOrder) {
        return SortOrder.Airdate;
    }

    @Override
    public Locale vetoRequestParameter(Locale locale) {
        return Locale.ENGLISH;
    }

    @Override
    protected String getLanguageCode(Locale locale) {
        return Locale.ENGLISH.getLanguage();
    }

    @Override
    public SearchResult id(Series series) {
        Integer n = series.getExternalId(XDB.TVmaze);
        return n == null ? null : new SearchResult((int)n, series.getName(), series.getAliasNames());
    }

    @Override
    public List<SearchResult> fetchSearchResult(String string, Locale locale) throws Exception {
        Movie movie;
        Object object = this.request("search/shows?" + WebRequest.encodeParameters(Collections.singletonMap("q", string)));
        List<SearchResult> list = JsonUtilities.streamJsonObjects(object).map(map -> {
            Integer id = JsonUtilities.getInteger(map, "id");
            String name = JsonUtilities.getString(map, "name");
            SimpleDate premiereDate = JsonUtilities.getStringValue(map, "premiereDate", SimpleDate::parse);
            if (premiereDate != null) {
                name = lambda$fetchSearchResult$3(name, premiereDate);
            }
            return new SearchResult(id, name);
        }).collect(Collectors.toList());
        if (list.isEmpty() && (movie = Movie.matchNameYear(string)) != null) {
            list = this.fetchSearchResult(movie.getName(), locale);
        }
        return list;
    }

    /*
     * Exception decompiling
     */
    protected SeriesInfo fetchSeriesInfo(SearchResult var1_1, SortOrder var2_2, Locale var3_3) throws Exception {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * java.lang.UnsupportedOperationException
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.NewAnonymousArray.getDimSize(NewAnonymousArray.java:142)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.isNewArrayLambda(LambdaRewriter.java:455)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteDynamicExpression(LambdaRewriter.java:409)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteDynamicExpression(LambdaRewriter.java:167)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:105)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper.applyForwards(ExpressionRewriterHelper.java:12)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation.applyExpressionRewriterToArgs(StaticFunctionInvokation.java:103)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.StaticFunctionInvokation.applyExpressionRewriter(StaticFunctionInvokation.java:90)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:103)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper.applyForwards(ExpressionRewriterHelper.java:12)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriterToArgs(AbstractMemberFunctionInvokation.java:101)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriter(AbstractMemberFunctionInvokation.java:88)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:103)
         *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredExpressionStatement.rewriteExpressions(StructuredExpressionStatement.java:70)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewrite(LambdaRewriter.java:88)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.rewriteLambdas(Op04StructuredStatement.java:1137)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:912)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    @Override
    protected AbstractEpisodeListProvider.SeriesData fetchSeriesData(SearchResult searchResult, SortOrder sortOrder, Locale locale) throws Exception {
        SeriesInfo seriesInfo = this.fetchSeriesInfo(searchResult, sortOrder, locale);
        Object object = this.request("shows/" + seriesInfo.getId() + "/episodes");
        List<Episode> list = JsonUtilities.streamJsonObjects(object).map(map -> {
            Integer n = JsonUtilities.getInteger(map, "id");
            Integer n2 = JsonUtilities.getInteger(map, "season");
            Integer n3 = JsonUtilities.getInteger(map, "number");
            String string = JsonUtilities.getString(map, "name");
            SimpleDate simpleDate = JsonUtilities.getStringValue(map, "airdate", SimpleDate::parse);
            Integer n4 = JsonUtilities.getInteger(map, "runtime");
            return new Episode(seriesInfo.getName(), n2, n3, string, null, null, simpleDate, n4, n, null, new SeriesInfo(seriesInfo));
        }).collect(Collectors.toList());
        return new AbstractEpisodeListProvider.SeriesData(seriesInfo, list);
    }

    protected Object request(String string) throws Exception {
        Cache cache = Cache.getConcurrentCache(this.getName(), CacheType.Monthly);
        return cache.json(string, this::getResource).expire(Cache.ONE_WEEK).get();
    }

    protected URL getResource(String string) throws Exception {
        return WebRequest.newURL("https://api.tvmaze.com/" + string);
    }

    @Override
    public URI getEpisodeListLink(SearchResult searchResult) {
        return URI.create("https://www.tvmaze.com/shows/" + searchResult.getId());
    }

    private static /* synthetic */ Instant lambda$fetchSeriesInfo$9(String string) {
        return Instant.ofEpochSecond(Long.parseLong(string));
    }

    private static /* synthetic */ URL lambda$fetchSeriesInfo$8(String string) {
        return WebRequest.parseURL(string);
    }

    private static /* synthetic */ String[] lambda$fetchSearchResult$4(int n) {
        return new String[n];
    }

    private static /* synthetic */ String lambda$fetchSearchResult$3(String string, SimpleDate simpleDate) {
        return string + " (" + simpleDate.year + ")";
    }
}

