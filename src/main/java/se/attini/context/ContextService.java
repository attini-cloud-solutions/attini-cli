package se.attini.context;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import se.attini.AttiniNotInstalledException;
import se.attini.InvalidCredentialsException;
import se.attini.client.AwsClientFactory;
import se.attini.domain.DistributionName;
import se.attini.domain.Environment;
import se.attini.domain.EnvironmentName;
import se.attini.environment.EnvironmentService;
import se.attini.profile.ProfileFacade;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.Output;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

public class ContextService {

    private final AwsClientFactory awsClientFactory;
    private final ProfileFacade profileFacade;
    private final EnvironmentService environmentService;

    public ContextService(AwsClientFactory awsClientFactory,
                          ProfileFacade profileFacade,
                          EnvironmentService environmentService) {
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
        this.profileFacade = requireNonNull(profileFacade, "profileFacade");
        this.environmentService = requireNonNull(environmentService, "environmentService");
    }

    public Context getContext(GetContextRequest request) {

        try (DynamoDbClient dynamoDbClient = awsClientFactory.dynamoClient();
             StsClient stsClient = awsClientFactory.stsClient()) {

            CompletableFuture<Optional<String>> attiniVersion =
                    CompletableFuture.supplyAsync(this::getAttiniSetupVersion);

            CompletableFuture<Set<String>> envListFuture = CompletableFuture.supplyAsync(() -> environmentService.getEnvironments()
                                                                                                                 .stream()

                                                                                                                 .map(Environment::getName)
                                                                                                                 .map(EnvironmentName::getName)
                                                                                                                 .collect(
                                                                                                                         Collectors.toSet()));


            CompletableFuture<List<EnvironmentContext>> contextFuture =
                    CompletableFuture.supplyAsync(() -> dynamoDbClient.query(getDistributionRequest(request.getDistributionName()
                                                                                                           .orElse(null)))
                                                                      .items()
                                                                      .stream()
                                                                      .filter(map -> map.get(
                                                                              "environment") != null)
                                                                      .filter(map -> request.getEnvironment()
                                                                                            .map(environment1 -> environment1.getName()
                                                                                                                             .equals(
                                                                                                                                     map.get("environment")
                                                                                                                                        .s()))
                                                                                            .orElse(true))
                                                                      .collect(
                                                                              Collectors.groupingBy(
                                                                                      map -> map.get(
                                                                                                        "environment")
                                                                                                .s()))
                                                                      .entrySet()
                                                                      .stream()
                                                                      .map((Map.Entry<String, List<Map<String, AttributeValue>>> entry) -> createEnvironmentContext(
                                                                              entry,
                                                                              dynamoDbClient,
                                                                              envListFuture))
                                                                      .collect(toList()));


            Optional<String> attiniSetupVersion = attiniVersion.join();
            if (attiniSetupVersion.isEmpty()) {
                throw new AttiniNotInstalledException();
            }

            GetCallerIdentityResponse callerIdentity = stsClient
                    .getCallerIdentity();

            return Context.builder()
                          .setAccount(callerIdentity.account())
                          .setAttiniVersion(attiniSetupVersion.get())
                          .setUser(callerIdentity.arn())
                          .setRegion(profileFacade.getRegion().getName())
                          .setEnvironments(contextFuture.join()).build();
        } catch (CompletionException e) {
            if (e.getCause() instanceof InvalidCredentialsException) {
                throw (InvalidCredentialsException) e.getCause();
            }
            throw e;
        }

    }

    private Optional<String> getAttiniSetupVersion() {
        try (CloudFormationClient cloudFormationClient = awsClientFactory.cfnClient()) {
            return cloudFormationClient
                    .describeStacks(DescribeStacksRequest.builder()
                                                         .stackName(
                                                                 "attini-setup")
                                                         .build())
                    .stacks()
                    .get(0)
                    .outputs()
                    .stream()
                    .filter(output -> output.outputKey()
                                            .equals("AttiniSetupVersion"))
                    .map(Output::outputValue)
                    .findAny();

        } catch (CloudFormationException e) {
            if (e.awsErrorDetails().errorCode().equals("InvalidClientTokenId")) {
                throw new InvalidCredentialsException(e.awsErrorDetails().errorMessage(), e);
            }
            return Optional.empty();
        }
    }


    private QueryRequest getDistributionRequest(DistributionName distributionName) {

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

        expressionAttributeValues.put(":v_resourceType", AttributeValue.builder()
                                                                       .s("Distribution")
                                                                       .build());

        QueryRequest.Builder builder = QueryRequest.builder()
                                                   .tableName("AttiniResourceStatesV1")
                                                   .keyConditionExpression("resourceType=:v_resourceType");

        if (distributionName != null) {
            builder.filterExpression("distributionName=:v_distName");
            expressionAttributeValues.put(":v_distName", AttributeValue.builder()
                                                                       .s(distributionName.getName())
                                                                       .build());
        }
        builder.expressionAttributeValues(expressionAttributeValues);
        return builder.build();
    }

    private EnvironmentContext createEnvironmentContext(Map.Entry<String, List<Map<String, AttributeValue>>> entry,
                                                        DynamoDbClient dynamoDbClient,
                                                        CompletableFuture<Set<String>> environmentListFuture) {


        List<DistributionContext> dists = entry.getValue()
                                               .stream()
                                               .parallel()
                                               .map(map -> {
                                                   Map<String, String> distributionTags =
                                                           map.get("distributionTags") == null ? Collections.emptyMap()
                                                                                               : map.get(
                                                                                                            "distributionTags").m()
                                                                                                    .entrySet()
                                                                                                    .stream()
                                                                                                    .collect(
                                                                                                            Collectors.toMap(
                                                                                                                    Map.Entry::getKey,
                                                                                                                    distTagEntry -> distTagEntry.getValue()
                                                                                                                                                .s()));
                                                   return new DistributionContext(map.get("distributionName")
                                                                                     .s(),
                                                                                  map.get("distributionId")
                                                                                     .s(),
                                                                                  getDeploymentPlans(dynamoDbClient,
                                                                                                     map),
                                                                                  distributionTags,
                                                                                  map.get("version") != null ? map.get(
                                                                                          "version").s() : null);
                                               })
                                               .collect(toList());


        try {
            Boolean environmentIsActive = environmentListFuture.thenApply(strings -> strings.contains(entry.getKey()))
                                                               .get();

            List<String> warnings = environmentIsActive ? null : List.of("Environment has been deleted");

            return new EnvironmentContext(entry.getKey(), dists, warnings);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private List<DeploymentPlanContext> getDeploymentPlans(DynamoDbClient dynamoDbClient,
                                                           Map<String, AttributeValue> map) {


        if (map.get("initStackName") == null) {
            return Collections.emptyList();
        }
        Map<String, AttributeValue> initStackItem = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                                         .tableName(
                                                                                                 "AttiniResourceStatesV1")
                                                                                         .key(Map.of("resourceType",
                                                                                                     AttributeValue.builder()
                                                                                                                   .s("InitDeployCloudformationStack")
                                                                                                                   .build(),
                                                                                                     "name",
                                                                                                     map.get("initStackName")))
                                                                                         .build()).item();
        if (initStackItem.containsKey("sfnArns")) {
            return initStackItem.get("sfnArns")
                                .ss()
                                .stream()
                                .map(s -> createDeploymentPlanContext(dynamoDbClient, s))
                                .collect(toList());
        }
        return Collections.emptyList();
    }


    private DeploymentPlanContext createDeploymentPlanContext(DynamoDbClient dynamoDbClient,
                                                              String sfnArn) {
        Map<String, AttributeValue> item = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                                .tableName("AttiniResourceStatesV1")
                                                                                .key(Map.of("resourceType",
                                                                                            AttributeValue.builder()
                                                                                                          .s("DeploymentPlan")
                                                                                                          .build(),
                                                                                            "name",
                                                                                            AttributeValue.builder()
                                                                                                          .s(sfnArn)
                                                                                                          .build()))
                                                                                .build()).item();

        String[] arnSplit = sfnArn.split(":");

        if (item.get("status") == null || item.get("startTime") == null) {
            return new DeploymentPlanContext(arnSplit[arnSplit.length - 1],
                                             "Could not get status or start time. This could be due to the distribution being deployed with an old version of the Attini framework.");
        }

        return new DeploymentPlanContext(arnSplit[arnSplit.length - 1],
                                         item.get("status").s(),
                                         Instant.ofEpochMilli(Long.parseLong(item.get("startTime").n()))
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDateTime()
                                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

}
