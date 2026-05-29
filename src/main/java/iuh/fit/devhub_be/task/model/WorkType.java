package iuh.fit.devhub_be.task.model;

/**
 * The kind of work a task represents. Only an {@link #EPIC} may act as a parent
 * for other tasks (see FEAT-003 §7).
 */
public enum WorkType {
    EPIC,
    TASK,
    BUG
}
