package dk.cintix.librarian.infrastructure.json;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonObject implements JsonValue {
    private final Map<String, JsonValue> values;

    public JsonObject(Map<String, JsonValue> values) {
        this.values = new LinkedHashMap<>(values);
    }

    public Map<String, JsonValue> values() { return values; }

    public JsonValue get(String key) { return values.get(key); }

    public String getString(String key) {
        JsonValue v = values.get(key);
        return v != null && !v.isNull() ? v.asString() : null;
    }

    public Boolean getBoolean(String key) {
        JsonValue v = values.get(key);
        return v != null && !v.isNull() ? v.asBoolean() : null;
    }

    public JsonObject getObject(String key) {
        JsonValue v = values.get(key);
        return v instanceof JsonObject obj ? obj : null;
    }

    public Double getNumber(String key) {
        JsonValue v = values.get(key);
        return v != null && !v.isNull() ? v.asNumber() : null;
    }

    public int size() { return values.size(); }

    @Override
    public String toString() { return values.toString(); }
}
