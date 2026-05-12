package dk.cintix.librarian.infrastructure.json;

import java.util.ArrayList;
import java.util.List;

public final class JsonArray implements JsonValue {
    private final List<JsonValue> values;

    public JsonArray(List<JsonValue> values) {
        this.values = new ArrayList<>(values);
    }

    public List<JsonValue> values() { return values; }

    public int size() { return values.size(); }

    @Override
    public String toString() { return values.toString(); }
}
