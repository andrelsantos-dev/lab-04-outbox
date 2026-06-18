# Phase 03 — Outbox Event Processing Worker

## Goal

Implement asynchronous processing for pending outbox events.

This phase focuses on proving the second half of the Transactional Outbox Pattern:

Business transaction persistence → event publication → event completion.

---

## Implemented

* Added `EventPublisher` abstraction
* Implemented `LogEventPublisher`
* Created `OutboxWorker`
* Added `OutboxWorkerRepository`
* Introduced dedicated worker database access
* Implemented event publication flow
* Added integration test validating end-to-end processing

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
    Node_Worker["OutboxWorker.processPending()"]
    Node_Pub["EventPublisher.publish()"]
    Node_Updated["published_at updated"]

%% Relationships
    subgraph "Synchronous Transaction (In-App)"
        Node_Req -->|1. Incoming HTTP| Node_Filter
        Node_Filter -->|2. Sets TenantContext| Node_Service
        Node_Service -->|3. Begins Tx & Writes Business Data| Node_Repo
        Node_Repo -->|4. Writes Outbox Record in Same Tx| Node_Outbox
        Node_Outbox -->|5. Atomically Persists All| Node_Commit
    end

    subgraph "Asynchronous Processing (Background)"
        Node_Commit -->|6. Triggers Asynchronously| Node_Worker
        Node_Worker -->|7. Polls & Dispatches Event| Node_Pub
        Node_Pub -->|8. Flags Record as Complete| Node_Updated
    end

%% Styling
    style Node_Req fill:#f9f,stroke:#333,stroke-width:2px
    style Node_Filter fill:#f96,stroke:#333,stroke-width:2px
    style Node_Service fill:#bbf,stroke:#333,stroke-width:2px
    style Node_Repo fill:#ff9999,stroke:#333,stroke-width:2px
    style Node_Outbox fill:#ffff99,stroke:#333,stroke-width:2px
    style Node_Commit fill:#99ff99,stroke:#333,stroke-width:2px
    style Node_Worker fill:#e1bbf7,stroke:#333,stroke-width:2px
    style Node_Pub fill:#99bbf,stroke:#333,stroke-width:2px
    style Node_Updated fill:#bbf7e1,stroke:#333,stroke-width:2px
```

---

## Architectural Decisions

### Worker reads events outside tenant session

The application uses PostgreSQL Row Level Security (RLS).

Because pending events must be processed across multiple tenants, the worker cannot depend on tenant-scoped application access.

A dedicated operational access path was introduced for event discovery.

---

### Worker uses dedicated JDBC access

Instead of introducing multiple Spring Data sources and transaction managers, the worker repository uses a dedicated `JdbcTemplate`.

Reason:

* Keep the lab focused on Outbox Pattern
* Avoid infrastructure complexity
* Keep processing explicit

---

### Event publication remains infrastructure agnostic

The publisher implementation currently logs events.

Reason:

The goal is validating the processing loop, not integrating external brokers.

Future implementations may replace the publisher with Kafka, SQS or EventBridge.

---

## Testing Strategy

Integration test validates:

* Patient creation
* Outbox persistence
* Pending event discovery
* Worker execution
* Event completion (`published_at`)
* Publication execution

---

## Lessons Learned

* Database session state is connection scoped
* RLS affects asynchronous processing
* Workers operate differently from request flows
* Infrastructure concerns should not dominate business flow
* Transactional Outbox starts simple before introducing messaging platforms
