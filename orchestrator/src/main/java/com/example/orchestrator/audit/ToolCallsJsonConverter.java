package com.example.orchestrator.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * JPA converter that serialises the {@link AuditEvent#getToolCalls()} list
 * to/from a JSON text column. Beats child-table modelling for this case
 * because:
 *   • the list is small (≤ ~10 entries per run)
 *   • the dashboard always consumes the whole list at once
 *   • no FK joins needed when rehydrating events on boot
 *
 * <p>Errors during serialise are swallowed to an empty list with a WARN
 * — losing the audit detail of one row is preferable to dying mid-save
 * on a malformed payload.
 */
@Converter(autoApply = false)
public class ToolCallsJsonConverter
        implements AttributeConverter<List<AuditEvent.ToolCallRecord>, String> {

    private static final Logger log = LoggerFactory.getLogger(ToolCallsJsonConverter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private static final TypeReference<List<AuditEvent.ToolCallRecord>> LIST_TR =
            new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<AuditEvent.ToolCallRecord> attribute) {
        if (attribute == null || attribute.isEmpty()) return "[]";
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            log.warn("Failed to serialise toolCalls to JSON, storing empty list: {}", e.toString());
            return "[]";
        }
    }

    @Override
    public List<AuditEvent.ToolCallRecord> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new CopyOnWriteArrayList<>();
        try {
            List<AuditEvent.ToolCallRecord> list = MAPPER.readValue(dbData, LIST_TR);
            return new CopyOnWriteArrayList<>(list);
        } catch (Exception e) {
            log.warn("Failed to parse toolCalls JSON, returning empty list: {}", e.toString());
            return new CopyOnWriteArrayList<>();
        }
    }
}
