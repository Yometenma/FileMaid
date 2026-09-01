package net.filemaid.server;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties={"filemaid.auth.enabled=true","filemaid.db-path=build/auth-security-test.db","filemaid.roots[0].id=media","filemaid.roots[0].path=build/auth-media"})
@AutoConfigureMockMvc
class AuthSecurityTest {
    @Autowired MockMvc mvc;
    @AfterAll static void cleanup()throws Exception{Files.deleteIfExists(Path.of("build/auth-security-test.db"));}
    @Test void requiresFirstRunSetupAndProtectsApiWithSessionAndCsrf()throws Exception{
        Files.createDirectories(Path.of("build/auth-media"));
        mvc.perform(get("/api/v1/auth/status")).andExpect(status().isOk()).andExpect(jsonPath("$.configured").value(false));
        mvc.perform(get("/api/v1/roots")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/setup").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"a-secure-password\"}"))
                .andExpect(status().isOk());
        var login=mvc.perform(post("/api/v1/auth/login").with(csrf()).param("username","admin").param("password","a-secure-password"))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession session=(MockHttpSession)login.getRequest().getSession(false);
        mvc.perform(get("/api/v1/roots").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/logout").session(session).with(csrf())).andExpect(status().isOk());
    }

    @Test void reportsFriendlyPasswordLengthError()throws Exception{
        mvc.perform(post("/api/v1/auth/setup").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("密码长度必须在 12-128 个字符之间"));
    }
}
