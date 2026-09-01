package net.filemaid.format;

import net.filemaid.format.AutoScaleInteger;

public class FileSize
extends AutoScaleInteger {
    public FileSize(long l) {
        super(l, AutoScaleInteger.Scale.AUTO);
    }

    public FileSize(long l, AutoScaleInteger.Scale scale) {
        super(l, scale);
    }

    @Override
    public FileSize scale(AutoScaleInteger.Scale scale) {
        return new FileSize(this.value, scale);
    }

    public FileSize getGB() {
        return this.scale(AutoScaleInteger.Scale.G);
    }

    public FileSize getMB() {
        return this.scale(AutoScaleInteger.Scale.M);
    }

    public FileSize getKB() {
        return this.scale(AutoScaleInteger.Scale.K);
    }

    @Override
    public String getUnit(AutoScaleInteger.Scale scale) {
        switch (scale) {
            case G: {
                return "GB";
            }
            case M: {
                return "MB";
            }
            case K: {
                return "kB";
            }
        }
        return "bytes";
    }
}

