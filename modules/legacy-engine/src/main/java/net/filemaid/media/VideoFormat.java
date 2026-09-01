package net.filemaid.media;

import java.util.Arrays;
import java.util.ResourceBundle;
import net.filemaid.util.RegularExpressions;

public class VideoFormat {
    public static final VideoFormat DEFAULT_GROUPS = new VideoFormat(VideoFormat.getIntArrayProperty("resolution.steps.w"), VideoFormat.getIntArrayProperty("resolution.steps.h"));
    private final int[] ws;
    private final int[] hs;

    public VideoFormat(int[] nArray, int[] nArray2) {
        this.ws = nArray;
        this.hs = nArray2;
    }

    public int guessFormat(int n, int n2) {
        int n3 = 0;
        for (int i = 0; i < this.ws.length - 1; ++i) {
            if (n < this.ws[i] && n2 < this.hs[i] && (n <= this.ws[i + 1] || n2 <= this.hs[i + 1])) continue;
            n3 = this.hs[i];
            break;
        }
        if (n3 > 0) {
            return n3;
        }
        throw new IllegalArgumentException("Illegal resolution: [" + n + ", " + n2 + "]");
    }

    public int[][] groups() {
        int[][] nArray = new int[this.ws.length][2];
        for (int i = 0; i < this.ws.length; ++i) {
            nArray[i][0] = this.ws[i];
            nArray[i][1] = this.hs[i];
        }
        return nArray;
    }

    public String toString() {
        return Arrays.deepToString((Object[])this.groups());
    }

    private static int[] getIntArrayProperty(String string) {
        return RegularExpressions.SPACE.splitAsStream(ResourceBundle.getBundle(VideoFormat.class.getName()).getString(string)).mapToInt(Integer::parseInt).toArray();
    }
}

