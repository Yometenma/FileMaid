package net.filemaid.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.filemaid.application.service.SettingsService;
import org.springframework.stereotype.Service;

/** Copies the SQLite database into a sibling backups/ directory and prunes old copies. */
@Service
public final class BackupService {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
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
        try {
            Files.copy(db, backupDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) { throw new IllegalStateException("备份失败", failure); }
        prune(backupDir);
        return name;
    }

    public List<BackupEntry> listBackups() {
        Path backupDir = backupsDirectory();
        if (!Files.isDirectory(backupDir)) return List.of();
        try (Stream<Path> files = Files.list(backupDir)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".db"))
                    .map(this::toEntry)
                    .sorted(Comparator.comparing(BackupEntry::time).reversed())
                    .toList();
        } catch (IOException failure) { throw new IllegalStateException("无法读取备份目录", failure); }
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
                    .filter(path -> path.getFileName().toString().endsWith(".db"))
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

    private Path backupsDirectory() {
        Path parent = databaseFile().getParent();
        return (parent == null ? Path.of("backups") : parent.resolve("backups")).toAbsolutePath().normalize();
    }

    public record BackupEntry(String file, long size, String time) { }
}
