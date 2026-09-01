package net.filemaid.ui;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import net.filemaid.ResourceManager;
import net.filemaid.Settings;
import net.filemaid.ui.ThemeSupport;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.ui.SelectButton;
import net.filemaid.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

public class SelectButtonTextField<T>
extends JComponent {
    private SelectButton<T> selectButton = new SelectButton();
    private JComboBox<String> editor = new JComboBox();

    public SelectButtonTextField() {
        this.editor.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 1, ((LineBorder)this.selectButton.getBorder()).getLineColor()));
        this.editor.setPrototypeDisplayValue("X");
        this.editor.setRenderer(new CompletionCellRenderer());
        this.editor.setUI(new TextFieldComboBoxUI(this.selectButton));
        this.editor.setMaximumRowCount(10);
        this.selectButton.addActionListener(actionEvent -> this.getEditor().requestFocusInWindow());
        this.setLayout((LayoutManager)new MigLayout("insets 0, fill, nogrid, novisualpadding"));
        this.add(this.selectButton, "h pref!, w pref!, sizegroupy editor");
        this.add(this.editor, "gap 0px, w 195px!, sizegroupy editor");
    }

    public String getText() {
        return ((TextFieldComboBoxUI)this.editor.getUI()).getEditor().getText();
    }

    public void setText(String string) {
        this.editor.getEditor().setItem(string);
    }

    public Document getEditorDocument() {
        return ((TextFieldComboBoxUI)this.editor.getUI()).getEditor().getDocument();
    }

    public JComboBox<String> getEditor() {
        return this.editor;
    }

    public SelectButton<T> getSelectButton() {
        return this.selectButton;
    }

    private class CompletionCellRenderer
    extends DefaultListCellRenderer {
        private CompletionCellRenderer() {
        }

        @Override
        public Component getListCellRendererComponent(JList<?> jList, Object object, int n, boolean bl, boolean bl2) {
            super.getListCellRendererComponent(jList, object, n, bl, bl2);
            this.setBorder(new EmptyBorder(1, 4, 1, 4));
            String string = SelectButtonTextField.this.getText().substring(0, ((TextFieldComboBoxUI)SelectButtonTextField.this.editor.getUI()).getEditor().getSelectionStart());
            Matcher matcher = Pattern.compile(string, 18).matcher(object.toString());
            StringBuffer stringBuffer = new StringBuffer("<html><nobr>");
            if (matcher.find()) {
                if (bl) {
                    matcher.appendReplacement(stringBuffer, "<span style='font-weight:bold;'>$0</span>");
                } else {
                    matcher.appendReplacement(stringBuffer, "<span style='color:" + SwingUI.toHex(jList.getSelectionBackground()) + "; font-weight:bold;'>$0</span>");
                }
            }
            matcher.appendTail(stringBuffer);
            stringBuffer.append("</nobr></html>");
            this.setText(stringBuffer.toString());
            return this;
        }
    }

    private static class TextFieldComboBoxUI
    extends BasicComboBoxUI {
        private SelectButton<?> button;

        public TextFieldComboBoxUI(SelectButton<?> selectButton) {
            this.button = selectButton;
        }

        @Override
        protected JButton createArrowButton() {
            return new JButton(ResourceManager.getIcon("action.list"));
        }

        @Override
        public void configureArrowButton() {
            super.configureArrowButton();
            this.arrowButton.setUI(new BasicButtonUI());
            this.arrowButton.setOpaque(false);
            this.arrowButton.setBorder(BorderFactory.createEmptyBorder());
            this.arrowButton.setContentAreaFilled(false);
            this.arrowButton.setFocusPainted(false);
            this.arrowButton.setFocusable(false);
            if (ThemeSupport.getTheme().isDark()) {
                this.arrowButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 3));
            }
            if (ThemeSupport.getTheme().isDark() || Settings.isMacApp()) {
                this.arrowButton.setContentAreaFilled(true);
            }
        }

        @Override
        protected void configureEditor() {
            JTextComponent jTextComponent = this.getEditor();
            jTextComponent.setDocument(this.createDocument());
            this.arrowButton.setBackground(jTextComponent.getBackground());
            jTextComponent.setEnabled(this.comboBox.isEnabled());
            jTextComponent.setFocusable(this.comboBox.isFocusable());
            jTextComponent.setFont(this.comboBox.getFont());
            jTextComponent.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
            jTextComponent.addFocusListener(this.createFocusListener());
            jTextComponent.getDocument().addDocumentListener(new DocumentListener(){

                @Override
                public void changedUpdate(DocumentEvent documentEvent) {
                    popup.getList().repaint();
                }

                @Override
                public void insertUpdate(DocumentEvent documentEvent) {
                    popup.getList().repaint();
                }

                @Override
                public void removeUpdate(DocumentEvent documentEvent) {
                    popup.getList().repaint();
                }
            });
            this.popup.getList().setPrototypeCellValue("X");
        }

        public JTextComponent getEditor() {
            return (JTextComponent)this.editor;
        }

        @Override
        protected ComboPopup createPopup() {
            BasicComboPopup basicComboPopup = new BasicComboPopup(this.comboBox){

                @Override
                public void show(Component component, int n, int n2) {
                    super.show(component, n - button.getWidth(), n2);
                }

                @Override
                protected Rectangle computePopupBounds(int n, int n2, int n3, int n4) {
                    Rectangle rectangle = super.computePopupBounds(n, n2, n3, n4);
                    rectangle.width += button.getWidth();
                    return rectangle;
                }
            };
            basicComboPopup.setLightWeightPopupEnabled(false);
            return basicComboPopup;
        }

        @Override
        protected FocusListener createFocusListener() {
            return new BasicComboBoxUI.FocusHandler(){

                @Override
                public void focusLost(FocusEvent focusEvent) {
                    if (TextFieldComboBoxUI.this.isPopupVisible(TextFieldComboBoxUI.this.comboBox)) {
                        TextFieldComboBoxUI.this.setPopupVisible(TextFieldComboBoxUI.this.comboBox, false);
                    }
                }
            };
        }

        protected Document createDocument() {
            return new PlainDocument(){

                private String filter(String string2) {
                    return RegularExpressions.NEWLINE.splitAsStream(string2).filter((String string) -> string.length() > 0 && string.length() < 100).findFirst().orElse("");
                }

                @Override
                public void insertString(int n, String string, AttributeSet attributeSet) throws BadLocationException {
                    super.insertString(n, this.filter(string), attributeSet);
                }

                @Override
                public void replace(int n, int n2, String string, AttributeSet attributeSet) throws BadLocationException {
                    super.replace(n, n2, this.filter(string), attributeSet);
                }
            };
        }
    }
}

