package iuh.fit.devhub_be.notification.controller;

import iuh.fit.devhub_be.common.dto.ApiResponse;
import iuh.fit.devhub_be.notification.dto.response.NotificationResponse;
import iuh.fit.devhub_be.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;



    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        NotificationResponse updated = notificationService.markAsRead(id, currentUserId(jwt));
        return ResponseEntity.ok(ApiResponse.success(updated, "Notification marked as read"));
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
