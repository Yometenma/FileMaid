package net.filemaid.web;

import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Artwork
implements Serializable {
    protected String[] tags;
    protected URL url;
    protected String language;
    protected Double rating;

    public Artwork() {
    }

    public Artwork(URL uRL, Locale locale, Double d, Object ... objectArray) {
        this.tags = (String[])Arrays.stream(objectArray).filter(Objects::nonNull).map(Object::toString).toArray(String[]::new);
        this.url = uRL;
        this.language = Artwork.getLanguageCode(locale);
        this.rating = d;
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(Arrays.asList(this.tags));
    }

    public URL getUrl() {
        return this.url;
    }

    public Locale getLanguage() {
        return this.language == null ? null : Locale.forLanguageTag(this.language);
    }

    public double getRating() {
        return this.rating == null ? 0.0 : this.rating;
    }

    public boolean has(Predicate<String> predicate) {
        return Arrays.stream(this.tags).anyMatch(predicate);
    }

    public boolean matches(Object ... objectArray) {
        if (objectArray == null || objectArray.length == 0) {
            return true;
        }
        return Arrays.stream(objectArray).filter(Objects::nonNull).map(Object::toString).allMatch(string -> {
            if (string.equalsIgnoreCase(this.language)) return true;
            if (!this.has(string::equalsIgnoreCase)) return false;
            return true;
        });
    }

    public String getExtension() {
        String string = this.url.getPath();
        int n = string.lastIndexOf(46);
        if (n > 0 && n > string.lastIndexOf(47)) {
            return string.substring(n + 1);
        }
        return null;
    }

    public int hashCode() {
        return this.url.hashCode();
    }

    public boolean equals(Object object) {
        if (object instanceof Artwork) {
            Artwork artwork = (Artwork)object;
            return this.url.sameFile(artwork.url);
        }
        return false;
    }

    public String toString() {
        return "[" + String.join((CharSequence)"/", this.tags) + ", " + this.language + ", " + this.rating + ", " + this.url + "]";
    }

    public static Comparator<Artwork> ratingOrder() {
        return Comparator.comparing(Artwork::getRating, Collections.reverseOrder());
    }

    public static Comparator<Artwork> noLanguageOrder() {
        return Comparator.comparingInt(artwork -> artwork.language == null ? 0 : 1);
    }

    public static Comparator<Artwork> languageOrder(Locale ... localeArray) {
        if (localeArray == null || localeArray.length == 0) {
            return Artwork.noLanguageOrder();
        }
        List list = Arrays.stream(localeArray).map(Artwork::getLanguageCode).distinct().collect(Collectors.toList());
        Comparator<Artwork> comparator = Comparator.comparingInt(artwork -> {
            int n = list.indexOf(artwork.language);
            return n >= 0 ? n : Integer.MAX_VALUE;
        });
        return comparator.thenComparing(Artwork.noLanguageOrder());
    }

    public static Comparator<Artwork> relevanceOrder(Locale ... localeArray) {
        Comparator<Artwork> comparator = Artwork.languageOrder(localeArray);
        return comparator.thenComparing(Artwork.ratingOrder());
    }

    private static String getLanguageCode(Locale locale) {
        return locale == null || locale.getLanguage().isEmpty() ? null : locale.getLanguage();
    }
}

