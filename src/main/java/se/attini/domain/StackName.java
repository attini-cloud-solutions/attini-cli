package se.attini.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class StackName {

    private final String name;

    private StackName(String name) {
        this.name = requireNonNull(name, "name");
    }

    public String getName() {
        return name;
    }

    public static StackName create(String name){
        return new StackName(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StackName that = (StackName) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "StackName{" +
               "name='" + name + '\'' +
               '}';
    }
}
