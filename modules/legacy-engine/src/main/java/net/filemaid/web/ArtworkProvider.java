package net.filemaid.web;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.filemaid.web.Artwork;

public interface ArtworkProvider {
    public List<Artwork> getArtwork(int var1, Locale var2) throws Exception;

    default public List<Artwork> getArtwork(int n, String string, Locale locale) throws Exception {
        if (string == null) {
            return this.getArtwork(n, locale);
        }
        return this.getArtwork(n, locale).stream().filter(artwork -> artwork.matches(string)).collect(Collectors.toList());
    }
}

