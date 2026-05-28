package iuh.fit.devhub_be.auth.service;

import iuh.fit.devhub_be.auth.dto.request.LoginRequest;
import iuh.fit.devhub_be.auth.dto.request.RegisterRequest;
import iuh.fit.devhub_be.auth.dto.response.AuthPair;
import iuh.fit.devhub_be.auth.model.RefreshToken;
import iuh.fit.devhub_be.auth.model.Role;
import iuh.fit.devhub_be.auth.model.User;
import iuh.fit.devhub_be.auth.repository.RefreshTokenRepository;
import iuh.fit.devhub_be.auth.repository.RoleRepository;
import iuh.fit.devhub_be.auth.repository.UserRepository;
import iuh.fit.devhub_be.auth.service.impl.AuthServiceImpl;
import iuh.fit.devhub_be.common.exception.BadRequestException;
import iuh.fit.devhub_be.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtEncoder jwtEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 300L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800L);
    }

    // ─── Fixtures ────────────────────────────────────────────────────────────

    private Role buildRole() {
        Role role = new Role();
        role.setName("USER");
        return role;
    }

    private User buildUser(Role role) {
        User user = new User();
        user.setEmail("duc@example.com");
        user.setUserName("duc");
        user.setPassword("$2a$10$hashedPassword");
        user.setRole(role);
        return user;
    }

    private void stubJwtEncoder() {
        Jwt jwt = Jwt.withTokenValue("mock.access.token")
                .header("alg", "HS256")
                .claim("sub", "test-subject")
                .build();
        when(jwtEncoder.encode(any())).thenReturn(jwt);
    }

    // ─── register() ──────────────────────────────────────────────────────────

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("duc", "duc@example.com", "password123");
        Role role = buildRole();
        User saved = buildUser(role);

        when(userRepository.existsByEmail("duc@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        authService.register(request);

        verify(userRepository).existsByEmail("duc@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterWithDuplicateEmail_ThrowsBadRequestException() {
        RegisterRequest request = new RegisterRequest("duc", "dup@example.com", "password123");
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.register(request));

        assertEquals("Email already in use", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(roleRepository, never()).findByName(anyString());
    }

    @Test
    void testRegisterWhenUserRoleNotFound_ThrowsIllegalStateException() {
        RegisterRequest request = new RegisterRequest("duc", "duc@example.com", "password123");
        when(userRepository.existsByEmail("duc@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> authService.register(request));

        verify(userRepository, never()).save(any());
    }

    // ─── login() ─────────────────────────────────────────────────────────────

    @Test
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("duc@example.com", "password123");
        Role role = buildRole();
        User user = buildUser(role);

        when(userRepository.findByEmail("duc@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", user.getPassword())).thenReturn(true);
        when(refreshTokenRepository.save(any())).thenReturn(new RefreshToken());
        stubJwtEncoder();

        AuthPair result = authService.login(request);

        assertNotNull(result);
        assertNotNull(result.rawRefreshToken());
        assertNotNull(result.authResponse());
        assertEquals("Bearer", result.authResponse().tokenType());
        assertEquals("mock.access.token", result.authResponse().accessToken());
        assertEquals("duc@example.com", result.authResponse().user().email());
        assertEquals("duc", result.authResponse().user().userName());
        assertEquals("USER", result.authResponse().user().roleName());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testLoginWithEmailNotFound_ThrowsUnauthorizedException() {
        LoginRequest request = new LoginRequest("ghost@example.com", "password123");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(request));

        assertEquals("Invalid credentials", ex.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void testLoginWithWrongPassword_ThrowsUnauthorizedException() {
        LoginRequest request = new LoginRequest("duc@example.com", "wrongPass");
        Role role = buildRole();
        User user = buildUser(role);

        when(userRepository.findByEmail("duc@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", user.getPassword())).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(request));

        assertEquals("Invalid credentials", ex.getMessage());
        verify(refreshTokenRepository, never()).save(any());
    }

    // ─── refresh() ───────────────────────────────────────────────────────────

    @Test
    void testRefreshSuccess() {
        Role role = buildRole();
        User user = buildUser(role);

        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any())).thenReturn(new RefreshToken());
        stubJwtEncoder();

        AuthPair result = authService.refresh("validRawToken");

        assertNotNull(result);
        assertNotNull(result.rawRefreshToken());
        assertEquals("Bearer", result.authResponse().tokenType());
        verify(refreshTokenRepository).delete(stored);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testRefreshTokenIsRotated_NewTokenDiffersFromOld() {
        Role role = buildRole();
        User user = buildUser(role);

        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any())).thenReturn(new RefreshToken());
        stubJwtEncoder();

        AuthPair first = authService.refresh("tokenA");
        AuthPair second = authService.refresh("tokenB");

        // Each call produces a fresh raw token
        assertNotEquals(first.rawRefreshToken(), second.rawRefreshToken());
    }

    @Test
    void testRefreshWithInvalidToken_ThrowsUnauthorizedException() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.refresh("invalidToken"));

        assertEquals("Invalid or expired refresh token", ex.getMessage());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void testRefreshWithExpiredToken_ThrowsUnauthorizedException() {
        RefreshToken expired = new RefreshToken();
        expired.setExpiresAt(Instant.now().minusSeconds(1));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.refresh("expiredToken"));

        assertEquals("Refresh token expired", ex.getMessage());
        verify(refreshTokenRepository).delete(expired);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void testRefreshWithTokenExpiringExactlyNow_ThrowsUnauthorizedException() {
        // boundary: expiresAt == now (isBefore returns false for equal, but this is a tight window)
        // Use a token that expired 1ms ago to ensure it is reliably in the past
        RefreshToken expiredJustNow = new RefreshToken();
        expiredJustNow.setExpiresAt(Instant.now().minusMillis(1));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredJustNow));

        assertThrows(UnauthorizedException.class,
                () -> authService.refresh("justExpiredToken"));

        verify(refreshTokenRepository).delete(expiredJustNow);
    }

    // ─── logout() ────────────────────────────────────────────────────────────

    @Test
    void testLogoutSuccess() {
        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        authService.logout("validToken");

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void testLogoutWithUnknownToken_IsNoOp() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.logout("unknownToken"));

        verify(refreshTokenRepository, never()).delete(any());
    }
}
