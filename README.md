# Lab 04 — Outbox Pattern (Asclépio)

## Overview

This lab implements the Transactional Outbox Pattern to guarantee consistency between database state changes and asynchronous event publication.

The project evolves incrementally through multiple phases, introducing production-inspired reliability mechanisms while keeping the implementation focused and easy to understand.

Goals:

* Persist domain changes and integration events in the same transaction
* Publish events asynchronously
* Prevent event loss
* Support retry-based processing
* Guarantee idempotent event execution
* Isolate permanently failing events using a Dead Letter Queue (DLQ)
* Validate architectural behavior using PostgreSQL and integration tests

---

## Previous Labs

* Lab 01 — Flyway + Testcontainers
* Lab 02 — PostgreSQL Row Level Security
* Lab 03 — Multi-Tenant System with PostgreSQL RLS

---

## Stack

* Java 21
* Spring Boot
* PostgreSQL
* Flyway
* Testcontainers

---

## Current Scope

The current implementation includes a complete Transactional Outbox workflow:

* Transactional event persistence
* Background worker processing
* Automatic scheduler execution
* Retry mechanism
* Idempotent event processing
* Dead Letter Queue (DLQ)
* Multi-tenant support using PostgreSQL Row Level Security
* Integration testing with PostgreSQL Testcontainers

Future phases will focus on external message brokers and production-oriented messaging infrastructure.

---

## Project Structure

```text
src
├── patient
├── tenant
├── outbox
├── config
└── integration
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
* Phase 07 — Dead Letter Queue (DLQ)
* Phase 08 — RabbitMQ Integration
* Phase 09 — RabbitMQ Consumer

---

## Run locally

Start local infrastructure:

```bash
make up
```

Run the application using the `dev` profile:

```bash
make dev
```

---

## Design Principles

* Labs are self-contained and concept-oriented
* Simplicity over framework abstraction
* Real integration tests over mocks
* Database as source of truth
* Progressive architecture evolution

### Testing with RLS

Integration tests that validate persisted data after HTTP requests may require opening a new tenant context.

This happens because PostgreSQL Row Level Security relies on connection session state (`app.current_tenant`), while test assertions execute in a separate transaction and connection.

For this reason, tests use a small helper (`executeAsTenant`) to recreate the tenant context for post-request validations.

---

## Testing Scope

This project intentionally favors integration tests more heavily than a typical production application.

The goal of the lab is educational: validate architectural behavior across multiple layers and make infrastructure concerns observable.

Examples explored in this repository include:

* Transaction boundaries
* Row Level Security (RLS)
* Tenant propagation
* Transactional Outbox
* Background processing
* Scheduler execution
* Retry handling
* Idempotent processing
* Dead Letter Queue (DLQ)

Because these concerns emerge from component interaction, many scenarios are exercised end-to-end.

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

This repository intentionally leans toward integration testing to maximize learning and architectural visibility rather than optimize execution speed.
