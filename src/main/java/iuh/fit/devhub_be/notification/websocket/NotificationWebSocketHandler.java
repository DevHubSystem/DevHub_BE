package iuh.fit.devhub_be.notification.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Raw WebSocket handler that keeps a registry of live sessions per user and
 * pushes notification payloads to a given recipient.
 *
 * <p><b>Auth is deferred (FEAT-005 §10):</b> the recipient is identified by a
 * {@code ?userId=<uuid>} query parameter on the handshake URL. This is insecure
 * and temporary — to be replaced by JWT handshake authentication.
 *
 * <p>The session registry is in-memory, so delivery only works within a single
 * application instance (not cluster-safe).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    // Self-contained mapper (findAndAddModules picks up JavaTimeModule so Instant
    // serializes as ISO-8601). Kept independent of the MVC ObjectMapper bean so the
    // handler has no auto-configuration ordering dependency.
    private final ObjectMapper objectMapper;

    /** userId -> open sessions (a user may have multiple tabs/devices). */
    private final Map<UUID, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID userId = resolveUserId(session);
        if (userId == null) {
            session.close(CloseStatus.BAD_DATA.withReason("Missing or invalid userId"));
            return;
        }
        sessionsByUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("WebSocket connected for user {} (session {})", userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID userId = resolveUserId(session);
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByUser.remove(userId);
            }
        }
    }

    /**
     * Serialize {@code payload} to JSON and send it to every open session of
     * {@code userId}. No-op if the user has no live session. Send failures are
     * logged and swallowed so they never break the caller's transaction.
     */
    public void sendToUser(UUID userId, Object payload) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification for user {}", userId, e);
            return;
        }

        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                // WebSocketSession is not safe for concurrent sends.
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException e) {
                log.warn("Failed to push notification to session {} (user {})", session.getId(), userId, e);
            }
        }
    }

    private UUID resolveUserId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String param : uri.getQuery().split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "userId".equals(kv[0])) {
                try {
                    return UUID.fromString(kv[1]);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
