package net.filemaid;

public interface ExitCode {
    public static final int SUCCESS = 0;
    public static final int ERROR = 1;
    public static final int BAD_LICENSE = 2;
    public static final int FAILURE = 3;
    public static final int DIE = 4;
    public static final int NOOP = 100;

    public static String getErrorMessage(int n) {
        switch (n) {
            case 0: {
                return "Done";
            }
            case 1: {
                return "Error";
            }
            case 2: {
                return "Bad License";
            }
            case 3: {
                return "Failure";
            }
            case 4: {
                return "Abort";
            }
            case 100: {
                return "Done";
            }
        }
        return "Error (" + n + ")";
    }

    public static String getErrorKaomoji(int n) {
        switch (n) {
            case 0: {
                return "\u30fe(\uff20\u2312\u30fc\u2312\uff20)\u30ce";
            }
            case 1: {
                return "(o_O)";
            }
            case 2: {
                return "(>_<)";
            }
            case 3: {
                return "(\u00d7_\u00d7)\u2312\u2606";
            }
            case 4: {
                return "(\u00d7_\u00d7)";
            }
            case 100: {
                return "\u00af\\_(\u30c4)_/\u00af";
            }
        }
        return "/\u2572/\\\u256d[\u2609\ufe4f\u2609]\u256e/\\\u2571\\";
    }
}

