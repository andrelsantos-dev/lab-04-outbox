CREATE TABLE tenants (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     name VARCHAR(255) NOT NULL,
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE patients (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Automatically assigns the current tenant
    -- configured in the database session.
      tenant_id UUID NOT NULL REFERENCES tenants(id) DEFAULT current_setting('app.current_tenant', true)::uuid,
      name VARCHAR(255) NOT NULL,
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_patients_tenant ON patients (tenant_id);