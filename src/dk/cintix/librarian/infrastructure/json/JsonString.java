package dk.cintix.librarian.infrastructure.json;

public record JsonString(String value) implements JsonValue {
    @Override
    public String toString() { return '"' + value + '"'; }
}
