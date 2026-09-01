package net.filemaid.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.util.JsonUtilities;
import net.filemaid.web.Geocode;
import net.filemaid.web.WebRequest;

public class GoogleMapsClient
implements Geocode {
    private final String apikey;

    public GoogleMapsClient(String string) {
        this.apikey = string;
    }

    private String getCoordinatePair(double d, double d2) {
        return String.format(Locale.ROOT, "%.2f,%.2f", d, d2);
    }

    @Override
    public Map<Geocode.AddressComponent, String> locate(double d, double d2) throws Exception {
        Object object = Cache.getConcurrentCache("geocode", CacheType.Persistent).json(this.getCoordinatePair(d, d2), string -> WebRequest.newURL("https://maps.googleapis.com/maps/api/geocode/json?latlng=" + string + "&sensor=false&key=" + this.apikey)).expire(Cache.NEVER).get();
        return JsonUtilities.streamJsonObjects(object, "results").map(map -> {
            Map<Geocode.AddressComponent, String> enumMap = new EnumMap<Geocode.AddressComponent, String>(Geocode.AddressComponent.class);
            JsonUtilities.streamJsonObjects(map, "address_components").forEach(map2 -> {
                String string = JsonUtilities.getString(map2, "long_name");
                if (string != null) {
                    for (Object type : JsonUtilities.getArray(map2, "types")) {
                        Arrays.stream(Geocode.AddressComponent.values()).filter(addressComponent -> addressComponent.name().equals(type)).findFirst().ifPresent(addressComponent -> enumMap.putIfAbsent((Geocode.AddressComponent)((Object)((Object)addressComponent)), string));
                    }
                }
            });
            return enumMap;
        }).max(Comparator.comparingInt(Map::size)).orElse(Collections.emptyMap());
    }
}

