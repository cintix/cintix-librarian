package dk.cintix.librarian.infrastructure.json;

public record JsonNumber(double value) implements JsonValue {
    @Override
    public String toString() { return String.valueOf(value); }
}
