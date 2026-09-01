package net.filemaid.media;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Icon;
import net.filemaid.Logging;
import net.filemaid.MetaAttributeView;
import net.filemaid.ResourceManager;
import net.filemaid.media.ImageMetadata;
import net.filemaid.media.MetaAttributes;
import net.filemaid.util.FileUtilities;
import net.filemaid.util.PlainFileXattrView;
import net.filemaid.util.ReadOnlyFile;
import net.filemaid.web.Datasource;
import net.filemaid.web.Episode;
import net.filemaid.web.Movie;

public enum LocalDatasource implements Datasource
{
    XATTR,
    EXIF,
    FILE;


    @Override
    public String getIdentifier() {
        switch (this) {
            case XATTR: {
                return "xattr";
            }
            case EXIF: {
                return "exif";
            }
        }
        return "file";
    }

    @Override
    public String getName() {
        switch (this) {
            case XATTR: {
                return "Extended Attributes";
            }
            case EXIF: {
                return "Exif Metadata";
            }
        }
        return "Plain File";
    }

    @Override
    public Icon getIcon() {
        switch (this) {
            case XATTR: {
                return ResourceManager.getIcon("search.xattr");
            }
            case EXIF: {
                return ResourceManager.getIcon("search.exif");
            }
        }
        return ResourceManager.getIcon("search.generic");
    }

    public Map<File, Object> match(Collection<File> collection) {
        LinkedHashMap<File, Object> linkedHashMap = new LinkedHashMap<File, Object>(collection.size());
        for (File file : collection) {
            Object object = this.match(file);
            if (object == null) continue;
            linkedHashMap.put(file, object);
        }
        return linkedHashMap;
    }

    public Object match(File file) {
        switch (this) {
            case XATTR: {
                Object object = this.readFile(file);
                if (object == null && file.isDirectory()) {
                    return this.readDirectory(file);
                }
                return object;
            }
        }
        return this.readFile(file);
    }

    public Object readFile(File file) {
        try {
            switch (this) {
                case XATTR: {
                    Object object = new MetaAttributes(file).getObject();
                    if (object != null) {
                        return object;
                    }
                    MetaAttributes metaAttributes = new MetaAttributes(new MetaAttributeView(new PlainFileXattrView(file, new File(".xattr"))), null);
                    return metaAttributes.getObject();
                }
                case EXIF: {
                    ImageMetadata imageMetadata;
                    if (ImageMetadata.SUPPORTED_FILE_TYPES.accept(file) && (imageMetadata = new ImageMetadata(file)).getDateTaken().isPresent()) {
                        return new PhotoFile(file, imageMetadata);
                    }
                    return null;
                }
            }
            return file;
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.cause(this, file, exception));
            return null;
        }
    }

    public Object readDirectory(File file) {
        switch (this) {
            case XATTR: {
                for (File file2 : FileUtilities.listFiles(file, FileUtilities.FILES, FileUtilities.HUMAN_NAME_ORDER)) {
                    Object object = this.readFile(file2);
                    if (object instanceof Episode) {
                        return (Episode)object;
                    }
                    if (!(object instanceof Movie)) continue;
                    return (Movie)object;
                }
                return null;
            }
        }
        return null;
    }

    public static class PhotoFile
    extends ReadOnlyFile {
        private final ImageMetadata metadata;

        public PhotoFile(File file, ImageMetadata imageMetadata) {
            super(file);
            this.metadata = imageMetadata;
        }

        public ImageMetadata getMetadata() {
            return this.metadata;
        }
    }
}

