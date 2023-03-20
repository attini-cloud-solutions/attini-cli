package se.attini.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class DistributionName {

    private final String name;

    private DistributionName(String name) {
        this.name = requireNonNull(name, "name");
    }

    public String getName() {
        return name;
    }

    public static DistributionName create(String name){
        return new DistributionName(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DistributionName that = (DistributionName) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "DistributionName{" +
               "name='" + name + '\'' +
               '}';
    }
}
