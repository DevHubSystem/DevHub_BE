package iuh.fit.devhub_be.auth.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UserResponse user
) {}
