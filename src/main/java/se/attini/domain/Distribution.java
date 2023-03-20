package se.attini.domain;

import java.util.Objects;

public class Distribution {
    private final DistributionName distributionName;
    private final DistributionId distributionId;

    private Distribution(Builder builder) {
        this.distributionName = builder.distributionName;
        this.distributionId = builder.distributionId;
    }

    public static Builder builder() {
        return new Builder();
    }


    @Override
    public String toString() {
        return "Distribution{" +
               "distributionName=" + distributionName +
               ", distributionId=" + distributionId +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Distribution that = (Distribution) o;
        return Objects.equals(distributionName, that.distributionName) &&
               Objects.equals(distributionId, that.distributionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distributionName, distributionId);
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public DistributionId getDistributionId() {
        return distributionId;
    }

    public static class Builder {
        private DistributionName distributionName;
        private DistributionId distributionId;

        private Builder() {
        }

        public Builder setDistributionName(DistributionName distributionName) {
            this.distributionName = distributionName;
            return this;
        }


        public Builder setDistributionId(DistributionId distributionId) {
            this.distributionId = distributionId;
            return this;
        }

        public Builder of(Distribution distribution) {
            this.distributionName = distribution.distributionName;
            this.distributionId = distribution.distributionId;
            return this;
        }

        public Distribution build() {
            return new Distribution(this);
        }
    }
}
