package net.filemaid;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.filemaid.Logging;
import net.filemaid.util.RegularExpressions;

public class Language
implements Serializable {
    private final String iso_639_1;
    private final String iso_639_3;
    private final String iso_639_2B;
    private final String tag;
    private final String[] names;
    public static final Comparator<Language> ALPHABETIC_ORDER = Comparator.comparing(Language::getName, String::compareToIgnoreCase);

    public Language(String string, String string2, String string3, String string4, String[] stringArray) {
        this.iso_639_1 = string;
        this.iso_639_3 = string2;
        this.iso_639_2B = string3;
        this.tag = string4;
        this.names = (String[])stringArray.clone();
    }

    public String getCode() {
        return this.iso_639_1;
    }

    public String getISO2() {
        return this.iso_639_1;
    }

    public String getISO3() {
        return this.iso_639_3;
    }

    public String getISO3B() {
        return this.iso_639_2B;
    }

    public String getTag() {
        return this.tag;
    }

    public String getName() {
        return this.names[0];
    }

    public List<String> getNames() {
        return Collections.unmodifiableList(Arrays.asList(this.names));
    }

    public String toString() {
        return this.iso_639_3;
    }

    public Locale getLocale() {
        return Locale.forLanguageTag(this.tag);
    }

    public boolean matches(Language language) {
        if (language == null) {
            return false;
        }
        return this.getCode().equals(language.getCode());
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean matches(String string) {
        if (string == null) return false;
        if (string.isEmpty()) {
            return false;
        }
        if (Stream.of(this.iso_639_1, this.iso_639_2B, this.iso_639_3, this.tag).anyMatch(string::equalsIgnoreCase)) return true;
        if (!Arrays.stream(this.names).anyMatch(string::equalsIgnoreCase)) return false;
        return true;
    }

    public Language clone() {
        return new Language(this.iso_639_1, this.iso_639_3, this.iso_639_2B, this.tag, this.names);
    }

    public static Language getLanguage(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        try {
            String[] stringArray = RegularExpressions.TAB.split(Language.getProperty(string), 4);
            return new Language(string, stringArray[0], stringArray[1], stringArray[2], RegularExpressions.TAB.split(stringArray[3]));
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.message("Unexpected language code", string));
            return new Language(string, string, string, string, new String[]{string});
        }
    }

    public static List<Language> getLanguages(String ... stringArray) {
        return Arrays.stream(stringArray).map(Language::getLanguage).collect(Collectors.toList());
    }

    public static Language getLanguage(Locale locale) {
        return locale == null ? null : Language.findLanguage(locale.getLanguage());
    }

    public static Language findLanguage(String string) {
        return Language.availableLanguages().stream().filter(language -> language.matches(string)).findFirst().orElse(null);
    }

    public static Language forName(String string) {
        Language language = Language.findLanguage(string);
        if (language != null) {
            return language;
        }
        throw new IllegalArgumentException(string + " not in " + Language.availableLanguages());
    }

    public static List<Language> availableLanguages() {
        String string = Language.getProperty("languages.ui");
        return Language.getLanguages(RegularExpressions.SPACE.split(string));
    }

    public static List<Language> commonLanguages() {
        String string = Language.getProperty("languages.common");
        return Language.getLanguages(RegularExpressions.SPACE.split(string));
    }

    public static List<Language> preferredLanguages() {
        Stream<String> stream = Stream.of(Locale.ENGLISH, Locale.getDefault()).map(Locale::getLanguage);
        stream = Stream.concat(stream, RegularExpressions.SPACE.splitAsStream(Language.getProperty("languages.common"))).distinct();
        return stream.map(Language::getLanguage).collect(Collectors.toList());
    }

    public static Language defaultLanguage() {
        return Language.getLanguage("en");
    }

    private static String getProperty(String string) {
        return ResourceBundle.getBundle(Language.class.getName()).getString(string);
    }
}

