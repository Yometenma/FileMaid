package net.filemaid.subtitle;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import net.filemaid.Logging;
import net.filemaid.subtitle.SubtitleElement;

public abstract class SubtitleReader
implements Iterator<SubtitleElement>,
Closeable {
    protected Scanner scanner;
    protected SubtitleElement current;

    public SubtitleReader(Scanner scanner) {
        this.scanner = scanner;
    }

    protected abstract SubtitleElement readNext() throws Exception;

    @Override
    public boolean hasNext() {
        while (this.current == null && this.scanner.hasNextLine()) {
            try {
                this.current = this.readNext();
            }
            catch (Exception exception) {
                Logging.debug.finest(Logging.cause(exception));
            }
        }
        return this.current != null;
    }

    @Override
    public SubtitleElement next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }
        try {
            SubtitleElement subtitleElement = this.current;
            return subtitleElement;
        }
        finally {
            this.current = null;
        }
    }

    @Override
    public void close() {
        this.scanner.close();
    }

    public List<SubtitleElement> decode() {
        ArrayList<SubtitleElement> arrayList = new ArrayList<SubtitleElement>(2048);
        while (this.hasNext()) {
            arrayList.add(this.next());
        }
        return arrayList;
    }
}

