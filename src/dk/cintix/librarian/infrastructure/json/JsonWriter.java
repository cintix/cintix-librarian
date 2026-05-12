package dk.cintix.librarian.infrastructure.json;

import java.util.Map;

public final class JsonWriter {
    private final StringBuilder sb;
    private final int indent;
    private int level;

    private JsonWriter(int indent) {
        this.sb = new StringBuilder();
        this.indent = indent;
        this.level = 0;
    }

    public static String write(JsonValue value, boolean pretty) {
        JsonWriter writer = new JsonWriter(pretty ? 2 : 0);
        writer.writeValue(value);
        writer.sb.append('\n');
        return writer.sb.toString();
    }

    private void writeValue(JsonValue value) {
        if (value instanceof JsonObject obj) {
            writeObject(obj);
        } else if (value instanceof JsonArray arr) {
            writeArray(arr);
        } else if (value instanceof JsonString str) {
            writeString(str.value());
        } else if (value instanceof JsonNumber num) {
            sb.append(formatNumber(num.value()));
        } else if (value instanceof JsonBoolean bool) {
            sb.append(bool.value());
        } else if (value.isNull()) {
            sb.append("null");
        }
    }

    private void writeObject(JsonObject obj) {
        sb.append('{');
        if (obj.values().isEmpty()) {
            sb.append('}');
            return;
        }
        level++;
        boolean first = true;
        for (Map.Entry<String, JsonValue> entry : obj.values().entrySet()) {
            if (!first) sb.append(',');
            first = false;
            newLine();
            writeString(entry.getKey());
            sb.append(indent > 0 ? ": " : ":");
            writeValue(entry.getValue());
        }
        level--;
        newLine();
        sb.append('}');
    }

    private void writeArray(JsonArray arr) {
        sb.append('[');
        if (arr.values().isEmpty()) {
            sb.append(']');
            return;
        }
        level++;
        boolean first = true;
        for (JsonValue value : arr.values()) {
            if (!first) sb.append(',');
            first = false;
            newLine();
            writeValue(value);
        }
        level--;
        newLine();
        sb.append(']');
    }

    private void writeString(String value) {
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\t' -> sb.append("\\t");
                case '\r' -> sb.append("\\r");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> sb.append(c);
            }
        }
        sb.append('"');
    }

    private void newLine() {
        if (indent > 0) {
            sb.append('\n');
            sb.append(" ".repeat(level * indent));
        }
    }

    private static String formatNumber(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
