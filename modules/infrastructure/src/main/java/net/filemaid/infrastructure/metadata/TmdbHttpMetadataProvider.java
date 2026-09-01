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

public final class TmdbHttpMetadataProvider implements MetadataProvider {
    private final String apiKey;
    private final HttpClient client;
    private final String endpoint;
    private final Duration timeout;
    private final ObjectMapper json = new ObjectMapper();

    public TmdbHttpMetadataProvider(String apiKey) {
        this(apiKey, null, null, null);
    }

    public TmdbHttpMetadataProvider(String apiKey, String endpoint, HttpClient client, Duration timeout) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.endpoint = endpoint == null || endpoint.isBlank() ? "https://api.themoviedb.org/3" : endpoint.replaceAll("/+$", "");
        this.client = client == null ? HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build() : client;
        this.timeout = timeout == null ? Duration.ofSeconds(20) : timeout;
    }

    @Override public String id() { return "tmdb"; }
    @Override public boolean available() { return !apiKey.isBlank(); }
    @Override public String status() { return available() ? "TMDB 轻量客户端已就绪" : "未配置 TMDB API Key（FILEMAID_TMDB_API_KEY）"; }

    @Override
    public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) throws Exception {
        String category = type == MetadataType.MOVIE ? "movie" : "tv";
        String language = locale == null ? "zh-CN" : locale.toLanguageTag();
        String url = endpoint + "/search/" + category
                + "?api_key=" + encode(apiKey) + "&query=" + encode(query) + "&language=" + encode(language);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("TMDB 请求失败（HTTP " + response.statusCode() + "）");
        }
        List<MetadataCandidate> values = new ArrayList<>();
        for (JsonNode item : json.readTree(response.body()).path("results")) {
            if (values.size() >= limit) break;
            String title = text(item, type == MetadataType.MOVIE ? "title" : "name");
            String original = text(item, type == MetadataType.MOVIE ? "original_title" : "original_name");
            String date = text(item, type == MetadataType.MOVIE ? "release_date" : "first_air_date");
            Integer year = date != null && date.length() >= 4 ? parseYear(date.substring(0, 4)) : null;
            List<String> aliases = original == null || original.equals(title) ? List.of() : List.of(original);
            String poster=text(item,"poster_path");
            values.add(new MetadataCandidate(id(), item.path("id").asText(), type, title, aliases, year, text(item, "overview"), poster==null?null:"https://image.tmdb.org/t/p/w500"+poster));
        }
        return values;
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String text(JsonNode node, String field) { String value = node.path(field).asText(null); return value == null || value.isBlank() ? null : value; }
    private Integer parseYear(String value) { try { return Integer.valueOf(value); } catch (NumberFormatException ignored) { return null; } }
}
