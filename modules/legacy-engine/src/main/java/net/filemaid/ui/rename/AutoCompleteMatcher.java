package net.filemaid.ui.rename;

import java.awt.Component;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.filemaid.similarity.Match;
import net.filemaid.ui.rename.AutoDetectionMode;
import net.filemaid.ui.rename.AutoSelectionMode;
import net.filemaid.ui.rename.MatchMode;
import net.filemaid.web.SortOrder;

interface AutoCompleteMatcher {
    public List<Match<File, ?>> match(Collection<File> var1, MatchMode var2, SortOrder var3, Locale var4, AutoDetectionMode var5, Set<AutoSelectionMode> var6, Component var7) throws Exception;
}

