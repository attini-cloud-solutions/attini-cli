package se.attini.deployment;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import se.attini.AwsAccountFacade;
import se.attini.client.AwsClientFactory;
import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;
import se.attini.environment.EnvironmentUserInput;
import se.attini.profile.ProfileFacade;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

public class ContinueDeploymentService {


    private final AwsClientFactory awsClientFactory;
    private final AwsAccountFacade awsAccountFacade;
    private final ProfileFacade profileFacade;
    private final EnvironmentUserInput environmentUserInput;

    public ContinueDeploymentService(AwsClientFactory awsClientFactory,
                                     AwsAccountFacade awsAccountFacade,
                                     ProfileFacade profileFacade,
                                     EnvironmentUserInput environmentUserInput) {
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
        this.awsAccountFacade = requireNonNull(awsAccountFacade, "awsAccountFacade");
        this.profileFacade = requireNonNull(profileFacade, "profileFacade");
        this.environmentUserInput = requireNonNull(environmentUserInput, "environmentUserInput");
    }

    public void continueDeployment(ContinueDeploymentRequest request) {

        EnvironmentName environmentName = environmentUserInput.getEnvironment(request)
                                                              .getName();

        try(DynamoDbClient dynamoDbClient = awsClientFactory.dynamoClient()) {
            AttributeValue sfnTokenAttribute = dynamoDbClient
                    .getItem(GetItemRequest.builder()
                                           .key(createKey(request.getDistributionName(),
                                                          request.getStepName(),
                                                          environmentName))
                                           .tableName("AttiniResourceStatesV1")
                                           .build())
                    .item()
                    .get("sfnToken");

            if (sfnTokenAttribute == null){
                throw new IllegalArgumentException("No distribution with name %s has a running deployment in the current account.".formatted(request.getDistributionName().getName()));
            }
            String sfnToken = sfnTokenAttribute.s();

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode objectNode = objectMapper.createObjectNode();
            ObjectNode node = objectNode.put("environment", environmentName.getName())
                                        .put("distributionName", request.getDistributionName().getName())
                                        .put("stepName", request.getStepName())
                                        .put("abort", request.isAbort())
                                        .put("sfnToken", sfnToken);

            request.getMessage().ifPresent(message -> node.put("message", message));

            try (SnsClient snsClient = awsClientFactory.snsClient()) {
                snsClient
                        .publish(PublishRequest.builder()
                                               .message(node.toString())
                                               .topicArn(getTopic())
                                               .messageAttributes(Map.of("type",
                                                                         MessageAttributeValue.builder()
                                                                                              .dataType(
                                                                                                      "String")
                                                                                              .stringValue(
                                                                                                      "manualApprovalEvent")
                                                                                              .build()))
                                               .build());

            }
        }
    }

    private String getTopic() {
        return "arn:aws:sns:%s:%s:attini-respond-to-cfn-event".formatted(profileFacade.getRegion()
                                                                                      .getName(),
                                                                         awsAccountFacade.getAccount());

    }

    private static Map<String, AttributeValue> createKey(DistributionName distributionName,
                                                         String stepName,
                                                         EnvironmentName environment) {
        return Map.of("resourceType",
                      AttributeValue.builder().s("ManualApproval").build(),
                      "name",
                      AttributeValue.builder()
                                    .s(environment.getName() + "-" + distributionName.getName() + "-" + stepName)
                                    .build());
    }


}
