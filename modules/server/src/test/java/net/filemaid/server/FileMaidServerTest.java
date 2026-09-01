package net.filemaid.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

@SpringBootTest(properties = {
        "filemaid.roots[0].id=media",
        "filemaid.roots[0].path=build/test-media",
        "filemaid.roots[0].writable=false",
        "filemaid.metadata.tvmaze-enabled=false",
        "filemaid.metadata.anidb-enabled=false",
        "filemaid.db-path=build/test.db"
})
@AutoConfigureMockMvc
class FileMaidServerTest {
    @Autowired ApplicationContext context;
    @Autowired MockMvc mvc;
    private final Path mediaDirectory = Path.of("build/test-media").toAbsolutePath();

    @BeforeEach
    void createTestMedia() throws Exception {
        Files.createDirectories(mediaDirectory);
        Files.writeString(mediaDirectory.resolve("Example.S01E01.mkv"), "video");
    }

    @AfterEach
    void removeTestMedia() throws Exception {
        Files.deleteIfExists(mediaDirectory.resolve("Example.S01E01.mkv"));
        Files.deleteIfExists(mediaDirectory);
    }

    @Test
    void startsApplicationContext() {
        assertThat(context).isNotNull();
    }

    @Test
    void reportsMetadataProviderAsUnavailableWithoutSecret() throws Exception {
        mvc.perform(get("/api/v1/metadata/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("tmdb"))
                .andExpect(jsonPath("$[0].available").value(false));

        mvc.perform(get("/api/v1/metadata/search").param("query", "Example"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void scanReturnsOnlyRootRelativePaths() throws Exception {
        mvc.perform(get("/api/v1/roots/media/scan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path").value("Example.S01E01.mkv"))
                .andExpect(jsonPath("$[0].kind").value("VIDEO"));
    }

    @Test
    void parsesAndPreviewsEpisodeName() throws Exception {
        mvc.perform(post("/api/v1/media/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"names\":[\"Example.Show.S01E02.1080p.mkv\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("EPISODE"))
                .andExpect(jsonPath("$[0].season").value(1))
                .andExpect(jsonPath("$[0].episodes[0]").value(2));

        mvc.perform(post("/api/v1/rename-plans/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paths\":[\"incoming/Example.Show.S01E02.1080p.mkv\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].target").value("TV Shows/Example Show/Season 01/Example Show - S01E02.mkv"));
    }

    @Test
    void movieYearIsNotMisclassifiedAsSeasonAndEpisode() throws Exception {
        mvc.perform(post("/api/v1/media/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"names\":[\"Example.Movie.2024.2160p.BluRay.mkv\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("MOVIE"))
                .andExpect(jsonPath("$[0].year").value(2024));
    }

    @Test
    void confirmedMetadataChangesPreviewTitleWithoutWritingFiles() throws Exception {
        mvc.perform(post("/api/v1/rename-plans/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paths":["incoming/Example.Show.S01E02.mkv"],"selections":[{
                                  "source":"incoming/Example.Show.S01E02.mkv","provider":"tmdb","id":"99",
                                  "type":"SERIES","title":"正式剧名","year":2024
                                }]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].target").value("TV Shows/正式剧名/Season 01/正式剧名 - S01E02.mkv"))
                .andExpect(jsonPath("$[0].metadata.id").value("99"));
    }

    @Test
    void analyzesBatchIntoMediaGroups() throws Exception {
        mvc.perform(post("/api/v1/media/groups/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paths\":[\"show/Example.Show.S01E01.mkv\",\"show/Example.Show.S01E01.zh.ass\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("SERIES"))
                .andExpect(jsonPath("$[0].members.length()").value(2))
                .andExpect(jsonPath("$[0].members[1].companionOf").value("show/Example.Show.S01E01.mkv"));
    }
}
