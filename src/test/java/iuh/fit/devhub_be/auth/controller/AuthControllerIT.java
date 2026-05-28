package iuh.fit.devhub_be.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.devhub_be.auth.dto.request.LoginRequest;
import iuh.fit.devhub_be.auth.dto.request.RegisterRequest;
import iuh.fit.devhub_be.auth.repository.RefreshTokenRepository;
import iuh.fit.devhub_be.auth.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("devhub_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("JWT_SECRET", () -> "test-integration-secret-key-for-testing-purposes-only");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    private static final String AUTH = "/api/v1/auth";

    @BeforeEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ─── POST /auth/register ──────────────────────────────────────────────────

    @Test
    void testRegisterEndpoint_Post_Success() throws Exception {
        mockMvc.perform(post(AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("duc", "duc@test.com", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    void testRegisterEndpoint_Post_DbState_UserPersistedWithHashedPassword() throws Exception {
        mockMvc.perform(post(AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("duc", "duc@test.com", "password123"))))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("duc@test.com").orElseThrow();
        assertEquals("duc", user.getUserName());
        assertTrue(user.getPassword().startsWith("$2a$"), "Password must be BCrypt hashed");
        assertEquals("USER", user.getRole().getName());
    }

    @Test
    void testRegisterEndpoint_Post_DuplicateEmail_Returns400() throws Exception {
        registerUser("duc", "duc@test.com", "password123");

        mockMvc.perform(post(AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("other", "duc@test.com", "password456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    void testRegisterEndpoint_Post_BlankUserName_Returns400() throws Exception {
        mockMvc.perform(post(AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName":"","email":"duc@test.com","password":"password123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testRegisterEndpoint_Post_InvalidEmailFormat_Returns400() throws Exception {
        mockMvc.perform(post(AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("duc", "not-an-email", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testRegisterEndpoint_Post_PasswordTooShort_Returns400() throws Exception {
        mockMvc.perform(post(AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("duc", "duc@test.com", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── POST /auth/login ─────────────────────────────────────────────────────

    @Test
    void testLoginEndpoint_Post_Success() throws Exception {
        registerUser("duc", "duc@test.com", "password123");

        mockMvc.perform(post(AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("duc@test.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("duc@test.com"))
                .andExpect(jsonPath("$.data.user.userName").value("duc"))
                .andExpect(jsonPath("$.data.user.roleName").value("USER"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    void testLoginEndpoint_Post_DbState_RefreshTokenRowCreated() throws Exception {
        registerUser("duc", "duc@test.com", "password123");
        assertEquals(0, refreshTokenRepository.count());

        mockMvc.perform(post(AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("duc@test.com", "password123"))))
                .andExpect(status().isOk());

        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    void testLoginEndpoint_Post_UnknownEmail_Returns401() throws Exception {
        mockMvc.perform(post(AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("ghost@test.com", "password123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void testLoginEndpoint_Post_WrongPassword_Returns401() throws Exception {
        registerUser("duc", "duc@test.com", "password123");

        mockMvc.perform(post(AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest("duc@test.com", "wrongPassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void testLoginEndpoint_Post_BlankEmail_Returns400() throws Exception {
        mockMvc.perform(post(AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":"password123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── POST /auth/refresh ───────────────────────────────────────────────────

    @Test
    void testRefreshEndpoint_Post_Success() throws Exception {
        String rawToken = loginAndGetCookie("duc", "duc@test.com", "password123");

        mockMvc.perform(post(AUTH + "/refresh")
                        .cookie(new Cookie("refreshToken", rawToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    void testRefreshEndpoint_Post_TokenIsRotated() throws Exception {
        String oldToken = loginAndGetCookie("duc", "duc@test.com", "password123");
        assertEquals(1, refreshTokenRepository.count());

        MvcResult result = mockMvc.perform(post(AUTH + "/refresh")
                        .cookie(new Cookie("refreshToken", oldToken)))
                .andExpect(status().isOk())
                .andReturn();

        String newToken = extractCookieValue(result);
        assertNotEquals(oldToken, newToken, "Refresh token must rotate on each use");
        assertEquals(1, refreshTokenRepository.count(), "Exactly one RT row must remain after rotation");
    }

    @Test
    void testRefreshEndpoint_Post_OldTokenRejectedAfterRotation() throws Exception {
        String oldToken = loginAndGetCookie("duc", "duc@test.com", "password123");

        mockMvc.perform(post(AUTH + "/refresh")
                        .cookie(new Cookie("refreshToken", oldToken)))
                .andExpect(status().isOk());

        // Old token is gone — using it again must fail
        mockMvc.perform(post(AUTH + "/refresh")
                        .cookie(new Cookie("refreshToken", oldToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRefreshEndpoint_Post_MissingCookie_Returns401() throws Exception {
        mockMvc.perform(post(AUTH + "/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Refresh token missing"));
    }

    @Test
    void testRefreshEndpoint_Post_InvalidToken_Returns401() throws Exception {
        mockMvc.perform(post(AUTH + "/refresh")
                        .cookie(new Cookie("refreshToken", "completely-invalid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── POST /auth/logout ────────────────────────────────────────────────────

    @Test
    void testLogoutEndpoint_Post_Success() throws Exception {
        String rawToken = loginAndGetCookie("duc", "duc@test.com", "password123");
        assertEquals(1, refreshTokenRepository.count());

        mockMvc.perform(post(AUTH + "/logout")
                        .cookie(new Cookie("refreshToken", rawToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        assertEquals(0, refreshTokenRepository.count(), "RT row must be deleted on logout");
    }

    @Test
    void testLogoutEndpoint_Post_CookieClearedInResponse() throws Exception {
        String rawToken = loginAndGetCookie("duc", "duc@test.com", "password123");

        MvcResult result = mockMvc.perform(post(AUTH + "/logout")
                        .cookie(new Cookie("refreshToken", rawToken)))
                .andExpect(status().isOk())
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader, "Set-Cookie header must be present on logout");
        assertTrue(setCookieHeader.contains("Max-Age=0"), "Cookie must be cleared with Max-Age=0");
    }

    @Test
    void testLogoutEndpoint_Post_NoCookie_IsIdempotent() throws Exception {
        mockMvc.perform(post(AUTH + "/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void testLogoutEndpoint_Post_TokenCannotBeUsedAfterLogout() throws Exception {
        String rawToken = loginAndGetCookie("duc", "duc@test.com", "password123");

        mockMvc.perform(post(AUTH + "/logout")
                        .cookie(new Cookie("refreshToken", rawToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post(AUTH + "/refresh")
                        .cookie(new Cookie("refreshToken", rawToken)))
                .andExpect(status().isUnauthorized());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void registerUser(String userName, String email, String password) throws Exception {
        mockMvc.perform(post(AUTH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(userName, email, password))))
                .andExpect(status().isCreated());
    }

    private String loginAndGetCookie(String userName, String email, String password) throws Exception {
        registerUser(userName, email, password);
        MvcResult result = mockMvc.perform(post(AUTH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, password))))
                .andReturn();
        String cookie = extractCookieValue(result);
        assertNotNull(cookie, "Login must set a refreshToken cookie");
        return cookie;
    }

    private String extractCookieValue(MvcResult result) {
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        if (header == null) return null;
        return Arrays.stream(header.split(";"))
                .map(String::trim)
                .filter(s -> s.startsWith("refreshToken="))
                .map(s -> s.substring("refreshToken=".length()))
                .findFirst()
                .orElse(null);
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
