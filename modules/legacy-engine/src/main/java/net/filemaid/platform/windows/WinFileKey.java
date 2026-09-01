package net.filemaid.platform.windows;

import java.math.BigInteger;
import java.util.Objects;

public class WinFileKey {
    private final long volumeSerialNumber;
    private final BigInteger fileId;

    public WinFileKey(long l, BigInteger bigInteger) {
        this.volumeSerialNumber = l;
        this.fileId = bigInteger;
    }

    public int hashCode() {
        return Objects.hash(this.volumeSerialNumber, this.fileId);
    }

    public boolean equals(Object object) {
        if (object instanceof WinFileKey) {
            WinFileKey winFileKey = (WinFileKey)object;
            return this.volumeSerialNumber == winFileKey.volumeSerialNumber && this.fileId.equals(winFileKey.fileId);
        }
        return false;
    }

    public String toString() {
        return String.format("(vol=%016x,fid=%032x)", this.volumeSerialNumber, this.fileId);
    }
}

