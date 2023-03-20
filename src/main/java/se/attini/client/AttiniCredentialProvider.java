package se.attini.client;

import static software.amazon.awssdk.profiles.ProfileProperty.SSO_ACCOUNT_ID;
import static software.amazon.awssdk.profiles.ProfileProperty.SSO_REGION;
import static software.amazon.awssdk.profiles.ProfileProperty.SSO_ROLE_NAME;
import static software.amazon.awssdk.utils.UserHomeDirectoryUtils.userHomeDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.services.sso.SsoClient;
import software.amazon.awssdk.services.sso.auth.SsoCredentialsProvider;
import software.amazon.awssdk.services.sso.internal.SsoTokenFileUtils;
import software.amazon.awssdk.services.sso.model.GetRoleCredentialsRequest;

class AttiniCredentialProvider implements AwsCredentialsProvider {

    private static final DefaultCredentialsProvider DEFAULT_PROVIDER = DefaultCredentialsProvider.create();

    private static SsoCredentialsProvider SSO_CREDENTIALS_PROVIDER;
    private static final String TOKEN_DIRECTORY = Paths.get(userHomeDirectory(), ".aws", "sso", "cache").toString();


    public static AttiniCredentialProvider create() {
        return new AttiniCredentialProvider();
    }


    private AttiniCredentialProvider() {
    }

    @Override
    public AwsCredentials resolveCredentials() {

        try {
            return DEFAULT_PROVIDER.resolveCredentials();

        } catch (SdkClientException originalException) {
            try {
                return getSSOProvider().resolveCredentials();
            } catch (Exception newException) {
                // if it does not work, no one as to know....
                throw originalException;
            }

        }
    }

    private SsoCredentialsProvider getSsoCredentialsProvider(software.amazon.awssdk.profiles.Profile profile,
                                                             SsoClient ssoClient) {

        Path tokenFilePath = SsoTokenFileUtils.generateCachedTokenPath(profile.properties()
                                                                              .get(ProfileProperty.SSO_START_URL),
                                                                       TOKEN_DIRECTORY);

        GetRoleCredentialsRequest getRoleCredentialsRequest =
                GetRoleCredentialsRequest.builder()
                                         .roleName(profile.property(SSO_ROLE_NAME)
                                                          .orElseThrow(missingSsoPropertyException(SSO_ROLE_NAME)))
                                         .accountId(profile.property(SSO_ACCOUNT_ID)
                                                           .orElseThrow(missingSsoPropertyException(SSO_ACCOUNT_ID)))
                                         .accessToken(getAccessToken(tokenFilePath))
                                         .build();


        //Set to variable with the public interface as the type because the builder method returns the
        // protected class implementation
        SsoCredentialsProvider.Builder builder = SsoCredentialsProvider.builder();

        return builder.ssoClient(
                              ssoClient)
                      .refreshRequest(getRoleCredentialsRequest)
                      .build();
    }

    private static String getAccessToken(Path tokenFilePath) {
        try {
            return new ObjectMapper().readTree(tokenFilePath.toFile()).path("accessToken").textValue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private SsoCredentialsProvider getSSOProvider() {
        if (SSO_CREDENTIALS_PROVIDER == null) {
            software.amazon.awssdk.profiles.Profile profile = ProfileFile.defaultProfileFile()
                                                                         .profiles()
                                                                         .get(ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow());


            SSO_CREDENTIALS_PROVIDER = getSsoCredentialsProvider(profile, createSsoClient(profile));
        }

        return SSO_CREDENTIALS_PROVIDER;
    }

    private SsoClient createSsoClient(software.amazon.awssdk.profiles.Profile profile) {
        return SsoClient.builder()
                        .credentialsProvider(AnonymousCredentialsProvider.create())
                        .region(software.amazon.awssdk.regions.Region.of(profile.property(SSO_REGION).orElseThrow(
                                missingSsoPropertyException(SSO_REGION))))
                        .build();
    }

    private static Supplier<IllegalArgumentException> missingSsoPropertyException(String property) {
        return () -> new IllegalArgumentException("%s is missing from profile".formatted(property));
    }
}
