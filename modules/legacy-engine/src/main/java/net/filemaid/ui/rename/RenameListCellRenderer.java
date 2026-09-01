package net.filemaid.ui.rename;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.SwingWorker;
import net.filemaid.ResourceManager;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.rename.FormattedFuture;
import net.filemaid.ui.rename.RenameModel;
import net.filemaid.ui.rename.StringMatch;
import net.filemaid.ui.rename.TextColorizer;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ReadOnlyFile;
import net.filemaid.util.ui.BufferedGraphics;
import net.filemaid.util.ui.DefaultFancyListCellRenderer;
import net.filemaid.util.ui.GradientStyle;

abstract class RenameListCellRenderer
extends DefaultFancyListCellRenderer {
    private TypeRenderer typeRenderer = new TypeRenderer();
    private Color noMatchGradientBeginColor = new Color(0xB7B7B7);
    private Color noMatchGradientEndColor = new Color(0x9A9A9A);
    private Color noMatchForegroundColor = ThemeSupport.getTheme().isDark() ? this.noMatchGradientEndColor : this.noMatchGradientBeginColor;
    private Color warningGradientBeginColor = Color.RED;
    private Color warningGradientEndColor = new Color(14423100);

    public static RenameListCellRenderer create(final RenameModel renameModel) {
        return new RenameListCellRenderer(){

            @Override
            protected boolean preserveExtension() {
                return renameModel.preserveExtension();
            }

            @Override
            protected boolean hasComplement(Object object, int n) {
                if (object instanceof FormattedFuture) {
                    FormattedFuture formattedFuture = (FormattedFuture)object;
                    return formattedFuture.hasComplement();
                }
                return renameModel.hasComplement(n);
            }

            @Override
            protected String getType(Object object, int n) {
                if (object instanceof FormattedFuture) {
                    FormattedFuture formattedFuture = (FormattedFuture)object;
                    return this.getType(formattedFuture.getMatch().getCandidate());
                }
                return n < renameModel.size() ? this.getType((File)renameModel.getMatch(n).getCandidate()) : null;
            }
        };
    }

    public static RenameListCellRenderer create(final List<File> list, final boolean bl) {
        return new RenameListCellRenderer(){

            @Override
            protected boolean preserveExtension() {
                return bl;
            }

            @Override
            protected boolean hasComplement(Object object, int n) {
                return true;
            }

            @Override
            protected String getType(Object object, int n) {
                return n < list.size() ? this.getType((File)list.get(n)) : null;
            }
        };
    }

    public RenameListCellRenderer() {
        super(new Insets(4, 7, 4, 7));
        this.setHighlightingEnabled(false);
        this.setLayout(new BoxLayout(this, 2));
        this.add(Box.createHorizontalGlue());
        this.add(this.typeRenderer);
    }

    protected abstract boolean preserveExtension();

    protected abstract boolean hasComplement(Object var1, int var2);

    protected abstract String getType(Object var1, int var2);

    @Override
    public Dimension getPreferredSize() {
        Dimension dimension = super.getPreferredSize();
        dimension.height = 28;
        return dimension;
    }

    @Override
    public void configureListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
        Object object2;
        super.configureListCellRendererComponent(jList, object, n, bl, bl2);
        this.setOpaque(false);
        this.setIcon(null);
        this.typeRenderer.setVisible(false);
        this.typeRenderer.setEnabled(true);
        if (!this.hasComplement(object, n)) {
            if (bl) {
                this.setGradientColors(this.noMatchGradientBeginColor, this.noMatchGradientEndColor);
            } else {
                this.setForeground(this.noMatchForegroundColor);
                this.typeRenderer.setEnabled(false);
            }
        }
        if (this.preserveExtension() && (object2 = this.getType(object, n)) != null) {
            this.typeRenderer.setText((String)object2);
            this.typeRenderer.setVisible(true);
        }
        if (object instanceof File) {
            object2 = (File)object;
            if (this.preserveExtension()) {
                this.setText(FileUtilities.getName((File)object2));
            } else {
                this.setText(bl || !this.hasComplement(object, n) ? FileUtilities.abbreviatePath((File)object2) : TextColorizer.colorizeFilePath((File)object2));
            }
        } else if (object instanceof FormattedFuture) {
            object2 = (FormattedFuture)object;
            if (((FormattedFuture)object2).isError()) {
                this.setText(((FormattedFuture)object2).toString());
                this.setIcon(ResourceManager.getIcon("dialog.cancel"));
                return;
            }
            float f = ((FormattedFuture)object2).getMatchProbablity();
            boolean bl3 = ((FormattedFuture)object2).getMatch() instanceof StringMatch;
            if (bl3) {
                this.setIcon(ResourceManager.getIcon("search.literal"));
                f = 1.0f;
            }
            if (((FormattedFuture)object2).isReady()) {
                if (((FormattedFuture)object2).hasComplement() && ((FormattedFuture)object2).hasExtension()) {
                    this.setText(((FormattedFuture)object2).getDisplayPath(bl || f < 1.0f));
                    if (((FormattedFuture)object2).getMatch().getCandidate().isDirectory()) {
                        this.typeRenderer.setText("Folder");
                    } else {
                        String string = FileUtilities.getExtension(((FormattedFuture)object2).getDestinationFile());
                        if (string == null) {
                            this.typeRenderer.setText("MISSING EXTENSION");
                        } else {
                            this.typeRenderer.setText(string.toLowerCase(Locale.ROOT));
                        }
                    }
                    this.typeRenderer.setVisible(true);
                } else {
                    this.setText(((FormattedFuture)object2).getDisplayPath(bl || f < 1.0f || !((FormattedFuture)object2).hasComplement()));
                }
            } else {
                this.setText(((FormattedFuture)object2).preview());
            }
            switch (((SwingWorker)object2).getState()) {
                case PENDING: {
                    this.setIcon(ResourceManager.getIcon("worker.pending"));
                    break;
                }
                case STARTED: {
                    this.setIcon(ResourceManager.getIcon("worker.started"));
                    break;
                }
            }
            if (((FormattedFuture)object2).hasComplement()) {
                this.setOpaque(true);
                this.setBackground(ThemeSupport.withAlpha(this.warningGradientBeginColor, (1.0f - f) * 0.5f));
                if (f < 1.0f && !bl3 && bl) {
                    this.setGradientColors(this.warningGradientBeginColor, this.warningGradientEndColor);
                    this.setIcon(ResourceManager.getIcon("status.warning"));
                }
                if (((FormattedFuture)object2).isReady() && !bl3) {
                    this.setIcon(this.getStatusIcon((FormattedFuture)object2));
                }
            }
        }
    }

    private Icon getStatusIcon(FormattedFuture formattedFuture) {
        File file = formattedFuture.getMatch().getCandidate();
        if (file == null) {
            return null;
        }
        ReadOnlyFile readOnlyFile = formattedFuture.getDestinationFile();
        if (readOnlyFile == null || !((File)readOnlyFile).exists()) {
            return null;
        }
        if (!file.getPath().equalsIgnoreCase(readOnlyFile.getPath())) {
            return ResourceManager.getIcon("dialog.cancel");
        }
        if (file.getName().equals(((File)readOnlyFile).getName())) {
            return ResourceManager.getIcon("dialog.continue");
        }
        return null;
    }

    protected String getType(File file) {
        if (file == null) {
            return null;
        }
        if (file.isDirectory()) {
            return "Folder";
        }
        String string = FileUtilities.getExtension(file.getName());
        if (string != null) {
            return string.toLowerCase(Locale.ROOT);
        }
        return "File";
    }

    private static class TypeRenderer
    extends DefaultListCellRenderer {
        private final Color gradientBeginColor = new Color(0xFFCC00);
        private final Color gradientEndColor = new Color(0xFF9900);
        private final int arc = 10;
        private boolean visible = false;
        private final Map<String, BufferedGraphics> bufferedGraphics = new HashMap<String, BufferedGraphics>();
        private static final AlphaComposite ALPHA_ENABLED = AlphaComposite.SrcOver;
        private static final AlphaComposite ALPHA_DISABLED = AlphaComposite.SrcOver.derive(0.35f);

        public TypeRenderer() {
            this.setOpaque(false);
            this.setForeground(new Color(0x141414));
            this.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 5));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (!this.visible) {
                return;
            }
            Graphics2D graphics2D = (Graphics2D)graphics;
            graphics2D.setComposite(this.isEnabled() ? ALPHA_ENABLED : ALPHA_DISABLED);
            this.bufferedGraphics.computeIfAbsent(this.getText(), string -> new BufferedGraphics()).draw(graphics2D, this::render, this);
        }

        protected void render(Graphics2D graphics2D) {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D.Float float_ = new RoundRectangle2D.Float(10.0f, 0.0f, this.getWidth() - 10, this.getHeight(), 10.0f, 10.0f);
            graphics2D.setPaint(GradientStyle.TOP_TO_BOTTOM.getGradientPaint(float_, this.gradientBeginColor, this.gradientEndColor));
            graphics2D.fill(float_);
            graphics2D.setFont(this.getFont());
            graphics2D.setPaint(this.getForeground());
            Rectangle2D rectangle2D = graphics2D.getFontMetrics().getStringBounds(this.getText(), graphics2D);
            graphics2D.drawString(this.getText(), (float)(float_.getCenterX() - rectangle2D.getX() - rectangle2D.getWidth() / 2.0), (float)(float_.getCenterY() - rectangle2D.getY() - rectangle2D.getHeight() / 2.0));
        }

        @Override
        public void setVisible(boolean bl) {
            this.visible = bl;
        }
    }
}

