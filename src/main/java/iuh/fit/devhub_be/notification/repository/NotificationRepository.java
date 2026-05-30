package iuh.fit.devhub_be.notification.repository;

import iuh.fit.devhub_be.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllByRecipientIdOrderByCreatedAtDesc(UUID recipientId);
}
