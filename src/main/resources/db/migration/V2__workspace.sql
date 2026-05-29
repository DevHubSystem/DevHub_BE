-- V2__workspace.sql
-- FEAT-002: Workspace Management — workspaces table + workspace_members join table.
--
-- Column types mirror the Hibernate-generated schema so `ddl-auto: validate` passes.

CREATE TABLE workspaces (
    id          UUID NOT NULL,
    created_at  TIMESTAMP(6) WITH TIME ZONE,
    updated_at  TIMESTAMP(6) WITH TIME ZONE,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    owner_id    UUID NOT NULL,
    CONSTRAINT workspaces_pkey PRIMARY KEY (id),
    CONSTRAINT fk_workspaces_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE TABLE workspace_members (
    workspace_id UUID NOT NULL,
    user_id      UUID NOT NULL,
    CONSTRAINT workspace_members_pkey PRIMARY KEY (workspace_id, user_id),
    CONSTRAINT fk_workspace_members_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id),
    CONSTRAINT fk_workspace_members_user FOREIGN KEY (user_id) REFERENCES users (id)
);
