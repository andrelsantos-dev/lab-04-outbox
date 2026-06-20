ALTER TABLE outbox_events ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE outbox_events ADD COLUMN last_error TEXT NULL;