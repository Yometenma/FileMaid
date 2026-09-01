package net.filemaid.hash;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.hash.Hash;
import net.filemaid.hash.HashType;
import net.filemaid.hash.VerificationFileReader;
import net.filemaid.util.FileUtilities;

public final class VerificationUtilities {
    public static final Pattern EMBEDDED_CHECKSUM = Pattern.compile("(?<=\\[|\\()(\\p{XDigit}{8})(?=\\]|\\))");

    public static String getEmbeddedChecksum(CharSequence charSequence) {
        Matcher matcher = EMBEDDED_CHECKSUM.matcher(charSequence);
        String string = null;
        while (matcher.find()) {
            string = matcher.group();
        }
        return string;
    }

    public static String getHashFromVerificationFile(File file, HashType hashType, int n) throws IOException {
        return VerificationUtilities.getHashFromVerificationFile(file.getParentFile(), file, hashType, 0, n);
    }

    private static String getHashFromVerificationFile(File file, File file2, HashType hashType, int n, int n2) throws IOException {
        if (file == null || n > n2) {
            return null;
        }
        for (File file3 : FileUtilities.getChildren(file, hashType.getFilter())) {
            VerificationFileReader verificationFileReader = hashType.newReader(file3);
            try {
                while (verificationFileReader.hasNext()) {
                    Map.Entry<File, String> entry = verificationFileReader.next();
                    File file4 = new File(file, entry.getKey().getPath());
                    if (!file2.equals(file4)) continue;
                    return entry.getValue();
                }
            }
            finally {
                if (verificationFileReader == null) continue;
                verificationFileReader.close();
            }
        }
        return VerificationUtilities.getHashFromVerificationFile(file.getParentFile(), file2, hashType, n + 1, n2);
    }

    public static HashType getHashType(File file) {
        for (HashType hashType : HashType.values()) {
            if (!hashType.getFilter().accept(file)) continue;
            return hashType;
        }
        return null;
    }

    public static HashType getHashType(String string) {
        for (HashType hashType : HashType.values()) {
            if (!hashType.toString().equalsIgnoreCase(string) && !hashType.getAlgorithm().equalsIgnoreCase(string) && !hashType.getFilter().acceptExtension(string)) continue;
            return hashType;
        }
        return null;
    }

    public static String computeHash(File file, HashType hashType) throws IOException {
        Hash hash = hashType.newHash();
        try (FileInputStream fileInputStream = new FileInputStream(file);){
            byte[] byArray = new byte[0x400000];
            int n = 0;
            while ((n = ((InputStream)fileInputStream).read(byArray)) >= 0) {
                hash.update(byArray, 0, n);
                if (!Thread.interrupted()) continue;
                throw new CancellationException();
            }
        }
        return hash.digest();
    }

    public static String computeHash(byte[] byArray, HashType hashType) {
        Hash hash = hashType.newHash();
        hash.update(byArray, 0, byArray.length);
        return hash.digest();
    }

    public static String crc32(File file) throws IOException {
        return VerificationUtilities.computeHash(file, HashType.SFV);
    }

    public static String crc32(byte[] byArray) {
        return VerificationUtilities.computeHash(byArray, HashType.SFV);
    }

    public static String md5(File file) throws IOException {
        return VerificationUtilities.computeHash(file, HashType.MD5);
    }

    public static String md5(byte[] byArray) {
        return VerificationUtilities.computeHash(byArray, HashType.MD5);
    }

    public static String sha256(File file) throws IOException {
        return VerificationUtilities.computeHash(file, HashType.SHA256);
    }

    public static String sha256(byte[] byArray) {
        return VerificationUtilities.computeHash(byArray, HashType.SHA256);
    }

    private VerificationUtilities() {
        throw new UnsupportedOperationException();
    }
}

