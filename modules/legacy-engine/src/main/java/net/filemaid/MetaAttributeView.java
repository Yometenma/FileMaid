package net.filemaid;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.filemaid.Logging;
import net.filemaid.platform.bsd.ExtAttrView;
import net.filemaid.platform.mac.MacXattrView;
import net.filemaid.util.CompressorXattrView;
import net.filemaid.util.DefaultXattrView;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.PlainFileXattrView;
import net.filemaid.util.SystemProperty;
import net.filemaid.util.XattrView;

public class MetaAttributeView
extends AbstractMap<String, String> {
    private final XattrView fs;
    public static final Implementation PLATFORM = Implementation.getDefault();

    public MetaAttributeView(File file) throws IOException {
        this.fs = MetaAttributeView.getXattrView(file);
    }

    public MetaAttributeView(XattrView xattrView) {
        this.fs = xattrView;
    }

    @Override
    public String get(Object object) {
        try {
            ByteBuffer byteBuffer = this.read(object.toString());
            if (byteBuffer != null) {
                return StandardCharsets.UTF_8.decode(byteBuffer).toString();
            }
        }
        catch (Throwable throwable) {
            Logging.debug.warning(Logging.cause("Failed to read xattr key", object, throwable));
        }
        return null;
    }

    @Override
    public String put(String string, String string2) {
        try {
            if (string2 == null || string2.isEmpty()) {
                this.delete(string);
            } else {
                this.write(string, StandardCharsets.UTF_8.encode(string2));
            }
        }
        catch (Throwable throwable) {
            Logging.debug.warning(Logging.cause("Failed to write xattr key", string, throwable));
        }
        return null;
    }

    @Override
    public void clear() {
        try {
            for (String string : this.list()) {
                this.delete(string);
            }
        }
        catch (Throwable throwable) {
            Logging.debug.warning(Logging.cause("Failed to clear xattr", throwable));
        }
    }

    public List<String> list() {
        try {
            return this.fs.list();
        }
        catch (Throwable throwable) {
            Logging.debug.warning(Logging.cause("Failed to list xattr", throwable));
            return Collections.emptyList();
        }
    }

    public ByteBuffer read(String string) throws IOException {
        return this.fs.read(string);
    }

    public void write(String string, ByteBuffer byteBuffer) throws IOException {
        this.fs.write(string, byteBuffer);
    }

    public void delete(String string) throws IOException {
        this.fs.delete(string);
    }

    @Override
    public Set<Map.Entry<String, String>> entrySet() {
        return new AttributeSet(this.list());
    }

    public static XattrView getXattrView(File file) throws IOException {
        return PLATFORM.getXattrView(file.getAbsoluteFile());
    }

    private class AttributeSet
    extends AbstractSet<Map.Entry<String, String>> {
        private final List<String> keys;

        public AttributeSet(List<String> list) {
            this.keys = list;
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            return this.keys.stream().<Map.Entry<String, String>>map(string -> new AttributeEntry((String)string)).iterator();
        }

        @Override
        public int size() {
            return this.keys.size();
        }

        @Override
        public String toString() {
            return this.keys.toString();
        }
    }

    public static enum Implementation {
        PLAIN_FILE{

            @Override
            public boolean isSupported() {
                return XATTR_STORE != null;
            }

            @Override
            public XattrView getXattrView(File file) throws IOException {
                return new PlainFileXattrView(file, XATTR_STORE);
            }
        }
        ,
        NTFS_ADS{

            @Override
            public boolean isSupported() {
                return !FileUtilities.UNIX;
            }

            @Override
            public XattrView getXattrView(File file) throws IOException {
                return new DefaultXattrView.PreserveLastModified(file);
            }
        }
        ,
        MACOS_XATTR{

            @Override
            public boolean isSupported() {
                return FileUtilities.UNIX && System.getProperty("os.name").contains("Mac");
            }

            @Override
            public XattrView getXattrView(File file) {
                return new MacXattrView(file);
            }
        }
        ,
        BSD_EXTATTR{

            @Override
            public boolean isSupported() {
                return FileUtilities.UNIX && System.getProperty("os.name").contains("BSD");
            }

            @Override
            public XattrView getXattrView(File file) {
                return new ExtAttrView(file);
            }
        }
        ,
        DEFAULT{

            @Override
            public boolean isSupported() {
                return true;
            }

            @Override
            public XattrView getXattrView(File file) throws IOException {
                return CompressorXattrView.compress(new DefaultXattrView(file), true);
            }
        };

        public static final File XATTR_STORE;

        public abstract boolean isSupported();

        public abstract XattrView getXattrView(File var1) throws IOException;

        public static Implementation getDefault() {
            return Arrays.stream(Implementation.values()).filter(Implementation::isSupported).findFirst().orElse(DEFAULT);
        }

        static {
            XATTR_STORE = SystemProperty.optional("net.filemaid.xattr.store", File::new).orElse(null);
        }
    }

    private class AttributeEntry
    implements Map.Entry<String, String> {
        private final String key;

        public AttributeEntry(String string) {
            this.key = string;
        }

        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public String getValue() {
            return MetaAttributeView.this.get(this.key);
        }

        @Override
        public String setValue(String string) {
            return MetaAttributeView.this.put(this.key, string);
        }

        public String toString() {
            return this.getKey() + "=" + this.getValue();
        }
    }
}

