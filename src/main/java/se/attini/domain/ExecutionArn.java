package se.attini.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class ExecutionArn {

    private final String value;

    private ExecutionArn(String id) {
        this.value = requireNonNull(id, "value");
    }

    public String getValue() {
        return value;
    }

    public String getSfnArn(){
        return value.substring(0,value.lastIndexOf(":")).replace(":execution:", ":stateMachine:");
    }
    public static ExecutionArn create(String value){
        return new ExecutionArn(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionArn that = (ExecutionArn) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ExecutionArn{" +
               "id='" + value + '\'' +
               '}';
    }
}
