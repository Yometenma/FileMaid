package net.filemaid.infrastructure.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.filemaid.application.port.MetadataProvider;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;

/**
 * TheTVDB v4 metadata provider. Logs in with an API key (and optional PIN) to
 * obtain a bearer token, then searches series/movies. Network access is explicit
 * and only happens when a key is configured.
 */
public final class TvdbHttpMetadataProvider implements MetadataProvider {
    private final String apiKey;
    private final String pin;
    private final HttpClient client;
    private final ObjectMapper json = new ObjectMapper();
    private volatile String token;

    public TvdbHttpMetadataProvider(String apiKey, String pin) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.pin = pin == null ? "" : pin.trim();
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override public String id() { return "tvdb"; }
    @Override public boolean available() { return !apiKey.isBlank(); }
    @Override public String status() { return available() ? "TVDB 客户端已就绪" : "未配置 TVDB API Key（FILEMAID_TVDB_API_KEY）"; }

    @Override
    public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) throws Exception {
        String typeParam = type == MetadataType.MOVIE ? "movie" : "series";
        String url = "https://api4.thetvdb.com/v4/search?q=" + encode(query) + "&type=" + typeParam;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + authorizationToken())
                .header("Accept", "application/json")
                .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("TVDB 请求失败（HTTP " + response.statusCode() + "）");
        }
        List<MetadataCandidate> values = new ArrayList<>();
        for (JsonNode item : json.readTree(response.body()).path("data")) {
            if (values.size() >= limit) break;
            String title = text(item, "name");
            String firstAired = text(item, "firstAired");
            Integer year = firstAired != null && firstAired.length() >= 4 ? parseYear(firstAired.substring(0, 4)) : null;
            values.add(new MetadataCandidate(id(), item.path("id").asText(), type, title, List.of(), year, text(item, "overview")));
        }
        return values;
    }

    private synchronized String authorizationToken() throws Exception {
        if (token != null) return token;
        ObjectNode body = json.createObjectNode();
        body.put("apikey", apiKey);
        if (!pin.isBlank()) body.put("pin", pin);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api4.thetvdb.com/v4/login"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("TVDB 登录失败（HTTP " + response.statusCode() + "）");
        }
        token = json.readTree(response.body()).path("data").path("token").asText();
        return token;
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String text(JsonNode node, String field) { String value = node.path(field).asText(null); return value == null || value.isBlank() ? null : value; }
    private Integer parseYear(String value) { try { return Integer.valueOf(value); } catch (NumberFormatException ignored) { return null; } }
}
