package net.filemaid.infrastructure.postprocess;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Set;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import net.filemaid.application.port.MediaPostProcessor;
import net.filemaid.core.model.MetadataSelection;

public final class LocalMediaPostProcessor implements MediaPostProcessor {
    private static final long MAX_ARTWORK_BYTES = 25L * 1024 * 1024;
    private static final Set<String> ARTWORK_HOSTS = Set.of("image.tmdb.org", "artworks.thetvdb.com", "static.tvmaze.com", "m.media-amazon.com");
    private final HttpClient client;
    public LocalMediaPostProcessor() { this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build()); }
    public LocalMediaPostProcessor(HttpClient client) { this.client=client; }

    @Override public Path writeKodiNfo(Path mediaFile, MetadataSelection metadata) throws Exception {
        Path target=sibling(mediaFile,"nfo");
        if(Files.exists(target))throw new IllegalStateException("NFO 已存在（不覆盖）: "+target.getFileName());
        String root=metadata.type().name().equals("MOVIE")?"movie":"episodedetails";
        StringWriter output=new StringWriter(); XMLStreamWriter xml=XMLOutputFactory.newFactory().createXMLStreamWriter(output);
        xml.writeStartDocument("UTF-8","1.0");xml.writeStartElement(root);element(xml,"title",metadata.title());element(xml,"uniqueid",metadata.id());element(xml,"source",metadata.provider());if(metadata.year()!=null)element(xml,"year",metadata.year().toString());xml.writeEndElement();xml.writeEndDocument();xml.close();
        Files.writeString(target,output.toString(),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);return target;
    }

    @Override public Path downloadArtwork(Path mediaFile,String artworkUrl,String artworkType) throws Exception {
        URI uri=URI.create(artworkUrl);if(!"https".equalsIgnoreCase(uri.getScheme())||!ARTWORK_HOSTS.contains(uri.getHost()))throw new IllegalArgumentException("不允许的封面地址");
        String fileName="FANART".equalsIgnoreCase(artworkType)?"fanart.jpg":"poster.jpg";Path target=mediaFile.getParent().resolve(fileName);if(Files.exists(target))throw new IllegalStateException("封面已存在（不覆盖）: "+fileName);
        HttpRequest request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60)).GET().build();HttpResponse<InputStream> response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());
        if(response.statusCode()<200||response.statusCode()>=300)throw new IllegalStateException("封面下载失败（HTTP "+response.statusCode()+"）");
        String contentType=response.headers().firstValue("content-type").orElse("");if(!contentType.toLowerCase().startsWith("image/"))throw new IllegalStateException("下载内容不是图片");
        Path temp=Files.createTempFile(mediaFile.getParent(),".filemaid-artwork-",".tmp");
        try(InputStream input=response.body()){copyBounded(input,temp,MAX_ARTWORK_BYTES);Files.move(temp,target);}finally{Files.deleteIfExists(temp);}
        return target;
    }
    void copyBounded(InputStream input,Path temp,long limit)throws Exception{
        try(OutputStream output=Files.newOutputStream(temp,StandardOpenOption.TRUNCATE_EXISTING)){
            byte[] buffer=new byte[8192];long total=0;int read;
            while((read=input.read(buffer))>=0){total+=read;if(total>limit)throw new IllegalStateException("封面文件超过 25 MB");output.write(buffer,0,read);}
        }
    }
    private Path sibling(Path media,String extension){String name=media.getFileName().toString();int dot=name.lastIndexOf('.');String base=dot>0?name.substring(0,dot):name;return media.resolveSibling(base+"."+extension);}
    private void element(XMLStreamWriter xml,String name,String value)throws Exception{xml.writeStartElement(name);xml.writeCharacters(value==null?"":value);xml.writeEndElement();}
}
