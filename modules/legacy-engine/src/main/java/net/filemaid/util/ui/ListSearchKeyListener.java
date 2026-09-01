package net.filemaid.util.ui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.JList;
import net.filemaid.util.RegularExpressions;

public class ListSearchKeyListener
extends KeyAdapter {
    private long lastKeyTyped = 0L;
    private long lastKeyTypedTimeout = 1500L;
    private String query = "";
    private static final Pattern INPUT_KEY_PATTERN = Pattern.compile("[\\p{Graph}\\p{Blank}]+");

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        if (this.query.isEmpty()) {
            return;
        }
        switch (keyEvent.getKeyCode()) {
            case 39: {
                this.handleArrowKey(keyEvent, 1);
                break;
            }
            case 37: {
                this.handleArrowKey(keyEvent, -1);
                break;
            }
            case 8: 
            case 127: {
                this.handleDeleteKey(keyEvent);
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {
        String string = String.valueOf(keyEvent.getKeyChar());
        if (INPUT_KEY_PATTERN.matcher(string).matches()) {
            this.handleInputKey(keyEvent);
        }
    }

    protected void handleInputKey(KeyEvent keyEvent) {
        if (this.lastKeyTyped + this.lastKeyTypedTimeout < keyEvent.getWhen()) {
            this.query = "";
        }
        this.lastKeyTyped = keyEvent.getWhen();
        this.query = this.query + keyEvent.getKeyChar();
        this.commit(keyEvent);
    }

    protected void handleDeleteKey(KeyEvent keyEvent) {
        this.lastKeyTyped = keyEvent.getWhen();
        this.query = this.query.substring(0, this.query.length() - 1);
        this.commit(keyEvent);
    }

    protected void handleArrowKey(KeyEvent keyEvent, int n) {
        JList jList = (JList)keyEvent.getSource();
        int n2 = jList.getSelectedIndex();
        if (n2 < 0) {
            return;
        }
        this.find(jList, n2 + n, n > 0);
        keyEvent.consume();
    }

    protected void commit(KeyEvent keyEvent) {
        JList jList;
        int n = (jList = (JList)keyEvent.getSource()).getSelectedIndex();
        this.find(jList, n < 0 ? 0 : n, true);
        keyEvent.consume();
    }

    protected Pattern pattern() {
        String string2 = RegularExpressions.SPACE.splitAsStream(this.query).filter(string -> !string.isEmpty()).map(string -> Pattern.quote(string)).collect(Collectors.joining("[\\p{Blank}\\p{Punct}]+"));
        return Pattern.compile(string2, 258);
    }

    protected void find(JList jList, int n, boolean bl) {
        boolean bl2 = this.query.isEmpty() ? false : (bl ? this.next(jList, this.pattern(), n) : this.previous(jList, this.pattern(), n));
        if (!bl2) {
            jList.clearSelection();
        }
    }

    protected boolean next(JList jList, Pattern pattern, int n) {
        int n2;
        for (n2 = n; n2 < jList.getModel().getSize(); ++n2) {
            if (!this.match(jList, n2, pattern)) continue;
            return true;
        }
        for (n2 = 0; n2 < n; ++n2) {
            if (!this.match(jList, n2, pattern)) continue;
            return true;
        }
        return false;
    }

    protected boolean previous(JList jList, Pattern pattern, int n) {
        int n2;
        for (n2 = n; n2 >= 0; --n2) {
            if (!this.match(jList, n2, pattern)) continue;
            return true;
        }
        for (n2 = jList.getModel().getSize() - 1; n2 > n; --n2) {
            if (!this.match(jList, n2, pattern)) continue;
            return true;
        }
        return false;
    }

    protected boolean match(JList jList, int n, Pattern pattern) {
        String string = jList.getModel().getElementAt(n).toString();
        if (pattern.matcher(string).find()) {
            jList.setSelectedIndex(n);
            jList.ensureIndexIsVisible(n);
            return true;
        }
        return false;
    }
}

