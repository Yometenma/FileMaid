package net.filemaid.vfs;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.filemaid.vfs.FileInfo;
import net.filemaid.vfs.SimpleFileInfo;

public class Torrent {
    private String name;
    private String encoding;
    private String createdBy;
    private String announce;
    private String comment;
    private Long creationDate;
    private Long pieceLength;
    private List<FileInfo> files;
    private boolean singleFileTorrent;

    protected Torrent() {
    }

    public Torrent(File file) throws IOException {
        this(Torrent.decodeTorrent(file));
    }

    public Torrent(Map<?, ?> map2) {
        this.createdBy = this.getString(map2.get("created by"));
        this.announce = this.getString(map2.get("announce"));
        this.comment = this.getString(map2.get("comment"));
        this.creationDate = this.getLong(map2.get("creation date"));
        Map<?, ?> map3 = this.getMap(map2.get("info"));
        this.name = this.getString(map3.get("name"));
        this.pieceLength = this.getLong(map3.get("piece length"));
        if (map3.containsKey("files")) {
            this.singleFileTorrent = false;
            this.files = this.getList(map3.get("files")).stream().map(this::getMap).map(map -> {
                String string = this.getList(map.get("path")).stream().map(Object::toString).collect(Collectors.joining("/"));
                long l = this.getLong(map.get("length"));
                return new SimpleFileInfo(string, l);
            }).collect(Collectors.toList());
        } else {
            this.singleFileTorrent = true;
            this.files = Collections.singletonList(new SimpleFileInfo(this.name, this.getLong(map3.get("length"))));
        }
    }

    private static Map<?, ?> decodeTorrent(File file) throws IOException {
        byte[] byArray = Files.readAllBytes(file.toPath());
        return (Map)new Bencode().decode(byArray, Type.DICTIONARY);
    }

    private String getString(Object object) {
        if (object instanceof CharSequence) {
            return object.toString();
        }
        return "";
    }

    private long getLong(Object object) {
        if (object instanceof Number) {
            return ((Number)object).longValue();
        }
        return -1L;
    }

    private Map<?, ?> getMap(Object object) {
        if (object instanceof Map) {
            return (Map)object;
        }
        return Collections.emptyMap();
    }

    private List<?> getList(Object object) {
        if (object instanceof List) {
            return (List)object;
        }
        return Collections.emptyList();
    }

    public String getAnnounce() {
        return this.announce;
    }

    public String getComment() {
        return this.comment;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public Long getCreationDate() {
        return this.creationDate;
    }

    public String getEncoding() {
        return this.encoding;
    }

    public List<FileInfo> getFiles() {
        return Collections.unmodifiableList(this.files);
    }

    public String getName() {
        return this.name;
    }

    public Long getPieceLength() {
        return this.pieceLength;
    }

    public boolean isSingleFileTorrent() {
        return this.singleFileTorrent;
    }
}

