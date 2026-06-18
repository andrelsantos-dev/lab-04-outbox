# Lab 04 — Outbox Pattern (Asclépio)

## Overview

This lab introduces the Transactional Outbox Pattern to guarantee consistency between database state changes and event publication.

The implementation builds on concepts explored in previous labs while simplifying unrelated components to keep focus on transactional consistency.

Goals:

* Persist domain changes and integration events in the same transaction
* Publish events asynchronously
* Prevent event loss
* Support idempotent event processing
* Validate behavior using PostgreSQL and integration tests

---

## Previous Labs

* Lab 01 - Flyway + Testcontainers
* Lab 02 - PostgreSQL Row Level Security
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

Current foundation prepared for this lab:

* Multi-tenant environment using PostgreSQL RLS
* Patient module simplified for focused experimentation
* Integration tests using PostgreSQL containers
* Explicit datasource configuration

Future phases will introduce the Outbox implementation.

---

## Project Structure

```text
src
├── patient
├── tenant
├── config
└── integration
```

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
