package net.filemaid.similarity;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.filemaid.media.MediaFileUtilities;
import net.filemaid.similarity.Match;
import net.filemaid.util.FileUtilities;

public class DerivateCollection
extends AbstractMap<File, List<File>> {
    private final Map<File, List<File>> derivates;
    private final List<File> orphans;

    public DerivateCollection(Map<File, List<File>> map, List<File> list) {
        this.derivates = map;
        this.orphans = list;
    }

    @Override
    public Set<Map.Entry<File, List<File>>> entrySet() {
        return this.derivates.entrySet();
    }

    public List<File> orphans() {
        return this.orphans;
    }

    public <T> Stream<Match<File, T>> matches(File file2, Supplier<T> supplier) {
        return Stream.of(file2).flatMap(file -> {
            Stream stream = Stream.of(Match.of(file, supplier.get()));
            List<File> list = this.derivates.get(file);
            if (list == null) {
                return stream;
            }
            return Stream.concat(stream, list.stream().map(file3 -> Match.of(file3, supplier.get())));
        });
    }

    public static DerivateCollection derive(Collection<File> collection, Collection<File> ... collectionArray) {
        HashMap<File, List<File>> hashMap = new HashMap<File, List<File>>();
        ArrayList<File> arrayList = new ArrayList<File>();
        LinkedHashSet<File> linkedHashSet = new LinkedHashSet<File>(collection);
        Stream.of(collectionArray).forEach(linkedHashSet::removeAll);
        for (File file3 : linkedHashSet) {
            File file4 = Stream.of(collectionArray).flatMap(Collection::stream).filter(file2 -> FileUtilities.sameParentFolder(file3, file2) && MediaFileUtilities.isDerived(file3, file2)).findFirst().orElse(null);
            if (file4 != null) {
                hashMap.computeIfAbsent(file4, file -> new ArrayList(1)).add(file3);
                continue;
            }
            arrayList.add(file3);
        }
        return new DerivateCollection(hashMap, arrayList);
    }

}

