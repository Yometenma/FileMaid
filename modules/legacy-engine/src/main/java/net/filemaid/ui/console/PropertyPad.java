package net.filemaid.ui.console;

import java.awt.Component;
import java.awt.LayoutManager;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;
import net.filemaid.Logging;
import net.filemaid.ResourceManager;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.ui.GlassOptionPane;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.IOUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class PropertyPad
extends JComponent {
    private File store;
    private RSyntaxTextArea editor = this.createEditor();

    public static void showPropertyPad(File file, Component component) {
        PropertyPad propertyPad = new PropertyPad(file);
        JButton jButton = SwingUI.newButton("Reset", actionEvent -> propertyPad.reset());
        GlassOptionPane glassOptionPane = GlassOptionPane.showConfigurationDialog(propertyPad, jButton, FileUtilities.abbreviatePath(file), ResourceManager.getIcon("action.settings"), component, propertyPad2 -> {
            try {
                propertyPad.store();
                System.exit(0);
            }
            catch (Exception exception) {
                Logging.log.severe(Logging.cause(exception));
            }
        });
        glassOptionPane.confirm.putValue("Name", "Save & Exit");
        glassOptionPane.cancel.putValue("Name", "Cancel");
    }

    public PropertyPad(File file) {
        this.store = file;
        this.setLayout((LayoutManager)new MigLayout("insets 0, fill"));
        this.add((Component)this.wrapEditor("System Properties", this.editor), "wmin 540px, hmin 300px, hmax 485px, grow");
        this.restore();
    }

    public void reset() {
        try {
            this.editor.setText(IOUtils.toString((URL)this.getClass().getResource("PropertyPad.properties"), (Charset)StandardCharsets.UTF_8));
            this.editor.setCaretPosition(0);
        }
        catch (Exception exception) {
            Logging.log.warning(Logging.cause(exception));
        }
    }

    public void restore() {
        try {
            if (this.store.isFile()) {
                this.editor.setText(FileUtilities.readTextFile(this.store));
                this.editor.setCaretPosition(0);
            } else {
                this.reset();
            }
        }
        catch (Exception exception) {
            Logging.log.warning(Logging.cause(exception));
        }
    }

    public File store() throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(this.editor.getText()));
        if (properties.isEmpty()) {
            if (this.store.isFile()) {
                FileUtilities.delete(this.store);
            }
            return null;
        }
        return FileUtilities.writeFile(StandardCharsets.UTF_8.encode(this.editor.getText()), this.store);
    }

    private RSyntaxTextArea createEditor() {
        RSyntaxTextArea rSyntaxTextArea = new RSyntaxTextArea(){

            protected void appendFoldingMenu(JPopupMenu jPopupMenu) {
            }
        };
        try {
            Theme.load((InputStream)Theme.class.getResourceAsStream(ThemeSupport.getTheme().isDark() ? "themes/monokai.xml" : "themes/eclipse.xml")).apply(rSyntaxTextArea);
        }
        catch (Exception exception) {
            Logging.trace(exception);
        }
        rSyntaxTextArea.setSyntaxEditingStyle("text/properties");
        rSyntaxTextArea.setAnimateBracketMatching(false);
        rSyntaxTextArea.setAntiAliasingEnabled(true);
        rSyntaxTextArea.setAutoIndentEnabled(false);
        rSyntaxTextArea.setBracketMatchingEnabled(false);
        rSyntaxTextArea.setCloseCurlyBraces(false);
        rSyntaxTextArea.setClearWhitespaceLinesEnabled(true);
        rSyntaxTextArea.setCodeFoldingEnabled(false);
        rSyntaxTextArea.setHighlightSecondaryLanguages(false);
        rSyntaxTextArea.setRoundedSelectionEdges(false);
        rSyntaxTextArea.setTabsEmulated(false);
        return rSyntaxTextArea;
    }

    private RTextScrollPane wrapEditor(String string, RSyntaxTextArea rSyntaxTextArea) {
        RTextScrollPane rTextScrollPane = new RTextScrollPane((RTextArea)rSyntaxTextArea, true);
        rTextScrollPane.setVerticalScrollBarPolicy(20);
        rTextScrollPane.setHorizontalScrollBarPolicy(30);
        rTextScrollPane.setBorder((Border)BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(string), rTextScrollPane.getBorder()));
        rTextScrollPane.setOpaque(false);
        return rTextScrollPane;
    }
}

