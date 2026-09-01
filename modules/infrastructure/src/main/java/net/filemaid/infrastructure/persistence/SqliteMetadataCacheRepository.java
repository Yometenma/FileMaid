package net.filemaid.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import net.filemaid.application.port.MetadataCacheRepository;
import net.filemaid.core.model.MetadataCandidate;

public final class SqliteMetadataCacheRepository implements MetadataCacheRepository {
    private static final TypeReference<List<MetadataCandidate>> CANDIDATES = new TypeReference<>() {};
    private final String jdbcUrl;
    private final ObjectMapper json = new ObjectMapper();

    public SqliteMetadataCacheRepository(String dbPath) {
        String value = dbPath == null || dbPath.isBlank() ? "./config/filemaid.db" : dbPath;
        Path file = Path.of(value);
        try { if (file.getParent() != null) Files.createDirectories(file.getParent()); }
        catch (Exception failure) { throw new IllegalStateException("Failed to create metadata cache directory", failure); }
        jdbcUrl = "jdbc:sqlite:" + value;
        try (var connection = DriverManager.getConnection(jdbcUrl); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS metadata_cache (cache_key TEXT PRIMARY KEY, provider TEXT NOT NULL, payload TEXT NOT NULL, created_at TEXT NOT NULL, ttl_seconds INTEGER NOT NULL)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_metadata_cache_created_at ON metadata_cache(created_at)");
            statement.execute("CREATE TABLE IF NOT EXISTS metadata_usage (provider TEXT NOT NULL, credential_hash TEXT NOT NULL, usage_date TEXT NOT NULL, request_count INTEGER NOT NULL, PRIMARY KEY(provider,credential_hash,usage_date))");
        } catch (Exception failure) { throw new IllegalStateException("Failed to initialize metadata cache schema", failure); }
    }

    @Override public Optional<Entry> find(String key) {
        try (var connection = DriverManager.getConnection(jdbcUrl); var statement = connection.prepareStatement("SELECT payload,created_at,ttl_seconds FROM metadata_cache WHERE cache_key=?")) {
            statement.setString(1, key);
            try (var row = statement.executeQuery()) {
                if (!row.next()) return Optional.empty();
                return Optional.of(new Entry(json.readValue(row.getString(1), CANDIDATES), Instant.parse(row.getString(2)), row.getLong(3)));
            }
        } catch (Exception failure) { throw new IllegalStateException("Failed to read metadata cache", failure); }
    }

    @Override public void save(String key, String provider, List<MetadataCandidate> candidates, Instant createdAt, long ttlSeconds) {
        String sql = "INSERT INTO metadata_cache(cache_key,provider,payload,created_at,ttl_seconds) VALUES(?,?,?,?,?) ON CONFLICT(cache_key) DO UPDATE SET provider=excluded.provider,payload=excluded.payload,created_at=excluded.created_at,ttl_seconds=excluded.ttl_seconds";
        try (var connection = DriverManager.getConnection(jdbcUrl); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, key); statement.setString(2, provider); statement.setString(3, json.writeValueAsString(candidates));
            statement.setString(4, createdAt.toString()); statement.setLong(5, ttlSeconds); statement.executeUpdate();
        } catch (Exception failure) { throw new IllegalStateException("Failed to save metadata cache", failure); }
    }

    @Override public int deleteOlderThan(Instant cutoff) {
        try (var connection = DriverManager.getConnection(jdbcUrl); var statement = connection.prepareStatement("DELETE FROM metadata_cache WHERE created_at < ?")) {
            statement.setString(1, cutoff.toString()); return statement.executeUpdate();
        } catch (Exception failure) { throw new IllegalStateException("Failed to prune metadata cache", failure); }
    }

    @Override public boolean tryConsumeDaily(String provider, String credentialHash, LocalDate date, int limit) {
        if (limit <= 0) return true;
        String sql = "INSERT INTO metadata_usage(provider,credential_hash,usage_date,request_count) VALUES(?,?,?,1) "
                + "ON CONFLICT(provider,credential_hash,usage_date) DO UPDATE SET request_count=request_count+1 WHERE request_count < ?";
        try (var connection = DriverManager.getConnection(jdbcUrl); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, provider); statement.setString(2, credentialHash);
            statement.setString(3, date.toString()); statement.setInt(4, limit);
            return statement.executeUpdate() == 1;
        } catch (Exception failure) { throw new IllegalStateException("Failed to update metadata request budget", failure); }
    }
}
