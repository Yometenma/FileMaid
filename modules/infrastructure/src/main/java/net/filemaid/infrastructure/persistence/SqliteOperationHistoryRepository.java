package net.filemaid.infrastructure.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.filemaid.application.port.OperationHistoryRepository;
import net.filemaid.core.model.OperationRecord;
import net.filemaid.core.model.OperationResult;
import net.filemaid.core.model.RenameOperation;

/** SQLite-backed operation history. Shares the same database file as other repositories. */
public final class SqliteOperationHistoryRepository implements OperationHistoryRepository {
    private final String jdbcUrl;

    public SqliteOperationHistoryRepository(String dbPath) {
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
                    CREATE TABLE IF NOT EXISTS operation_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        batch_id TEXT,
                        source TEXT NOT NULL,
                        target TEXT NOT NULL,
                        type TEXT NOT NULL,
                        success INTEGER NOT NULL,
                        error TEXT,
                        timestamp TEXT NOT NULL
                    )
                    """);
            try {
                statement.execute("ALTER TABLE operation_history ADD COLUMN batch_id TEXT");
            } catch (java.sql.SQLException failure) {
                String message = failure.getMessage() == null ? "" : failure.getMessage().toLowerCase(java.util.Locale.ROOT);
                if (!message.contains("duplicate column")) throw failure;
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to initialize operation history schema", failure);
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(jdbcUrl);
    }

    @Override
    public List<OperationRecord> append(List<OperationResult> results) {
        return append(java.util.UUID.randomUUID().toString(), results);
    }

    @Override
    public List<OperationRecord> append(String batchId, List<OperationResult> results) {
        if (results == null || results.isEmpty()) return List.of();
        String sql = "INSERT INTO operation_history (batch_id, source, target, type, success, error, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Instant timestamp = Instant.now();
        List<OperationRecord> records = new ArrayList<>();
        try (Connection connection = connection()) {
            for (OperationResult result : results) {
                try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, batchId);
                    statement.setString(2, result.source());
                    statement.setString(3, result.target());
                    statement.setString(4, result.type().name());
                    statement.setInt(5, result.success() ? 1 : 0);
                    statement.setString(6, result.error());
                    statement.setString(7, timestamp.toString());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (keys.next()) {
                            records.add(new OperationRecord(keys.getLong(1), batchId, result.source(), result.target(),
                                    result.type(), result.success(), result.error(), timestamp));
                        }
                    }
                }
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to append operation history", failure);
        }
        return records;
    }

    @Override
    public List<OperationRecord> findAll() {
        String sql = "SELECT id, batch_id, source, target, type, success, error, timestamp FROM operation_history ORDER BY id DESC";
        List<OperationRecord> records = new ArrayList<>();
        try (Connection connection = connection(); Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) records.add(map(resultSet));
            return records;
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to list operation history", failure);
        }
    }

    @Override
    public Optional<OperationRecord> findById(long id) {
        String sql = "SELECT id, batch_id, source, target, type, success, error, timestamp FROM operation_history WHERE id = ?";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to find operation", failure);
        }
    }

    @Override
    public int deleteOlderThan(Instant cutoff) {
        if (cutoff == null) return 0;
        String sql = "DELETE FROM operation_history WHERE timestamp < ?";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cutoff.toString());
            return statement.executeUpdate();
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to clean up operation history", failure);
        }
    }

    private OperationRecord map(ResultSet resultSet) throws Exception {
        return new OperationRecord(
                resultSet.getLong("id"),
                resultSet.getString("batch_id"),
                resultSet.getString("source"),
                resultSet.getString("target"),
                RenameOperation.OperationType.valueOf(resultSet.getString("type")),
                resultSet.getInt("success") != 0,
                resultSet.getString("error"),
                Instant.parse(resultSet.getString("timestamp")));
    }
}
