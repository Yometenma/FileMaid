package net.filemaid.ui.rename;

import java.io.File;
import java.util.Map;
import javax.script.ScriptException;
import net.filemaid.ApplicationFolder;
import net.filemaid.Logging;
import net.filemaid.format.ExpressionFileFormat;
import net.filemaid.format.MediaBindingBean;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.similarity.Match;
import net.filemaid.ui.rename.MatchFormatter;
import net.filemaid.ui.rename.MatchFormatterType;
import net.filemaid.util.FileUtilities;

class ExpressionFormatter
implements MatchFormatter {
    private String expression;
    private ExpressionFileFormat format;
    private MatchFormatterType type;

    public ExpressionFormatter(String string, MatchFormatterType matchFormatterType) {
        if (string == null || string.isEmpty()) {
            throw new IllegalArgumentException("Expression must not be null or empty");
        }
        this.expression = string;
        this.type = matchFormatterType;
    }

    public MatchFormatterType getType() {
        return this.type;
    }

    public String getFormatExpression() {
        return this.expression;
    }

    public ExpressionFileFormat getFormat() throws ScriptException {
        ExpressionFormatter expressionFormatter = this;
        synchronized (expressionFormatter) {
            if (this.format == null) {
                this.format = new ExpressionFileFormat(this.expression);
            }
            return this.format;
        }
    }

    public boolean sameExpression(String string) {
        if (this.getFormatExpression().equals(string)) {
            ExpressionFormatter expressionFormatter = this;
            synchronized (expressionFormatter) {
                try {
                    return this.format == null || this.format.sameExpression(string);
                }
                catch (Exception exception) {
                }
            }
        }
        return false;
    }

    public boolean equals(Object object) {
        if (object instanceof ExpressionFormatter) {
            ExpressionFormatter expressionFormatter = (ExpressionFormatter)object;
            return this.getType().equals(expressionFormatter.getType()) && this.sameExpression(expressionFormatter.getFormatExpression());
        }
        return false;
    }

    @Override
    public boolean canFormat(Match<?, ?> match) {
        return this.type.canFormat(match) && (match.getCandidate() == null || match.getCandidate() instanceof File);
    }

    @Override
    public String preview(Match<?, ?> match) {
        return this.type.preview(match);
    }

    @Override
    public String format(Match<?, ?> match, boolean bl, Map<?, ?> map) throws ScriptException {
        MediaBindingBean mediaBindingBean = new MediaBindingBean(match.getValue(), (File)match.getCandidate(), (Map<File, ?>)map);
        String string = this.getFormat().format(mediaBindingBean);
        return this.resolvePath((File)match.getCandidate(), FileUtilities.normalizePathSeparators(string));
    }

    protected String resolvePath(File file, String string) {
        String[] stringArray = string.split("/", 2);
        if (stringArray.length < 2) {
            return string;
        }
        if (stringArray[0].equals(".") || stringArray[0].equals("..")) {
            return string;
        }
        if (stringArray[0].equals("~")) {
            return ApplicationFolder.UserHome.resolve(string.substring(1)).getAbsolutePath();
        }
        File file2 = new File(string);
        if (file2.isAbsolute()) {
            return string;
        }
        if (file != null) {
            try {
                File file3 = MediaFileUtilities.getStructureRoot(file);
                if (file3 != null) {
                    for (File file4 : FileUtilities.listPath(file2.getParentFile())) {
                        if (MediaFileUtilities.isVolumeRoot(file3)) break;
                        if (!MediaFileUtilities.isStructureRoot(file4)) continue;
                        if (file3.getParentFile() == null || !FileUtilities.isWritable(file3.getParentFile())) break;
                        file3 = file3.getParentFile();
                    }
                    return new File(file3, string).getAbsolutePath();
                }
            }
            catch (Exception exception) {
                Logging.trace("Failed to resolve structure root: " + file, exception);
            }
        }
        return string;
    }
}

