package net.filemaid.infrastructure.metadata;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import net.filemaid.application.port.MetadataProvider;
import net.filemaid.core.model.MetadataCandidate;
import net.filemaid.core.model.MetadataType;
import net.filemaid.core.model.RankedCandidate;

/**
 * AniDB provider for anime titles. AniDB exposes a public title index
 * ({@code anime-titles.dat.gz}); this provider downloads it once, parses it, and
 * matches locally. Index download is lazy and cached for the process lifetime.
 */
public final class AnidbHttpMetadataProvider implements MetadataProvider {
    private static final String DEFAULT_INDEX_URL = "https://anidb.net/api/anime-titles.dat.gz";
    private final boolean enabled;
    private final HttpClient client;
    private final String indexUrl;
    private final Duration timeout;
    private final Path cacheFile;
    private volatile List<MetadataCandidate> cachedIndex;

    public AnidbHttpMetadataProvider(boolean enabled) {
        this(enabled, null, null, null, null);
    }

    public AnidbHttpMetadataProvider(boolean enabled, String endpoint, HttpClient client, Duration timeout) {
        this(enabled, endpoint, client, timeout, null);
    }

    public AnidbHttpMetadataProvider(boolean enabled, String endpoint, HttpClient client, Duration timeout, Path cacheFile) {
        this.enabled = enabled;
        this.indexUrl = endpoint == null || endpoint.isBlank() ? DEFAULT_INDEX_URL : endpoint;
        this.client = client == null ? HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build() : client;
        this.timeout = timeout == null ? Duration.ofSeconds(120) : timeout;
        this.cacheFile = cacheFile;
    }

    @Override public String id() { return "anidb"; }
    @Override public boolean available() { return enabled; }
    @Override public String status() { return "AniDB 索引客户端已就绪（无需密钥）"; }

    @Override
    public List<MetadataCandidate> search(String query, MetadataType type, Locale locale, int limit) throws Exception {
        if (type == MetadataType.MOVIE) return List.of();
        List<MetadataCandidate> index = loadIndex();
        String q = normalize(query);
        if (q.isEmpty()) return List.of();
        List<RankedCandidate> matches = new ArrayList<>();
        for (MetadataCandidate candidate : index) {
            if (normalize(candidate.title()).contains(q)) {
                matches.add(new RankedCandidate(candidate, 1.0f));
                continue;
            }
            for (String alt : candidate.alternativeTitles()) {
                if (normalize(alt).contains(q)) {
                    matches.add(new RankedCandidate(candidate, 0.9f));
                    break;
                }
            }
        }
        return matches.stream()
                .sorted(Comparator.comparingDouble(RankedCandidate::score).reversed())
                .limit(limit)
                .map(RankedCandidate::candidate)
                .toList();
    }

    private synchronized List<MetadataCandidate> loadIndex() throws Exception {
        if (cachedIndex != null) return cachedIndex;
        if (cacheFile != null && Files.isRegularFile(cacheFile)
                && System.currentTimeMillis() - Files.getLastModifiedTime(cacheFile).toMillis() < Duration.ofHours(24).toMillis()) {
            cachedIndex = parseTitles(Files.readString(cacheFile, StandardCharsets.UTF_8));
            return cachedIndex;
        }
        try {
            cachedIndex = downloadIndex();
            return cachedIndex;
        } catch (Exception failure) {
            if (cacheFile != null && Files.isRegularFile(cacheFile)) {
                cachedIndex = parseTitles(Files.readString(cacheFile, StandardCharsets.UTF_8));
                return cachedIndex;
            }
            throw failure;
        }
    }

    private List<MetadataCandidate> downloadIndex() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(indexUrl))
                .timeout(timeout)
                .GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("AniDB 索引下载失败（HTTP " + response.statusCode() + "）");
        }
        StringBuilder sb = new StringBuilder(1 << 20);
        try (InputStream in = response.body();
             GZIPInputStream gz = new GZIPInputStream(in);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gz, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        if (cacheFile != null) {
            Files.createDirectories(cacheFile.toAbsolutePath().getParent());
            Path temporary = cacheFile.resolveSibling(cacheFile.getFileName() + ".tmp");
            Files.writeString(temporary, sb, StandardCharsets.UTF_8);
            Files.move(temporary, cacheFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return parseTitles(sb.toString());
    }

    static List<MetadataCandidate> parseTitles(String tsv) {
        Map<String, List<String>> byId = new LinkedHashMap<>();
        for (String line : tsv.split("\n")) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] parts = line.split("\\|");
            if (parts.length < 4) continue;
            String type = parts[1];
            String lang = parts[2];
            if (!Set.of("1", "2", "4").contains(type)) continue;
            if (!Set.of("en", "ja", "x-jat").contains(lang)) continue;
            byId.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(parts[3]);
        }
        List<MetadataCandidate> index = new ArrayList<>();
        for (var entry : byId.entrySet()) {
            List<String> titles = entry.getValue().stream().distinct().limit(6).toList();
            if (titles.isEmpty()) continue;
            String main = titles.get(0);
            List<String> aliases = titles.size() > 1 ? titles.subList(1, titles.size()) : List.of();
            index.add(new MetadataCandidate("anidb", entry.getKey(), MetadataType.SERIES, main, aliases, null, null));
        }
        return index;
    }

    private String normalize(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", ""); }
}
