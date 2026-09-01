package net.filemaid.hash;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Locale;
import net.filemaid.hash.Hash;

public class MessageDigestHash
implements Hash {
    private final MessageDigest md;

    public MessageDigestHash(String string) {
        try {
            this.md = MessageDigest.getInstance(string);
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public MessageDigestHash(MessageDigest messageDigest) {
        this.md = messageDigest;
    }

    @Override
    public void update(byte[] byArray, int n, int n2) {
        this.md.update(byArray, n, n2);
    }

    @Override
    public String digest() {
        return String.format(Locale.ROOT, "%0" + this.md.getDigestLength() * 2 + "x", new BigInteger(1, this.md.digest()));
    }
}

