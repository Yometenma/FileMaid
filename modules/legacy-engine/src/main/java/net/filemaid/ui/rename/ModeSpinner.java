package net.filemaid.ui.rename;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.Arrays;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicButtonUI;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.rename.Mode;
import net.filemaid.util.ui.RoundDecoration;
import net.filemaid.util.ui.SwingUI;
import net.filemaid.web.Datasource;
import net.miginfocom.swing.MigLayout;

public class ModeSpinner
extends JComponent {
    public static final String VALUE_PROPERTY = "value";
    private Mode value;
    private boolean editable;

    public ModeSpinner(Mode mode, boolean bl) {
        this.editable = bl;
        this.setLayout((LayoutManager)new MigLayout("insets 0, gap 2px", "", "align 40%"));
        this.setValue(mode);
    }

    protected JButton createSpinnerButton(Mode mode, Icon icon) {
        Action action = SwingUI.newAction("Switch to " + mode + " Mode", new RoundDecoration(icon, icon.getIconWidth(), icon.getIconHeight(), ThemeSupport.getPanelBackground(), ThemeSupport.getColor(0xD7D7D7)), actionEvent -> this.setValue(mode));
        JButton jButton = SwingUI.createImageButton(action);
        jButton.setCursor(Cursor.getPredefinedCursor(12));
        jButton.setUI(new BasicButtonUI());
        jButton.setBorder(BorderFactory.createEmptyBorder());
        jButton.setBorderPainted(false);
        return jButton;
    }

    protected JComponent createDatasourceButton(Mode mode, Datasource datasource) {
        JLabel jLabel = new JLabel(datasource.getIcon());
        jLabel.setToolTipText(String.format("%s Mode: %s", new Object[]{mode, datasource.getName()}));
        return jLabel;
    }

    public void setValue(Mode mode) {
        this.value = mode;
        this.removeAll();
        Arrays.stream(mode.getDatasources()).map(datasource -> this.createDatasourceButton(mode, (Datasource)datasource)).forEach(jComponent -> this.add((Component)jComponent, "w 16px!"));
        if (this.editable) {
            this.add(this.createSpinnerButton(mode.cycle(-1), Arrow.PREVIOUS), "gap after 5px, w 24px!", 0);
            this.add((Component)this.createSpinnerButton(mode.cycle(1), Arrow.NEXT), "gap before 4px, w 24px!");
        }
        this.revalidate();
        this.repaint();
        this.firePropertyChange(VALUE_PROPERTY, null, (Object)mode);
    }

    public Mode getValue() {
        return this.value;
    }

    private static enum Arrow implements Icon
    {
        NEXT{

            @Override
            protected Shape getShape() {
                GeneralPath generalPath = new GeneralPath(0);
                int n = 10;
                int n2 = 8;
                int n3 = 5;
                generalPath.moveTo(n, n2);
                generalPath.lineTo(n + n3, n2 + n3);
                generalPath.lineTo(n, n2 + 2 * n3);
                generalPath.lineTo(n, n2);
                return generalPath;
            }
        }
        ,
        PREVIOUS{

            @Override
            protected Shape getShape() {
                GeneralPath generalPath = new GeneralPath(0);
                int n = 14;
                int n2 = 8;
                int n3 = 5;
                generalPath.moveTo(n, n2);
                generalPath.lineTo(n - n3, n2 + n3);
                generalPath.lineTo(n, n2 + 2 * n3);
                generalPath.lineTo(n, n2);
                return generalPath;
            }
        };

        private final Shape shape = this.getShape();

        protected abstract Shape getShape();

        @Override
        public void paintIcon(Component component, Graphics graphics, int n, int n2) {
            Graphics2D graphics2D = (Graphics2D)graphics;
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setPaint(Color.black);
            graphics2D.fill(this.shape);
        }

        @Override
        public int getIconWidth() {
            return 24;
        }

        @Override
        public int getIconHeight() {
            return 24;
        }
    }
}

