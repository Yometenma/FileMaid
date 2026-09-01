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

/** TVMaze provider (series only, no API key required). */
public final class TvMazeHttpMetadataProvider implements MetadataProvider {
    private final boolean enabled;
    private final HttpClient client;
    private final String endpoint;
    private final Duration timeout;
    private final ObjectMapper json = new ObjectMapper();

    public TvMazeHttpMetadataProvider(boolean enabled) {
        this(enabled, null, null, null);
    }

    public TvMazeHttpMetadataProvider(boolean enabled, String endpoint, HttpClient client, Duration timeout) {
        this.enabled = enabled;
        this.endpoint = endpoint == null || endpoint.isBlank() ? "https://api.tvmaze.com" : endpoint.replaceAll("/+$", "");
        this.client = client == null ? HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build() : client;
        this.timeout = timeout == null ? Duration.ofSeconds(20) : timeout;
    }

    @Override public String id() { return "tvmaze"; }
    @Override public boolean available() { return enabled; }
    @Override public String status() { return "TVMaze 客户端已就绪（无需密钥）"; }

    @Override
    public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) throws Exception {
        if (type == MetadataType.MOVIE) return List.of();
        String url = endpoint + "/search/shows?q=" + encode(query);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new MetadataHttpException("TVMaze", response.statusCode(), response.headers());
        }
        List<MetadataCandidate> values = new ArrayList<>();
        for (JsonNode item : json.readTree(response.body())) {
            if (values.size() >= limit) break;
            JsonNode show = item.path("show");
            String title = text(show, "name");
            String premiered = text(show, "premiered");
            Integer year = premiered != null && premiered.length() >= 4 ? parseYear(premiered.substring(0, 4)) : null;
            values.add(new MetadataCandidate(id(), show.path("id").asText(), type, title, List.of(), year, text(show, "summary"), show.path("image").path("original").asText(null)));
        }
        return values;
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String text(JsonNode node, String field) { String value = node.path(field).asText(null); return value == null || value.isBlank() ? null : value; }
    private Integer parseYear(String value) { try { return Integer.valueOf(value); } catch (NumberFormatException ignored) { return null; } }
}
