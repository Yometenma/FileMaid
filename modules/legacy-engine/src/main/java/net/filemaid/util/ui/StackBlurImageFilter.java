package net.filemaid.util.ui;

import java.awt.image.BufferedImage;

public class StackBlurImageFilter {
    private final int radius;
    private final int iterations;

    public StackBlurImageFilter(int n, int n2) {
        this.radius = n;
        this.iterations = n2;
    }

    public BufferedImage filter(BufferedImage bufferedImage) {
        int n = bufferedImage.getWidth();
        int n2 = bufferedImage.getHeight();
        int[] nArray = new int[n * n2];
        int[] nArray2 = new int[n * n2];
        bufferedImage.getRaster().getDataElements(0, 0, n, n2, nArray);
        for (int i = 0; i < this.iterations; ++i) {
            this.blur(nArray, nArray2, n, n2, this.radius);
            this.blur(nArray2, nArray, n2, n, this.radius);
        }
        bufferedImage.getRaster().setDataElements(0, 0, n, n2, nArray);
        return bufferedImage;
    }

    private void blur(int[] nArray, int[] nArray2, int n, int n2, int n3) {
        int n4;
        int n5 = n3 * 2 + 1;
        int n6 = n3 + 1;
        int n7 = 0;
        int[] nArray3 = new int[256 * n5];
        for (int i = 0; i < nArray3.length; ++i) {
            nArray3[i] = i / n5;
        }
        int[] nArray4 = new int[n6];
        if (n3 < n) {
            for (n4 = 0; n4 < nArray4.length; ++n4) {
                nArray4[n4] = n4;
            }
        } else {
            for (n4 = 0; n4 < n; ++n4) {
                nArray4[n4] = n4;
            }
            for (n4 = n; n4 < nArray4.length; ++n4) {
                nArray4[n4] = n - 1;
            }
        }
        for (n4 = 0; n4 < n2; ++n4) {
            int n8;
            int n9 = 0;
            int n10 = 0;
            int n11 = 0;
            int n12 = 0;
            int n13 = n4;
            int n14 = nArray[n7];
            n12 += n6 * (n14 >> 24 & 0xFF);
            n11 += n6 * (n14 >> 16 & 0xFF);
            n10 += n6 * (n14 >> 8 & 0xFF);
            n9 += n6 * (n14 & 0xFF);
            for (n8 = 1; n8 <= n3; ++n8) {
                n14 = nArray[n7 + nArray4[n8]];
                n12 += n14 >> 24 & 0xFF;
                n11 += n14 >> 16 & 0xFF;
                n10 += n14 >> 8 & 0xFF;
                n9 += n14 & 0xFF;
            }
            for (n8 = 0; n8 < n; ++n8) {
                int n15;
                nArray2[n13] = nArray3[n12] << 24 | nArray3[n11] << 16 | nArray3[n10] << 8 | nArray3[n9];
                n13 += n2;
                int n16 = n8 + n6;
                if (n16 >= n) {
                    n16 = n - 1;
                }
                if ((n15 = n8 - n3) < 0) {
                    n15 = 0;
                }
                int n17 = nArray[n7 + n16];
                int n18 = nArray[n7 + n15];
                n12 += n17 >> 24 & 0xFF;
                n12 -= n18 >> 24 & 0xFF;
                n11 += n17 >> 16 & 0xFF;
                n11 -= n18 >> 16 & 0xFF;
                n10 += n17 >> 8 & 0xFF;
                n10 -= n18 >> 8 & 0xFF;
                n9 += n17 & 0xFF;
                n9 -= n18 & 0xFF;
            }
            n7 += n;
        }
    }
}

