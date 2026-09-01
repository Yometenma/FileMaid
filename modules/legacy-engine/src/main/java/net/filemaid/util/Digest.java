package net.filemaid.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.filemaid.util.ByteBufferOutputStream;

public class Digest {
    public static String md5(String string) {
        return Digest.md5(StandardCharsets.UTF_8.encode(string));
    }

    public static String md5(byte[] byArray) {
        return Digest.md5(ByteBuffer.wrap(byArray));
    }

    public static String md5(URL uRL) {
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
        try (InputStream inputStream = uRL.openStream();){
            byteBufferOutputStream.transferFully(inputStream);
        }
        catch (IOException iOException) {
            throw new UncheckedIOException(iOException);
        }
        return Digest.md5(byteBufferOutputStream.getByteBuffer());
    }

    public static String md5(ByteBuffer byteBuffer) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(byteBuffer);
            return String.format(Locale.ROOT, "%032x", new BigInteger(1, messageDigest.digest()));
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }

    public static String sha256(String string) {
        return Digest.sha256(StandardCharsets.UTF_8.encode(string));
    }

    public static String sha256(byte[] byArray) {
        return Digest.sha256(ByteBuffer.wrap(byArray));
    }

    public static String sha256(URL uRL) {
        ByteBufferOutputStream byteBufferOutputStream = new ByteBufferOutputStream(65536);
        try (InputStream inputStream = uRL.openStream();){
            byteBufferOutputStream.transferFully(inputStream);
        }
        catch (IOException iOException) {
            throw new UncheckedIOException(iOException);
        }
        return Digest.sha256(byteBufferOutputStream.getByteBuffer());
    }

    public static String sha256(ByteBuffer byteBuffer) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(byteBuffer);
            return String.format(Locale.ROOT, "%064x", new BigInteger(1, messageDigest.digest()));
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(noSuchAlgorithmException);
        }
    }

    public static String digestApplicationClass(Class<?> clazz, String string) throws Exception {
        File file = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
        String string2 = Digest.digestApplicationJar(file);
        if (string != null && !string.equals(string2)) {
            throw new IncompatibleClassChangeError(string2);
        }
        return string2;
    }

    public static String digestApplicationJar(File file) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] byArray = new byte[65536];
        try (JarFile jarFile = new JarFile(file, false);){
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                if (!jarEntry.getName().endsWith(".class")) continue;
                messageDigest.update(jarEntry.getName().getBytes(StandardCharsets.UTF_8));
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                int n = 0;
                while ((n = inputStream.read(byArray)) >= 0) {
                    messageDigest.update(byArray, 0, n);
                }
            }
        }
        return String.format(Locale.ROOT, "%064x", new BigInteger(1, messageDigest.digest()));
    }
}

