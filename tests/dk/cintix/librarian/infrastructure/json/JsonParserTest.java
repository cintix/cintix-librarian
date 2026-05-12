package dk.cintix.librarian.infrastructure.json;

import dk.cintix.librarian.Assert;

public final class JsonParserTest {

    public static void main(String[] args) {
        Assert.reset();
        System.out.println("=== JsonParser Tests ===\n");

        testParseString();
        testParseNumber();
        testParseBoolean();
        testParseNull();
        testParseEmptyObject();
        testParseSimpleObject();
        testParseNestedObject();
        testParseEmptyArray();
        testParseSimpleArray();
        testParseNestedArray();
        testParseComplexObject();
        testParseStringWithEscapes();
        testParseRealWorldConfig();
        testParseErrorUnexpectedChar();
        testParseErrorUnterminatedString();
        testParseErrorTrailingContent();
        testParseErrorInvalidToken();

        Assert.summary();
        if (!Assert.allPassed()) System.exit(1);
    }

    static void testParseString() {
        Assert.setCurrentTest("testParseString");
        JsonValue v = JsonParser.parse("\"hello\"");
        Assert.isTrue(v instanceof JsonString);
        Assert.equal("hello", v.asString());
    }

    static void testParseNumber() {
        Assert.setCurrentTest("testParseNumber");
        JsonValue v = JsonParser.parse("42");
        Assert.isTrue(v instanceof JsonNumber);
        Assert.equal(42.0, v.asNumber(), 0.001);
    }

    static void testParseBoolean() {
        Assert.setCurrentTest("testParseBoolean");
        JsonValue t = JsonParser.parse("true");
        Assert.isTrue(t instanceof JsonBoolean);
        Assert.isTrue(t.asBoolean());
        JsonValue f = JsonParser.parse("false");
        Assert.isFalse(f.asBoolean());
    }

    static void testParseNull() {
        Assert.setCurrentTest("testParseNull");
        JsonValue v = JsonParser.parse("null");
        Assert.isTrue(v.isNull());
    }

    static void testParseEmptyObject() {
        Assert.setCurrentTest("testParseEmptyObject");
        JsonValue v = JsonParser.parse("{}");
        Assert.isTrue(v instanceof JsonObject);
        Assert.equal(0, ((JsonObject) v).size());
    }

    static void testParseSimpleObject() {
        Assert.setCurrentTest("testParseSimpleObject");
        JsonValue v = JsonParser.parse("{\"key\":\"value\"}");
        Assert.isTrue(v instanceof JsonObject);
        JsonObject obj = (JsonObject) v;
        Assert.equal("value", obj.getString("key"));
    }

    static void testParseNestedObject() {
        Assert.setCurrentTest("testParseNestedObject");
        JsonValue v = JsonParser.parse("{\"outer\":{\"inner\":123}}");
        JsonObject obj = v.asObject();
        JsonObject inner = obj.getObject("outer");
        Assert.notNull(inner);
        Assert.equal(123.0, inner.getNumber("inner"), 0.001);
    }

    static void testParseEmptyArray() {
        Assert.setCurrentTest("testParseEmptyArray");
        JsonValue v = JsonParser.parse("[]");
        Assert.isTrue(v instanceof JsonArray);
        Assert.equal(0, ((JsonArray) v).size());
    }

    static void testParseSimpleArray() {
        Assert.setCurrentTest("testParseSimpleArray");
        JsonValue v = JsonParser.parse("[1,2,3]");
        Assert.equal(3, ((JsonArray) v).size());
    }

    static void testParseNestedArray() {
        Assert.setCurrentTest("testParseNestedArray");
        JsonValue v = JsonParser.parse("[[1,2],[3,4]]");
        Assert.equal(2, ((JsonArray) v).size());
    }

    static void testParseComplexObject() {
        Assert.setCurrentTest("testParseComplexObject");
        JsonValue v = JsonParser.parse("{\"name\":\"test\",\"count\":5,\"items\":[1,2]}");
        JsonObject obj = v.asObject();
        Assert.equal("test", obj.getString("name"));
        Assert.equal(5.0, obj.getNumber("count"), 0.001);
    }

    static void testParseStringWithEscapes() {
        Assert.setCurrentTest("testParseStringWithEscapes");
        JsonValue v = JsonParser.parse("\"line1\\nline2\\tindented\"");
        Assert.isTrue(v.asString().contains("\n"));
        Assert.isTrue(v.asString().contains("\t"));
    }

    static void testParseRealWorldConfig() {
        Assert.setCurrentTest("testParseRealWorldConfig");
        JsonValue v = JsonParser.parse("{\"libDir\":\"lib\",\"dependencies\":{\"a:b\":\"1.0\"}}");
        JsonObject obj = v.asObject();
        Assert.equal("lib", obj.getString("libDir"));
        Assert.notNull(obj.getObject("dependencies"));
    }

    static void testParseErrorUnexpectedChar() {
        Assert.setCurrentTest("testParseErrorUnexpectedChar");
        Assert.throwsException(JsonParseException.class, () -> JsonParser.parse("@"));
    }

    static void testParseErrorUnterminatedString() {
        Assert.setCurrentTest("testParseErrorUnterminatedString");
        Assert.throwsException(JsonParseException.class, () -> JsonParser.parse("\"unterminated"));
    }

    static void testParseErrorTrailingContent() {
        Assert.setCurrentTest("testParseErrorTrailingContent");
        Assert.throwsException(JsonParseException.class, () -> JsonParser.parse("{}garbage"));
    }

    static void testParseErrorInvalidToken() {
        Assert.setCurrentTest("testParseErrorInvalidToken");
        Assert.throwsException(JsonParseException.class, () -> JsonParser.parse("xyz"));
    }
}
