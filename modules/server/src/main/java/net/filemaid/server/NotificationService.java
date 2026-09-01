package net.filemaid.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import net.filemaid.application.service.SettingsService;
import net.filemaid.core.model.Task;
import org.springframework.stereotype.Service;

/** Sends outbound notifications (webhook) for operational checks. */
@Service
public final class NotificationService {
    private static final String WEBHOOK_KEY = "notification.webhookUrl";
    private static final String PAYLOAD = "{\"text\":\"FileMaid 测试通知\"}";
    private final SettingsService settings;

    public NotificationService(SettingsService settings) { this.settings = settings; }

    public NotificationResult sendTest() {
        String url = settings.value(WEBHOOK_KEY, "").trim();
        if (url.isBlank()) throw new IllegalArgumentException("未配置通知 Webhook 地址");
        URI uri = validateUrl(url);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(PAYLOAD))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new NotificationResult(true, response.statusCode(), null);
        } catch (Exception failure) {
            return new NotificationResult(false, 0, failure.getMessage());
        }
    }

    public NotificationResult sendTaskCompletion(Task task) {
        String url = settings.value(WEBHOOK_KEY, "").trim();
        if (url.isBlank()) return new NotificationResult(false, 0, "未配置");
        try {
            String payload = new ObjectMapper().writeValueAsString(java.util.Map.of(
                    "event", "filemaid.task.completed", "taskId", task.id(), "type", task.type(),
                    "status", task.status().name(), "message", task.error() == null ? task.message() : task.error(),
                    "completedAt", task.updatedAt().toString()));
            HttpRequest request = HttpRequest.newBuilder(validateUrl(url)).timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            return new NotificationResult(response.statusCode() >= 200 && response.statusCode() < 300, response.statusCode(), null);
        } catch (Exception failure) { return new NotificationResult(false, 0, failure.getMessage()); }
    }

    private URI validateUrl(String url) {
        URI uri;
        try { uri = URI.create(url); } catch (IllegalArgumentException failure) { throw new IllegalArgumentException("Webhook 地址无效", failure); }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null) {
            throw new IllegalArgumentException("Webhook 地址必须是 HTTP(S) 地址");
        }
        return uri;
    }

    public record NotificationResult(boolean success, int status, String error) { }
}
