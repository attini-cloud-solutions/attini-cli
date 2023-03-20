package se.attini.setup;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import se.attini.client.AwsClientFactory;
import se.attini.profile.ProfileFacade;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

public class SetupVersionsService {

    private final AwsClientFactory awsClientFactory;
    private final ProfileFacade profileFacade;

    public SetupVersionsService(AwsClientFactory awsClientFactory, ProfileFacade profileFacade) {
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
        this.profileFacade = requireNonNull(profileFacade, "profileFacade");
    }

    public List<String> getSetupVersions() {

        try (S3Client s3Client = awsClientFactory.s3Client()) {
            return s3Client.listObjectsV2(ListObjectsV2Request.builder()
                                                              .bucket("attini-artifacts-" + profileFacade.getRegion().getName())
                                                               .build())
                           .contents()
                           .stream()
                           .map(S3Object::key)
                           .map(s -> s.split("/")[1])
                           .distinct()
                           .filter(s -> !s.equals("latest"))
                           .sorted(sortVersions())
                           .limit(10)
                           .collect(Collectors.toList());
        }
    }

    private Comparator<String> sortVersions() {
        return (o1, o2) -> {
            int o1Number = Integer.parseInt(o1.replace(".", ""));
            int o2Number = Integer.parseInt(o2.replace(".", ""));
            return o2Number - o1Number;
        };
    }
}
