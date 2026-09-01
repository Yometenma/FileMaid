package net.filemaid.hash;

import java.util.Locale;
import java.util.zip.Checksum;
import net.filemaid.hash.Hash;

public class ChecksumHash
implements Hash {
    private final Checksum checksum;

    public ChecksumHash(Checksum checksum) {
        this.checksum = checksum;
    }

    @Override
    public void update(byte[] byArray, int n, int n2) {
        this.checksum.update(byArray, n, n2);
    }

    @Override
    public String digest() {
        return String.format(Locale.ROOT, "%08X", this.checksum.getValue());
    }
}

