# Phase 02 — Transactional Outbox Persistence

## Goal

Implement the transactional persistence layer for the Outbox Pattern.

Guarantee that business writes and event registration occur inside the same database transaction.

---

## Implemented

* Created `outbox_events` table
* Added tenant isolation using PostgreSQL RLS
* Implemented `OutboxService`
* Added event metadata abstraction
* Persisted event payload as JSON
* Added integration tests validating patient creation + outbox event

---

## Flow

```mermaid
graph TD
    %% Node Declarations
    Node_Req["Request"]
    Node_Filter["Tenant Filter"]
    Node_Service["PatientService (@Transactional)"]
    Node_Repo["PatientRepository.save()"]
    Node_Outbox["OutboxService.record()"]
    Node_Commit["COMMIT"]

    %% Relationships
    Node_Req -->|1. Incoming HTTP| Node_Filter
    Node_Filter -->|2. Sets ThreadLocal Context| Node_Service
    Node_Service -->|3. Begins Transaction & Runs Business Logic| Node_Repo
    Node_Repo -->|4. Persists Patient Data via RLS| Node_Outbox
    Node_Outbox -->|5. Writes Outbox Event in Same Session| Node_Commit

    %% Styling
    style Node_Req fill:#f9f,stroke:#333,stroke-width:2px
    style Node_Filter fill:#f96,stroke:#333,stroke-width:2px
    style Node_Service fill:#bbf,stroke:#333,stroke-width:2px
    style Node_Repo fill:#ff9999,stroke:#333,stroke-width:2px
    style Node_Outbox fill:#ffff99,stroke:#333,stroke-width:2px
    style Node_Commit fill:#99ff99,stroke:#333,stroke-width:2px
```

---

## Architectural Decisions

### Event payload is created in the application layer

Event construction remains simple and explicit.

No mapper or event converter abstraction was introduced.

Reason:
Keep focus on transactional consistency instead of event modeling.

---

### RLS remains enabled for outbox table

Outbox events are tenant scoped.

Tests that validate persisted state after request completion open a new tenant context because RLS depends on connection session state.

---

### Event payload does not mirror full entity state

Only relevant business information is persisted.

Example:

```json
{
  "patientId": "...",
  "name": "CLIENT NEW"
}
```

---

## Lessons Learned

* Database session state is connection scoped
* RLS works independently from application memory
* Transaction boundaries differ from request boundaries
* Event-driven consistency starts with persistence, not publishing
