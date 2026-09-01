package net.filemaid.web;

import java.io.FileNotFoundException;
import java.net.URL;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.util.JsonUtilities;
import net.filemaid.web.Artwork;
import net.filemaid.web.WebRequest;

public enum Manami {
    AniDB;

    private final Pattern source = Pattern.compile("anidb.net/anime/(\\d+)");

    public Map<Integer, Artwork> getArtwork() throws Exception {
        HashMap<Integer, Artwork> hashMap = new HashMap<Integer, Artwork>();
        this.getRecords().forEach((n, object) -> {
            URL uRL = JsonUtilities.getStringValue(object, "picture", string -> WebRequest.parseURL(string));
            if (uRL != null && uRL.getQuery() == null && uRL.getPath().endsWith(".jpg")) {
                hashMap.put((Integer)n, new Artwork(uRL, null, null, "manami", "picture"));
            }
        });
        return hashMap;
    }

    public Optional<Integer> matchID(String string) {
        Matcher matcher = this.source.matcher(string);
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        return Optional.empty();
    }

    public Map<Integer, Object> getRecords() throws Exception {
        HashMap<Integer, Object> hashMap = new HashMap<Integer, Object>();
        Object object = this.request("anime-offline-database-minified.json");
        JsonUtilities.streamJsonObjects(object, "data").forEach(map2 -> {
            for (String string : JsonUtilities.getStringArray(map2, "sources")) {
                this.matchID(string).ifPresent(n -> hashMap.put((Integer)n, map2));
            }
        });
        return hashMap;
    }

    private Object request(String string2) throws Exception {
        Cache cache = this.getCache();
        try {
            Object object = cache.json(string2, string -> {
                Object releaseObject = cache.json("https://api.github.com/repos/manami-project/anime-offline-database/releases", WebRequest::newURL).expire(Cache.ONE_MONTH).get();
                return JsonUtilities.streamJsonObjects(releaseObject).flatMap(map -> JsonUtilities.streamJsonObjects(map, "assets")).filter(map -> string.equals(JsonUtilities.getString(map, "name"))).max(Comparator.comparing(map -> JsonUtilities.getStringValue(map, "updated_at", Instant::parse))).map(map -> JsonUtilities.getStringValue(map, "browser_download_url", WebRequest::parseURL)).orElseThrow(() -> new FileNotFoundException(string + " not found"));
            }).expire(Cache.ONE_MONTH).get();
            return object;
        }
        finally {
            cache.flush();
        }
    }

    public Cache getCache() {
        return Cache.getCache("manami", CacheType.Persistent);
    }
}

