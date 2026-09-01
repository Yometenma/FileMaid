package net.filemaid.infrastructure.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.filemaid.application.port.MatchDecisionRepository;
import net.filemaid.core.model.MetadataSelection;
import net.filemaid.core.model.MetadataType;

/** SQLite-backed match decision store. Creates the table on first use. */
public final class SqliteMatchDecisionRepository implements MatchDecisionRepository {
    private final String jdbcUrl;

    public SqliteMatchDecisionRepository(String dbPath) {
        String path = dbPath == null || dbPath.isBlank() ? "./config/filemaid.db" : dbPath;
        Path file = Path.of(path);
        if (file.getParent() != null) {
            try { Files.createDirectories(file.getParent()); } catch (Exception ignored) { /* best effort */ }
        }
        this.jdbcUrl = "jdbc:sqlite:" + path;
        initSchema();
    }

    private void initSchema() {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS match_decisions (
                        source TEXT PRIMARY KEY,
                        provider TEXT NOT NULL,
                        metadata_id TEXT NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        year INTEGER
                    )
                    """);
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to initialize SQLite schema", failure);
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(jdbcUrl);
    }

    @Override
    public void save(MetadataSelection selection) {
        String sql = """
                INSERT INTO match_decisions (source, provider, metadata_id, type, title, year)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(source) DO UPDATE SET
                    provider = excluded.provider,
                    metadata_id = excluded.metadata_id,
                    type = excluded.type,
                    title = excluded.title,
                    year = excluded.year
                """;
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, selection.source());
            statement.setString(2, selection.provider());
            statement.setString(3, selection.id());
            statement.setString(4, selection.type().name());
            statement.setString(5, selection.title());
            if (selection.year() == null) statement.setNull(6, java.sql.Types.INTEGER);
            else statement.setInt(6, selection.year());
            statement.executeUpdate();
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to save match decision", failure);
        }
    }

    @Override
    public Optional<MetadataSelection> findBySource(String source) {
        String sql = "SELECT source, provider, metadata_id, type, title, year FROM match_decisions WHERE source = ?";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, source);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to find match decision", failure);
        }
    }

    @Override
    public List<MetadataSelection> findAll() {
        String sql = "SELECT source, provider, metadata_id, type, title, year FROM match_decisions ORDER BY source";
        List<MetadataSelection> result = new ArrayList<>();
        try (Connection connection = connection(); Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) result.add(map(resultSet));
            return result;
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to list match decisions", failure);
        }
    }

    private MetadataSelection map(ResultSet resultSet) throws Exception {
        int year = resultSet.getInt("year");
        Integer yearValue = resultSet.wasNull() ? null : year;
        return new MetadataSelection(
                resultSet.getString("source"),
                resultSet.getString("provider"),
                resultSet.getString("metadata_id"),
                MetadataType.valueOf(resultSet.getString("type")),
                resultSet.getString("title"),
                yearValue);
    }
}
