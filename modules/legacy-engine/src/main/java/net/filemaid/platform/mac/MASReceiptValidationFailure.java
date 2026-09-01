package net.filemaid.platform.mac;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import net.filemaid.platform.mac.MASReceipt;

public class MASReceiptValidationFailure
extends Throwable {
    private MASReceipt receipt;

    public MASReceiptValidationFailure(String string, MASReceipt mASReceipt) {
        super(string);
        this.receipt = Objects.requireNonNull(mASReceipt);
    }

    @Override
    public String getMessage() {
        Supplier[] supplierArray = new Supplier[4];
        supplierArray[0] = () -> super.getMessage();
        supplierArray[1] = this.receipt::getBundleIdentifier;
        supplierArray[2] = this.receipt::getAppVersion;
        supplierArray[3] = this.receipt::getReceiptCreationDate;
        return MASReceiptValidationFailure.format("%s: %s %s [%s]", supplierArray);
    }

    private static String format(String string, Supplier ... supplierArray) {
        return String.format(string, Arrays.stream(supplierArray).map(supplier -> {
            try {
                return supplier.get();
            }
            catch (Exception exception) {
                return exception;
            }
        }).toArray());
    }

    public static void relaunch() {
        System.err.println("173 MAS Receipt Validation Failure");
        System.exit(173);
    }
}

