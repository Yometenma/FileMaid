package net.filemaid.format;

import net.filemaid.format.AutoUnitDecimal;

public class ChannelCount
extends AutoUnitDecimal {
    private final boolean layout;

    public ChannelCount(double d, boolean bl) {
        super(d);
        this.layout = bl;
    }

    @Override
    public String getUnit() {
        return this.layout ? "" : "ch";
    }

    @Override
    public int getFractionDigits() {
        return this.layout ? 1 : 0;
    }

    public static ChannelCount count(Number number) {
        return new ChannelCount(number.intValue(), false);
    }

    public static ChannelCount layout(Number number) {
        return new ChannelCount(number.doubleValue(), true);
    }
}

