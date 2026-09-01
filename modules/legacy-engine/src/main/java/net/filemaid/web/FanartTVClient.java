package net.filemaid.web;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.Icon;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.Logging;
import net.filemaid.util.JsonUtilities;
import net.filemaid.web.Artwork;
import net.filemaid.web.ArtworkProvider;
import net.filemaid.web.Datasource;
import net.filemaid.web.WebRequest;

public class FanartTVClient
implements Datasource {
    private String apikey;
    public final ArtworkProvider TheTVDB = (n, locale) -> this.getArtwork(n, "tv", locale);
    public final ArtworkProvider TheMovieDB = (n, locale) -> this.getArtwork(n, "movies", locale);

    public FanartTVClient(String string) {
        this.apikey = string;
    }

    @Override
    public String getIdentifier() {
        return "FanartTV";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    public List<Artwork> getArtwork(int n, String string, Locale locale) throws Exception {
        return this.requestArtwork(string + "/" + n, locale);
    }

    public List<Artwork> requestArtwork(String string, Locale locale) throws Exception {
        Object object3 = this.request(string);
        ArrayList<Artwork> arrayList = new ArrayList<Artwork>();
        JsonUtilities.asMap(object3).forEach((object, object2) -> {
            for (Object item : JsonUtilities.asArray(object2)) {
                URL uRL = JsonUtilities.getStringValue(item, "url", s -> WebRequest.parseURL(s.replace(" ", "%20")));
                Locale lang = JsonUtilities.getStringValue(item, "lang", Locale::forLanguageTag);
                Double d = JsonUtilities.getDouble(item, "likes");
                String string2 = JsonUtilities.getString(item, "season");
                String string3 = JsonUtilities.getString(item, "disc_type");
                if (uRL == null) {
                    Logging.debug.warning(Logging.message("Bad artwork response", JsonUtilities.json(item)));
                    continue;
                }
                arrayList.add(new Artwork(uRL, lang, d, "fanart", object, string2, string3));
            }
        });
        arrayList.sort(Artwork.relevanceOrder(locale, Locale.ENGLISH).thenComparing(FanartTVClient.hdOrder()));
        return arrayList;
    }

    private Object request(String string) throws Exception {
        Cache cache = Cache.getConcurrentCache(this.getName(), CacheType.Monthly);
        return cache.json(string, this::getResource).expire(Cache.ONE_MONTH).get();
    }

    private URL getResource(String string) throws Exception {
        return WebRequest.newURL("https://webservice.fanart.tv/v3/" + string + "?api_key=" + this.apikey);
    }

    private static Comparator<Artwork> hdOrder() {
        return Comparator.comparingInt(artwork -> artwork.has(string -> string.startsWith("hd")) ? 0 : 1);
    }
}

