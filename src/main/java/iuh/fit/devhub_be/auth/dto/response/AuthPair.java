package iuh.fit.devhub_be.auth.dto.response;

public record AuthPair(
        AuthResponse authResponse,
        String rawRefreshToken
) {}
