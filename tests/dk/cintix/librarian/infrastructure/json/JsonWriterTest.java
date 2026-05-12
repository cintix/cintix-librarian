package dk.cintix.librarian.infrastructure.json;

import dk.cintix.librarian.Assert;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonWriterTest {

    public static void main(String[] args) {
        Assert.reset();
        System.out.println("=== JsonWriter Tests ===\n");

        testWriteString();
        testWriteNumber();
        testWriteBoolean();
        testWriteNull();
        testWriteEmptyObject();
        testWriteSimpleObject();
        testWriteNestedObject();
        testWriteEmptyArray();
        testWriteArray();
        testWriteCompact();
        testWritePretty();
        testRoundtripSimple();
        testRoundtripComplex();

        Assert.summary();
        if (!Assert.allPassed()) System.exit(1);
    }

    static void testWriteString() {
        Assert.setCurrentTest("testWriteString");
        String json = JsonWriter.write(new JsonString("hello"), false).trim();
        Assert.isTrue(json.contains("\"hello\""));
    }

    static void testWriteNumber() {
        Assert.setCurrentTest("testWriteNumber");
        String json = JsonWriter.write(new JsonNumber(42), false).trim();
        Assert.isTrue(json.contains("42"));
    }

    static void testWriteBoolean() {
        Assert.setCurrentTest("testWriteBoolean");
        String json = JsonWriter.write(new JsonBoolean(true), false).trim();
        Assert.isTrue(json.contains("true"));
    }

    static void testWriteNull() {
        Assert.setCurrentTest("testWriteNull");
        String json = JsonWriter.write(JsonNull.INSTANCE, false).trim();
        Assert.isTrue(json.contains("null"));
    }

    static void testWriteEmptyObject() {
        Assert.setCurrentTest("testWriteEmptyObject");
        String json = JsonWriter.write(new JsonObject(Map.of()), false).trim();
        Assert.isTrue(json.contains("{}"));
    }

    static void testWriteSimpleObject() {
        Assert.setCurrentTest("testWriteSimpleObject");
        Map<String, JsonValue> map = new LinkedHashMap<>();
        map.put("key", new JsonString("value"));
        String json = JsonWriter.write(new JsonObject(map), false).trim();
        Assert.isTrue(json.contains("\"key\""));
        Assert.isTrue(json.contains("\"value\""));
    }

    static void testWriteNestedObject() {
        Assert.setCurrentTest("testWriteNestedObject");
        Map<String, JsonValue> inner = new LinkedHashMap<>();
        inner.put("a", new JsonNumber(1));
        Map<String, JsonValue> outer = new LinkedHashMap<>();
        outer.put("inner", new JsonObject(inner));
        String json = JsonWriter.write(new JsonObject(outer), false).trim();
        Assert.isTrue(json.contains("\"inner\""));
    }

    static void testWriteEmptyArray() {
        Assert.setCurrentTest("testWriteEmptyArray");
        String json = JsonWriter.write(new JsonArray(List.of()), false).trim();
        Assert.isTrue(json.contains("[]"));
    }

    static void testWriteArray() {
        Assert.setCurrentTest("testWriteArray");
        JsonArray arr = new JsonArray(List.of(new JsonNumber(1), new JsonNumber(2)));
        String json = JsonWriter.write(arr, false).trim();
        Assert.isTrue(json.contains("[1,2]"));
    }

    static void testWriteCompact() {
        Assert.setCurrentTest("testWriteCompact");
        Map<String, JsonValue> map = new LinkedHashMap<>();
        map.put("a", new JsonNumber(1));
        String json = JsonWriter.write(new JsonObject(map), false).trim();
        Assert.isFalse(json.contains("\n"));
    }

    static void testWritePretty() {
        Assert.setCurrentTest("testWritePretty");
        Map<String, JsonValue> map = new LinkedHashMap<>();
        map.put("a", new JsonNumber(1));
        String json = JsonWriter.write(new JsonObject(map), true).trim();
        Assert.isTrue(json.contains("\n"));
    }

    static void testRoundtripSimple() {
        Assert.setCurrentTest("testRoundtripSimple");
        String original = "{\"key\":\"value\"}";
        JsonValue parsed = JsonParser.parse(original);
        String written = JsonWriter.write(parsed, false).trim();
        JsonValue reparsed = JsonParser.parse(written);
        Assert.equal("value", reparsed.asObject().getString("key"));
    }

    static void testRoundtripComplex() {
        Assert.setCurrentTest("testRoundtripComplex");
        String original = "{\"name\":\"test\",\"items\":[1,2,3]}";
        JsonValue parsed = JsonParser.parse(original);
        String written = JsonWriter.write(parsed, false).trim();
        JsonValue reparsed = JsonParser.parse(written);
        Assert.equal("test", reparsed.asObject().getString("name"));
    }
}
