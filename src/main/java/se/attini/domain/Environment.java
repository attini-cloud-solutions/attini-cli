package se.attini.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;

@Introspected
@ReflectiveAccess
public class Environment {

    private final EnvironmentName name;
    private final EnvironmentType environmentType;

    private Environment(EnvironmentName name, EnvironmentType type) {
        this.name = requireNonNull(name, "name");
        this.environmentType = requireNonNull(type, "type");
    }

    public EnvironmentName getName() {
        return name;
    }

    public EnvironmentType getType() {
        return environmentType;
    }

    public static Environment create(EnvironmentName name, EnvironmentType type){
        return new Environment(name, type);
    }

    @Override
    public String toString() {
        return "Environment{" +
               "name='" + name + '\'' +
               ", type=" + environmentType +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Environment that = (Environment) o;
        return Objects.equals(name, that.name) && environmentType == that.environmentType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, environmentType);
    }
}
