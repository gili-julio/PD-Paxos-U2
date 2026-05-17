package br.ufrn.middleware.identification;

import java.util.Objects;

/**
 * Object Id (Identification Pattern). Stable logical identifier of a Remote Object,
 * independent of its physical location or lifecycle.
 */
public final class ObjectId {
    private final String value;

    private ObjectId(String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("ObjectId cannot be blank");
        this.value = value;
    }

    public static ObjectId of(String value) { return new ObjectId(value); }

    public String value() { return value; }

    @Override public boolean equals(Object o) {
        return o instanceof ObjectId oid && oid.value.equals(value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value; }
}
