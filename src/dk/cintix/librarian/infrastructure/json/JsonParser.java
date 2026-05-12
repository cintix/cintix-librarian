package dk.cintix.librarian.infrastructure.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonParser {
    private final String input;
    private int pos;

    private JsonParser(String input) {
        this.input = input;
        this.pos = 0;
    }

    public static JsonValue parse(String input) {
        JsonParser parser = new JsonParser(input);
        JsonValue value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos < parser.input.length()) {
            throw new JsonParseException("Unexpected trailing content at position " + parser.pos);
        }
        return value;
    }

    private JsonValue readValue() {
        skipWhitespace();
        if (pos >= input.length()) {
            throw new JsonParseException("Unexpected end of input");
        }
        char c = input.charAt(pos);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't', 'f' -> readBoolean();
            case 'n' -> readNull();
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) {
                    yield readNumber();
                }
                throw new JsonParseException("Unexpected character '" + c + "' at position " + pos);
            }
        };
    }

    private JsonObject readObject() {
        expect('{');
        skipWhitespace();
        Map<String, JsonValue> map = new LinkedHashMap<>();
        if (peek() == '}') {
            pos++;
            return new JsonObject(map);
        }
        while (true) {
            skipWhitespace();
            JsonString key = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            JsonValue value = readValue();
            map.put(key.value(), value);
            skipWhitespace();
            char c = input.charAt(pos);
            if (c == '}') {
                pos++;
                break;
            }
            if (c == ',') {
                pos++;
            } else {
                throw new JsonParseException("Expected ',' or '}' at position " + pos);
            }
        }
        return new JsonObject(map);
    }

    private JsonArray readArray() {
        expect('[');
        skipWhitespace();
        List<JsonValue> list = new ArrayList<>();
        if (peek() == ']') {
            pos++;
            return new JsonArray(list);
        }
        while (true) {
            skipWhitespace();
            list.add(readValue());
            skipWhitespace();
            char c = input.charAt(pos);
            if (c == ']') {
                pos++;
                break;
            }
            if (c == ',') {
                pos++;
            } else {
                throw new JsonParseException("Expected ',' or ']' at position " + pos);
            }
        }
        return new JsonArray(list);
    }

    private JsonString readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '"') {
                pos++;
                return new JsonString(sb.toString());
            }
            if (c == '\\') {
                pos++;
                if (pos >= input.length()) throw new JsonParseException("Unexpected end of input in escape");
                char escaped = input.charAt(pos);
                switch (escaped) {
                    case '"', '\\', '/' -> sb.append(escaped);
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (pos + 4 >= input.length())
                            throw new JsonParseException("Unexpected end of input in unicode escape");
                        String hex = input.substring(pos + 1, pos + 5);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                    }
                    default -> throw new JsonParseException("Invalid escape: \\" + escaped);
                }
            } else {
                sb.append(c);
            }
            pos++;
        }
        throw new JsonParseException("Unterminated string");
    }

    private JsonBoolean readBoolean() {
        if (input.startsWith("true", pos)) {
            pos += 4;
            return new JsonBoolean(true);
        }
        if (input.startsWith("false", pos)) {
            pos += 5;
            return new JsonBoolean(false);
        }
        throw new JsonParseException("Invalid token at position " + pos);
    }

    private JsonNull readNull() {
        if (input.startsWith("null", pos)) {
            pos += 4;
            return JsonNull.INSTANCE;
        }
        throw new JsonParseException("Invalid token at position " + pos);
    }

    private JsonNumber readNumber() {
        int start = pos;
        if (peek() == '-') pos++;
        while (pos < input.length() && (input.charAt(pos) >= '0' && input.charAt(pos) <= '9')) pos++;
        if (pos < input.length() && input.charAt(pos) == '.') {
            pos++;
            while (pos < input.length() && (input.charAt(pos) >= '0' && input.charAt(pos) <= '9')) pos++;
        }
        if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
            pos++;
            if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) pos++;
            while (pos < input.length() && (input.charAt(pos) >= '0' && input.charAt(pos) <= '9')) pos++;
        }
        return new JsonNumber(Double.parseDouble(input.substring(start, pos)));
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
    }

    private char peek() {
        skipWhitespace();
        return pos < input.length() ? input.charAt(pos) : 0;
    }

    private void expect(char c) {
        if (pos >= input.length() || input.charAt(pos) != c) {
            throw new JsonParseException("Expected '" + c + "' at position " + pos);
        }
        pos++;
    }
}
