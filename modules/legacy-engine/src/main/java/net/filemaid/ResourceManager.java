package net.filemaid;

import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import net.filemaid.MemoryCache;
import net.filemaid.Settings;
import net.filemaid.web.WebRequest;
import org.imgscalr.Scalr;

public final class ResourceManager {
    private static final MemoryCache<String, Icon> cache = MemoryCache.forMinutes();
    public static final double PRIMARY_SCALE_FACTOR = GraphicsEnvironment.isHeadless() ? 1.0 : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform().getScaleX();

    public static Icon getIcon(String string2) {
        return cache.get(string2, string -> {
            Deque<URL> deque = ResourceManager.getMultiResolutionImageResource(string);
            if (deque.isEmpty()) {
                return null;
            }
            return new ImageIcon(ResourceManager.getMultiResolutionImage(deque));
        });
    }

    public static List<Image> getApplicationIconImages() {
        if (Settings.isWindowsApp()) {
            BaseMultiResolutionImage baseMultiResolutionImage = new BaseMultiResolutionImage(ResourceManager.readImage(ResourceManager.getImageResource("window.icon16")), ResourceManager.readImage(ResourceManager.getImageResource("window.icon16@2x")));
            BaseMultiResolutionImage baseMultiResolutionImage2 = new BaseMultiResolutionImage(ResourceManager.readImage(ResourceManager.getImageResource("window.icon64")), ResourceManager.readImage(ResourceManager.getImageResource("window.icon64@2x")));
            return Arrays.asList(baseMultiResolutionImage, baseMultiResolutionImage2);
        }
        BufferedImage bufferedImage = ResourceManager.readImage(ResourceManager.getImageResource("window.icon64@2x"));
        return Arrays.asList(bufferedImage);
    }

    public static Icon getFlagIcon(String string) {
        if (Settings.isUWP()) {
            return null;
        }
        return ResourceManager.getIcon("flags/" + string);
    }

    private static Deque<URL> getMultiResolutionImageResource(String string) {
        return Stream.of(string, string + "@2x").map(ResourceManager::getImageResource).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayDeque::new));
    }

    private static URL getImageResource(String string) {
        return ResourceManager.class.getResource("resources/" + string + ".png");
    }

    private static Image getMultiResolutionImage(Deque<URL> deque) {
        if (PRIMARY_SCALE_FACTOR == 1.0) {
            return ResourceManager.readImage(deque.getFirst());
        }
        ArrayList<BufferedImage> arrayList = new ArrayList<BufferedImage>(deque.size());
        for (URL uRL : deque) {
            arrayList.add(ResourceManager.readImage(uRL));
        }
        if (arrayList.size() > 1) {
            if (PRIMARY_SCALE_FACTOR > 1.0 && PRIMARY_SCALE_FACTOR < 2.0) {
                arrayList.add(1, ResourceManager.scale(PRIMARY_SCALE_FACTOR / 2.0, (BufferedImage)arrayList.get(1)));
            } else if (PRIMARY_SCALE_FACTOR > 2.0) {
                arrayList.add(ResourceManager.scale(PRIMARY_SCALE_FACTOR / 2.0, (BufferedImage)arrayList.get(1)));
            }
        }
        return new BaseMultiResolutionImage((Image[])arrayList.toArray(Image[]::new));
    }

    public static ByteBuffer getResource(String string) {
        try {
            InputStream inputStream = ResourceManager.getMultiResolutionImageResource(string).getLast().openStream();
            try {
                return WebRequest.gunzip(inputStream);
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage readImage(URL uRL) {
        try {
            return ImageIO.read(new MemoryCacheImageInputStream(uRL.openStream()));
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    public static BufferedImage readImage(byte[] byArray) {
        try {
            return ImageIO.read(new MemoryCacheImageInputStream(new ByteArrayInputStream(byArray)));
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    public static Image getMultiResolutionImage(BufferedImage bufferedImage, double d) {
        if (PRIMARY_SCALE_FACTOR == 1.0 && d == 1.0) {
            return bufferedImage;
        }
        ArrayList<BufferedImage> arrayList = new ArrayList<BufferedImage>(3);
        arrayList.add(bufferedImage);
        if (d > 1.0) {
            arrayList.add(0, ResourceManager.scale(1.0 / d, bufferedImage));
        }
        if (PRIMARY_SCALE_FACTOR > 1.0 && PRIMARY_SCALE_FACTOR < d) {
            arrayList.add(1, ResourceManager.scale(PRIMARY_SCALE_FACTOR / d, bufferedImage));
        } else if (PRIMARY_SCALE_FACTOR > d) {
            arrayList.add(ResourceManager.scale(PRIMARY_SCALE_FACTOR / d, bufferedImage));
        }
        return new BaseMultiResolutionImage((Image[])arrayList.toArray(Image[]::new));
    }

    private static BufferedImage scale(double d, BufferedImage bufferedImage) {
        int n = (int)(d * (double)bufferedImage.getWidth());
        int n2 = (int)(d * (double)bufferedImage.getHeight());
        return Scalr.resize((BufferedImage)bufferedImage, (Scalr.Method)Scalr.Method.ULTRA_QUALITY, (Scalr.Mode)Scalr.Mode.FIT_TO_WIDTH, (int)n, (int)n2, (BufferedImageOp[])new BufferedImageOp[]{Scalr.OP_ANTIALIAS});
    }
}

