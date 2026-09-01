package net.filemaid.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.util.FileUtilities;

public class ExtensionFileFilter
implements FileFilter,
FilenameFilter {
    public static final ExtensionFileFilter WILDCARD = new ExtensionFileFilter("*");
    private final String[] extensions;
    private Set<String> lookup;

    public ExtensionFileFilter(String ... stringArray) {
        this.extensions = stringArray;
    }

    private Set<String> getLookup() {
        if (this.lookup == null) {
            this.lookup = this.extensions().map(string -> string.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        }
        return this.lookup;
    }

    private boolean has(String string) {
        return string != null && this.getLookup().contains(string.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean accept(File file, String string) {
        return this.accept(string);
    }

    @Override
    public boolean accept(File file) {
        return this.accept(file.getName());
    }

    public boolean accept(String string) {
        return this.acceptExtension(FileUtilities.getExtension(string));
    }

    public boolean acceptExtension(String string) {
        return this.acceptAny() || this.has(string);
    }

    public boolean acceptAny() {
        return this.extensions.length == 1 && this.extensions[0].equals("*");
    }

    public String extension() {
        return this.extensions[0];
    }

    public Stream<String> extensions() {
        return Arrays.stream(this.extensions);
    }

    public String[] array() {
        return (String[])this.extensions().toArray(String[]::new);
    }

    public String[] glob() {
        return (String[])this.extensions().map("*."::concat).toArray(String[]::new);
    }

    public String toString() {
        return String.join((CharSequence)"; ", this.glob());
    }
}

