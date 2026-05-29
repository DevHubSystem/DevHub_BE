-- V3__task.sql
-- FEAT-003: Task Creation — tasks table + task_assignees join table.
--
-- Column types mirror the Hibernate-generated schema so `ddl-auto: validate` passes.
-- work_type / status / priority are @Enumerated(EnumType.STRING) -> VARCHAR.

CREATE TABLE tasks (
    id             UUID NOT NULL,
    created_at     TIMESTAMP(6) WITH TIME ZONE,
    updated_at     TIMESTAMP(6) WITH TIME ZONE,
    name           VARCHAR(255) NOT NULL,
    work_type      VARCHAR(255) NOT NULL,
    status         VARCHAR(255) NOT NULL,
    priority       VARCHAR(255) NOT NULL,
    due_date       DATE,
    description    TEXT,
    workspace_id   UUID NOT NULL,
    parent_task_id UUID,
    CONSTRAINT tasks_pkey PRIMARY KEY (id),
    CONSTRAINT fk_tasks_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id),
    CONSTRAINT fk_tasks_parent FOREIGN KEY (parent_task_id) REFERENCES tasks (id)
);

CREATE TABLE task_assignees (
    task_id UUID NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT task_assignees_pkey PRIMARY KEY (task_id, user_id),
    CONSTRAINT fk_task_assignees_task FOREIGN KEY (task_id) REFERENCES tasks (id),
    CONSTRAINT fk_task_assignees_user FOREIGN KEY (user_id) REFERENCES users (id)
);
