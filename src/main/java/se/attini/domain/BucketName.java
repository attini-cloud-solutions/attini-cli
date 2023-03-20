package se.attini.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class BucketName {


    private final String name;

    private BucketName(String name) {
        this.name = requireNonNull(name, "name");
    }

    public String getName() {
        return name;
    }

    public static BucketName create(String name){
        return new BucketName(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BucketName that = (BucketName) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "BucketName{" +
               "name='" + name + '\'' +
               '}';
    }
}
