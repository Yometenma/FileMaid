package net.filemaid.ui.subtitle;

import java.awt.Component;
import java.awt.Insets;
import java.awt.LayoutManager;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import net.filemaid.Language;
import net.filemaid.ResourceManager;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.ui.subtitle.SubtitlePackage;
import net.filemaid.ui.subtitle.SubtitlePackageFeatureLink;
import net.filemaid.util.ui.AbstractFancyListCellRenderer;
import net.filemaid.util.ui.HorizontalRule;
import net.miginfocom.swing.MigLayout;

class SubtitlePackageCellRenderer
extends AbstractFancyListCellRenderer {
    private final JLabel titleLabel = new JLabel();
    private final JLabel languageLabel = new JLabel();

    public SubtitlePackageCellRenderer() {
        super(new Insets(5, 5, 5, 5));
        this.setHighlightingEnabled(false);
        this.setLayout((LayoutManager)new MigLayout("fill, nogrid, insets 0"));
        this.add((Component)this.languageLabel, "hidemode 3, w 85px!");
        this.add(this.titleLabel);
        HorizontalRule.south(this, 2, ThemeSupport.getColor(0xEEEEEE), ThemeSupport.getPanelBackground());
    }

    @Override
    public void configureListCellRendererComponent(JList jList, Object object, int n, boolean bl, boolean bl2) {
        super.configureListCellRendererComponent(jList, object, n, bl, bl2);
        SubtitlePackage subtitlePackage = (SubtitlePackage)object;
        this.titleLabel.setText(subtitlePackage.getName());
        this.titleLabel.setIcon(this.getIcon(subtitlePackage));
        if (this.languageLabel.isVisible()) {
            Language language = subtitlePackage.getLanguage();
            if (language != null) {
                this.languageLabel.setText(language.getName());
                this.languageLabel.setIcon(ResourceManager.getFlagIcon(language.getCode()));
            } else {
                this.languageLabel.setText("Unkown Language");
                this.languageLabel.setIcon(ResourceManager.getFlagIcon("undefined"));
            }
        }
        this.titleLabel.setForeground(bl ? jList.getSelectionForeground() : jList.getForeground());
        this.languageLabel.setForeground(bl ? jList.getSelectionForeground() : jList.getForeground());
        this.setBorderPainted(n < jList.getModel().getSize() - 1);
    }

    private Icon getIcon(SubtitlePackage subtitlePackage) {
        if (subtitlePackage.isDownload()) {
            switch (subtitlePackage.getDownload().getPhase()) {
                case PENDING: {
                    return ResourceManager.getIcon("bullet.green");
                }
                case WAITING: {
                    return ResourceManager.getIcon("worker.pending");
                }
                case DOWNLOADING: {
                    return ResourceManager.getIcon("package.fetch");
                }
                case EXTRACTING: {
                    return ResourceManager.getIcon("package.extract");
                }
                case DONE: {
                    return ResourceManager.getIcon("status.ok");
                }
            }
        }
        if (subtitlePackage instanceof SubtitlePackageFeatureLink) {
            return ((SubtitlePackageFeatureLink)subtitlePackage).getIcon();
        }
        return null;
    }

    public JLabel getLanguageLabel() {
        return this.languageLabel;
    }

    @Override
    public void validate() {
        Object object = this.getTreeLock();
        synchronized (object) {
            this.validateTree();
        }
    }
}

