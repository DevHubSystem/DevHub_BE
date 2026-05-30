package iuh.fit.devhub_be.notification.service;

import iuh.fit.devhub_be.auth.model.User;
import iuh.fit.devhub_be.notification.dto.response.NotificationResponse;

import java.util.List;
import java.util.UUID;

/**
 * Entry point other modules use to raise notifications. Implementations persist
 * the notification and push it to any live WebSocket session of the recipient.
 */
public interface NotificationService {

    /**
     * Notify {@code recipient} that they were added to a workspace.
     *
     * @param recipient     the newly added member (the user to notify)
     * @param workspaceId   the workspace's id (stored as the notification reference)
     * @param workspaceName the workspace's name (used in the message text)
     */
    void notifyWorkspaceMemberAdded(User recipient, UUID workspaceId, String workspaceName);

    /** List the given user's notifications, newest first. */
    List<NotificationResponse> listMine(UUID userId);

    /** Mark one of the user's notifications as read. */
    NotificationResponse markAsRead(UUID notificationId, UUID userId);
}
