package net.filemaid.format;

import net.filemaid.format.AutoUnitDecimal;

public class FrameRate
extends AutoUnitDecimal {
    public FrameRate(double d) {
        super(d);
    }

    @Override
    public String getUnit() {
        return " fps";
    }

    public static FrameRate parse(String string) {
        double d = Double.parseDouble(string);
        return new FrameRate(d);
    }
}

