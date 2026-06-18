package com.alssant.asclepio.outbox;

import com.alssant.asclepio.outbox.dto.EventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class OutboxWorkerRepository {
    private final JdbcTemplate workerTemplate;
    private final ObjectMapper mapper;

    public OutboxWorkerRepository(@Value("${spring.datasource.url}") String url,
                                  @Value("${worker.datasource.username}") String username,
                                  @Value("${worker.datasource.password}") String password,
                                  ObjectMapper mapper) {
        this.mapper = mapper;

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
                WHERE published_at IS NULL
                ORDER BY created_at
                """, outboxEventRowMapper());
    }

    private RowMapper<OutboxEvent> outboxEventRowMapper() {
        return (rs, rowNum) -> {

            OutboxEvent event = new OutboxEvent();

            event.setId(rs.getObject("id", UUID.class));
            event.setTenantId(rs.getObject("tenant_id", UUID.class));
            event.setAggregateType(rs.getString("aggregate_type"));
            event.setAggregateId(rs.getObject("aggregate_id", UUID.class));
            event.setEventType(EventType.valueOf(rs.getString("event_type")));
            Object payload = rs.getObject("payload");
            event.setPayload(parsePayload(payload));

            event.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            Timestamp published = rs.getTimestamp("published_at");
            if (published != null) {
                event.setPublishedAt(published.toInstant());
            }
            return event;

        };
    }

    private JsonNode parsePayload(Object payload) {

        if (payload == null) {
            return null;
        }

        try {
            return mapper.readTree(payload.toString());

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "Failed to parse outbox payload",
                    e
            );

        }

    }
}