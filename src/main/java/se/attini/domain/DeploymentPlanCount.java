package se.attini.domain;

import java.util.Objects;

public class DeploymentPlanCount {

   private final int value;

    private DeploymentPlanCount(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static DeploymentPlanCount create(String value){
        try {
            return new DeploymentPlanCount(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("A deployment plan count has to be a number");
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeploymentPlanCount that = (DeploymentPlanCount) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "DeploymentPlanCount{" +
               "value=" + value +
               '}';
    }
}
