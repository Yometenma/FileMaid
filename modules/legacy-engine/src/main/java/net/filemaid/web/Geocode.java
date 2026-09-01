package net.filemaid.web;

import java.util.Map;

public interface Geocode {
    public Map<AddressComponent, String> locate(double var1, double var3) throws Exception;

    public static enum AddressComponent {
        country,
        administrative_area_level_1,
        administrative_area_level_2,
        administrative_area_level_3,
        administrative_area_level_4,
        sublocality,
        neighborhood,
        route,
        postal_code;

    }
}

