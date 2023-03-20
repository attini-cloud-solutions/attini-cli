package se.attini.removestack;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import se.attini.domain.Region;
import se.attini.domain.StackName;

public class RemoveStackResourceRequest  {

    private final StackName stackName;
    private final Region stackRegion;
    private final String accountId;

    private final boolean deleteStack;

    private RemoveStackResourceRequest(Builder builder) {
        this.stackName = requireNonNull(builder.stackName, "stackName");
        this.stackRegion = builder.stackRegion;
        this.accountId = builder.accountId;
        this.deleteStack = builder.deleteStack;
    }

    public static Builder builder() {
        return new Builder();
    }

    public StackName getStackName() {
        return stackName;
    }

    public Optional<Region> getStackRegion() {
        return Optional.ofNullable(stackRegion);
    }

    public Optional<String> getAccountId() {
        return Optional.ofNullable(accountId);
    }

    public boolean isDeleteStack() {
        return deleteStack;
    }

    public static class Builder {
        private StackName stackName;
        private Region stackRegion;
        private String accountId;
        private boolean deleteStack;

        private Builder() {
        }

        public Builder setStackName(StackName stackName) {
            this.stackName = stackName;
            return this;
        }

        public Builder setStackRegion(Region stackRegion) {
            this.stackRegion = stackRegion;
            return this;
        }

        public Builder setAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder setDeleteStack(boolean deleteStack) {
            this.deleteStack = deleteStack;
            return this;
        }


        public RemoveStackResourceRequest build() {
            return new RemoveStackResourceRequest(this);
        }
    }
}
