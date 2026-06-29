CREATE TABLE audit_entries
(
    id             UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    tenant_id      UUID         NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    payload        JSONB,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);


ALTER TABLE audit_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_entries FORCE ROW LEVEL SECURITY;

CREATE
POLICY audit_entries_tenant_isolation ON audit_entries
USING (
    tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid
)
WITH CHECK (
    tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid
);

GRANT SELECT, INSERT ON audit_entries TO app_user;