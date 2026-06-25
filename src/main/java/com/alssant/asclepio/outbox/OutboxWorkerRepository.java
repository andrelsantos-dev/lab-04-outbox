package com.alssant.asclepio.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OutboxWorkerRepository {
    private final JdbcTemplate workerTemplate;
    private final OutboxEventRowMapper outboxEventRowMapper;

    public OutboxWorkerRepository(@Value("${spring.datasource.url}") String url,
                                  @Value("${worker.datasource.username}") String username,
                                  @Value("${worker.datasource.password}") String password,
                                  OutboxEventRowMapper outboxEventRowMapper) {
        this.outboxEventRowMapper = outboxEventRowMapper;

        DriverManagerDataSource ds = new DriverManagerDataSource();

        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);

        this.workerTemplate = new JdbcTemplate(ds);
    }

    public List<OutboxEvent> findPending() {
        return workerTemplate.query("""
                SELECT *
                FROM outbox_events
                WHERE published_at IS NULL AND (dead_letter IS NULL OR dead_letter = false)
                ORDER BY created_at
                """, outboxEventRowMapper);
    }
}