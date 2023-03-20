package se.attini.environment;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import se.attini.AttiniNotInstalledException;
import se.attini.InvalidCredentialsException;
import se.attini.client.AwsClientFactory;
import se.attini.domain.Environment;
import se.attini.domain.EnvironmentName;
import se.attini.domain.EnvironmentType;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

public class EnvironmentService {

    private static final String ATTINI_RESOURCE_STATES_TABLE_NAME = "AttiniResourceStatesV1";
    private final AwsClientFactory awsClientFactory;

    public EnvironmentService(AwsClientFactory awsClientFactory) {
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
    }


    public List<Environment> getEnvironments() {

        try (DynamoDbClient client =  awsClientFactory.dynamoClient()){
            return client.query(QueryRequest.builder().tableName(
                                                              ATTINI_RESOURCE_STATES_TABLE_NAME)
                                                      .keyConditionExpression(
                                                              "resourceType=:v_resourceType")
                                                      .expressionAttributeValues(
                                                              Map.of(
                                                                      ":v_resourceType",
                                                                      AttributeValue.builder()
                                                                                    .s("Environment")
                                                                                    .build()))
                                                      .build())
                                   .items()
                                   .stream()
                                   .map(map -> Environment.create(EnvironmentName.create(map.get("name").s()),
                                                                  EnvironmentType.fromString(map.get("environmentType")
                                                                                                .s())))
                                   .collect(Collectors.toList());
        } catch (ResourceNotFoundException e) {
           throw new AttiniNotInstalledException(e);
        }catch (DynamoDbException e){
            if (e.awsErrorDetails().errorCode().equals("UnrecognizedClientException")){
                throw new InvalidCredentialsException(e.awsErrorDetails().errorMessage(), e);
            }
            throw e;
        }
    }


    public void removeEnvironment(RemoveEnvironmentRequest request) {
        try (DynamoDbClient dynamoDbClient = awsClientFactory.dynamoClient()) {
            dynamoDbClient
                    .deleteItem(DeleteItemRequest.builder()
                                                 .tableName(ATTINI_RESOURCE_STATES_TABLE_NAME)
                                                 .key(Map.of("resourceType",
                                                             AttributeValue.builder().s("Environment").build(),
                                                             "name",
                                                             AttributeValue.builder()
                                                                           .s(request.environment()
                                                                                     .getName())
                                                                           .build()))
                                                 .build());
        }
    }

    public void createEnvironment(CreateEnvironmentRequest request) {

        try (DynamoDbClient dynamoDbClient = awsClientFactory.dynamoClient()) {
            dynamoDbClient.putItem(PutItemRequest.builder()
                                                 .tableName(ATTINI_RESOURCE_STATES_TABLE_NAME)
                                                 .item(Map.of("resourceType",
                                                              AttributeValue.builder().s("Environment").build(),
                                                              "name",
                                                              AttributeValue.builder()
                                                                            .s(request.environment().getName())
                                                                            .build(),
                                                              "environmentType",
                                                              AttributeValue.builder()
                                                                            .s(request.type().getValue())
                                                                            .build()))
                                                 .build());
        }catch (ResourceNotFoundException e){
            throw new AttiniNotInstalledException();
        } catch (DynamoDbException e){
            if (e.awsErrorDetails().errorCode().equals("UnrecognizedClientException")){
                throw new InvalidCredentialsException(e.awsErrorDetails().errorMessage(), e);
            }
            throw e;
        }

    }

}
