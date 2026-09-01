package net.filemaid.util.ui;

import java.awt.Color;
import java.awt.image.RGBImageFilter;

public class ColorTintImageFilter
extends RGBImageFilter {
    private Color color;
    private float intensity;

    public ColorTintImageFilter(Color color, float f) {
        this.color = color;
        this.intensity = f;
        this.canFilterIndexColorModel = true;
    }

    @Override
    public int filterRGB(int n, int n2, int n3) {
        Color color = new Color(n3, true);
        int n4 = (int)((float)color.getRed() * (1.0f - this.intensity) + (float)this.color.getRed() * this.intensity);
        int n5 = (int)((float)color.getGreen() * (1.0f - this.intensity) + (float)this.color.getGreen() * this.intensity);
        int n6 = (int)((float)color.getBlue() * (1.0f - this.intensity) + (float)this.color.getBlue() * this.intensity);
        return new Color(n4, n5, n6, color.getAlpha()).getRGB();
    }
}

