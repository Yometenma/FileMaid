package net.filemaid.platform.mac;

import com.sun.jna.platform.mac.IOKit;
import com.sun.jna.platform.mac.IOKitUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import net.filemaid.platform.mac.MASReceiptValidationFailure;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;

public class MASReceipt {
    private final CMSSignedData pkcs;
    private final ASN1Primitive asn1;

    public static MASReceipt read(String string) throws FileNotFoundException {
        if (string == null || string.isEmpty()) {
            throw new FileNotFoundException("receipt");
        }
        File file = new File(string);
        if (!file.exists()) {
            throw new FileNotFoundException(string);
        }
        try {
            return new MASReceipt(Files.readAllBytes(file.toPath()));
        }
        catch (Exception exception) {
            throw new FileNotFoundException(string + ": " + exception.getMessage());
        }
    }

    public MASReceipt(byte[] byArray) throws IOException, CMSException {
        this.pkcs = new CMSSignedData(byArray);
        this.asn1 = new ASN1InputStream((byte[])this.pkcs.getSignedContent().getContent()).readObject();
    }

    public void check(String string) throws MASReceiptValidationFailure {
        if (!this.verifySignature()) {
            throw new MASReceiptValidationFailure("Signature mismatch", this);
        }
        if (!this.getBundleIdentifier().equals(string)) {
            throw new MASReceiptValidationFailure("Bundle mismatch", this);
        }
        if (!this.verifyHash()) {
            throw new MASReceiptValidationFailure("Hash mismatch", this);
        }
    }

    public boolean verifySignature() {
        try {
            for (SignerInformation signerInformation : this.pkcs.getSignerInfos().getSigners()) {
                for (Object object : this.pkcs.getCertificates().getMatches(signerInformation.getSID())) {
                    X509CertificateHolder x509CertificateHolder = (X509CertificateHolder)object;
                    X509Certificate x509Certificate = new JcaX509CertificateConverter().getCertificate(x509CertificateHolder);
                    SignerInformationVerifier signerInformationVerifier = new JcaSimpleSignerInfoVerifierBuilder().build(x509Certificate);
                    if (signerInformation.verify(signerInformationVerifier)) continue;
                    return false;
                }
            }
            return true;
        }
        catch (Exception exception) {
            throw new IllegalStateException("Receipt Signature", exception);
        }
    }

    public boolean verifyHash() {
        try {
            byte[] byArray = this.getOpaqueValue();
            byte[] byArray2 = this.getBundleIdentifierBytes();
            byte[] byArray3 = this.getSHA1Hash();
            for (byte[] byArray4 : MASReceipt.copy_mac_address_en0_en1()) {
                if (!MASReceipt.verifyHash(byArray4, byArray, byArray2, byArray3)) continue;
                return true;
            }
            return MASReceipt.verifyHash(MASReceipt.copy_mac_address(), byArray, byArray2, byArray3);
        }
        catch (Exception exception) {
            throw new IllegalStateException("Receipt Hash: " + exception.getMessage(), exception);
        }
    }

    private static boolean verifyHash(byte[] byArray, byte[] byArray2, byte[] byArray3, byte[] byArray4) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        messageDigest.update(byArray);
        messageDigest.update(byArray2);
        messageDigest.update(byArray3);
        return Arrays.equals(messageDigest.digest(), byArray4);
    }

    public String getBundleIdentifier() {
        return this.getStringField(2);
    }

    public String getAppVersion() {
        return this.getStringField(3);
    }

    public byte[] getBundleIdentifierBytes() {
        return this.getField(2);
    }

    public byte[] getOpaqueValue() {
        return this.getField(4);
    }

    public byte[] getSHA1Hash() {
        return this.getField(5);
    }

    public String getOriginalApplicationVersion() {
        return this.getStringField(19);
    }

    public Instant getReceiptCreationDate() {
        return this.getDateField(12);
    }

    protected byte[] getField(int n) {
        for (ASN1Encodable aSN1Encodable : ASN1Set.getInstance(this.asn1)) {
            ASN1Sequence aSN1Sequence = ASN1Sequence.getInstance(aSN1Encodable);
            ASN1Integer aSN1Integer = ASN1Integer.getInstance(aSN1Sequence.getObjectAt(0));
            if (n != aSN1Integer.getValue().intValue()) continue;
            return DEROctetString.getInstance(aSN1Sequence.getObjectAt(2)).getOctets();
        }
        throw new IllegalStateException("Receipt Field " + n);
    }

    protected String getStringField(int n) {
        try {
            return ASN1Primitive.fromByteArray(this.getField(n)).toString();
        }
        catch (IOException iOException) {
            throw new IllegalStateException(iOException);
        }
    }

    protected Instant getDateField(int n) {
        return Instant.parse(this.getStringField(n));
    }

    public static byte[] copy_mac_address() {
        IOKit.IOIterator iOIterator = IOKitUtil.getMatchingServices((String)"IOEthernetInterface");
        if (iOIterator == null) {
            throw new IllegalStateException("IOEthernetInterface");
        }
        IOKit.IORegistryEntry iORegistryEntry = iOIterator.next();
        while (iORegistryEntry != null) {
            IOKit.IORegistryEntry iORegistryEntry2;
            byte[] byArray;
            Boolean bl = iORegistryEntry.getBooleanProperty("IOPrimaryInterface");
            if (bl != null && bl.booleanValue() && (byArray = (iORegistryEntry2 = iORegistryEntry.getParentEntry("IOService")).getByteArrayProperty("IOMACAddress")) != null) {
                return byArray;
            }
            iORegistryEntry = iOIterator.next();
        }
        throw new IllegalStateException("IOMACAddress");
    }

    public static List<byte[]> copy_mac_address_en0_en1() throws IOException {
        ArrayList<byte[]> arrayList = new ArrayList<byte[]>(2);
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements()) {
            NetworkInterface networkInterface = enumeration.nextElement();
            String string = networkInterface.getName();
            if (string == null || !string.startsWith("en")) continue;
            try {
                byte[] byArray = networkInterface.getHardwareAddress();
                if (byArray == null) continue;
                arrayList.add(byArray);
            }
            catch (Exception exception) {}
        }
        return arrayList;
    }
}

