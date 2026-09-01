package net.filemaid.infrastructure.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;
import net.filemaid.application.port.SettingsRepository;

public final class SqliteSettingsRepository implements SettingsRepository {
    private final String jdbcUrl;

    public SqliteSettingsRepository(String dbPath) {
        String value = dbPath == null || dbPath.isBlank() ? "./config/filemaid.db" : dbPath;
        Path file = Path.of(value);
        try { if (file.getParent() != null) Files.createDirectories(file.getParent()); }
        catch (Exception failure) { throw new IllegalStateException("Failed to create settings directory", failure); }
        jdbcUrl = "jdbc:sqlite:" + value;
        try (var connection = DriverManager.getConnection(jdbcUrl); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT NOT NULL, updated_at TEXT NOT NULL)");
        } catch (Exception failure) { throw new IllegalStateException("Failed to initialize settings schema", failure); }
    }

    @Override public Map<String, String> findAll() {
        Map<String, String> values = new LinkedHashMap<>();
        try (var connection = DriverManager.getConnection(jdbcUrl); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT key, value FROM settings ORDER BY key")) {
            while (rows.next()) values.put(rows.getString(1), rows.getString(2));
            return values;
        } catch (Exception failure) { throw new IllegalStateException("Failed to load settings", failure); }
    }

    @Override public void saveAll(Map<String, String> values) {
        String sql = "INSERT INTO settings(key,value,updated_at) VALUES(?,?,CURRENT_TIMESTAMP) "
                + "ON CONFLICT(key) DO UPDATE SET value=excluded.value, updated_at=CURRENT_TIMESTAMP";
        try (var connection = DriverManager.getConnection(jdbcUrl); var statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (var entry : values.entrySet()) {
                statement.setString(1, entry.getKey()); statement.setString(2, entry.getValue()); statement.addBatch();
            }
            statement.executeBatch(); connection.commit();
        } catch (Exception failure) { throw new IllegalStateException("Failed to save settings", failure); }
    }
}
