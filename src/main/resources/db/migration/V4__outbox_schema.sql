-- Outbox Events table
CREATE TABLE outbox_events
(
    id             UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    -- Automatically assigns the current tenant
    -- configured in the database session.
    tenant_id      UUID         NOT NULL DEFAULT current_setting('app.current_tenant', true)::uuid,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at   TIMESTAMP
);

CREATE INDEX idx_outbox_tenant ON outbox_events (tenant_id);

-- Policies
ALTER TABLE outbox_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE outbox_events FORCE ROW LEVEL SECURITY;

CREATE
POLICY outbox_tenant_isolation ON outbox_events
USING (
    tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid
)
WITH CHECK (
    tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid
);
-- Permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON outbox_events TO app_user;
