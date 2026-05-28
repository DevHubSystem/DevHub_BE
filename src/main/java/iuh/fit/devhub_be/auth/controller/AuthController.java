package iuh.fit.devhub_be.auth.controller;

import iuh.fit.devhub_be.auth.dto.request.LoginRequest;
import iuh.fit.devhub_be.auth.dto.request.RegisterRequest;
import iuh.fit.devhub_be.auth.dto.response.AuthPair;
import iuh.fit.devhub_be.auth.dto.response.AuthResponse;
import iuh.fit.devhub_be.auth.service.AuthService;
import iuh.fit.devhub_be.common.dto.ApiResponse;
import iuh.fit.devhub_be.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @RequestBody @Valid RegisterRequest request){
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null,"Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response) {
        AuthPair result = authService.login(request);
        setRefreshTokenCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(result.authResponse(), "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String rawRefreshToken,
            HttpServletResponse response) {
        if (rawRefreshToken == null) {
            throw new UnauthorizedException("Refresh token missing");
        }
        AuthPair result = authService.refresh(rawRefreshToken);
        setRefreshTokenCookie(response, result.rawRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(result.authResponse()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refreshToken", required = false) String rawRefreshToken,
            HttpServletResponse response) {
        if (rawRefreshToken != null) {
            authService.logout(rawRefreshToken);
        }
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String rawToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", rawToken)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(Duration.ofSeconds(refreshTokenExpiration))
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
