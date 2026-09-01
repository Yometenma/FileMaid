package net.filemaid;

import com.traneptora.jxlatte.JXLDecoder;
import com.traneptora.jxlatte.io.PNGWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.BitSet;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.Logging;
import net.filemaid.Resource;
import net.filemaid.ResourceManager;
import net.filemaid.util.ByteBufferInputStream;
import net.filemaid.util.IntSet;
import net.filemaid.web.ThumbnailProvider;
import net.filemaid.web.WebRequest;
import org.tukaani.xz.XZInputStream;

public enum ThumbnailServices implements ThumbnailProvider
{
    TheMovieDB,
    TheMovieDB_TV,
    TheTVDB,
    AniDB;

    private final Resource<IntSet> index = Resource.lazy(this::getIndex);

    protected URL getResource(String string) throws Exception {
        return WebRequest.newURL("https://api.filebot.net/thumb/" + this.ordinal() + "/" + string);
    }

    protected Cache getCache() {
        return Cache.getCache("thumbnail_" + this.ordinal(), CacheType.Persistent);
    }

    protected IntSet getIndex() throws Exception {
        byte[] byArray = this.getCache().bytes(0, n -> this.getResource("index.bitset.xz"), XZInputStream::new).expire(Cache.ONE_MONTH).get();
        return IntSet.of(BitSet.valueOf(byArray));
    }

    protected boolean exists(int n) {
        try {
            return this.index.get().contains(n);
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause("Failed to retrieve thumbnail index", this.ordinal(), exception));
            return false;
        }
    }

    @Override
    public Icon getThumbnail(int n, ThumbnailProvider.ResolutionVariant resolutionVariant) throws Exception {
        if (n > 0 && this.exists(n)) {
            try {
                int n3 = resolutionVariant == ThumbnailProvider.ResolutionVariant.NORMAL ? n : -n;
                byte[] byArray = this.getCache().image(n3, n2 -> this.getResource(resolutionVariant.scaleFactor + "/" + n + ".jxl"), byteBuffer -> ThumbnailServices.decode(byteBuffer)).get();
                if (byArray != null && byArray.length > 0) {
                    return new ImageIcon(ResourceManager.getMultiResolutionImage(ResourceManager.readImage(byArray), resolutionVariant.scaleFactor));
                }
            }
            catch (Exception exception) {
                Logging.debug.warning(Logging.cause("Failed to retrieve thumbnail", this.ordinal() + "/" + resolutionVariant.scaleFactor + "/" + n, exception));
            }
        }
        return null;
    }

    private static synchronized byte[] decode(ByteBuffer byteBuffer) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(24576);
        new PNGWriter(new JXLDecoder((InputStream)new ByteBufferInputStream(byteBuffer.duplicate())).decode()).write((OutputStream)byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
}

