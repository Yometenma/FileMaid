package net.filemaid.platform.posix;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.filemaid.platform.posix.GVFS;

public class PlatformGVFS
implements GVFS {
    private final File gvfs;

    public PlatformGVFS(File file) {
        this.gvfs = file;
    }

    @Override
    public File getPathForURI(String string) {
        return this.getPathForURI(this.parseURI(string));
    }

    public File getPathForURI(URI uRI) {
        return Protocol.forName(uRI.getScheme()).getFile(this.gvfs, uRI);
    }

    public URI parseURI(String string) {
        string = string.replace("[", "%5B").replace("]", "%5D");
        return URI.create(string);
    }

    public String toString() {
        return this.getClass().getSimpleName() + " [" + this.gvfs + "]";
    }

    public static enum Protocol {
        FILE{

            @Override
            public File getFile(File file, URI uRI) {
                return new File(uRI);
            }

            @Override
            public String getPath(URI uRI) {
                return new File(uRI).getPath();
            }
        }
        ,
        SMB{

            @Override
            public String getPath(URI uRI) {
                StringBuilder stringBuilder = new StringBuilder("smb-share:");
                stringBuilder.append("server=").append(uRI.getHost());
                if (uRI.getUserInfo() != null) {
                    stringBuilder.append(",user=").append(uRI.getUserInfo());
                }
                stringBuilder.append(",share=").append(uRI.getPath().substring(1));
                return stringBuilder.toString();
            }
        }
        ,
        AFP{

            @Override
            public String getPath(URI uRI) {
                StringBuilder stringBuilder = new StringBuilder("afp-volume:");
                stringBuilder.append("host=").append(uRI.getHost());
                if (uRI.getUserInfo() != null) {
                    stringBuilder.append(",user=").append(uRI.getUserInfo());
                }
                stringBuilder.append(",volume=").append(uRI.getPath().substring(1));
                return stringBuilder.toString();
            }
        }
        ,
        SFTP{

            @Override
            public String getPath(URI uRI) {
                StringBuilder stringBuilder = new StringBuilder("sftp:");
                stringBuilder.append("host=").append(uRI.getHost());
                if (uRI.getUserInfo() != null) {
                    stringBuilder.append(",user=").append(uRI.getUserInfo());
                }
                stringBuilder.append(uRI.getPath());
                return stringBuilder.toString();
            }
        };


        public abstract String getPath(URI var1);

        public File getFile(File file, URI uRI) {
            return new File(file, this.getPath(uRI));
        }

        public static List<String> names() {
            return Arrays.stream(Protocol.values()).map(Enum::name).collect(Collectors.toList());
        }

        public static Protocol forName(String string) {
            for (Protocol protocol : Protocol.values()) {
                if (!protocol.name().equalsIgnoreCase(string)) continue;
                return protocol;
            }
            throw new IllegalArgumentException(string + " not in " + Protocol.names());
        }
    }
}

