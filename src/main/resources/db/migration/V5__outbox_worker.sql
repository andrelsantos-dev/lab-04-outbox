GRANT USAGE ON SCHEMA public TO worker_user;

GRANT SELECT, UPDATE
    ON outbox_events
    TO worker_user;
