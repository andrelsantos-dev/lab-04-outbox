ALTER TABLE outbox_events
    ADD COLUMN failed_at TIMESTAMPTZ;

ALTER TABLE outbox_events
    ADD COLUMN dead_letter BOOLEAN DEFAULT FALSE;

ALTER TABLE outbox_events
    ADD COLUMN failure_reason TEXT;