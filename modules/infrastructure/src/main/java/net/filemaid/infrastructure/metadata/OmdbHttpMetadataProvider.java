package net.filemaid.infrastructure.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** OMDb provider (movies and series, requires an API key). */
public final class OmdbHttpMetadataProvider implements MetadataProvider {
    private final String apiKey;
    private final HttpClient client;
    private final String endpoint;
    private final Duration timeout;
    private final ObjectMapper json = new ObjectMapper();

    public OmdbHttpMetadataProvider(String apiKey) {
        this(apiKey, null, null, null);
    }

    public OmdbHttpMetadataProvider(String apiKey, String endpoint, HttpClient client, Duration timeout) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.endpoint = endpoint == null || endpoint.isBlank() ? "https://www.omdbapi.com/" : endpoint;
        this.client = client == null ? HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build() : client;
        this.timeout = timeout == null ? Duration.ofSeconds(20) : timeout;
    }

    @Override public String id() { return "omdb"; }
    @Override public boolean available() { return !apiKey.isBlank(); }
    @Override public String status() { return available() ? "OMDb 客户端已就绪" : "未配置 OMDb API Key（FILEMAID_OMDB_API_KEY）"; }

    @Override
    public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) throws Exception {
        String typeParam = type == MetadataType.MOVIE ? "movie" : "series";
        String url = endpoint + (endpoint.contains("?") ? "&" : "?") + "apikey=" + encode(apiKey) + "&s=" + encode(query) + "&type=" + typeParam;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OMDb 请求失败（HTTP " + response.statusCode() + "）");
        }
        List<MetadataCandidate> values = new ArrayList<>();
        JsonNode search = json.readTree(response.body()).path("Search");
        if (search.isArray()) {
            for (JsonNode item : search) {
                if (values.size() >= limit) break;
                String title = text(item, "Title");
                String yearStr = text(item, "Year");
                Integer year = yearStr != null && yearStr.length() >= 4 ? parseYear(yearStr.substring(0, 4)) : null;
                String poster=text(item,"Poster");
                values.add(new MetadataCandidate(id(), text(item, "imdbID"), type, title, List.of(), year, null, poster != null && poster.startsWith("https://") ? poster : null));
            }
        }
        return values;
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String text(JsonNode node, String field) { String value = node.path(field).asText(null); return value == null || value.isBlank() ? null : value; }
    private Integer parseYear(String value) { try { return Integer.valueOf(value); } catch (NumberFormatException ignored) { return null; } }
}
