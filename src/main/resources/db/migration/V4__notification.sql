-- V4__notification.sql
-- FEAT-005: Workspace Member Notification — notifications table.
--
-- Column types mirror the Hibernate-generated schema so `ddl-auto: validate` passes.
-- type is @Enumerated(EnumType.STRING) -> VARCHAR; read maps to column is_read.

CREATE TABLE notifications (
    id           UUID NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE,
    updated_at   TIMESTAMP(6) WITH TIME ZONE,
    recipient_id UUID NOT NULL,
    type         VARCHAR(255) NOT NULL,
    message      VARCHAR(255) NOT NULL,
    is_read      BOOLEAN NOT NULL,
    reference_id UUID,
    CONSTRAINT notifications_pkey PRIMARY KEY (id),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id)
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_id);
