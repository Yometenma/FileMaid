package net.filemaid.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.filemaid.application.service.SettingsService;
import org.springframework.stereotype.Service;

/** Creates a transactionally consistent SQLite snapshot in a sibling backups/ directory. */
@Service
public final class BackupService {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final Pattern BACKUP_NAME = Pattern.compile("filemaid-\\d{8}-\\d{6}(?:-\\d{3})?\\.db");
    private final FileMaidProperties properties;
    private final SettingsService settings;

    public BackupService(FileMaidProperties properties, SettingsService settings) {
        this.properties = properties;
        this.settings = settings;
    }

    public String createBackup() {
        Path db = databaseFile();
        if (!Files.isRegularFile(db)) throw new IllegalStateException("数据库文件不存在");
        Path backupDir = backupsDirectory();
        try { Files.createDirectories(backupDir); } catch (IOException failure) { throw new IllegalStateException("无法创建备份目录", failure); }
        String name = "filemaid-" + LocalDateTime.now().format(TIMESTAMP) + ".db";
        Path target = backupDir.resolve(name);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + db);
             var statement = connection.prepareStatement("VACUUM INTO ?")) {
            statement.setString(1, target.toString());
            statement.execute();
            verifySnapshot(target);
        } catch (Exception failure) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            throw new IllegalStateException("备份失败", failure);
        }
        prune(backupDir);
        return name;
    }

    public List<BackupEntry> listBackups() {
        Path backupDir = backupsDirectory();
        if (!Files.isDirectory(backupDir)) return List.of();
        try (Stream<Path> files = Files.list(backupDir)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> BACKUP_NAME.matcher(path.getFileName().toString()).matches())
                    .map(this::toEntry)
                    .sorted(Comparator.comparing(BackupEntry::time).reversed())
                    .toList();
        } catch (IOException failure) { throw new IllegalStateException("无法读取备份目录", failure); }
    }

    public Path backupFile(String name) {
        if (name == null || !BACKUP_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("备份文件名无效");
        }
        Path directory = backupsDirectory();
        Path file = directory.resolve(name).normalize();
        if (!file.getParent().equals(directory) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("备份文件不存在");
        }
        return file;
    }

    private BackupEntry toEntry(Path path) {
        try {
            long size = Files.size(path);
            FileTime modified = Files.getLastModifiedTime(path);
            return new BackupEntry(path.getFileName().toString(), size, modified.toInstant().toString());
        } catch (IOException failure) { throw new IllegalStateException("无法读取备份信息", failure); }
    }

    private void prune(Path backupDir) {
        int retention = retentionCount();
        List<Path> backups;
        try (Stream<Path> files = Files.list(backupDir)) {
            backups = files.filter(Files::isRegularFile)
                    .filter(path -> BACKUP_NAME.matcher(path.getFileName().toString()).matches())
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException failure) { throw new IllegalStateException("无法清理旧备份", failure); }
        int excess = backups.size() - retention;
        for (int i = 0; i < excess; i++) {
            try { Files.deleteIfExists(backups.get(i)); } catch (IOException ignored) { }
        }
    }

    private int retentionCount() {
        String raw = settings.value("system.databaseBackupRetention", "7").trim();
        try { return Math.max(1, Integer.parseInt(raw)); } catch (NumberFormatException ignored) { return 7; }
    }

    private Path databaseFile() { return Path.of(properties.dbPath()).toAbsolutePath().normalize(); }

    private void verifySnapshot(Path file) throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
             var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA quick_check")) {
            if (!result.next() || !"ok".equalsIgnoreCase(result.getString(1))) {
                throw new IllegalStateException("数据库备份完整性校验失败");
            }
        }
    }

    private Path backupsDirectory() {
        Path parent = databaseFile().getParent();
        return (parent == null ? Path.of("backups") : parent.resolve("backups")).toAbsolutePath().normalize();
    }

    public record BackupEntry(String file, long size, String time) { }
}
