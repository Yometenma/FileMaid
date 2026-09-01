package net.filemaid.server;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties={"filemaid.auth.enabled=false","filemaid.db-path=build/auth-disabled-test.db","filemaid.roots[0].id=media","filemaid.roots[0].path=build/auth-disabled-media"})
@AutoConfigureMockMvc
class AuthDisabledTest {
    @Autowired MockMvc mvc;

    @AfterAll static void cleanup()throws Exception{
        Files.deleteIfExists(Path.of("build/auth-disabled-test.db"));
    }

    @Test void reportsDisabledStatusWithoutCsrfToken()throws Exception{
        mvc.perform(get("/api/v1/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.authenticated").value(false));
    }
}
