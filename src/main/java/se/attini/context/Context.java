package se.attini.context;

import java.util.List;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;

@Introspected
@ReflectiveAccess
public class Context {
    public final String attiniVersion;
    public final String account;
    public final String user;
    public final String region;
    public final List<EnvironmentContext> environments;

    private Context(Builder builder) {
        this.account = builder.account;
        this.user = builder.user;
        this.region = builder.region;
        this.environments = builder.environments;
        this.attiniVersion = builder.attiniVersion;
    }

    public static Builder builder() {
        return new Builder();
    }


    public String getAccount() {
        return account;
    }

    public String getUser() {
        return user;
    }

    public String getRegion() {
        return region;
    }

    public String getAttiniVersion() {
        return attiniVersion;
    }

    public List<EnvironmentContext> getEnvironments() {
        return environments;
    }

    public static class Builder {
        private String account;
        private String user;
        private String region;
        private String attiniVersion;
        private List<EnvironmentContext> environments;

        private Builder() {
        }

        public Builder setAccount(String account) {
            this.account = account;
            return this;
        }

        public Builder setUser(String user) {
            this.user = user;
            return this;
        }

        public Builder setRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder setAttiniVersion(String attiniVersion){
            this.attiniVersion = attiniVersion;
            return this;
        }

        public Builder setEnvironments(List<EnvironmentContext> environments) {
            this.environments = environments;
            return this;
        }

        public Builder of(Context context) {
            this.account = context.account;
            this.user = context.user;
            this.region = context.region;
            this.environments = context.environments;
            this.attiniVersion = context.attiniVersion;
            return this;
        }

        public Context build() {
            return new Context(this);
        }
    }


    @Override
    public String toString() {
        return "Context{" +
               "attiniVersion='" + attiniVersion + '\'' +
               ", account='" + account + '\'' +
               ", user='" + user + '\'' +
               ", region='" + region + '\'' +
               ", environments=" + environments +
               '}';
    }
}
