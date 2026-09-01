package net.filemaid.ui.rename;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.filemaid.media.LocalDatasource;
import net.filemaid.similarity.Match;
import net.filemaid.ui.rename.AutoCompleteMatcher;
import net.filemaid.ui.rename.AutoDetectionMode;
import net.filemaid.ui.rename.AutoSelectionMode;
import net.filemaid.ui.rename.MatchMode;
import net.filemaid.web.SortOrder;

public class LocalFileMatcher
implements AutoCompleteMatcher {
    private final LocalDatasource datasource;

    public LocalFileMatcher(LocalDatasource localDatasource) {
        this.datasource = localDatasource;
    }

    @Override
    public List<Match<File, ?>> match(Collection<File> collection, MatchMode matchMode, SortOrder sortOrder, Locale locale, AutoDetectionMode autoDetectionMode, Set<AutoSelectionMode> set, Component component) throws Exception {
        ArrayList arrayList = new ArrayList(collection.size());
        for (File file : collection) {
            Object object = this.datasource.match(file);
            if (object != null) {
                arrayList.add(Match.of(file, object));
            }
            if (!set.contains((Object)AutoSelectionMode.Cancel)) continue;
            break;
        }
        return arrayList;
    }
}

