package se.attini.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class DistributionId {

    private final String id;

    public final static DistributionId NOT_RESOLVABLE = DistributionId.create("not_defined");

    private DistributionId(String id) {
        this.id = requireNonNull(id, "id");
    }

    public String getId() {
        return id;
    }

    public static DistributionId create(String id){
        return new DistributionId(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DistributionId that = (DistributionId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DistributionId{" +
               "id='" + id + '\'' +
               '}';
    }
}
