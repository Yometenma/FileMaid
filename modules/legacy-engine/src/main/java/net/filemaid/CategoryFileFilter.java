package net.filemaid;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.filemaid.util.ExtensionFileFilter;

public class CategoryFileFilter
extends ExtensionFileFilter {
    private final String description;
    private final List<Type> types = new ArrayList<Type>();

    public CategoryFileFilter(String string, ExtensionFileFilter ... extensionFileFilterArray) {
        super(new String[0]);
        this.description = string;
        for (ExtensionFileFilter extensionFileFilter : extensionFileFilterArray) {
            this.add(string, extensionFileFilter);
        }
    }

    public String getDescription() {
        return this.description;
    }

    public void add(String string, ExtensionFileFilter extensionFileFilter) {
        this.types.add(new Type(string, extensionFileFilter));
    }

    public void add(Type type) {
        this.types.add(type);
    }

    @Override
    public boolean acceptExtension(String string) {
        return this.types.stream().anyMatch(type -> type.getFilter().acceptExtension(string));
    }

    @Override
    public boolean acceptAny() {
        return this.types.stream().anyMatch(type -> type.getFilter().acceptAny());
    }

    @Override
    public Stream<String> extensions() {
        return this.types.stream().flatMap(type -> type.getFilter().extensions());
    }

    @Override
    public String extension() {
        return this.extensions().findFirst().get();
    }

    public boolean hasTypes() {
        return this.types.stream().anyMatch(type -> !type.getFilter().acceptAny());
    }

    public void each(BiConsumer<String, ExtensionFileFilter> biConsumer) {
        this.types.forEach(type -> biConsumer.accept(type.getDescription(), type.getFilter()));
    }

    public static class Type {
        private final String description;
        private final ExtensionFileFilter filter;

        public Type(String string, ExtensionFileFilter extensionFileFilter) {
            this.description = string;
            this.filter = extensionFileFilter;
        }

        public String getDescription() {
            return this.description;
        }

        public ExtensionFileFilter getFilter() {
            return this.filter;
        }
    }
}

