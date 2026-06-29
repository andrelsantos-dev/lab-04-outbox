# Phase 09 — RabbitMQ Consumer

## Objective

This phase completes the event-driven flow by introducing a RabbitMQ consumer.

Previous phases focused on reliably publishing integration events through the Transactional Outbox Pattern. This phase demonstrates how a consumer can process those events while preserving tenant isolation using PostgreSQL Row Level Security (RLS).

---

## What was implemented

### RabbitMQ Consumer

A dedicated consumer was introduced to receive integration events published by the Outbox Worker.

Responsibilities:

* Receive messages from RabbitMQ
* Deserialize the integration event
* Restore the tenant execution context
* Execute application logic
* Persist audit information

The consumer remains intentionally small, delegating business logic to the service layer.

---

### Audit Module

Instead of invoking another business module directly, the consumer records every processed event in a dedicated audit table.

This simulates an independent subsystem that reacts to domain events without introducing coupling to the Patient module.

Audit information includes:

* Tenant identifier
* Aggregate identifier
* Aggregate type
* Event type
* Event payload
* Processing timestamp

---

### Tenant Context Restoration

Unlike HTTP requests, RabbitMQ consumers do not execute inside the request pipeline.

For this reason, the tenant context must be reconstructed before executing any database operation.

Processing flow:

```text
RabbitMQ Message
        │
        ▼
EventEnvelope
        │
        ▼
Restore TenantContext
        │
        ▼
Persist Audit Entry
        │
        ▼
Clear TenantContext
```

This guarantees that PostgreSQL RLS policies continue protecting data access during asynchronous processing.

---

### Event Contract Evolution

The integration contract was extended to include the tenant identifier inside the event metadata.

```text
EventMetadata

├── tenantId
├── aggregateId
├── aggregateType
└── eventType
```

The tenant identifier is considered infrastructure metadata rather than business payload, allowing consumers to restore execution context independently from the domain event.

---

## Row Level Security

The audit table is also protected by PostgreSQL Row Level Security.

The consumer must restore the tenant context before persisting data, otherwise PostgreSQL rejects the insert operation.

This demonstrates that tenant isolation remains enforced regardless of how the application is invoked.

Both HTTP requests and asynchronous consumers follow the same security model.

---

## Integration Testing

Two end-to-end integration scenarios were added.

### Event Consumption

Validates the complete processing pipeline:

```text
HTTP Request
      │
      ▼
Patient Created
      │
      ▼
Transactional Outbox
      │
      ▼
RabbitMQ
      │
      ▼
Rabbit Consumer
      │
      ▼
Audit Entry
```

The test verifies:

* Event consumption
* Metadata preservation
* Payload integrity
* Audit persistence

---

### Tenant Isolation

A second integration test creates events for multiple tenants.

After asynchronous processing completes, each tenant can only access its own audit records through PostgreSQL RLS.

This validates that the consumer restores the correct tenant context before executing business logic.

---

## Design Decisions

### Consumer Responsibility

The RabbitMQ listener intentionally contains minimal logic.

Its responsibilities are limited to:

* Receiving messages
* Restoring execution context
* Delegating processing

Business rules remain inside the service layer.

---

### Audit as a Separate Concern

The audit module represents an independent consumer of integration events.

Although implemented within the same application for learning purposes, it models a separate bounded context that reacts to published events without coupling itself to the producer.

---

## Lessons Learned

* RabbitMQ consumers require explicit reconstruction of application context.
* Tenant isolation must be preserved across asynchronous execution paths.
* Integration event contracts should include infrastructure metadata required by consumers.
* PostgreSQL Row Level Security protects asynchronous processing exactly as it protects HTTP requests.
* `ENABLE ROW LEVEL SECURITY` and `FORCE ROW LEVEL SECURITY` serve different purposes and both may be required depending on the desired behavior.
* End-to-end integration tests are effective at validating architectural properties such as tenant isolation and event-driven communication.
