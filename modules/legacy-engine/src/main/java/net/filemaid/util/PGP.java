package net.filemaid.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.filemaid.util.RegularExpressions;
import net.filemaid.util.StringUtilities;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;

public class PGP {
    public static final String PGP_SIGNED_MESSAGE_TEMPLATE = "-----BEGIN PGP SIGNED MESSAGE-----\nHash: SHA512\n\nProduct: FileBot\nName: alice\nEmail: alice@example.com\nOrder: P123456789\nIssue-Date: 2018-06-01\nValid-Until: 2019-06-02\n-----BEGIN PGP SIGNATURE-----\n\n...\n-----END PGP SIGNATURE-----";
    private static final Pattern PGP_SIGNED_MESSAGE = Pattern.compile("-----BEGIN.PGP.SIGNED.MESSAGE-----[^\u00b7]*?-----END.PGP.SIGNATURE-----");
    private static final String PGP_NEWLINE = "\r\n";

    public static String verifyClearSignMessage(CharSequence charSequence, byte[] byArray) {
        try {
            int n;
            byte[] byArray2 = PGP.normalize(charSequence).getBytes(StandardCharsets.UTF_8);
            ArmoredInputStream armoredInputStream = new ArmoredInputStream(new ByteArrayInputStream(byArray2));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(256);
            while ((n = armoredInputStream.read()) >= 0 && armoredInputStream.isClearText()) {
                byteArrayOutputStream.write(n);
            }
            PGPPublicKeyRing pGPPublicKeyRing = new PGPPublicKeyRing(byArray, (KeyFingerPrintCalculator)new BcKeyFingerprintCalculator());
            PGPPublicKey pGPPublicKey = pGPPublicKeyRing.getPublicKey();
            PGPSignatureList pGPSignatureList = (PGPSignatureList)new BcPGPObjectFactory(armoredInputStream).nextObject();
            if (pGPSignatureList == null || pGPSignatureList.isEmpty()) {
                throw new PGPException("Bad Signature");
            }
            PGPSignature pGPSignature = pGPSignatureList.get(0);
            pGPSignature.init(new BcPGPContentVerifierBuilderProvider(), pGPPublicKey);
            byte[] byArray3 = byteArrayOutputStream.toByteArray();
            pGPSignature.update(byArray3, 0, byArray3.length - PGP_NEWLINE.length());
            if (!pGPSignature.verify()) {
                throw new PGPException("Bad Signature");
            }
            return new String(byArray3, StandardCharsets.UTF_8);
        }
        catch (IOException | PGPException exception) {
            throw new NoSuchElementException(PGP.message(exception.getMessage(), charSequence));
        }
    }

    private static String normalize(CharSequence charSequence) {
        return RegularExpressions.LINEBREAK.splitAsStream(charSequence).map(string -> {
            string = RegularExpressions.SPACE.matcher((CharSequence)string).replaceAll(" ");
            string = RegularExpressions.INVISIBLE_CONTROL_CHARACTERS.matcher((CharSequence)string).replaceAll("");
            return string.trim();
        }).collect(Collectors.joining(PGP_NEWLINE));
    }

    private static String message(String string, CharSequence charSequence) {
        return string + " (" + charSequence.length() + " chars, " + StringUtilities.lineCount(charSequence) + " lines)";
    }

    public static String findClearSignMessage(CharSequence charSequence) {
        Matcher matcher = PGP_SIGNED_MESSAGE.matcher(charSequence);
        if (matcher.find()) {
            return PGP.normalize(matcher.group());
        }
        throw new NoSuchElementException(PGP.message("PGP SIGNED MESSAGE not found", charSequence));
    }

    public static String findClearSignMessage(Readable readable) {
        String string = new Scanner(readable).findWithinHorizon(PGP_SIGNED_MESSAGE, 0);
        if (string != null) {
            return PGP.normalize(string);
        }
        throw new NoSuchElementException("PGP SIGNED MESSAGE not found");
    }
}

