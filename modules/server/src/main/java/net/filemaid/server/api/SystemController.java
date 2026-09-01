package net.filemaid.server.api;

import java.util.Map;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import net.filemaid.server.FileMaidProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    private final FileMaidProperties properties;
    public SystemController(FileMaidProperties properties) { this.properties = properties; }
    @GetMapping("/health")
    Map<String, String> health() { return Map.of("status", "UP", "service", "filemaid"); }

    @GetMapping("/diagnostics")
    Diagnostics diagnostics() {
        List<Check> roots = properties.roots().stream().map(root -> {
            Path path = root.path().toAbsolutePath().normalize();
            boolean exists = Files.isDirectory(path);
            boolean readable = exists && Files.isReadable(path);
            boolean writable = exists && Files.isWritable(path) && root.writable();
            String detail = !exists ? "目录不存在" : !readable ? "目录不可读" : root.writable() && !Files.isWritable(path) ? "配置为可写但没有写权限" : root.writable() ? "可读写" : "只读";
            return new Check("root:" + root.id(), exists && readable, detail, writable ? "writable" : "readonly");
        }).toList();
        Path db = Path.of(properties.dbPath()).toAbsolutePath().normalize();
        Path dbParent = db.getParent();
        boolean databaseReady = Files.exists(db) ? Files.isWritable(db) : dbParent != null && Files.isWritable(dbParent);
        boolean ffprobe = ffprobeAvailable();
        return new Diagnostics("0.1.0", System.getProperty("java.version"), databaseReady, ffprobe, roots);
    }

    private boolean ffprobeAvailable() {
        try {
            Process process = new ProcessBuilder(properties.probe().ffprobePath(), "-version").redirectErrorStream(true).start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) process.destroyForcibly();
            return finished && process.exitValue() == 0;
        } catch (Exception ignored) { return false; }
    }

    record Diagnostics(String version, String javaVersion, boolean databaseWritable, boolean ffprobeAvailable, List<Check> roots) { }
    record Check(String id, boolean healthy, String detail, String mode) { }
}
