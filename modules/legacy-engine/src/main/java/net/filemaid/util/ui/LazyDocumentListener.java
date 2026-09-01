package net.filemaid.util.ui;

import java.util.function.Consumer;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class LazyDocumentListener
implements DocumentListener {
    private Timer timer;
    private DocumentEvent lastEvent = null;

    public LazyDocumentListener(Consumer<DocumentEvent> consumer) {
        this(200, consumer);
    }

    public LazyDocumentListener(int n, Consumer<DocumentEvent> consumer) {
        this.timer = new Timer(n, actionEvent -> {
            consumer.accept(this.lastEvent);
            this.lastEvent = null;
        });
        this.timer.setRepeats(false);
    }

    @Override
    public void changedUpdate(DocumentEvent documentEvent) {
        this.lastEvent = documentEvent;
        this.timer.restart();
    }

    @Override
    public void insertUpdate(DocumentEvent documentEvent) {
        this.lastEvent = documentEvent;
        this.timer.restart();
    }

    @Override
    public void removeUpdate(DocumentEvent documentEvent) {
        this.lastEvent = documentEvent;
        this.timer.restart();
    }
}

