package net.filemaid.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public enum BOM {
    UTF_8((byte) -17, (byte) -69, (byte) -65),
    UTF_16BE((byte) -2, (byte) -1),
    UTF_16LE((byte) -1, (byte) -2),
    UTF_32BE((byte) 0, (byte) 0, (byte) -2, (byte) -1),
    UTF_32LE((byte) -1, (byte) -2, (byte) 0, (byte) 0),
    GB_18030((byte) -124, (byte) 49, (byte) -107, (byte) 51);

    public static final int SIZE = 4;
    private byte[] bom;

    private BOM(byte ... byArray) {
        this.bom = byArray;
    }

    public int size() {
        return this.bom.length;
    }

    public boolean matches(byte[] byArray) {
        if (byArray.length < this.bom.length) {
            return false;
        }
        for (int i = 0; i < this.bom.length; ++i) {
            if (this.bom[i] == byArray[i]) continue;
            return false;
        }
        return true;
    }

    public Charset getCharset() {
        switch (this) {
            case UTF_8: {
                return StandardCharsets.UTF_8;
            }
            case UTF_16BE: {
                return StandardCharsets.UTF_16BE;
            }
            case UTF_16LE: {
                return StandardCharsets.UTF_16LE;
            }
            case UTF_32BE: {
                return Charset.forName("UTF-32BE");
            }
            case UTF_32LE: {
                return Charset.forName("UTF-32LE");
            }
            case GB_18030: {
                return Charset.forName("GB18030");
            }
        }
        return null;
    }

    public static BOM detect(byte[] byArray) {
        for (BOM bOM : BOM.values()) {
            if (!bOM.matches(byArray)) continue;
            return bOM;
        }
        return null;
    }
}

