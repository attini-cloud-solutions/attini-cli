package se.attini.deployment.history;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import se.attini.client.AwsClientFactory;
import se.attini.deployment.GetDeploymentHistoryRequest;
import se.attini.deployment.GetDeploymentRequest;
import se.attini.deployment.NoDistributionFoundException;
import se.attini.domain.Deployment;
import se.attini.domain.DeploymentError;
import se.attini.domain.DeploymentPlanCount;
import se.attini.domain.DeploymentPlanStepError;
import se.attini.domain.DeploymentType;
import se.attini.domain.Distribution;
import se.attini.domain.DistributionId;
import se.attini.domain.DistributionName;
import se.attini.domain.Environment;
import se.attini.domain.EnvironmentName;
import se.attini.domain.ExecutionArn;
import se.attini.domain.InitStackError;
import se.attini.domain.Region;
import se.attini.domain.StackName;
import se.attini.environment.EnvironmentUserInput;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

public class DeploymentHistoryFacade {


    private static final String TABLE_NAME = "AttiniDeployDataV1";
    private static final String PARTITION_KEY_NAME = "deploymentName";
    public static final String DEPLOY_TIME_FIELD = "deploymentTime";
    public static final String DISTRIBUTION_NAME_FIELD = "distributionName";
    public static final String DISTRIBUTION_ID_FIELD = "distributionId";
    public static final String ENVIRONMENT_FIELD = "environment";
    public static final String ERROR_CODE_FIELD = "errorCode";
    public static final String ERROR_MESSAGE_FIELD = "errorMessage";
    public static final String STACK_NAME_FIELD = "stackName";

    public static final String DEPLOYMENT_TYPE_FIELD = "deploymentType";

    public static final String EXECUTION_ARNS_FIELD = "executionArns";
    public static final String DEPLOYMENT_PLAN_COUNT_FIELD = "deploymentPlanCount";
    public static final String INIT_STACK_ERRORS = "initStackErrors";
    public static final String ERRORS = "errors";
    public static final String INIT_STACK_UNCHANGED = "initStackUnchanged";

    public static final String DEPLOYMENT_PLAN_STATUS = "deploymentPlanStatus";

    public static final String DISTRIBUTION_TAGS = "distributionTags";

    public static final String ATTINI_STEPS = "attiniSteps";

    public static final String VERSION = "version";


    private final AwsClientFactory awsClientFactory;
    private final EnvironmentUserInput environmentUserInput;


    public DeploymentHistoryFacade(AwsClientFactory awsClientFactory, EnvironmentUserInput environmentUserInput) {
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
        this.environmentUserInput = requireNonNull(environmentUserInput, "environmentUserInput");
    }

    public List<Deployment> getDeploymentHistory(GetDeploymentHistoryRequest request) {
        return listDeploymentHistory(request.getDistributionName(), environmentUserInput.getEnvironment(request));
    }


    public Deployment getDeployment(GetDeploymentRequest request) {
        try (DynamoDbClient dynamoDbClient = awsClientFactory.dynamoClient()) {
            QueryRequest queryRequest = QueryRequest.builder()
                                                    .indexName("objectIdentifier")
                                                    .tableName(TABLE_NAME)
                                                    .keyConditionExpression(
                                                            "objectIdentifier=:v_objectIdentifier and deploymentName=:v_deployName")
                                                    .expressionAttributeValues(Map.of(":v_objectIdentifier",
                                                                                      AttributeValue.builder()
                                                                                                    .s(request.getObjectIdentifier()
                                                                                                              .getValue())
                                                                                                    .build(),
                                                                                      ":v_deployName",
                                                                                      AttributeValue.builder()
                                                                                                    .s(getDeploymentName(
                                                                                                            request.getDistributionName(),
                                                                                                            request.getEnvironment()))
                                                                                                    .build()))
                                                    .build();

            return dynamoDbClient.query(queryRequest)
                                 .items()
                                 .stream()
                                 .filter(valueMap -> !valueMap.get("deploymentTime").n().equals("0"))
                                 .map(DeploymentHistoryFacade::toDeployment)
                                 .findAny()
                                 .orElseThrow(NoDistributionFoundException::new);
        }

    }

    public Deployment getLatestDeployment(DistributionName distributionName,
                                          Environment environment) {
        try (DynamoDbClient client = awsClientFactory.dynamoClient()) {
            Map<String, AttributeValue> item = client
                    .getItem(GetItemRequest.builder()
                                           .tableName(TABLE_NAME)
                                           .key(Map.of("deploymentName",
                                                       AttributeValue.builder()
                                                                     .s(getDeploymentName(
                                                                             distributionName,
                                                                             environment))
                                                                     .build(),
                                                       "deploymentTime",
                                                       AttributeValue.builder()
                                                                     .n("0")
                                                                     .build()))
                                           .build()).item();
            return toDeployment(item);
        }
    }


    private List<Deployment> listDeploymentHistory(DistributionName distributionName,
                                                   Environment environment) {

        try (DynamoDbClient dynamoDbClient = awsClientFactory.dynamoClient()) {
            HashMap<String, String> nameMap = new HashMap<>();
            nameMap.put("#name", PARTITION_KEY_NAME);

            HashMap<String, AttributeValue> valueMap = new HashMap<>();

            AttributeValue attributeValue = AttributeValue.builder()
                                                          .s(getDeploymentName(distributionName, environment))
                                                          .build();
            valueMap.put(":value", attributeValue);

            QueryRequest queryRequest = QueryRequest.builder()
                                                    .tableName(TABLE_NAME)
                                                    .keyConditionExpression("#name = :value")
                                                    .expressionAttributeNames(nameMap)
                                                    .expressionAttributeValues(valueMap)
                                                    .build();
            QueryResponse response = dynamoDbClient.query(queryRequest);


            return response.items()
                           .stream()
                           .filter(map -> !map.get("deploymentTime").n().equals("0"))
                           .map(DeploymentHistoryFacade::toDeployment)
                           .collect(Collectors.toList());
        }
    }

    private String getDeploymentName(DistributionName distributionName, Environment environment) {
        return environment.getName().getName() + "-" + distributionName.getName();
    }

    private static Deployment toDeployment(Map<String, AttributeValue> item) {

        Distribution.Builder distributionBuilder = Distribution.builder()
                                                               .setDistributionName(
                                                                       DistributionName.create(item.get(
                                                                               DISTRIBUTION_NAME_FIELD).s()));

        if (item.containsKey(DISTRIBUTION_ID_FIELD)) {
            distributionBuilder.setDistributionId(DistributionId.create(item.get(DISTRIBUTION_ID_FIELD).s()));
        } else {
            distributionBuilder.setDistributionId(DistributionId.NOT_RESOLVABLE);
        }

        Distribution distribution = distributionBuilder.build();

        Deployment.Builder builder = Deployment.builder()
                                               .setDistribution(distribution)
                                               .setDeployTime(Instant.ofEpochMilli(Long.parseLong(item.get(
                                                       DEPLOY_TIME_FIELD).n())))
                                               .setEnvironment(EnvironmentName.create(item.get(ENVIRONMENT_FIELD).s()));


        if (item.get(DEPLOYMENT_PLAN_COUNT_FIELD) != null) {
            builder.setDeploymentPlanCount(DeploymentPlanCount.create(item.get(
                    DEPLOYMENT_PLAN_COUNT_FIELD).n()));
        }


        if (item.get(ERROR_CODE_FIELD) != null) {
            builder.setDeploymentError(DeploymentError.create(item.get(ERROR_CODE_FIELD).s(), item.get(
                    ERROR_MESSAGE_FIELD).s()));
        }

        if (item.get(STACK_NAME_FIELD) != null) {
            builder.setStackName(StackName.create(item.get(STACK_NAME_FIELD).s()));
        }

        if (item.get(DEPLOYMENT_TYPE_FIELD) != null) {
            builder.setDeploymentType(item.get(DEPLOYMENT_TYPE_FIELD)
                                          .s()
                                          .equals("app") ? DeploymentType.APP : DeploymentType.PLATFORM);
        }

        if (item.get(INIT_STACK_ERRORS) != null) {
            List<InitStackError> initStackErrors = item.get(INIT_STACK_ERRORS)
                                                       .l()
                                                       .stream()
                                                       .map(AttributeValue::m)
                                                       .map(map -> new InitStackError(map.get("resourceName").s(),
                                                                                      map.get("resourceStatus").s(),
                                                                                      map.get("error").s()))
                                                       .collect(Collectors.toList());
            builder.setInitStackErrors(initStackErrors);
        }

        if (item.get(DISTRIBUTION_TAGS) != null) {
            builder.setDistributionTags(item.get(DISTRIBUTION_TAGS)
                                            .m()
                                            .entrySet()
                                            .stream()
                                            .collect(Collectors.toMap(Map.Entry::getKey,
                                                                      entry -> entry.getValue().s())));
        }

        if (item.get(ATTINI_STEPS) != null && item.get(ATTINI_STEPS).hasL()) {
            builder.setAttiniSteps(item.get(ATTINI_STEPS)
                                       .l()
                                       .stream()
                                       .map(AttributeValue::m)
                                       .collect(Collectors.toMap(map -> map.get("name").s(),
                                                                 map -> map.get("type").s())));
        }

        if (item.get(ERRORS) != null) {
            List<DeploymentPlanStepError> deploymentPlanStepErrors =
                    item.get(ERRORS)
                        .m()
                        .values()
                        .stream()
                        .map(AttributeValue::l)
                        .flatMap(Collection::stream)
                        .map(AttributeValue::m)
                        .map(map -> new DeploymentPlanStepError(map.get("stepName").s(),
                                                                StackName.create(map.get("stackName").s()),
                                                                map.get("resourceName").s(),
                                                                map.get("resourceStatus").s(),
                                                                map.get("error").s(),
                                                                Region.create(map.get("region").s())))
                        .collect(Collectors.toList());
            builder.setDeploymentPlanStepErrors(deploymentPlanStepErrors);
        }

        if (item.get(VERSION) != null) {
            builder.setVersion(item.get(VERSION).s());
        }

        if (item.get(INIT_STACK_UNCHANGED) != null) {
            builder.setInitStackUnchanged(item.get(INIT_STACK_UNCHANGED).bool());
        }

        if (item.get(DEPLOYMENT_PLAN_STATUS) != null) {
            builder.setDeploymentPlanStatus(item.get(DEPLOYMENT_PLAN_STATUS).s());
        }

        if (item.get(EXECUTION_ARNS_FIELD) != null && !item.get(EXECUTION_ARNS_FIELD).m().isEmpty()) {
            builder.setExecutionArn(item.get(EXECUTION_ARNS_FIELD)
                                        .m()
                                        .values()
                                        .stream()
                                        .map(AttributeValue::s)
                                        .map(ExecutionArn::create)
                                        .toList().get(0));
        }

        return builder.build();
    }

}
