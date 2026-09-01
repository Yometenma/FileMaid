package net.filemaid.ui.rename;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import net.filemaid.util.ui.GradientStyle;

class CharacterHighlightPainter
implements Highlighter.HighlightPainter {
    private Color gradientBeginColor;
    private Color gradientEndColor;

    public CharacterHighlightPainter(Color color, Color color2) {
        this.gradientBeginColor = color;
        this.gradientEndColor = color2;
    }

    @Override
    public void paint(Graphics graphics, int n, int n2, Shape shape, JTextComponent jTextComponent) {
        Graphics2D graphics2D = (Graphics2D)graphics;
        try {
            TextUI textUI = jTextComponent.getUI();
            Rectangle2D rectangle2D = textUI.modelToView2D(jTextComponent, n, Position.Bias.Backward);
            Rectangle2D rectangle2D2 = textUI.modelToView2D(jTextComponent, n2, Position.Bias.Backward);
            Rectangle2D rectangle2D3 = rectangle2D.createUnion(rectangle2D2);
            double d = rectangle2D3.getWidth() + 1.0;
            double d2 = rectangle2D3.getHeight();
            double d3 = rectangle2D3.getX() - 1.0;
            double d4 = rectangle2D3.getY();
            double d5 = 5.0;
            RoundRectangle2D.Double double_ = new RoundRectangle2D.Double(d3, d4, d, d2, d5, d5);
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setPaint(GradientStyle.TOP_TO_BOTTOM.getGradientPaint(double_, this.gradientBeginColor, this.gradientEndColor));
            graphics2D.fill(double_);
        }
        catch (BadLocationException badLocationException) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, badLocationException.toString(), badLocationException);
        }
    }
}

