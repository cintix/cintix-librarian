package dk.cintix.librarian.infrastructure.json;

import java.util.List;
import java.util.Map;

public sealed interface JsonValue permits JsonObject, JsonArray, JsonString, JsonNumber, JsonBoolean, JsonNull {

    JsonNull NULL = JsonNull.INSTANCE;

    static JsonObject of(Map<String, JsonValue> map) { return new JsonObject(map); }
    static JsonArray of(List<JsonValue> list) { return new JsonArray(list); }
    static JsonString of(String value) { return new JsonString(value); }
    static JsonNumber of(double value) { return new JsonNumber(value); }
    static JsonBoolean of(boolean value) { return new JsonBoolean(value); }

    default JsonObject asObject() { return (JsonObject) this; }
    default JsonArray asArray() { return (JsonArray) this; }
    default String asString() { return ((JsonString) this).value(); }
    default double asNumber() { return ((JsonNumber) this).value(); }
    default boolean asBoolean() { return ((JsonBoolean) this).value(); }
    default boolean isNull() { return this == JsonNull.INSTANCE; }
}
