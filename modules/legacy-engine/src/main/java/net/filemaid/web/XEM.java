package net.filemaid.web;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.util.JsonUtilities;
import net.filemaid.web.Episode;
import net.filemaid.web.EpisodeUtilities;
import net.filemaid.web.MultiEpisode;
import net.filemaid.web.SearchResult;
import net.filemaid.web.WebRequest;

public enum XEM {
    TheTVDB,
    AniDB,
    Scene,
    Tract;


    public String getOriginName() {
        switch (this) {
            case TheTVDB: {
                return "tvdb";
            }
            case AniDB: {
                return "anidb";
            }
            case Scene: {
                return "scene";
            }
            case Tract: {
                return "tract";
            }
        }
        return null;
    }

    public Episode map(Episode episode, XEM xEM) throws Exception {
        Integer n;
        Integer n2;
        if (this != TheTVDB) {
            throw new UnsupportedOperationException("XEM from " + this + " to " + xEM + " not supported");
        }
        Integer n3 = episode.getSeriesInfo().getId();
        Map<String, Map<String, Number>> map = this.getSingle(n3, n2 = Integer.valueOf(EpisodeUtilities.isSpecialEpisode(episode) ? 0 : episode.getSeason()), n = EpisodeUtilities.isSpecialEpisode(episode) ? episode.getSpecial() : episode.getEpisode());
        List<Episode> list = map.entrySet().stream().map(entry -> {
            if (((String)entry.getKey()).startsWith(xEM.getOriginName())) {
                Map item = (Map)entry.getValue();
                Integer season = JsonUtilities.getInteger(item, "season");
                Integer episodeNumber = JsonUtilities.getInteger(item, "episode");
                Integer absolute = JsonUtilities.getInteger(item, "absolute");
                return episodeNumber == null ? null : new Episode(episode.getSeriesName(), season, episodeNumber, null, absolute, null, episode.getAirdate(), null, null, null, null);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        if (list.size() == 1) {
            return (Episode)list.get(0);
        }
        if (list.size() > 1) {
            return new MultiEpisode(list);
        }
        return null;
    }

    public List<Map<String, Map<String, Number>>> getAll(Integer n) throws Exception {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>(2);
        linkedHashMap.put("origin", this.getOriginName());
        linkedHashMap.put("id", n);
        Object object = this.request("all", linkedHashMap);
        return (List)Arrays.asList(JsonUtilities.getArray(object, "data"));
    }

    public Map<String, Map<String, Number>> getSingle(Integer n, Integer n2, Integer n3) throws Exception {
        return this.getAll(n).stream().filter(map -> map.entrySet().stream().anyMatch(entry -> ((String)entry.getKey()).startsWith(this.getOriginName()) && Objects.equals(n2, JsonUtilities.getInteger(entry.getValue(), "season")) && Objects.equals(n3, JsonUtilities.getInteger(entry.getValue(), "episode")))).findFirst().orElse(Collections.emptyMap());
    }

    public List<SearchResult> getAllNames() throws Exception {
        LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>(4);
        linkedHashMap.put("origin", this.getOriginName());
        linkedHashMap.put("defaultNames", "1");
        Object object = this.request("allNames", linkedHashMap);
        return JsonUtilities.getMap(object, "data").entrySet().stream().map(entry -> {
            int n = Integer.parseInt(entry.getKey().toString());
            List list = Arrays.stream(JsonUtilities.asArray(entry.getValue())).filter(Objects::nonNull).map(Objects::toString).filter(string -> !string.isEmpty()).collect(Collectors.toList());
            if (list.isEmpty()) {
                return null;
            }
            return new SearchResult(n, (String)list.get(0), list.subList(1, list.size()));
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected Object request(String string, Map<String, Object> map) throws Exception {
        return this.request(string + "?" + WebRequest.encodeParameters(map));
    }

    protected Object request(String string) throws Exception {
        return this.getCache().json(string, this::getResource).expire(Cache.ONE_WEEK).get();
    }

    protected URL getResource(String string) throws Exception {
        return WebRequest.newURL("https://thexem.info/map/" + string);
    }

    protected Cache getCache() {
        return Cache.getConcurrentCache("xem", CacheType.Monthly);
    }

    public static List<String> names() {
        return Arrays.stream(XEM.values()).map(Enum::name).collect(Collectors.toList());
    }

    public static XEM forName(String string) {
        for (XEM xEM : XEM.values()) {
            if (!xEM.name().equalsIgnoreCase(string) && !xEM.getOriginName().equalsIgnoreCase(string)) continue;
            return xEM;
        }
        throw new IllegalArgumentException(string + " not in " + XEM.names());
    }
}

