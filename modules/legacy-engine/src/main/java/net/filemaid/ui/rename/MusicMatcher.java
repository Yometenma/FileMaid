package net.filemaid.ui.rename;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.filemaid.InvalidInputException;
import net.filemaid.MediaTypes;
import net.filemaid.similarity.DerivateCollection;
import net.filemaid.similarity.Match;
import net.filemaid.ui.rename.AutoCompleteMatcher;
import net.filemaid.ui.rename.AutoDetectionMode;
import net.filemaid.ui.rename.AutoSelectionMode;
import net.filemaid.ui.rename.MatchMode;
import net.filemaid.util.FileUtilities;
import net.filemaid.web.AudioTrack;
import net.filemaid.web.MusicLookupService;
import net.filemaid.web.SortOrder;

class MusicMatcher
implements AutoCompleteMatcher {
    private final MusicLookupService[] services;

    public MusicMatcher(MusicLookupService ... musicLookupServiceArray) {
        this.services = musicLookupServiceArray;
    }

    @Override
    public List<Match<File, ?>> match(Collection<File> collection, MatchMode matchMode, SortOrder sortOrder, Locale locale, AutoDetectionMode autoDetectionMode, Set<AutoSelectionMode> set, Component component) throws Exception {
        ArrayList arrayList = new ArrayList();
        LinkedHashSet<File> linkedHashSet = new LinkedHashSet<File>(FileUtilities.filter(collection, MediaTypes.AUDIO_FILES, MediaTypes.VIDEO_FILES));
        if (linkedHashSet.isEmpty()) {
            throw new InvalidInputException("No audio files have been selected. Please <Load> audio files.");
        }
        DerivateCollection derivateCollection = DerivateCollection.derive(collection, linkedHashSet);
        for (File file : linkedHashSet) {
            for (MusicLookupService musicLookupService : this.services) {
                List<AudioTrack> list = musicLookupService.lookup(file);
                if (list.size() <= 0) continue;
                derivateCollection.matches(file, list.get(0)::clone).forEach(arrayList::add);
                break;
            }
            if (!set.contains((Object)AutoSelectionMode.Cancel)) continue;
            break;
        }
        return arrayList;
    }
}

