package net.filemaid.format;

import net.filemaid.format.AutoScaleInteger;

public class BitRate
extends AutoScaleInteger {
    public BitRate(Number number) {
        super(number.longValue(), AutoScaleInteger.Scale.AUTO);
    }

    public BitRate(long l, AutoScaleInteger.Scale scale) {
        super(l, scale);
    }

    @Override
    public BitRate scale(AutoScaleInteger.Scale scale) {
        return new BitRate(this.value, scale);
    }

    public BitRate getGbps() {
        return this.scale(AutoScaleInteger.Scale.G);
    }

    public BitRate getMbps() {
        return this.scale(AutoScaleInteger.Scale.M);
    }

    public BitRate getKbps() {
        return this.scale(AutoScaleInteger.Scale.K);
    }

    @Override
    public String getUnit(AutoScaleInteger.Scale scale) {
        switch (scale) {
            case G: {
                return "Gbps";
            }
            case M: {
                return "Mbps";
            }
            case K: {
                return "kbps";
            }
        }
        return "bps";
    }

    public static BitRate parse(String string) {
        long l = (long)Double.parseDouble(string);
        return new BitRate(l, AutoScaleInteger.Scale.AUTO);
    }
}

