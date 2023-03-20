package se.attini.deployment;

import static java.util.Objects.requireNonNull;

import se.attini.AwsAccountFacade;
import se.attini.domain.BucketName;
import se.attini.profile.ProfileFacade;


public class DeploymentOrigin {
    private final ProfileFacade profileFacade;
    private final AwsAccountFacade awsAccountFacade;

    public DeploymentOrigin(ProfileFacade profileFacade,
                            AwsAccountFacade awsAccountFacade) {
        this.profileFacade = requireNonNull(profileFacade, "profileFacade");
        this.awsAccountFacade = requireNonNull(awsAccountFacade, "awsAccountFacade");
    }

    public BucketName getDeploymentOriginBucketName() {
        return BucketName.create("attini-deployment-origin-%s-%s".formatted(profileFacade.getRegion().getName(),
                                                                            awsAccountFacade.getAccount()));

    }
}
