package com.alssant.asclepio.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Component;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class OutboxEventRowMapper extends BeanPropertyRowMapper<OutboxEvent> {
    private static final String PAYLOAD = "payload";
    private final ObjectMapper mapper;

    public OutboxEventRowMapper(ObjectMapper mapper) {
        setMappedClass(OutboxEvent.class);
        this.mapper = mapper;
    }

    @Override
    protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor pd) throws SQLException {
        if (PAYLOAD.equalsIgnoreCase(pd.getName())) {
            return parsePayload(rs.getString(index));
        }

        return super.getColumnValue(rs, index, pd);
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
