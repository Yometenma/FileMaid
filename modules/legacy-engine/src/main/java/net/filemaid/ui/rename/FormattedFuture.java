package net.filemaid.ui.rename;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.swing.SwingWorker;
import net.filemaid.Logging;
import net.filemaid.Parallelism;
import net.filemaid.similarity.EpisodeMetrics;
import net.filemaid.similarity.Match;
import net.filemaid.ui.rename.ExpressionFormatter;
import net.filemaid.ui.rename.MatchFormatter;
import net.filemaid.ui.rename.TextColorizer;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ReadOnlyFile;
import net.filemaid.web.Episode;

class FormattedFuture
extends SwingWorker<File, Void> {
    private final Match<Object, File> match;
    private final boolean extension;
    private final Map<File, Object> context;
    private final MatchFormatter formatter;
    private String targetPath = null;
    private ReadOnlyFile destinationFile = null;
    private String error = null;
    private String displayPath = null;
    private String displayRainbowPath = null;
    private float matchProbability = 1.0f;
    private static final Parallelism evaluatorPool = new Parallelism("Evaluator", Parallelism.THREAD_POOL_SIZE.min());

    FormattedFuture(Match<Object, File> match, boolean bl, MatchFormatter matchFormatter, Map<File, Object> map) {
        this.match = match;
        this.extension = bl;
        this.formatter = matchFormatter;
        this.context = map;
    }

    public ReadOnlyFile getDestinationFile() {
        return this.destinationFile;
    }

    public String getTargetPath() {
        return this.targetPath;
    }

    public String getDisplayPath(boolean bl) {
        return bl ? this.displayPath : this.displayRainbowPath;
    }

    public Match<Object, File> getMatch() {
        return this.match;
    }

    public boolean hasComplement() {
        return this.match.getValue() != null && this.match.getCandidate() != null;
    }

    public boolean hasExtension() {
        return this.extension;
    }

    public boolean isComplexFormat() {
        return this.formatter instanceof ExpressionFormatter;
    }

    public String preview() {
        return this.formatter.preview(this.match).trim();
    }

    public boolean isReady() {
        return this.targetPath != null && this.isDone() && !this.isCancelled();
    }

    public boolean isError() {
        return this.error != null && this.isDone();
    }

    public float getMatchProbablity() {
        return this.matchProbability;
    }

    @Override
    protected File doInBackground() throws Exception {
        try {
            this.targetPath = this.formatter.format(this.match, this.extension, this.context).trim();
            String string = this.extension || !this.hasComplement() ? null : FileUtilities.getExtension(this.match.getCandidate());
            File file = new File((String)(string == null ? this.targetPath : this.targetPath + "." + string.toLowerCase(Locale.ROOT)));
            if (this.hasComplement()) {
                this.destinationFile = ReadOnlyFile.of(FileUtilities.resolveSibling(this.match.getCandidate(), file));
                this.destinationFile.stats();
                File file2 = this.extension ? this.destinationFile : new File(this.targetPath);
                this.displayPath = this.match.getCandidate().getParentFile().equals(this.destinationFile.getParentFile()) ? file2.getName() : FileUtilities.abbreviatePath(file2);
                this.displayRainbowPath = TextColorizer.colorizeFilePath(this.displayPath, this.extension);
                this.matchProbability = this.calculateMatchProbability(this.match);
            } else {
                this.displayPath = this.displayRainbowPath = FileUtilities.abbreviatePath(file);
            }
            return file;
        }
        catch (Exception exception) {
            this.error = "[" + Logging.cause(exception) + "] " + this.preview();
            throw exception;
        }
    }

    protected float calculateMatchProbability(Match<Object, File> match) {
        EpisodeMetrics episodeMetrics = new EpisodeMetrics();
        if (match.getValue() instanceof Episode) {
            float f = episodeMetrics.verification().getSimilarity(match.getValue(), match.getCandidate());
            return (f + 1.0f) / 2.0f;
        }
        float f = episodeMetrics.sanity().getSimilarity(match.getValue(), match.getCandidate());
        if (f != 0.0f) {
            return Math.max(f, 0.0f);
        }
        return 1.0f;
    }

    public String toString() {
        if (this.targetPath != null) {
            return this.targetPath;
        }
        if (this.error != null) {
            return this.error;
        }
        return this.preview();
    }

    public CompletableFuture<File> start() {
        return FormattedFuture.evaluatorPool().async(this);
    }

    public static Parallelism evaluatorPool() {
        return evaluatorPool;
    }
}

