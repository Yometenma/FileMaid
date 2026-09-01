package net.filemaid.ui.rename;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import net.filemaid.Logging;
import net.filemaid.util.ui.AbstractFancyListCellRenderer;
import net.filemaid.util.ui.SwingUI;

class HighlightListCellRenderer
extends AbstractFancyListCellRenderer {
    protected final JTextComponent textComponent = new JTextField();
    protected final Pattern pattern;
    protected final Highlighter.HighlightPainter highlightPainter;

    public HighlightListCellRenderer(Pattern pattern, Highlighter.HighlightPainter highlightPainter, int n) {
        super(new Insets(0, 0, 0, 0));
        this.pattern = pattern;
        this.highlightPainter = highlightPainter;
        this.setHighlightingEnabled(false);
        this.textComponent.setBorder(new EmptyBorder(n, n, n, n));
        this.textComponent.setBackground(SwingUI.TRANSLUCENT);
        this.add((Component)this.textComponent, "West");
        this.textComponent.getDocument().addDocumentListener(new HighlightUpdateListener());
    }

    @Override
    protected void configureListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
        super.configureListCellRendererComponent(jList, object, n, bl, bl2);
        this.textComponent.setText(object.toString());
    }

    protected void updateHighlighter() {
        this.textComponent.getHighlighter().removeAllHighlights();
        Matcher matcher = this.pattern.matcher(this.textComponent.getText());
        while (matcher.find()) {
            try {
                this.textComponent.getHighlighter().addHighlight(matcher.start(0), matcher.end(0), this.highlightPainter);
            }
            catch (BadLocationException badLocationException) {
                Logging.debug.severe(Logging.cause(badLocationException));
            }
        }
    }

    @Override
    public void setForeground(Color color) {
        super.setForeground(color);
        if (this.textComponent != null) {
            this.textComponent.setForeground(color);
        }
    }

    private class HighlightUpdateListener
    implements DocumentListener {
        private HighlightUpdateListener() {
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent) {
            HighlightListCellRenderer.this.updateHighlighter();
        }

        @Override
        public void insertUpdate(DocumentEvent documentEvent) {
            HighlightListCellRenderer.this.updateHighlighter();
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent) {
            HighlightListCellRenderer.this.updateHighlighter();
        }
    }
}

