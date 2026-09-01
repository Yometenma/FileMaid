package net.filemaid;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.filemaid.Cache;
import net.filemaid.CacheType;
import net.filemaid.CachedResource;
import net.filemaid.InvalidResponseException;
import net.filemaid.Logging;
import net.filemaid.Resource;
import net.filemaid.Settings;
import net.filemaid.UserData;
import net.filemaid.util.JsonUtilities;
import net.filemaid.util.PGP;
import net.filemaid.util.RegularExpressions;
import net.filemaid.web.WebRequest;

public final class License {
    private final String psm;
    private final Exception error;
    private final String product;
    private final String id;
    private final String email;
    private final Instant expires;
    public static final Resource.Memoized<License> INSTANCE = Resource.lazy(() -> License.parseLicenseFile(UserData.getLicenseFile()));

    private License(String string) {
        Map<String, String> map = License.getProperties(string);
        this.product = map.get("Product");
        this.id = map.get("Order");
        this.email = map.get("Email");
        this.expires = Optional.ofNullable(map.get("Valid-Until")).map(License::parseExpirationDate).orElse(null);
        this.psm = string;
        this.error = null;
    }

    private License(Exception exception) {
        this.product = null;
        this.id = null;
        this.email = null;
        this.expires = null;
        this.psm = null;
        this.error = exception;
    }

    public String email() {
        return this.email;
    }

    public boolean expires() {
        return this.expires != null;
    }

    public boolean error() {
        return this.error != null;
    }

    public Optional<Integer> remainingDays() {
        return Optional.ofNullable(this.expires).map(instant -> (int)ChronoUnit.DAYS.between(Instant.now(), (Temporal)instant));
    }

    public License check(boolean bl) throws Exception {
        if (this.error()) {
            throw this.error;
        }
        this.checkExpirationDate();
        this.verifyLicense(bl);
        return this;
    }

    private void checkExpirationDate() throws Exception {
        if (this.expires != null && Instant.now().isAfter(this.expires)) {
            throw new IllegalStateException(this.product + " License " + this.id + " has EXPIRED on " + License.formatExpirationDate(this.expires));
        }
    }

    private void verifyLicense(boolean bl) throws Exception {
        Cache cache = Cache.getCache("data", CacheType.Monthly);
        try {
            if (bl) {
                cache.remove(this.id);
            }
            Object object = cache.json(this.id, string -> WebRequest.newURL("https://license.filebot.net/verify/" + string)).fetch(CachedResource.post(() -> {
                Logging.debug.warning(Logging.format("Activate License [%s] on [%tc]", this.id, System.currentTimeMillis()));
                return this.psm.getBytes(StandardCharsets.UTF_8);
            }, () -> "application/octet-stream", () -> {
                LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
                linkedHashMap.put("Date", DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()));
                linkedHashMap.put("X-FileBot-OS", Settings.getSystemIdentifier());
                linkedHashMap.put("X-FileBot-PKG", Settings.DEPLOYMENT.name());
                return linkedHashMap;
            })).retry(cache.contains(this.id) ? 0 : 2).expire(Cache.ONE_MONTH).get();
            Integer n = JsonUtilities.getInteger(object, "status");
            String string2 = JsonUtilities.getString(object, "message");
            if (n == null || n != 200) {
                cache.remove(this.id);
                if (string2 == null) {
                    throw new InvalidResponseException("Response is empty", object);
                }
                throw new IllegalStateException(string2);
            }
        }
        finally {
            cache.flush();
        }
    }

    public String toString() {
        if (this.error()) {
            return "Bad License (" + this.error.getClass().getSimpleName() + ": " + this.error.getMessage() + ")";
        }
        if (this.expires()) {
            return this.product + " License " + this.id + " (Valid-Until: " + License.formatExpirationDate(this.expires) + ")";
        }
        return this.product + " License " + this.id;
    }

    public static void checkServerStatus() throws Exception {
        ByteBuffer byteBuffer = WebRequest.fetch(WebRequest.newURL("https://license.filebot.net/status.json"));
        Object object = JsonUtilities.readJson(StandardCharsets.UTF_8.decode(byteBuffer));
        if (JsonUtilities.getInteger(object, "status") != 200) {
            throw new IllegalStateException(JsonUtilities.getString(object, "message"));
        }
    }

    private static Map<String, String> getProperties(String string2) {
        String string3 = PGP.verifyClearSignMessage(string2, License.getPublicKey());
        return RegularExpressions.NEWLINE.splitAsStream(string3).map(string -> string.split(": ", 2)).collect(Collectors.toMap(stringArray -> stringArray[0], stringArray -> stringArray[1]));
    }

    private static Instant parseExpirationDate(String string) {
        return LocalDate.parse(string, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(ZoneOffset.UTC).plusDays(1L).minusSeconds(1L).toInstant();
    }

    private static String formatExpirationDate(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static License parseLicenseFile(File file) {
        try {
            if (file.length() <= 0L) {
                throw new FileNotFoundException("UNREGISTERED");
            }
            String string = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return new License(string);
        }
        catch (Exception exception) {
            return new License(exception);
        }
    }

    public static License parseLicenseText(String string) {
        try {
            return new License(string);
        }
        catch (Exception exception) {
            return new License(exception);
        }
    }

    public static License importLicense(String string, boolean bl) throws Exception {
        Resource.Memoized<License> memoized = INSTANCE;
        synchronized (memoized) {
            License license = License.parseLicenseText(string).check(bl);
            File file = UserData.getLicenseFile();
            if (file.exists() && !Settings.isNAS()) {
                file.delete();
            }
            Logging.log.config(Logging.format("Write [%s] to [%s]", license, file));
            Files.write(file.toPath(), string.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
            INSTANCE.clear();
            return license;
        }
    }

    public static byte[] getPublicKey() {
        return new byte[]{-103, 1, 13, 4, 91, 26, -79, -16, 1, 8, 0, -68, -49, 31, 86, -22, -22, 62, 63, -41, -21, -48, -80, -71, 83, -51, 54, 52, 107, 46, -35, 50, 21, -109, 83, 107, -54, 120, 79, -43, 62, 44, -115, 94, -101, 94, 58, -68, 49, -34, 32, 50, -115, -128, 3, 67, 109, 29, 3, 9, 24, -119, -33, 33, 118, -82, -9, -1, -49, 3, 43, 127, -121, -114, 68, -81, 90, 4, 7, 85, 48, 50, -8, -77, 109, 126, 51, -47, -98, -118, -14, 24, 7, 45, 89, -37, -11, -47, -9, -50, -44, 104, 95, -76, -124, 97, 38, -69, -105, 104, 11, -97, 11, 2, -51, -80, 37, 9, -29, 57, 22, -34, -105, 127, -54, -96, -69, 19, -45, 94, -98, -12, -19, 94, -45, 102, -115, 25, -37, -105, -24, 18, 35, -120, -19, 11, -25, 25, 17, 21, -45, 37, -52, 88, -21, 59, 126, -15, 81, -84, -71, -60, 92, 75, 23, 49, -17, 66, 78, 77, -116, 90, -102, -108, 100, -27, 28, 87, 73, 58, 95, 101, 81, -61, 116, 13, -99, 84, 73, -29, -125, -80, 84, -44, 63, 124, -56, 15, -14, -46, 99, -71, -53, -83, -77, -52, -112, -57, -34, -94, 93, 126, -83, -16, -53, -76, 64, -123, 44, 1, -126, -36, 49, 101, 70, 54, 119, -21, 20, 113, 55, -35, -73, 6, 88, -120, -109, 122, -107, 5, -125, 3, 59, 105, -103, 108, 12, -112, 70, 65, 75, -11, 118, -62, 50, -88, 104, -113, 44, -110, 34, -55, 66, -12, -53, -15, -95, 0, 17, 1, 0, 1, -76, 44, 70, 105, 108, 101, 66, 111, 116, 32, 76, 105, 99, 101, 110, 115, 101, 32, 77, 97, 115, 116, 101, 114, 32, 60, 108, 105, 99, 101, 110, 115, 101, 64, 102, 105, 108, 101, 98, 111, 116, 46, 110, 101, 116, 62, -119, 1, 56, 4, 19, 1, 2, 0, 34, 5, 2, 91, 26, -79, -16, 2, 27, 3, 6, 11, 9, 8, 7, 3, 2, 6, 21, 8, 2, 9, 10, 11, 4, 22, 2, 3, 1, 2, 30, 1, 2, 23, -128, 0, 10, 9, 16, -54, 18, -115, 8, 36, -50, 32, -39, -72, -35, 7, -3, 22, 0, -71, 2, 103, -14, 52, -1, -99, 88, 54, -27, 100, 109, 4, -1, -96, -19, 90, 127, 90, -82, -118, -20, -103, -80, 44, 72, -126, -57, 42, -87, -34, 58, 106, -57, -21, -108, 99, -47, -68, -57, 84, 88, -73, 86, 57, 95, -46, -40, 104, 63, 88, -60, -92, 92, -70, -100, 117, -25, -12, 104, 32, 104, -61, -8, 22, 124, -31, 119, -61, -3, -53, -87, -127, -11, 30, -108, 14, 104, -122, 28, 69, 112, -78, 71, 12, -48, 56, -91, 33, -105, 78, -27, 10, -59, 17, 103, 84, 73, -22, -7, -41, -91, 65, 48, -58, -45, -60, 127, 84, -18, 83, -73, 38, 89, 22, 80, 105, -95, 116, -106, -88, 63, 72, 124, 42, 39, -65, -46, -123, -107, -18, 100, 99, 105, 82, -49, 54, -7, -62, 104, 73, -29, -58, 45, 35, 71, 11, 99, 96, -88, -71, -80, 63, -34, -111, -17, 12, 47, -121, 72, -49, -117, -19, 39, -16, 93, -27, -28, 28, 69, -124, -13, -11, 73, -71, -44, 56, 55, -18, 52, -51, -30, 70, -100, -7, 43, -128, 124, 3, 91, -21, -30, -57, 58, 27, -92, -40, -81, -123, -68, 31, -39, 116, -30, 9, -4, 126, -71, 65, 83, -34, -63, -57, 105, -80, -106, 59, -16, -92, -50, -20, -13, -29, 6, -58, 6, -49, 15, -104, 101, 54, -80, 38, 7, 119, -110, 72, 125, 39, 110, -84, 66, 101, 54, -7, 33, -84, 27, 36, 24, -12, 74, -118, -86, -71, 1, 13, 4, 91, 26, -79, -16, 1, 8, 0, -60, -11, 70, -34, -72, -12, 54, 5, -35, 65, 31, 92, -85, 82, 79, -76, 84, 11, -2, -31, 97, -35, -109, 11, -80, 6, -40, -124, -128, 8, -77, 88, 112, -27, 80, 9, 42, 99, -69, 108, -5, -110, 39, -32, 96, 97, 103, 118, 77, -60, -51, 34, -14, -39, -61, 17, -119, 113, 54, 3, 48, -39, -55, 109, -17, 102, -96, -103, -6, -56, -24, -111, 70, -15, 107, 68, -78, -27, 110, 33, 64, 112, 108, -21, 89, -84, 47, 29, -27, 52, -7, -68, -47, 105, -116, 25, -102, -77, 80, 127, -72, 122, 22, 113, -36, -32, 89, 73, -60, 123, 6, -103, -57, 117, -88, -105, -63, -45, 39, -6, -17, 71, -47, 119, -57, 30, -76, 10, 91, 19, -86, 7, -67, -79, -34, -52, 122, 120, -88, 110, -33, 38, 27, -122, 119, 26, -125, 60, -32, 109, 66, -128, 98, 56, -56, -111, 85, -73, 32, 111, -47, -38, 95, -34, -65, 81, -55, -51, 91, 31, -109, 63, -19, 39, 81, 123, -9, -51, 51, 98, 92, -101, 106, -22, -39, 121, -7, -92, 30, -93, -28, 95, -93, 43, -104, 44, 100, -35, -111, -124, 82, -128, -38, -8, -52, 44, -101, -126, -8, -24, 25, 114, -57, 51, 44, -6, -98, -46, 23, -113, 101, -80, -24, 29, 38, -35, 18, -74, -80, -57, -84, 51, -103, 119, 35, -97, 56, 9, -19, 52, -38, 22, 118, -100, 124, 18, 35, 1, 71, 77, 90, -29, 28, 44, 36, -71, 0, 17, 1, 0, 1, -119, 1, 31, 4, 24, 1, 2, 0, 9, 5, 2, 91, 26, -79, -16, 2, 27, 12, 0, 10, 9, 16, -54, 18, -115, 8, 36, -50, 32, -39, -88, 106, 7, -1, 89, -10, 97, 45, -12, -12, -14, -39, 87, 100, -33, -34, 81, 39, 86, 92, 85, -26, 63, -1, 30, -31, 10, 89, 33, 103, 80, -106, -44, -58, -122, -88, -82, 82, 107, 34, -51, 123, 103, 110, 91, 93, 93, 77, 30, 80, -119, -70, -108, -37, -100, 43, -33, -17, -55, 90, -94, 3, -112, 66, 94, 125, 80, -93, 10, 97, -19, 73, 45, -90, 46, 59, 34, -91, -100, -112, -45, 11, -12, -81, -91, -125, -127, 116, 124, 124, -100, -3, -94, 23, 105, 104, 121, 51, -49, 110, 125, 37, 86, 113, -122, 63, 46, -11, -64, -37, 91, -14, 26, -4, 25, -35, 31, -24, 84, 6, 88, 114, 49, -20, 89, 19, -70, 110, 101, -70, 71, 50, -11, 36, 58, -89, 35, 109, -106, 54, 39, 27, -38, -57, -105, 97, -118, 103, 51, 23, 120, 75, -101, 71, 65, 67, 8, 75, 71, 18, 87, -13, 47, -92, 76, 114, 120, -49, 118, -108, 85, 57, 69, -33, -68, -95, 77, 52, -57, 81, -82, -21, 81, -107, -34, 67, 39, 108, -46, -83, 113, 125, 67, -4, -91, 71, -44, -99, 90, 61, -57, 119, -27, 121, 87, 114, -3, -103, 108, -48, -118, -96, -32, 67, -64, 69, -33, -27, -114, 38, 29, -7, 105, 36, 68, -2, 5, 86, 124, -10, -46, -17, 30, -98, 39, -31, 13, -126, 47, -97, 114, -34, 15, -74, 74, -64, -51, -54, 17, 1, 61, 49, 57, 102, -93, 108, -114, -80, -58, -26};
    }
}

