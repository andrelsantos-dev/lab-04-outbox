# Lab 04 — Transactional Outbox & RabbitMQ (Asclépio)

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ_4.3-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_17-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Testcontainers](https://img.shields.io/badge/Testcontainers-2496ED?style=for-the-badge&logo=testcontainers&logoColor=white)

## Overview

This lab implements the Transactional Outbox Pattern together with RabbitMQ to demonstrate reliable asynchronous communication in a multi-tenant application.

The project evolves incrementally through multiple phases, introducing production-inspired reliability mechanisms while keeping the implementation focused and easy to understand.

Goals:

* Persist domain changes and integration events within the same transaction
* Publish integration events asynchronously through RabbitMQ
* Consume events reliably
* Handle transient and permanent consumer failures
* Prevent message loss across producer and consumer
* Guarantee idempotent processing where appropriate
* Preserve tenant isolation during asynchronous execution
* Validate architectural behavior using PostgreSQL, RabbitMQ and end-to-end integration tests

---

## Previous Labs

* Lab 01 — Flyway + Testcontainers
* Lab 02 — PostgreSQL Row Level Security
* Lab 03 — Spring Multi-Tenancy with PostgreSQL RLS

---

## Stack

* Java 21
* Spring Boot
* PostgreSQL
* RabbitMQ
* Flyway
* Testcontainers

---

## Current Scope

The current implementation covers a complete event-driven workflow:

* Transactional Outbox persistence
* Background worker processing
* Scheduled event publication
* Retry and failure handling
* Idempotent processing
* RabbitMQ publisher
* RabbitMQ consumer
* Consumer retry and redelivery
* Dead Letter Queue (DLQ)
* Multi-tenant support using PostgreSQL Row Level Security
* End-to-end integration testing with PostgreSQL and RabbitMQ Testcontainers

---

## Project Structure

```text
src
├── audit
├── config
├── consumer
├── outbox
├── patient
├── tenant
└── integration
```

---

## Architecture Overview

```text
HTTP Request
      │
      ▼
Patient Service
      │
      ▼
Database Transaction
      │
      ├──────────────► Patient
      │
      └──────────────► Outbox
                           │
                           ▼
                     Outbox Worker
                           │
                           ▼
                      RabbitMQ
                           │
                           ▼
                    Rabbit Consumer
                           │
                           ▼
                     Audit Entries
```

---

## Architecture Evolution

This lab is intentionally developed in incremental phases.

Each phase documents:

* Architectural decisions
* Implementation details
* Testing strategy
* Lessons learned

Current phases:

* Phase 01 — Bootstrap
* Phase 02 — Transactional Outbox Persistence
* Phase 03 — Outbox Event Processing Worker
* Phase 04 — Failure Handling and Retry
* Phase 05 — Scheduler
* Phase 06 — Idempotent Event Processing
* Phase 07 — Outbox Dead Letter Queue
* Phase 08 — RabbitMQ Publisher
* Phase 09 — RabbitMQ Consumer
* Phase 10 — Consumer Failure Handling

---

## Run Locally

Start the local infrastructure:

```bash
make up
```

Run the application using the development profile:

```bash
make dev
```

---

## Design Principles

* Labs are self-contained and concept-oriented
* Simplicity over framework abstraction
* Real integration tests over mocks
* Infrastructure as part of the design
* Database as the source of truth
* Progressive architecture evolution

---

## Testing with RLS

Integration tests that validate persisted data after HTTP requests may require opening a new tenant context.

PostgreSQL Row Level Security relies on the connection session state (`app.current_tenant`), while test assertions execute using a different transaction and database connection.

For this reason, tests use a small helper (`executeAsTenant`) to recreate the tenant context before performing assertions.

---

## Testing Strategy

This project intentionally favors end-to-end integration tests more heavily than a typical production application.

The objective of the lab is educational: validate architectural behavior across multiple layers and make infrastructure concerns observable.

The integration test suite covers scenarios such as:

* Transaction boundaries
* PostgreSQL Row Level Security (RLS)
* Tenant context propagation
* Transactional Outbox
* Background worker execution
* Scheduler execution
* Retry handling
* Idempotent processing
* RabbitMQ publishing
* RabbitMQ consumption
* Consumer redelivery
* Dead Letter Queue routing

Because these concerns emerge from the interaction between application and infrastructure, most scenarios are validated end-to-end.

### Production Considerations

In a real-world application, the testing pyramid would typically favor more isolated tests:

```text
Unit Tests
↑↑↑↑↑↑↑↑

Service Tests
↑↑↑

Integration Tests
↑
```

Typical production guidance:

* Unit tests validate business rules
* Service tests validate orchestration
* Integration tests validate infrastructure boundaries
* End-to-end tests remain selective

This repository intentionally emphasizes integration testing to maximize learning, validate architectural behavior, and demonstrate real infrastructure interactions rather than optimize execution time.
