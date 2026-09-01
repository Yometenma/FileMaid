package net.filemaid.media;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.filemaid.Logging;
import net.filemaid.util.ExtensionFileFilter;
import net.filemaid.web.Geocode;

public class ImageMetadata {
    private final Metadata metadata;
    public static final FileFilter SUPPORTED_FILE_TYPES = new ExtensionFileFilter("jpg", "jpeg", "heic", "png", "webp", "gif", "ico", "bmp", "tif", "tiff", "psd", "pcx", "raw", "crw", "cr2", "nef", "orf", "raf", "rw2", "rwl", "srw", "arw", "dng", "x3f");

    public ImageMetadata(File file) throws ImageProcessingException, IOException {
        if (!SUPPORTED_FILE_TYPES.accept(file)) {
            throw new IllegalArgumentException("Image type not supported: " + file);
        }
        this.metadata = ImageMetadataReader.readMetadata((File)file);
    }

    public Map<String, String> snapshot() {
        return this.snapshot(Tag::getTagName);
    }

    public Map<String, String> snapshot(Function<Tag, String> function) {
        return this.snapshot(function, directory -> Stream.of("JPEG", "JFIF", "Interoperability", "Huffman", "File").noneMatch(directory.getName()::equals));
    }

    public Map<String, String> snapshot(Function<Tag, String> function, Predicate<Directory> predicate) {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
        for (Directory directory : this.metadata.getDirectories()) {
            if (!predicate.test(directory)) continue;
            for (Tag tag : directory.getTags()) {
                String string = tag.getDescription();
                if (string == null || string.length() <= 0) continue;
                linkedHashMap.put(function.apply(tag), string);
            }
        }
        return linkedHashMap;
    }

    public Optional<String> getName() {
        return this.extract(metadata -> (FileSystemDirectory)metadata.getFirstDirectoryOfType(FileSystemDirectory.class)).map(fileSystemDirectory -> fileSystemDirectory.getString(1));
    }

    public Optional<ZonedDateTime> getDateTaken() {
        return this.extract(metadata -> {
            for (ExifSubIFDDirectory exifSubIFDDirectory : metadata.getDirectoriesOfType(ExifSubIFDDirectory.class)) {
                Date date = exifSubIFDDirectory.getDateOriginal();
                if (date == null) continue;
                return date.toInstant().atZone(this.getTimeZone().orElse(ZoneOffset.UTC));
            }
            return null;
        });
    }

    public Optional<ZoneId> getTimeZone() {
        return this.extract(metadata -> {
            for (ExifSubIFDDirectory exifSubIFDDirectory : metadata.getDirectoriesOfType(ExifSubIFDDirectory.class)) {
                String string = exifSubIFDDirectory.getString(36881);
                if (string == null) continue;
                return ZoneId.of(string);
            }
            return null;
        });
    }

    public Optional<Map<CameraProperty, String>> getCameraModel() {
        return this.extract(metadata -> (ExifIFD0Directory)metadata.getFirstDirectoryOfType(ExifIFD0Directory.class)).map(exifIFD0Directory -> {
            String string = exifIFD0Directory.getDescription(271);
            String string2 = exifIFD0Directory.getDescription(272);
            Map<CameraProperty, String> enumMap = new EnumMap<CameraProperty, String>(CameraProperty.class);
            if (string != null) {
                enumMap.put(CameraProperty.maker, string);
            }
            if (string2 != null) {
                enumMap.put(CameraProperty.model, string2);
            }
            return enumMap;
        }).filter(map -> !map.isEmpty());
    }

    public Optional<GeoLocation> getLocationTaken() {
        return this.extract(metadata -> (GpsDirectory)metadata.getFirstDirectoryOfType(GpsDirectory.class)).map(GpsDirectory::getGeoLocation);
    }

    public Optional<Map<Geocode.AddressComponent, String>> getLocationTaken(Geocode geocode) {
        return this.getLocationTaken().map(geoLocation -> {
            try {
                return geocode.locate(geoLocation.getLatitude(), geoLocation.getLongitude());
            }
            catch (Exception exception) {
                Logging.trace(exception);
                return null;
            }
        });
    }

    public <T> Optional<T> extract(Function<Metadata, T> function) {
        try {
            return Optional.ofNullable(function.apply(this.metadata));
        }
        catch (Exception exception) {
            Logging.debug.finest(Logging.cause("Failed to extract image metadata", exception));
            return Optional.empty();
        }
    }

    public static enum CameraProperty {
        maker,
        model;

    }
}

