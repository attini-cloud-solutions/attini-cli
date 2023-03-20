package se.attini.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

public class EnvironmentName {

    private final String name;

    private EnvironmentName(String name) {
        this.name = requireNonNull(name, "name");
    }

    @JsonValue
    public String getName() {
        return name;
    }

    public static EnvironmentName create(String name){
        return new EnvironmentName(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnvironmentName environmentName1 = (EnvironmentName) o;
        return Objects.equals(name, environmentName1.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "EnvironmentName{" +
               "environmentName='" + name + '\'' +
               '}';
    }
}
