package net.filemaid.format;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Iterator;
import net.filemaid.format.StringBinding;

public class Resolution
implements Iterable<Integer>,
StringBinding {
    private final int width;
    private final int height;

    public Resolution(int n, int n2) {
        this.width = n;
        this.height = n2;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public Number getMpx() {
        return BigDecimal.valueOf((double)(this.width * this.height) / 1000000.0).setScale(1, RoundingMode.HALF_UP);
    }

    @Override
    public Iterator<Integer> iterator() {
        return Arrays.asList(this.width, this.height).iterator();
    }

    @Override
    public String toString() {
        return this.width + "x" + this.height;
    }

    public static Resolution parse(String string, String string2) {
        if (string == null || string.isEmpty() || string2 == null || string2.isEmpty()) {
            return null;
        }
        return new Resolution(Integer.parseInt(string), Integer.parseInt(string2));
    }
}

