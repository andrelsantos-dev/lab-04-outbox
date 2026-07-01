# Phase 10 — RabbitMQ Consumer Failure Handling

## Objective

This phase focuses on consumer resilience.

Previous phases demonstrated how integration events are reliably published using the Transactional Outbox Pattern and consumed through RabbitMQ.

The objective of this phase is to understand how RabbitMQ and Spring AMQP behave when message processing fails, distinguishing between transient failures, permanent failures, and Dead Letter Queue (DLQ) handling.

---

## What was implemented

### Transient Failure Handling

A transient failure was simulated by forcing the consumer to throw a `RuntimeException` during the first processing attempt.

The message was successfully processed after being redelivered, demonstrating that temporary failures do not necessarily result in message loss.

This scenario represents failures such as:

* Temporary database unavailability
* Network instability
* External service timeout

---

### Permanent Failure Handling

A second scenario introduced a permanent failure using `AmqpRejectAndDontRequeueException`.

Unlike generic exceptions, this exception explicitly instructs Spring AMQP that the message should not be retried.

The consumer immediately rejects the message, allowing the broker to apply its dead-letter policy.

This behavior is appropriate for poison messages, including:

* Invalid payloads
* Unsupported event versions
* Irrecoverable business validation failures

---

### Dead Letter Queue

A dedicated Dead Letter Exchange (DLX) and Dead Letter Queue (DLQ) were configured.

The main queue declares its dead-letter configuration, allowing RabbitMQ to automatically route rejected messages to the DLQ.

Processing flow:

```text
Producer
    │
    ▼
Main Exchange
    │
    ▼
Main Queue
    │
    ▼
Consumer
    │
    ├────────────── Success
    │                     │
    │                     ▼
    │              Business Logic
    │
    └────────────── Reject
                          │
                          ▼
               Dead Letter Exchange
                          │
                          ▼
                 Dead Letter Queue
```

The application does not manually move messages to the DLQ.

This routing is entirely managed by RabbitMQ.

---

## Integration Testing

Three end-to-end integration scenarios validate consumer behavior.

### Successful Consumption

Validates the complete event processing pipeline.

The consumer successfully receives the event and persists an audit entry.

---

### Transient Failure

A temporary exception is simulated.

The test validates that:

* the message is processed successfully after redelivery;
* no message is lost;
* the expected business effect is eventually produced.

---

### Dead Letter Queue

A poison message is simulated using `AmqpRejectAndDontRequeueException`.

The test validates that:

* the message is routed to the Dead Letter Queue;
* the original event envelope is preserved;
* metadata and payload remain unchanged;
* no audit record is persisted.

Rather than relying on logs or implementation details, the test inspects the Dead Letter Queue directly using `RabbitTemplate`, validating the observable behavior of the messaging infrastructure.

---

## Design Decisions

### Infrastructure-managed retries

The application no longer controls retry attempts after the message reaches RabbitMQ.

Once published, retry and redelivery become responsibilities of the messaging infrastructure.

This differs from the Transactional Outbox, where retry logic is implemented explicitly by the application before the event reaches the broker.

---

### Dead Letter Queue as Infrastructure

The Dead Letter Queue is treated as part of RabbitMQ infrastructure rather than application state.

Unlike the Outbox implementation, no additional database tables are required to represent failed consumer messages.

RabbitMQ itself becomes responsible for preserving rejected messages.

---

## Lessons Learned

* Transient failures and permanent failures require different handling strategies.
* `RuntimeException` results in message redelivery, allowing temporary failures to recover automatically.
* `AmqpRejectAndDontRequeueException` immediately rejects the message, bypassing redelivery.
* RabbitMQ routes rejected messages through a Dead Letter Exchange before they reach the Dead Letter Queue.
* A Dead Letter Queue is simply another RabbitMQ queue with a different routing path.
* Consumer retry policies and Transactional Outbox retries solve different problems and operate at different stages of the message lifecycle.
* End-to-end integration tests can validate messaging infrastructure directly by inspecting RabbitMQ queues instead of relying on log assertions.
