# Phase 05 — Scheduled Outbox Processing

## Goal

Automate outbox event processing through scheduled execution.

Previous phases required manual worker invocation to process pending events.

This phase introduces autonomous event polling and execution, allowing pending events to be processed continuously without explicit calls.

---

## Implemented

* Added `OutboxScheduler`
* Enabled Spring scheduling
* Automated execution of pending event processing
* Added scheduler configuration through application properties
* Introduced conditional scheduler activation
* Added integration test validating scheduled execution
* Refactored integration test structure

---

## Architecture

```text
HTTP Request
      ↓
PatientService
      ↓
Transaction
├── patient
└── outbox_event
      ↓
COMMIT
      ↓
OutboxScheduler
      ↓
OutboxWorker
      ↓
EventPublisher
      ↓
published_at
```

---

## Scheduling Strategy

The scheduler executes worker processing automatically.

Example:

```java
@Scheduled(
    fixedDelayString =
        "${worker.outbox.delay-ms}"
)
```

Current behavior:

```text
execution starts
↓
processing completes
↓
wait configured delay
↓
start next execution
```

---

## Architectural Decisions

### fixedDelay instead of fixedRate

`fixedDelay` was intentionally selected.

Reason:

The next execution only starts after the previous execution finishes.

This avoids overlapping executions and reduces the risk of concurrent processing.

Example:

```text
execution: 5s
delay: 2s

total cycle = 7s
```

This behavior proved especially useful during integration testing.

---

### Conditional Scheduler Activation

Scheduler activation became configurable.

Example:

```yaml
worker:
  outbox:
    enabled: true
```

Implementation:

```java
@ConditionalOnProperty(...)
```

Reason:

Allow test environments to disable background execution and keep test scenarios deterministic.

This pattern reflects a common production approach for feature toggling and environment-specific behavior.

---

## Testing Strategy

Integration tests validate:

### Scheduled Processing

* Create patient
* Persist outbox event
* Wait asynchronously
* Verify worker execution

### Worker Processing

* Publish pending events
* Preserve failures
* Update retry metadata

### Test Stability Improvements

Test infrastructure was refactored to reduce side effects.

Changes:

* Integration test split by responsibility
* Container reuse
* Scheduler isolation

---

## Test Refactoring

Integration tests were reorganized.

Before:

```text
PatientIntegrationTest
```

After:

```text
PatientIntegrationTest
OutboxWorkerIntegrationTest
```

Goals:

* Improve cohesion
* Reduce test maintenance cost
* Separate HTTP concerns from asynchronous processing

---

## Additional Learnings

### Given / When / Then

Tests started following a clearer execution narrative.

Structure:

```text
Given
→ scenario

When
→ action

Then
→ validation
```

The objective was improving readability rather than enforcing comments.

---

### Fluent Assertions

Assertions were progressively migrated from JUnit assertions to AssertJ.

Before:

```java
assertEquals(1, events.size());
```

After:

```java
assertThat(events).hasSize(1);
```

Observed benefits:

* More declarative style
* Better readability
* Lower chance of parameter inversion
* Better composition for complex validations

---

### Fluent Interfaces

This phase reinforced understanding of fluent APIs already used across the project.

Examples:

* MockMvc
* AssertJ
* Builder pattern

Common characteristic:

```text
object
→ chained operations
→ readable intent
```

---

## Lessons Learned

* Background execution introduces concurrency concerns
* Scheduler behavior impacts test reliability
* Test environments should control asynchronous execution
* Infrastructure code benefits from explicit activation controls
* Small architectural decisions simplify long-term maintenance

---

## Known Limitations

Current implementation intentionally does not include:

* Distributed scheduling
* Locking
* Batch processing
* Backoff strategy
* Dead Letter Queue
* Event broker integration
* Idempotent execution

These concerns remain intentionally deferred to future phases.
