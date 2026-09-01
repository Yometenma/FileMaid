package net.filemaid.ui.rename;

import java.awt.Font;
import java.io.InputStream;
import java.util.function.Consumer;
import javax.swing.event.DocumentEvent;
import net.filemaid.Logging;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.rename.FormatExpressionTokenMakerFactory;
import net.filemaid.util.StringUtilities;
import net.filemaid.util.ui.LazyDocumentListener;
import net.filemaid.util.ui.SwingUI;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;

public class FormatExpressionTextArea
extends RSyntaxTextArea {
    public FormatExpressionTextArea() {
        this(new RSyntaxDocument((TokenMakerFactory)new FormatExpressionTokenMakerFactory(), "text/groovy-format-expression"), true);
    }

    public FormatExpressionTextArea(RSyntaxDocument rSyntaxDocument, boolean bl) {
        super(rSyntaxDocument, "", 1, 80);
        try {
            Theme.load((InputStream)this.openTheme()).apply((RSyntaxTextArea)this);
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
        this.setAntiAliasingEnabled(true);
        this.setAnimateBracketMatching(true);
        this.setAutoIndentEnabled(true);
        this.setBracketMatchingEnabled(true);
        this.setCloseCurlyBraces(true);
        this.setCodeFoldingEnabled(false);
        this.setHyperlinksEnabled(false);
        this.setUseFocusableTips(false);
        this.setClearWhitespaceLinesEnabled(false);
        this.setHighlightCurrentLine(false);
        this.setHighlightSecondaryLanguages(false);
        this.setLineWrap(false);
        this.setPaintTabLines(false);
        this.setFont(new Font("Monospaced", 0, 14));
        this.setMarkOccurrences(false);
        this.setPaintMarkOccurrencesBorder(false);
        if (bl) {
            this.onChange(0, documentEvent -> {
                int n;
                int n2 = this.getRows();
                if (n2 != (n = (int)StringUtilities.lineCount(this.getText()))) {
                    this.setRows(n);
                    SwingUI.getWindow((Object)this).revalidate();
                }
            });
        }
        this.setBackground(ThemeSupport.getEditorBackground());
        this.setOpaque(true);
    }

    protected InputStream openTheme() {
        if (ThemeSupport.getTheme().isDark()) {
            return FormatExpressionTextArea.class.getResourceAsStream("FormatExpressionTextArea.Theme.Dark.xml");
        }
        return FormatExpressionTextArea.class.getResourceAsStream("FormatExpressionTextArea.Theme.xml");
    }

    public void onChange(Consumer<DocumentEvent> consumer) {
        this.getDocument().addDocumentListener(new LazyDocumentListener(consumer));
    }

    public void onChange(int n, Consumer<DocumentEvent> consumer) {
        this.getDocument().addDocumentListener(new LazyDocumentListener(n, consumer));
    }
}

