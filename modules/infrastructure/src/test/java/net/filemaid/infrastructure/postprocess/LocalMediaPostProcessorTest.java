package net.filemaid.infrastructure.postprocess;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.filemaid.core.model.MetadataSelection;
import net.filemaid.core.model.MetadataType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalMediaPostProcessorTest {
    @Test void writesEscapedKodiNfoWithoutOverwriting(@TempDir Path directory) throws Exception {
        Path media=Files.writeString(directory.resolve("episode.mkv"),"video");
        var processor=new LocalMediaPostProcessor();
        Path nfo=processor.writeKodiNfo(media,new MetadataSelection("episode.mkv","tmdb","42",MetadataType.SERIES,"A & B <C>",2025));
        String xml=Files.readString(nfo);
        assertTrue(xml.contains("A &amp; B &lt;C&gt;"));
        assertTrue(xml.contains("<uniqueid>42</uniqueid>"));
        assertThrows(IllegalStateException.class,()->processor.writeKodiNfo(media,new MetadataSelection("episode.mkv","tmdb","42",MetadataType.SERIES,"Title",2025)));
    }

    @Test void copyBoundedRejectsOversizedStream(@TempDir Path directory) throws Exception {
        Path temp=Files.createTempFile(directory,".filemaid-artwork-",".tmp");
        var processor=new LocalMediaPostProcessor();
        long limit=25L*1024*1024;
        InputStream oversized=new InputStream(){
            private long remaining=26L*1024*1024;
            @Override public int read(){return remaining-->0?0:-1;}
            @Override public int read(byte[] b,int off,int len){if(remaining<=0)return -1;int n=(int)Math.min(len,remaining);remaining-=n;return n;}
        };
        assertThrows(IllegalStateException.class,()->processor.copyBounded(oversized,temp,limit));
    }
}
