package net.filemaid.subtitle;

public class SubtitleElement {
    private final long start;
    private final long end;
    private final String text;

    public SubtitleElement(long l, long l2, String string) {
        this.start = l;
        this.end = l2;
        this.text = string;
    }

    public long getStart() {
        return this.start;
    }

    public long getEnd() {
        return this.end;
    }

    public String getText() {
        return this.text;
    }

    public String toString() {
        return String.format("[%s, %s] %s", this.start, this.end, this.text);
    }
}

