package iuh.fit.devhub_be.notification.model;

/**
 * Kinds of notification the system can raise. Persisted as a string
 * ({@code @Enumerated(EnumType.STRING)}), so reorder freely but never rename a
 * constant without a data migration.
 */
public enum NotificationType {
    WORKSPACE_MEMBER_ADDED
}
