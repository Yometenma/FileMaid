package net.filemaid.hash;

public interface Hash {
    public void update(byte[] var1, int var2, int var3);

    public String digest();
}

