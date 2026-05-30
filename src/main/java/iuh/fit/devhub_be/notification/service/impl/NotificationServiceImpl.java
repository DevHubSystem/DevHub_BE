package iuh.fit.devhub_be.notification.service.impl;

import iuh.fit.devhub_be.auth.model.User;
import iuh.fit.devhub_be.common.exception.ResourceNotFoundException;
import iuh.fit.devhub_be.notification.dto.response.NotificationResponse;
import iuh.fit.devhub_be.notification.model.Notification;
import iuh.fit.devhub_be.notification.model.NotificationType;
import iuh.fit.devhub_be.notification.repository.NotificationRepository;
import iuh.fit.devhub_be.notification.service.NotificationService;
import iuh.fit.devhub_be.notification.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationWebSocketHandler webSocketHandler;

    @Override
    @Async
    public void notifyWorkspaceMemberAdded(User recipient, UUID workspaceId, String workspaceName) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.WORKSPACE_MEMBER_ADDED);
        notification.setMessage("You were added to the workspace \"" + workspaceName + "\"");
        notification.setReferenceId(workspaceId);
        notification.setRead(false);

        Notification saved = notificationRepository.save(notification);

        // Push live if the recipient is connected; otherwise the persisted copy
        // is retrieved later via listMine(). A failed push must not break the
        // surrounding transaction — the handler swallows send errors.
        webSocketHandler.sendToUser(recipient.getId(), toResponse(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listMine(UUID userId) {
        return notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        // A notification belonging to another user is treated as not found — no
        // existence leak (consistent with the workspace read policy).
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }

        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getMessage(),
                n.isRead(),
                n.getReferenceId(),
                n.getCreatedAt()
        );
    }
}
