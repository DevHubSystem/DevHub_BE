package iuh.fit.devhub_be.auth.dto.response;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String userName,
        String email,
        String roleName
) {}
