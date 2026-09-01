package net.filemaid.media;

import java.io.File;
import net.filemaid.hash.HashType;
import net.filemaid.hash.VerificationUtilities;
import net.filemaid.media.CachedFileAttribute;

public enum XattrChecksum {
    CRC32;

    private final CachedFileAttribute cache = CachedFileAttribute.cache(this.name(), this.name(), fileKey -> this.compute(fileKey.getFile()));

    public String computeIfAbsent(File file) throws Exception {
        return this.cache.get(file);
    }

    public String compute(File file) throws Exception {
        return VerificationUtilities.computeHash(file, HashType.SFV);
    }
}

