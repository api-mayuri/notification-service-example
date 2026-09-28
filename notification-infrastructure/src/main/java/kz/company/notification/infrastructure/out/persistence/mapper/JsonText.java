package kz.company.notification.infrastructure.out.persistence.mapper;

import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/** Словари в текстовых колонках в виде JSON. */
@Component
public class JsonText {

    private static final TypeReference<LinkedHashMap<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };
    private static final TypeReference<LinkedHashMap<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final JsonMapper jsonMapper;

    public JsonText(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public String write(Map<String, ?> value) {
        return value == null || value.isEmpty() ? null : jsonMapper.writeValueAsString(value);
    }

    public Map<String, Object> readObjectMap(String json) {
        return json == null || json.isBlank() ? Map.of() : jsonMapper.readValue(json, OBJECT_MAP);
    }

    public Map<String, String> readStringMap(String json) {
        return json == null || json.isBlank() ? Map.of() : jsonMapper.readValue(json, STRING_MAP);
    }
}
