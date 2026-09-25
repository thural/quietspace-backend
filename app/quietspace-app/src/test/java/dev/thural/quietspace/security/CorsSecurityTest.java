package dev.thural.quietspace.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("dev")
class CorsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String ALLOWED_ORIGIN = "https://frontend-app.com";
    private static final String BLOCKED_ORIGIN = "https://evil-site.com";

    @Test
    void preflightRequest_withAllowedOrigin_shouldIncludeCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/posts")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void preflightRequest_withUnknownOrigin_shouldBeRejected() throws Exception {
        mockMvc.perform(options("/api/v1/auth/authenticate")
                        .header("Origin", BLOCKED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void actualRequest_withAllowedOrigin_shouldIncludeAllowOriginHeader() throws Exception {
        mockMvc.perform(get("/api/v1/auth/hello")
                        .header("Origin", ALLOWED_ORIGIN))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void actualRequest_withBlockedOrigin_shouldNotIncludeCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/auth/hello")
                        .header("Origin", BLOCKED_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void preflightRequest_withoutOrigin_shouldNotIncludeCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/posts")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isUnauthorized());
    }
}
