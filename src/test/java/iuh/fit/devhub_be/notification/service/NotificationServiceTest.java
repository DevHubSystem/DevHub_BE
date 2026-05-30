package iuh.fit.devhub_be.notification.service;

import iuh.fit.devhub_be.auth.model.User;
import iuh.fit.devhub_be.common.exception.ResourceNotFoundException;
import iuh.fit.devhub_be.notification.dto.response.NotificationResponse;
import iuh.fit.devhub_be.notification.model.Notification;
import iuh.fit.devhub_be.notification.model.NotificationType;
import iuh.fit.devhub_be.notification.repository.NotificationRepository;
import iuh.fit.devhub_be.notification.service.impl.NotificationServiceImpl;
import iuh.fit.devhub_be.notification.websocket.NotificationWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationWebSocketHandler webSocketHandler;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    // ─── Fixtures ────────────────────────────────────────────────────────────

    private User buildUser(UUID id, String userName) {
        User user = new User();
        user.setId(id);
        user.setUserName(userName);
        user.setEmail(userName + "@example.com");
        return user;
    }

    private Notification buildNotification(UUID id, User recipient, boolean read) {
        Notification n = new Notification();
        n.setId(id);
        n.setRecipient(recipient);
        n.setType(NotificationType.WORKSPACE_MEMBER_ADDED);
        n.setMessage("You were added to the workspace \"DevHub Team\"");
        n.setReferenceId(UUID.randomUUID());
        n.setRead(read);
        return n;
    }

    // ─── notifyWorkspaceMemberAdded() ──────────────────────────────────────────

    @Test
    void testNotifyWorkspaceMemberAdded_PersistsAndPushes() {
        UUID recipientId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User recipient = buildUser(recipientId, "lan");

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.notifyWorkspaceMemberAdded(recipient, workspaceId, "DevHub Team");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(recipient, saved.getRecipient());
        assertEquals(NotificationType.WORKSPACE_MEMBER_ADDED, saved.getType());
        assertEquals(workspaceId, saved.getReferenceId());
        assertFalse(saved.isRead());
        assertEquals("You were added to the workspace \"DevHub Team\"", saved.getMessage());

        // Pushed to the recipient's live sessions.
        ArgumentCaptor<NotificationResponse> pushCaptor = ArgumentCaptor.forClass(NotificationResponse.class);
        verify(webSocketHandler, times(1)).sendToUser(eq(recipientId), pushCaptor.capture());
        assertEquals(workspaceId, pushCaptor.getValue().referenceId());
        assertEquals(NotificationType.WORKSPACE_MEMBER_ADDED, pushCaptor.getValue().type());
    }

    // ─── listMine() ────────────────────────────────────────────────────────────

    @Test
    void testListMineReturnsResponsesNewestFirst() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "lan");
        Notification a = buildNotification(UUID.randomUUID(), user, false);
        Notification b = buildNotification(UUID.randomUUID(), user, true);

        when(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(a, b));

        List<NotificationResponse> result = notificationService.listMine(userId);

        assertEquals(2, result.size());
        assertEquals(a.getId(), result.get(0).id());
        assertEquals(b.getId(), result.get(1).id());
        verify(notificationRepository).findAllByRecipientIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void testListMineWhenNone_ReturnsEmptyList() {
        UUID userId = UUID.randomUUID();
        when(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of());

        assertTrue(notificationService.listMine(userId).isEmpty());
    }

    // ─── markAsRead() ────────────────────────────────────────────────────────────

    @Test
    void testMarkAsRead_Success() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        User user = buildUser(userId, "lan");
        Notification notification = buildNotification(notificationId, user, false);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse result = notificationService.markAsRead(notificationId, userId);

        assertTrue(result.read());
        assertTrue(notification.isRead());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void testMarkAsReadWhenNotFound_ThrowsResourceNotFound() {
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(notificationId, UUID.randomUUID()));

        assertEquals("Notification not found", ex.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void testMarkAsReadWhenBelongsToAnotherUser_ThrowsResourceNotFound() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        User owner = buildUser(ownerId, "lan");
        Notification notification = buildNotification(notificationId, owner, false);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // Another user's notification is treated as not found — no existence leak.
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(notificationId, otherUserId));

        assertEquals("Notification not found", ex.getMessage());
        verify(notificationRepository, never()).save(any());
    }
}
