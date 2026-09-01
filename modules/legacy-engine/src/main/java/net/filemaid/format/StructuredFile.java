package net.filemaid.format;

import java.util.Optional;
import net.filemaid.media.NamingStandard;
import net.filemaid.util.ReadOnlyFile;
import net.filemaid.web.Episode;
import net.filemaid.web.Movie;

public class StructuredFile
extends ReadOnlyFile {
    private final NamingStandard.Structure components;
    private final NamingStandard naming;
    private final Object object;

    private StructuredFile(NamingStandard.Structure structure, NamingStandard namingStandard, Object object) {
        super(structure.getPath(), false);
        this.components = structure;
        this.naming = namingStandard;
        this.object = object;
    }

    private StructuredFile apply(NamingStandard.Structure structure) {
        return new StructuredFile(structure, this.naming, this.object);
    }

    public StructuredFile category(String string) {
        return this.apply(this.components.category(string));
    }

    public StructuredFile library(String string) {
        return this.apply(this.components.library(string));
    }

    public StructuredFile collection(String string) {
        return this.apply(this.components.collection(string));
    }

    public StructuredFile group(String string) {
        return this.apply(this.components.group(string));
    }

    public StructuredFile name(String string) {
        return this.apply(this.components.name(string));
    }

    public StructuredFile number(String string) {
        return this.apply(this.components.number(string));
    }

    public StructuredFile title(String string) {
        return this.apply(this.components.title(string));
    }

    public StructuredFile tag(String string) {
        return this.apply(this.components.tag(string));
    }

    public StructuredFile part(String string) {
        return this.apply(this.components.part(string));
    }

    public StructuredFile suffix(String string) {
        return this.apply(this.components.suffix(string));
    }

    public StructuredFile getId() {
        if (this.object instanceof Episode) {
            Episode episode = (Episode)this.object;
            return this.apply(this.components.collection(this.naming.getID(episode.getSeriesInfo())));
        }
        if (this.object instanceof Movie) {
            Movie movie = (Movie)this.object;
            return this.apply(this.components.collection(this.naming.getID(movie)));
        }
        return this;
    }

    public StructuredFile getYear() {
        if (this.object instanceof Episode) {
            String string = NamingStandard.Default.getSeriesNameYear((Episode)this.object);
            return this.apply(this.components.collection(null).collection(string).name(null).name(string));
        }
        return this;
    }

    public StructuredFile getUnix() {
        return this.apply(this.components.validate(false));
    }

    public static StructuredFile of(Object object, NamingStandard namingStandard) {
        return Optional.ofNullable(object).map(namingStandard::getPath).map(structure -> new StructuredFile((NamingStandard.Structure)structure, namingStandard, object)).orElse(null);
    }
}

