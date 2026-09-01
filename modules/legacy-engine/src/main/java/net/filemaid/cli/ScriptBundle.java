package net.filemaid.cli;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import net.filemaid.Resource;
import net.filemaid.cli.CmdlineException;
import net.filemaid.cli.ScriptProvider;
import net.filemaid.util.ByteBufferOutputStream;
import net.filemaid.util.FileUtilities;

public class ScriptBundle
implements ScriptProvider {
    private final Resource<byte[]> bundle;
    private final Certificate certificate;

    public ScriptBundle(Resource<byte[]> resource) throws Exception {
        this(resource.memoize(), ScriptBundle.getCertificate());
    }

    private ScriptBundle(Resource<byte[]> resource, Certificate certificate) {
        this.bundle = resource;
        this.certificate = certificate;
    }

    private JarInputStream open() throws Exception {
        return new JarInputStream((InputStream)new ByteArrayInputStream(this.bundle.get()), true);
    }

    @Override
    public String getScript(String string) throws Exception {
        String string2 = string + ".groovy";
        try (JarInputStream jarInputStream = this.open();){
            JarEntry jarEntry = jarInputStream.getNextJarEntry();
            while (jarEntry != null) {
                block12: {
                    ByteBufferOutputStream byteBufferOutputStream;
                    block14: {
                        Certificate[] certificateArray;
                        block13: {
                            if (jarEntry.isDirectory() || !jarEntry.getName().equals(string2)) break block12;
                            byteBufferOutputStream = new ByteBufferOutputStream(jarEntry.getSize());
                            byteBufferOutputStream.transferFully(jarInputStream);
                            jarInputStream.closeEntry();
                            certificateArray = jarEntry.getCertificates();
                            if (certificateArray == null) break block13;
                            if (!Arrays.stream(jarEntry.getCertificates()).noneMatch(this.certificate::equals)) break block14;
                        }
                        throw new CmdlineException("Script not signed", Arrays.asList(certificateArray));
                    }
                    String string3 = StandardCharsets.UTF_8.decode(byteBufferOutputStream.getByteBuffer()).toString();
                    return string3;
                }
                jarEntry = jarInputStream.getNextJarEntry();
            }
        }
        throw new CmdlineException("Script not found: [" + string + "] not in " + this.list());
    }

    public List<String> list() throws Exception {
        ArrayList<String> arrayList = new ArrayList<String>();
        try (JarInputStream jarInputStream = this.open();){
            JarEntry jarEntry = jarInputStream.getNextJarEntry();
            while (jarEntry != null) {
                if (jarEntry.getName().endsWith(".groovy")) {
                    arrayList.add(FileUtilities.getNameWithoutExtension(jarEntry.getName()));
                }
                jarEntry = jarInputStream.getNextJarEntry();
            }
        }
        return arrayList;
    }

    public Map<String, String> getManifest() throws Exception {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        try (JarInputStream jarInputStream = this.open();){
            jarInputStream.getManifest().getMainAttributes().forEach((object, object2) -> linkedHashMap.put(object.toString(), object2.toString()));
        }
        return linkedHashMap;
    }

    public static URL getBundle() {
        return Resource.class.getProtectionDomain().getCodeSource().getLocation();
    }

    public static boolean update(ByteBuffer byteBuffer, int n, int n2) {
        try {
            ByteBuffer byteBuffer2 = byteBuffer.duplicate().position(n);
            ByteBuffer byteBuffer3 = byteBuffer.duplicate().limit(n).position(n2);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(ScriptBundle.getCertificate());
            signature.update(byteBuffer2);
            byte[] byArray = new byte[byteBuffer3.remaining()];
            byteBuffer3.get(byArray);
            return signature.verify(byArray);
        }
        catch (Exception exception) {
            return false;
        }
    }

    public static Certificate getCertificate() throws Exception {
        return CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(new byte[]{48, -126, 2, -25, 48, -126, 1, -49, -96, 3, 2, 1, 2, 2, 4, 73, -3, 33, 65, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 11, 5, 0, 48, 36, 49, 16, 48, 14, 6, 3, 85, 4, 10, 19, 7, 70, 105, 108, 101, 66, 111, 116, 49, 16, 48, 14, 6, 3, 85, 4, 3, 19, 7, 114, 101, 100, 110, 111, 97, 104, 48, 30, 23, 13, 49, 54, 48, 51, 51, 48, 48, 56, 48, 50, 53, 53, 90, 23, 13, 50, 54, 48, 51, 50, 56, 48, 56, 48, 50, 53, 53, 90, 48, 36, 49, 16, 48, 14, 6, 3, 85, 4, 10, 19, 7, 70, 105, 108, 101, 66, 111, 116, 49, 16, 48, 14, 6, 3, 85, 4, 3, 19, 7, 114, 101, 100, 110, 111, 97, 104, 48, -126, 1, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 1, 15, 0, 48, -126, 1, 10, 2, -126, 1, 1, 0, -112, -9, 44, 117, 77, 11, 72, -116, -29, -33, -103, 101, 54, 115, 48, 53, -117, 103, -108, 114, 6, 71, 76, -79, 99, 116, -126, 7, 15, -106, -119, 5, -13, -117, -125, 123, -76, 51, -70, -70, 115, -49, 67, 113, 76, 111, -17, 64, -34, -81, 42, 64, -39, -73, -6, 36, -55, 118, -15, 104, 78, -62, 17, 87, 78, -78, -7, 124, -8, 99, 8, 54, -30, -3, -117, 59, -43, 113, 114, -35, 120, 21, 41, 90, 16, -101, 107, 75, 31, -50, -65, 71, -11, 16, -42, 106, -84, -80, -22, -14, 85, 59, -89, 61, -125, 77, 62, -116, -93, -115, 33, -41, 37, 19, 123, -111, -102, 52, -68, -10, 76, -51, -106, -100, -90, -69, -89, -114, -77, -31, -49, -98, 39, -39, 47, 111, 18, 40, 74, 127, 70, -92, 89, -105, 57, -103, 99, -125, -74, -3, -37, -2, -127, 95, 42, 88, -115, -59, 84, 20, 68, -80, 115, 47, -100, 8, 82, 16, 77, 124, -25, -50, -64, 65, 99, -55, -33, 77, 44, -113, -74, -65, -3, -124, 50, -93, 104, 34, -99, 52, 85, -20, 2, 58, -93, -77, 12, 66, 61, 115, -113, 76, 49, 24, -112, -15, -44, -103, 15, -45, 121, -85, 54, 32, 36, -98, 63, -105, -22, -88, 102, 98, 61, -97, 11, -15, -13, 48, -128, -118, -7, -96, 6, -98, 79, 126, -91, 109, 126, 13, 67, 115, 12, 123, 1, 9, 50, 100, 66, -102, -118, -61, 25, -120, -50, 55, 2, 3, 1, 0, 1, -93, 33, 48, 31, 48, 29, 6, 3, 85, 29, 14, 4, 22, 4, 20, 100, 76, -1, -34, -9, -44, -101, 124, -67, -37, 17, -113, -36, 60, -103, -6, -57, 124, -120, 119, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 11, 5, 0, 3, -126, 1, 1, 0, 88, -127, 119, 98, 34, -11, -75, 32, 62, -80, -85, 90, 100, -115, 50, -41, -43, -102, -91, -50, 88, -45, -4, -2, 17, -15, -124, 110, -124, 23, 119, -91, -40, -67, 106, -55, -120, 60, 115, 123, 93, -27, -21, 118, 123, 59, 40, 82, 101, -88, 33, -107, -126, 101, 55, 34, -100, 31, -19, 50, 47, -77, 34, 50, 37, -125, -56, -6, -101, 71, -94, -108, 51, 7, 53, -39, -101, 102, 30, 78, 124, -85, 13, 115, 92, -54, -29, 73, 83, -124, -39, -112, 123, 29, -79, -23, -37, 90, 6, 62, -42, 96, 105, -111, -114, 65, -66, -114, 43, 46, 111, -108, -122, 10, -50, 17, 55, 13, 25, -102, -14, 13, -87, -3, 75, -17, -86, -74, -22, -1, -94, 65, 93, -76, 91, -43, -15, 0, 97, -105, 67, -74, -7, 121, -86, 106, 116, 90, -26, -43, -34, 98, -41, -39, 103, -116, -37, -54, 55, -41, 35, 65, 50, -3, -112, 47, 5, -21, -64, -11, -13, 106, 126, 11, -22, 91, 18, -76, 87, 107, 56, 63, 43, 8, 62, -7, -6, 58, 11, -5, -70, 9, -16, -98, 46, -12, -53, -101, -115, 97, 57, 109, -72, -48, -113, -89, -119, -121, -104, -83, 36, -87, 96, 88, -19, -29, -6, -109, -59, -52, 13, 84, 27, 11, -65, 13, 49, 56, 109, -114, 35, -109, -6, -54, 15, -25, -75, 94, -44, 122, -124, 14, 35, -57, -124, 64, 43, 56, -17, -125, -49, -18, 26, 98, 65, -56}));
    }
}

