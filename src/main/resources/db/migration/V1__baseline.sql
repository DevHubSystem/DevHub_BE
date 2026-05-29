-- V1__baseline.sql
-- Baseline schema snapshot of the auth module (users, roles, permissions, refresh_tokens).
--
-- On an EXISTING database this file is NOT executed: Flyway baselines at version 1
-- (spring.flyway.baseline-version=1) and records V1 as already applied.
-- On a FRESH database this file builds the initial schema.
--
-- Column types mirror the Hibernate-generated schema so `ddl-auto: validate` passes.

CREATE TABLE roles (
    id         UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE,
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    name       VARCHAR(255) NOT NULL,
    CONSTRAINT roles_pkey PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE users (
    id         UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE,
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    user_name  VARCHAR(255),
    role_id    UUID NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE permissions (
    id         UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE,
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    api_path   VARCHAR(255),
    method     VARCHAR(255),
    module     VARCHAR(255),
    name       VARCHAR(255),
    CONSTRAINT permissions_pkey PRIMARY KEY (id)
);

CREATE TABLE refresh_tokens (
    id         UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE,
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    user_id    UUID NOT NULL,
    CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);
