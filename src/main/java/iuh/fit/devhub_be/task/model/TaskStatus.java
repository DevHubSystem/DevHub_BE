package iuh.fit.devhub_be.task.model;

/**
 * Lifecycle status of a task. Defaults to {@link #PENDING} on creation.
 */
public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    DONE
}
