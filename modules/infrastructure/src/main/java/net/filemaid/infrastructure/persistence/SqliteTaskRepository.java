package net.filemaid.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.filemaid.application.port.TaskRepository;
import net.filemaid.core.model.Task;

/** Durable task state; unfinished work is explicitly failed after a server restart. */
public final class SqliteTaskRepository implements TaskRepository {
    private final String jdbcUrl;
    private final ObjectMapper json = new ObjectMapper();

    public SqliteTaskRepository(String dbPath) {
        String value = dbPath == null || dbPath.isBlank() ? "./config/filemaid.db" : dbPath;
        Path file = Path.of(value);
        try { if (file.getParent() != null) Files.createDirectories(file.getParent()); }
        catch (Exception failure) { throw new IllegalStateException("Failed to create task database directory", failure); }
        jdbcUrl = "jdbc:sqlite:" + value;
        try (var connection = DriverManager.getConnection(jdbcUrl); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS background_tasks (id TEXT PRIMARY KEY,type TEXT NOT NULL,status TEXT NOT NULL,progress INTEGER NOT NULL,message TEXT NOT NULL,result_json TEXT,error TEXT,created_at TEXT NOT NULL,updated_at TEXT NOT NULL)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_background_tasks_created_at ON background_tasks(created_at)");
            statement.executeUpdate("UPDATE background_tasks SET status='FAILED',error='服务重启，任务未能继续执行',updated_at='" + Instant.now() + "' WHERE status IN ('PENDING','RUNNING')");
        } catch (Exception failure) { throw new IllegalStateException("Failed to initialize task schema", failure); }
    }

    @Override public synchronized void save(Task task) {
        String sql = "INSERT INTO background_tasks(id,type,status,progress,message,result_json,error,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?) "
                + "ON CONFLICT(id) DO UPDATE SET type=excluded.type,status=excluded.status,progress=excluded.progress,message=excluded.message,result_json=excluded.result_json,error=excluded.error,updated_at=excluded.updated_at";
        try (var connection = DriverManager.getConnection(jdbcUrl); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, task.id()); statement.setString(2, task.type()); statement.setString(3, task.status().name());
            statement.setInt(4, task.progress()); statement.setString(5, task.message());
            statement.setString(6, task.result() == null ? null : json.writeValueAsString(task.result())); statement.setString(7, task.error());
            statement.setString(8, task.createdAt().toString()); statement.setString(9, task.updatedAt().toString()); statement.executeUpdate();
        } catch (Exception failure) { throw new IllegalStateException("Failed to save background task", failure); }
    }

    @Override public Optional<Task> findById(String id) {
        try (var connection = DriverManager.getConnection(jdbcUrl); var statement = connection.prepareStatement("SELECT * FROM background_tasks WHERE id=?")) {
            statement.setString(1, id);
            try (var row = statement.executeQuery()) { return row.next() ? Optional.of(read(row)) : Optional.empty(); }
        } catch (Exception failure) { throw new IllegalStateException("Failed to read background task", failure); }
    }

    @Override public List<Task> findAll() {
        try (var connection = DriverManager.getConnection(jdbcUrl); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT * FROM background_tasks ORDER BY created_at DESC")) {
            List<Task> tasks = new ArrayList<>(); while (rows.next()) tasks.add(read(rows)); return List.copyOf(tasks);
        } catch (Exception failure) { throw new IllegalStateException("Failed to list background tasks", failure); }
    }

    @Override public int removeOlderThan(Instant cutoff) {
        try (var connection = DriverManager.getConnection(jdbcUrl); var statement = connection.prepareStatement("DELETE FROM background_tasks WHERE created_at < ? AND status IN ('COMPLETED','FAILED','CANCELLED')")) {
            statement.setString(1, cutoff.toString()); return statement.executeUpdate();
        } catch (Exception failure) { throw new IllegalStateException("Failed to prune background tasks", failure); }
    }

    private Task read(java.sql.ResultSet row) throws Exception {
        String result = row.getString("result_json");
        return new Task(row.getString("id"), row.getString("type"), Task.Status.valueOf(row.getString("status")),
                row.getInt("progress"), row.getString("message"), result == null ? null : json.readValue(result, Object.class),
                row.getString("error"), Instant.parse(row.getString("created_at")), Instant.parse(row.getString("updated_at")));
    }
}
