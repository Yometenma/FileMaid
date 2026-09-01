package net.filemaid.subtitle;

import java.io.IOException;
import net.filemaid.subtitle.SubRipReader;
import net.filemaid.subtitle.SubtitleElement;

public class SubRipWriter {
    private Appendable out;
    private int lineNumber;

    public SubRipWriter(Appendable appendable) {
        this.out = appendable;
        this.lineNumber = 0;
    }

    public void write(SubtitleElement subtitleElement) throws IOException {
        this.out.append(Integer.toString(++this.lineNumber));
        this.out.append('\n');
        this.out.append(SubRipReader.formatInterval(subtitleElement.getStart(), subtitleElement.getEnd()));
        this.out.append('\n');
        this.out.append(subtitleElement.getText());
        this.out.append('\n');
        this.out.append('\n');
    }
}

