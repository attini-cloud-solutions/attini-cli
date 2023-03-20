package se.attini.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class IamRoleArn {
    private final String value;
    private IamRoleArn(String value) {
       if (value== null || !value.startsWith("arn:aws:iam::")){
           throw new DomainConversionError(value + " is not a valid IAM arn");
       }
        this.value = requireNonNull(value, "value");
    }

    public String getValue() {
        return value;
    }

    public static IamRoleArn create(String value) {
        return new IamRoleArn(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IamRoleArn that = (IamRoleArn) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "IamRoleArn{" +
               "value='" + value + '\'' +
               '}';
    }
}
