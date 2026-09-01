package net.filemaid.web;

import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.filemaid.CachedResource;
import net.filemaid.Logging;
import net.filemaid.web.Crew;
import net.filemaid.web.Person;
import net.filemaid.web.SimpleDate;
import net.filemaid.web.WebRequest;

public class MovieDetails
implements Crew,
Serializable {
    protected Map<Property, String> fields;
    protected String[] alternativeTitles;
    protected String[] genres;
    protected String[] keywords;
    protected String[] spokenLanguages;
    protected String[] productionCountries;
    protected String[] productionCompanies;
    protected Map<String, String> certifications;
    protected Map<String, String> translations;
    protected Person[] people;

    public MovieDetails() {
    }

    public MovieDetails(Map<Property, String> map, List<String> list, List<String> list2, List<String> list3, Map<String, String> map2, Map<String, String> map3, List<String> list4, List<String> list5, List<String> list6, List<Person> list7) {
        this.fields = new EnumMap<Property, String>(map);
        this.alternativeTitles = list.toArray(new String[0]);
        this.genres = list2.toArray(new String[0]);
        this.keywords = list3.toArray(new String[0]);
        this.certifications = new LinkedHashMap<String, String>(map2);
        this.translations = new LinkedHashMap<String, String>(map3);
        this.spokenLanguages = list4.toArray(new String[0]);
        this.productionCountries = list5.toArray(new String[0]);
        this.productionCompanies = list6.toArray(new String[0]);
        this.people = list7.toArray(new Person[0]);
    }

    public String get(Object object) {
        return this.fields.get((Object)Property.valueOf(object.toString()));
    }

    public String get(Property property) {
        return this.fields.get((Object)property);
    }

    private <T> T get(Property property, CachedResource.Transform<String, T> transform) {
        try {
            String string = this.fields.get((Object)property);
            if (string != null && !string.isEmpty()) {
                return transform.transform(string);
            }
        }
        catch (Exception exception) {
            Logging.debug.warning(Logging.message(new Object[]{"Invalid movie property", property, exception, this.fields}));
        }
        return null;
    }

    public String getName() {
        return this.get(Property.title);
    }

    public String getOriginalName() {
        return this.get(Property.original_title);
    }

    public String getOriginalLanguage() {
        return this.get(Property.original_language);
    }

    public String getCollection() {
        return this.get(Property.collection);
    }

    public String getCertification() {
        return this.get(Property.certification);
    }

    public String getStatus() {
        return this.get(Property.status);
    }

    public boolean isAdult() {
        return this.get(Property.adult, Boolean::parseBoolean);
    }

    public boolean isVideo() {
        return this.get(Property.video, Boolean::parseBoolean);
    }

    public String getTagline() {
        return this.get(Property.tagline);
    }

    public String getOverview() {
        return this.get(Property.overview);
    }

    public Integer getId() {
        return this.get(Property.id, Integer::parseInt);
    }

    public Integer getImdbId() {
        return this.get(Property.imdb_id, string -> Integer.parseInt(string.substring(2)));
    }

    public Integer getVotes() {
        return this.get(Property.vote_count, Integer::parseInt);
    }

    public Double getRating() {
        return this.get(Property.vote_average, Double::parseDouble);
    }

    public SimpleDate getReleased() {
        return this.get(Property.release_date, SimpleDate::parse);
    }

    public Integer getRuntime() {
        return this.get(Property.runtime, Integer::parseInt);
    }

    public Long getBudget() {
        return this.get(Property.budget, Long::parseLong);
    }

    public Long getRevenue() {
        return this.get(Property.revenue, Long::parseLong);
    }

    public Double getPopularity() {
        return this.get(Property.popularity, Double::parseDouble);
    }

    public URL getHomepage() {
        return this.get(Property.homepage, WebRequest::newURL);
    }

    public URL getPoster() {
        return this.get(Property.poster_path, WebRequest::newURL);
    }

    public URL getBackdrop() {
        return this.get(Property.backdrop_path, WebRequest::newURL);
    }

    public List<String> getGenres() {
        return Collections.unmodifiableList(Arrays.asList(this.genres));
    }

    public List<String> getKeywords() {
        return Collections.unmodifiableList(Arrays.asList(this.keywords));
    }

    public List<String> getSpokenLanguages() {
        return Collections.unmodifiableList(Arrays.asList(this.spokenLanguages));
    }

    @Override
    public List<Person> getCrew() {
        return Collections.unmodifiableList(Arrays.asList(this.people));
    }

    public Map<String, String> getCertifications() {
        return Collections.unmodifiableMap(this.certifications);
    }

    public Map<String, String> getTranslations() {
        return Collections.unmodifiableMap(this.translations);
    }

    public List<String> getProductionCountries() {
        return Collections.unmodifiableList(Arrays.asList(this.productionCountries));
    }

    public List<String> getProductionCompanies() {
        return Collections.unmodifiableList(Arrays.asList(this.productionCompanies));
    }

    public List<String> getAlternativeTitles() {
        return Collections.unmodifiableList(Arrays.asList(this.alternativeTitles));
    }

    public Map<Property, String> properties() {
        return Collections.unmodifiableMap(this.fields);
    }

    public String toString() {
        if (this.fields.containsKey((Object)Property.id)) {
            return this.get(Property.id);
        }
        if (this.fields.containsKey((Object)Property.imdb_id)) {
            return this.get(Property.imdb_id);
        }
        return this.fields.toString();
    }

    public static enum Property {
        adult,
        backdrop_path,
        budget,
        homepage,
        id,
        imdb_id,
        original_title,
        original_language,
        overview,
        popularity,
        poster_path,
        release_date,
        revenue,
        runtime,
        status,
        tagline,
        title,
        video,
        vote_average,
        vote_count,
        certification,
        collection;

    }
}

