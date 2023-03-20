package se.attini.deployment;

import static java.util.Objects.requireNonNull;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;

import se.attini.AwsAccountFacade;
import se.attini.client.AwsClientFactory;
import se.attini.domain.DistributionId;
import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;
import se.attini.domain.ExecutionArn;
import se.attini.domain.Region;
import se.attini.profile.ProfileFacade;

public class StepLoggerFactory {


    private final AwsClientFactory awsClientFactory;
    private final ProfileFacade profileFacade;
    private final AwsAccountFacade awsAccountFacade;

    private final ObjectMapper objectMapper;

    public StepLoggerFactory(AwsClientFactory awsClientFactory,
                             ProfileFacade profileFacade,
                             AwsAccountFacade awsAccountFacade) {
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
        this.profileFacade = profileFacade;
        this.awsAccountFacade = awsAccountFacade;
        this.objectMapper = new ObjectMapper();
    }

    public StepLogger getLogger(GetLoggerRequest getLoggerRequest,
                                String stepName) {
        if ("AttiniRunnerJob".equals(getLoggerRequest.type()) || "AttiniCdk".equals(getLoggerRequest.type())) {
            Region givenRegion = profileFacade.getRegion();
            String account = awsAccountFacade.getAccount();
            return new AttiniRunnerLogger(awsClientFactory.s3Client(),
                                          getLoggerRequest, stepName, account, givenRegion, objectMapper);
        }


        return Collections::emptyList;

    }

    public record GetLoggerRequest(DistributionName distributionName,
                                   DistributionId distributionId,
                                   EnvironmentName environment,
                                   ExecutionArn executionArn,
                                   String type) {

    }
}
