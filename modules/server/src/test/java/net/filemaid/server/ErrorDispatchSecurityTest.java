package net.filemaid.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * 用真实 Tomcat 容器验证 ERROR dispatch：未登录请求在 permitAll 路径上产生的容器级错误
 * （405/400）经 /error 转发后，不应被安全链的 authenticationEntryPoint 掩盖成 401「需要登录」。
 * MockMvc 不走真实 ERROR dispatch，无法覆盖此场景。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "filemaid.auth.enabled=true",
        "filemaid.db-path=build/error-dispatch-test.db",
        "filemaid.roots[0].id=media",
        "filemaid.roots[0].path=build/error-dispatch-media"
})
class ErrorDispatchSecurityTest {
    @Autowired TestRestTemplate rest;

    @AfterAll
    static void cleanup() throws Exception {
        Files.deleteIfExists(Path.of("build/error-dispatch-test.db"));
    }

    /** GET /status 触发 SpaCsrfTokenRequestHandler 生成并写回 XSRF-TOKEN cookie。 */
    private String xsrfToken() {
        ResponseEntity<String> status = rest.getForEntity("/api/v1/auth/status", String.class);
        String setCookie = status.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("Set-Cookie should carry XSRF-TOKEN").startsWith("XSRF-TOKEN=");
        return setCookie.substring("XSRF-TOKEN=".length()).split(";")[0].trim();
    }

    private HttpHeaders xsrfHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "XSRF-TOKEN=" + token);
        headers.add("X-XSRF-TOKEN", token);
        return headers;
    }

    @Test
    void methodNotAllowedSurvivesErrorDispatchWhenUnauthenticated() throws Exception {
        Files.createDirectories(Path.of("build/error-dispatch-media"));
        // POST 到只允许 GET 的 permitAll 端点 → 405 → sendError → ERROR dispatch 到 /error。
        // 若 /error 不在 permitAll，未登录的 ERROR dispatch 会被 entry point 改写成 401。
        HttpHeaders headers = xsrfHeaders(xsrfToken());
        ResponseEntity<String> response = rest.postForEntity("/api/v1/system/health",
                new HttpEntity<>("", headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void validationErrorReturnsFriendly400WhenUnauthenticated() throws Exception {
        Files.createDirectories(Path.of("build/error-dispatch-media"));
        // 用户原始症状：短密码 setup 曾返回 401「需要登录」；现在应返回 400 + 友好提示。
        HttpHeaders headers = xsrfHeaders(xsrfToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = rest.postForEntity("/api/v1/auth/setup",
                new HttpEntity<>("{\"username\":\"admin\",\"password\":\"short\"}", headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("密码长度");
    }
}
