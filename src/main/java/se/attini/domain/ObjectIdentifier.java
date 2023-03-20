package se.attini.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class ObjectIdentifier {


    private final String value;

    private ObjectIdentifier(String value) {
        this.value = requireNonNull(value, "value");
    }

    public String getValue() {
        return value;
    }

    public static ObjectIdentifier create(String value){
        return new ObjectIdentifier(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectIdentifier that = (ObjectIdentifier) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ObjectIdentifier{" +
               "value='" + value + '\'' +
               '}';
    }
}
