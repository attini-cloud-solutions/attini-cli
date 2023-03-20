package se.attini.domain;

import com.fasterxml.jackson.annotation.JsonValue;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;

@Introspected
@ReflectiveAccess
public enum EnvironmentType {
    PRODUCTION("production"), TEST("test");

    private final String value;

    EnvironmentType(String value) {
        this.value = value;
    }

    public static EnvironmentType fromString(String value) {
       return EnvironmentType.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
