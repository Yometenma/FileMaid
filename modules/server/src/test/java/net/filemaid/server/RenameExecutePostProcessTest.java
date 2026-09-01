package net.filemaid.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "filemaid.roots[0].id=media",
        "filemaid.roots[0].path=build/test-media-write",
        "filemaid.roots[0].writable=true",
        "filemaid.metadata.tvmaze-enabled=false",
        "filemaid.metadata.anidb-enabled=false",
        "filemaid.db-path=build/test-write.db",
        "filemaid.auth.enabled=false"
})
@AutoConfigureMockMvc
class RenameExecutePostProcessTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    private final Path mediaDirectory = Path.of("build/test-media-write").toAbsolutePath();

    @BeforeEach
    void createTestMedia() throws Exception {
        Files.createDirectories(mediaDirectory.resolve("incoming"));
        Files.writeString(mediaDirectory.resolve("incoming/Example.Show.S01E01.mkv"), "video");
    }

    @AfterEach
    void removeTestMedia() throws Exception {
        deleteRecursively(mediaDirectory);
    }

    @Test
    void executesRenameAndFrozenNfoInOneConfirmedPlan() throws Exception {
        String validateBody = """
                {"rootId":"media",
                 "operations":[{"source":"incoming/Example.Show.S01E01.mkv","target":"TV Shows/Example Show/Season 01/Example Show - S01E01.mkv","type":"MOVE"}],
                 "postProcess":{"generateNfo":true,"downloadArtwork":false,"artworkType":"POSTER",
                   "items":[{"source":"incoming/Example.Show.S01E01.mkv",
                     "metadata":{"source":"incoming/Example.Show.S01E01.mkv","provider":"tmdb","id":"99","type":"SERIES","title":"Example Show","year":2024},
                     "artworkUrl":null}]}}
                """;
        MvcResult validateResult = mvc.perform(post("/api/v1/rename-plans/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validateBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode validateJson = objectMapper.readTree(validateResult.getResponse().getContentAsString());
        assertThat(validateJson.get("valid").asBoolean()).isTrue();
        String token = validateJson.get("confirmationToken").asText();

        MvcResult executeResult = mvc.perform(post("/api/v1/rename-plans/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmationToken\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode results = objectMapper.readTree(executeResult.getResponse().getContentAsString());
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0).get("type").asText()).isEqualTo("MOVE");
        assertThat(results.get(0).get("success").asBoolean()).isTrue();
        assertThat(results.get(1).get("type").asText()).isEqualTo("NFO");
        assertThat(results.get(1).get("success").asBoolean()).isTrue();

        assertThat(Files.exists(mediaDirectory.resolve("TV Shows/Example Show/Season 01/Example Show - S01E01.mkv"))).isTrue();
        assertThat(Files.exists(mediaDirectory.resolve("TV Shows/Example Show/Season 01/Example Show - S01E01.nfo"))).isTrue();
        assertThat(Files.exists(mediaDirectory.resolve("incoming/Example.Show.S01E01.mkv"))).isFalse();
    }

    private void deleteRecursively(Path path) throws Exception {
        if (!Files.exists(path)) return;
        try (var stream = Files.walk(path)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Exception ignored) { }
            });
        }
    }
}
