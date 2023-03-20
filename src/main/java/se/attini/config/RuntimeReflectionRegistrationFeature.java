package se.attini.config;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import software.amazon.awssdk.services.sso.auth.SsoProfileCredentialsProviderFactory;

class RuntimeReflectionRegistrationFeature implements Feature {
    public void beforeAnalysis(BeforeAnalysisAccess access) {

        try {
            RuntimeReflection.register(SsoProfileCredentialsProviderFactory.class);
            RuntimeReflection.register(SsoProfileCredentialsProviderFactory.class.getDeclaredConstructor());
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "NoSuchMethodException was thrown when registering SsoProfileCredentialsProviderFactory constructor for reflection",
                    e);
        }


    }
}
