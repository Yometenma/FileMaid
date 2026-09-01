package net.filemaid.subtitle;

import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.ngram.NgramExtractor;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.BuiltInLanguages;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.InvalidInputException;
import net.filemaid.Language;
import net.filemaid.Logging;
import net.filemaid.MediaTypes;
import net.filemaid.Resource;
import net.filemaid.format.QueryExpression;
import net.filemaid.media.AutoDetection;
import net.filemaid.similarity.Match;
import net.filemaid.similarity.Matcher;
import net.filemaid.similarity.MetricAvg;
import net.filemaid.similarity.NameSimilarityMetric;
import net.filemaid.similarity.Normalization;
import net.filemaid.similarity.SequenceMatchSimilarity;
import net.filemaid.similarity.SimilarityComparator;
import net.filemaid.similarity.SimilarityMetric;
import net.filemaid.subtitle.SubRipWriter;
import net.filemaid.subtitle.SubtitleElement;
import net.filemaid.subtitle.SubtitleFormat;
import net.filemaid.subtitle.SubtitleMetrics;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.RegularExpressions;
import net.filemaid.vfs.ArchiveType;
import net.filemaid.vfs.FileInfo;
import net.filemaid.vfs.MemoryFile;
import net.filemaid.web.SubtitleDescriptor;
import net.filemaid.web.SubtitleLookupService;
import net.filemaid.web.SubtitleProvider;
import net.filemaid.web.SubtitleSearchResult;

public final class SubtitleUtilities {
    private static final Pattern MARKUP = Pattern.compile("[<]+[/]?[a-z]+[^>]*?[>]+|[{]+[\\\\][a-z][^}]*?[}]+|[{]+[a-z]+[:][a-z]+[}]+", 2);
    private static final Resource<LanguageDetectorBuilder> languageDetector = Resource.lazy(() -> LanguageDetectorBuilder.create((NgramExtractor)NgramExtractors.standard()).withProfiles((Iterable)new LanguageProfileReader().readBuiltIn((Collection)BuiltInLanguages.getLanguages())));

    public static Map<File, List<SubtitleDescriptor>> lookupSubtitlesByHash(SubtitleLookupService subtitleLookupService, Collection<File> collection, Locale locale, boolean bl, boolean bl2) throws Exception {
        Map<File, List<SubtitleDescriptor>> map = subtitleLookupService.getSubtitleList(collection.toArray(new File[0]), locale);
        LinkedHashMap<File, List<SubtitleDescriptor>> linkedHashMap = new LinkedHashMap<File, List<SubtitleDescriptor>>(map.size());
        map.forEach((file, list) -> {
            SubtitleDescriptor subtitleDescriptor = SubtitleUtilities.getBestMatch(file, list, bl2);
            if (subtitleDescriptor != null) {
                if (bl) {
                    Stream<SubtitleDescriptor> stream = Stream.of(subtitleDescriptor);
                    Stream<SubtitleDescriptor> stream2 = list.stream().filter(Predicate.isEqual(subtitleDescriptor).negate()).sorted(SimilarityComparator.compareTo(FileUtilities.getName(file), FileInfo::getName));
                    linkedHashMap.put((File)file, Stream.concat(stream, stream2).collect(Collectors.toList()));
                } else {
                    linkedHashMap.put((File)file, Collections.singletonList(subtitleDescriptor));
                }
            }
        });
        return linkedHashMap;
    }

    /*
     * Exception decompiling
     */
    public static Map<File, List<SubtitleDescriptor>> findSubtitlesByName(SubtitleProvider var0, Collection<File> var1_1, Locale var2_2, QueryExpression var3_3, boolean var4_4, boolean var5_5) throws Exception {
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
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriterToArgs(AbstractMemberFunctionInvokation.java:101)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriter(AbstractMemberFunctionInvokation.java:88)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:103)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriter(AbstractMemberFunctionInvokation.java:87)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:103)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.CastExpression.applyExpressionRewriter(CastExpression.java:128)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:103)
         *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment.rewriteExpressions(StructuredAssignment.java:146)
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

    private static Map<AutoDetection.Group, List<File>> group(Collection<File> collection, QueryExpression queryExpression, Locale locale, boolean bl) throws Exception {
        if (queryExpression != null) {
            return Collections.singletonMap(AutoDetection.Group.None, new ArrayList<File>(collection));
        }
        if (bl) {
            return AutoDetection.groupParallel(collection, locale);
        }
        return AutoDetection.group(collection, locale);
    }

    public static Map<File, SubtitleDescriptor> matchSubtitles(Collection<File> collection, Collection<SubtitleDescriptor> collection2) {
        LinkedHashMap<File, SubtitleDescriptor> linkedHashMap = new LinkedHashMap<File, SubtitleDescriptor>();
        SimilarityMetric[] similarityMetricArray = new SubtitleMetrics().matchSequence();
        Matcher<File, SubtitleDescriptor> matcher = new Matcher<File, SubtitleDescriptor>(collection, collection2, false, similarityMetricArray);
        for (Match<File, SubtitleDescriptor> match : matcher.match()) {
            linkedHashMap.put(match.getValue(), match.getCandidate());
        }
        return linkedHashMap;
    }

    protected static List<SubtitleSearchResult> findProbableSearchResults(SubtitleProvider subtitleProvider, Collection<String> collection, boolean bl, boolean bl2) throws Exception {
        ArrayList<SubtitleSearchResult> arrayList = new ArrayList<SubtitleSearchResult>();
        for (String string : collection) {
            Stream<SubtitleSearchResult> stream = subtitleProvider.search(string).stream().filter(subtitleSearchResult -> bl && subtitleSearchResult.isMovie() || bl2 && subtitleSearchResult.isSeries());
            arrayList.addAll(SubtitleUtilities.filterProbableSearchResults(string, stream::iterator, collection.size() == 1 ? 4 : 2));
        }
        return arrayList;
    }

    protected static List<SubtitleSearchResult> filterProbableSearchResults(String string, Iterable<SubtitleSearchResult> iterable, int n) {
        ArrayList<SubtitleSearchResult> arrayList = new ArrayList<SubtitleSearchResult>();
        MetricAvg metricAvg = new MetricAvg(new SequenceMatchSimilarity(), new NameSimilarityMetric());
        block0: for (SubtitleSearchResult subtitleSearchResult : iterable) {
            if (arrayList.size() > n) continue;
            for (String string2 : subtitleSearchResult.getEffectiveNames()) {
                if (!(metricAvg.getSimilarity(string, Normalization.removeTrailingBrackets(string2)) > 0.8f) && !string2.toLowerCase().startsWith(string.toLowerCase())) continue;
                arrayList.add(subtitleSearchResult);
                continue block0;
            }
        }
        return arrayList;
    }

    public static SubtitleDescriptor getBestMatch(File file, Collection<SubtitleDescriptor> collection, boolean bl) {
        if (file == null || collection == null || collection.isEmpty()) {
            return null;
        }
        SimilarityMetric similarityMetric = new SubtitleMetrics().verification();
        float f = bl ? 0.8f : 0.2f;
        for (Map.Entry<File, SubtitleDescriptor> entry : SubtitleUtilities.matchSubtitles(Collections.singleton(file), collection).entrySet()) {
            if (!(similarityMetric.getSimilarity(entry.getKey(), entry.getValue()) >= f)) continue;
            return entry.getValue();
        }
        return null;
    }

    public static List<SubtitleElement> decodeSubtitles(MemoryFile memoryFile) throws IOException {
        ArrayDeque<SubtitleFormat> arrayDeque = new ArrayDeque<SubtitleFormat>();
        for (SubtitleFormat format : SubtitleFormat.values()) {
            if (format.getFilter().accept(memoryFile.getName())) {
                arrayDeque.addFirst(format);
                continue;
            }
            arrayDeque.addLast(format);
        }
        String string = FileUtilities.decodeTextContent(memoryFile.getData(), true, StandardCharsets.UTF_8);
        for (SubtitleFormat subtitleFormat : arrayDeque) {
            List<SubtitleElement> elements = subtitleFormat.getDecoder().decode(string);
            if (elements.size() <= 0) continue;
            return elements;
        }
        throw new IOException("Subtitle format not supported");
    }

    public static ByteBuffer exportSubtitles(MemoryFile memoryFile, SubtitleFormat subtitleFormat, Charset charset) throws IOException {
        if (subtitleFormat == SubtitleFormat.SubRip) {
            StringBuilder stringBuilder = new StringBuilder(memoryFile.size());
            SubRipWriter subRipWriter = new SubRipWriter(stringBuilder);
            for (SubtitleElement subtitleElement : SubtitleUtilities.decodeSubtitles(memoryFile)) {
                String string = SubtitleUtilities.stripMarkup(subtitleElement.getText());
                if (string.contains("\ufffd")) {
                    throw new InvalidInputException("Failed to decode subtitles: " + subtitleElement);
                }
                if (string.isEmpty() || subtitleElement.getStart() >= subtitleElement.getEnd()) {
                    Logging.debug.finest(Logging.message("Ignore empty subtitle element", subtitleElement));
                    continue;
                }
                subRipWriter.write(new SubtitleElement(subtitleElement.getStart(), subtitleElement.getEnd(), string));
            }
            return charset.encode(CharBuffer.wrap(stringBuilder));
        }
        if (subtitleFormat == null) {
            return charset.encode(FileUtilities.decodeTextContent(memoryFile.getData(), true, StandardCharsets.UTF_8));
        }
        throw new InvalidInputException("Format not supported: " + subtitleFormat);
    }

    public static MemoryFile readSubtitleFile(File file) throws IOException {
        long l = file.length();
        if (l > 0L && l < 0x1400000L) {
            return new MemoryFile(file.getName(), ByteBuffer.wrap(FileUtilities.readFile(file)));
        }
        throw new IllegalArgumentException("Invalid subtitle file: " + file + " (" + FileUtilities.formatSize(l) + ")");
    }

    private static String stripMarkup(String string) {
        return MARKUP.matcher(string).replaceAll("").trim();
    }

    public static SubtitleFormat getSubtitleFormat(File file) {
        for (SubtitleFormat subtitleFormat : SubtitleFormat.values()) {
            if (!subtitleFormat.getFilter().accept(file)) continue;
            return subtitleFormat;
        }
        return null;
    }

    public static SubtitleFormat getSubtitleFormatByName(String string) {
        for (SubtitleFormat subtitleFormat : SubtitleFormat.values()) {
            if (subtitleFormat.name().equalsIgnoreCase(string)) {
                return subtitleFormat;
            }
            if (!subtitleFormat.getFilter().acceptExtension(string)) continue;
            return subtitleFormat;
        }
        return null;
    }

    public static String formatSubtitle(String string, String string2, String string3) {
        StringBuilder stringBuilder = new StringBuilder(string);
        if (string2 != null) {
            Language language = Language.findLanguage(string2);
            String string4 = language != null ? language.getISO3B() : RegularExpressions.NON_WORD.matcher(string2).replaceAll("");
            stringBuilder.append('.').append(string4);
        }
        if (string3 != null) {
            stringBuilder.append('.').append(string3);
        }
        return stringBuilder.toString();
    }

    public static MemoryFile fetchSubtitle(SubtitleDescriptor subtitleDescriptor) throws Exception {
        ByteBuffer byteBuffer = subtitleDescriptor.fetch();
        ArchiveType archiveType = ArchiveType.forName(subtitleDescriptor.getType());
        if (archiveType != ArchiveType.UNKOWN) {
            for (MemoryFile memoryFile : archiveType.fromData(byteBuffer)) {
                if (!MediaTypes.SUBTITLE_FILES.accept(memoryFile.getName())) continue;
                return memoryFile;
            }
        }
        return new MemoryFile(subtitleDescriptor.getPath(), byteBuffer);
    }

    public static Language detectSubtitleLanguage(File file) {
        try {
            MemoryFile memoryFile = SubtitleUtilities.readSubtitleFile(file);
            CharSequence charSequence = SubtitleUtilities.decodeSubtitles(memoryFile).stream().map(SubtitleElement::getText).collect(Collectors.joining("\n"));
            return SubtitleUtilities.detectSubtitleLanguage(charSequence);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            Logging.debug.finest(Logging.cause(illegalArgumentException));
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Failed to detect subtitle language", file, exception));
        }
        return null;
    }

    public static Language detectSubtitleLanguage(CharSequence charSequence) throws Exception {
        return languageDetector.get().build().getProbabilities(charSequence).stream().map(detectedLanguage -> detectedLanguage.getLocale().getLanguage()).map(Language::getLanguage).findFirst().orElse(null);
    }

    private SubtitleUtilities() {
        throw new UnsupportedOperationException();
    }

    private static /* synthetic */ void lambda$findSubtitlesByName$6(SimilarityMetric similarityMetric, float f, Map map, File file, SubtitleDescriptor subtitleDescriptor) {
        if (similarityMetric.getSimilarity(file, subtitleDescriptor) >= f) {
            ((List)map.get(file)).add(subtitleDescriptor);
        }
    }

    private static /* synthetic */ int[][] lambda$findSubtitlesByName$5(int n) {
        return new int[n][];
    }
}

