-- V5__workspace_reminder_key.sql
-- FEAT-006: Workspace Reminder Key — adds the public, unique 6-char identifier
-- used in URLs for workspace CRUD instead of the UUID id.
--
-- Column type mirrors the Hibernate-generated schema so `ddl-auto: validate` passes:
--   @Column(nullable = false, unique = true, length = 6) String reminderKey

-- 1. Add the column nullable first so any pre-existing rows can be backfilled.
ALTER TABLE workspaces ADD COLUMN reminder_key VARCHAR(6);

-- 2. Backfill existing rows with a deterministic, unique, format-valid key
--    ("W" + 5-digit sequence, e.g. W00001). id is a time-ordered UUID, so
--    ordering by it yields a stable creation order.
WITH numbered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM workspaces
)
UPDATE workspaces w
SET reminder_key = 'W' || LPAD(numbered.rn::text, 5, '0')
FROM numbered
WHERE w.id = numbered.id;

-- 3. Enforce NOT NULL + uniqueness now that every row has a value.
ALTER TABLE workspaces ALTER COLUMN reminder_key SET NOT NULL;
ALTER TABLE workspaces ADD CONSTRAINT uk_workspaces_reminder_key UNIQUE (reminder_key);
