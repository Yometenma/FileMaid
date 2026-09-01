package net.filemaid;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WTypes;
import java.io.FileNotFoundException;
import java.util.NoSuchElementException;
import net.filemaid.License;
import net.filemaid.LicenseError;
import net.filemaid.Logging;
import net.filemaid.platform.mac.MASReceipt;
import net.filemaid.platform.mac.MASReceiptValidationFailure;
import net.filemaid.platform.mac.MacAppUtilities;
import net.filemaid.platform.windows.PackageOrigin;
import net.filemaid.platform.windows.WinAppUtilities;
import net.filemaid.util.Digest;
import net.filemaid.web.HttpClientError;
import net.filemaid.web.HttpNetworkError;
import net.filemaid.web.HttpServerError;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.openpgp.PGPSignature;

public enum LicenseModel {
    PGPSignedMessage{

        @Override
        public License check() throws LicenseError {
            try {
                Digest.digestApplicationClass(PGPSignature.class, "e7c474f471d59f399b7837dfb13db96d488d7a22abd50e6c00191b48d9daa0dc");
                Digest.digestApplicationClass(Signer.class, "ec10f70d236ea88c978dbb88ae2b1a298b6049398287eb76b574b79c69315173");
                return License.INSTANCE.get().check(false);
            }
            catch (HttpServerError httpServerError) {
                throw new LicenseError("Server Error: " + httpServerError.getMessage(), httpServerError);
            }
            catch (HttpClientError httpClientError) {
                if (httpClientError.isRateLimited()) {
                    throw new LicenseError("Network Connection Error: Your VPN server / public IP has been temporarily banned [" + httpClientError.getStatus() + "]", httpClientError);
                }
                throw new LicenseError("Client Error: " + httpClientError.getMessage(), httpClientError);
            }
            catch (HttpNetworkError httpNetworkError) {
                throw new LicenseError("Network Error: " + httpNetworkError.getMessage(), httpNetworkError);
            }
            catch (FileNotFoundException | IllegalStateException | NoSuchElementException exception) {
                throw new LicenseError(exception.getMessage());
            }
            catch (Throwable throwable) {
                Logging.trace("Unknown Error", throwable);
                throw new LicenseError(throwable.toString());
            }
        }
    }
    ,
    MicrosoftStore{

        @Override
        public Object check() throws LicenseError {
            try {
                Digest.digestApplicationClass(Native.class, "8456be5fd4a53a4a24f612b72c487c80035f520ef8df067a85172c29b5928bd1");
                Digest.digestApplicationClass(WTypes.class, "076e22232ba01704e260f8dfba2c2050dec411ee9ef158c6e9dc53bf8da02f4a");
                String string = WinAppUtilities.getPackageFamilyName();
                if (string == null || !string.equals("PointPlanck.FileBot_49ex9gnthnt12")) {
                    throw new LicenseError("Bad Package: " + string);
                }
                PackageOrigin packageOrigin = WinAppUtilities.getStagedPackageOrigin();
                if (packageOrigin != PackageOrigin.Store) {
                    throw new LicenseError("Bad Receipt: " + packageOrigin);
                }
                return "Microsoft Store License";
            }
            catch (Throwable throwable) {
                throw new LicenseError(throwable.toString());
            }
        }
    }
    ,
    MacAppStore{

        @Override
        public Object check() throws LicenseError {
            try {
                Digest.digestApplicationClass(CMSSignedData.class, "389d8d95b23c1d52111551356a58a19e8d5c7eadc0c0193483ed9792e021142b");
                Digest.digestApplicationClass(ContentInfo.class, "a1bb504f07083547f0d7e66726310df04bcbd56a7b25d5f76bf4b5c7bbb987d3");
                Digest.digestApplicationClass(Signer.class, "ec10f70d236ea88c978dbb88ae2b1a298b6049398287eb76b574b79c69315173");
                String string = MacAppUtilities.getSigningIdentifier();
                if (string == null || string.length() > 0 && !string.equals("net.filemaid.FileBot")) {
                    throw new LicenseError("Bad Package: " + string);
                }
                try {
                    MASReceipt mASReceipt = MASReceipt.read(MacAppUtilities.NSBundle_mainBundle_appStoreReceiptURL_path());
                    mASReceipt.check("net.filemaid.FileBot");
                }
                catch (FileNotFoundException | MASReceiptValidationFailure throwable) {
                    throw new LicenseError("Bad Receipt: " + throwable.getMessage());
                }
                return "Mac App Store License";
            }
            catch (Throwable throwable) {
                throw new LicenseError(throwable.toString());
            }
        }
    };


    public boolean isFile() {
        return this == PGPSignedMessage;
    }

    public boolean isMASReceipt() {
        return this == MacAppStore;
    }

    public abstract Object check() throws LicenseError;
}

