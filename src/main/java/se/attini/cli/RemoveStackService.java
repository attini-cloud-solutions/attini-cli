package se.attini.cli;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import se.attini.AwsAccountFacade;
import se.attini.client.AwsClientFactory;
import se.attini.domain.Region;
import se.attini.profile.ProfileFacade;
import se.attini.removestack.RemoveStackResourceRequest;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.sts.StsClient;

public class RemoveStackService {
    private final AwsClientFactory awsClientFactory;
    private final ProfileFacade profileFacade;
    private final ConsolePrinter consolePrinter;
    private final AwsAccountFacade awsAccountFacade;

    public RemoveStackService(AwsClientFactory awsClientFactory,
                              ProfileFacade profileFacade,
                              ConsolePrinter consolePrinter,
                              AwsAccountFacade awsAccountFacade) {
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
        this.profileFacade = requireNonNull(profileFacade, "profileFacade");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
        this.awsAccountFacade = requireNonNull(awsAccountFacade, "awsAccountFacade");
    }

    public void removeStackResources(RemoveStackResourceRequest request) {

        try (DynamoDbClient dynamoDbClient = awsClientFactory.dynamoClient();
             CloudFormationClient cloudFormationClient = awsClientFactory.cfnClient()) {

            Region stackRegion = request.getStackRegion().orElseGet(profileFacade::getRegion);

            String account = request.getAccountId()
                                    .orElseGet(awsAccountFacade::getAccount);

            String resourceName = request.getStackName().getName() + "-" + stackRegion.getName() + "-" + account;

            dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                                                       .tableName("AttiniResourceStatesV1")
                                                       .key(Map.of("resourceType",
                                                                   AttributeValue.builder()
                                                                                 .s("CloudformationStack")
                                                                                 .build(),
                                                                   "name",
                                                                   AttributeValue.builder()
                                                                                 .s(resourceName)
                                                                                 .build()))
                                                       .build());

            consolePrinter.print(PrintItem.message("The Attini resources for the stack has been deleted"));


            if (request.isDeleteStack() && isSameAccount(request)) {
                try {
                    String stackId = getStackId(request.getStackName().getName(), cloudFormationClient);
                    cloudFormationClient.deleteStack(DeleteStackRequest.builder()
                                                                       .stackName(request.getStackName().getName())
                                                                       .build());
                    consolePrinter.print(PrintItem.message("Deleting stack, this may take a few minutes."));
                    Stack stack = getStack(request.getStackName().getName(), cloudFormationClient);
                    while (stack.stackStatus().equals(StackStatus.DELETE_IN_PROGRESS)) {
                        stack = getStack(stackId, cloudFormationClient);
                    }

                    if (!stack.stackStatus().equals(StackStatus.DELETE_COMPLETE)) {
                        throw new RuntimeException("Failed to delete stack, reason: " + stack.stackStatusReason());
                    }
                    consolePrinter.print(PrintItem.message("Stack deleted successfully"));
                } catch (CloudFormationException e) {
                    if (!e.getMessage().contains("does not exist")) {
                        throw e;
                    }
                    consolePrinter.print(PrintItem.message("Stack does not exist"));
                }

            } else if (request.isDeleteStack()) {
                consolePrinter.print(PrintItem.errorMessage("Delete stack cross account is not supported"));
            } else {
                consolePrinter.print(PrintItem.message(
                        "Note that the stack itself has not been deleted. To delete the stack rerun the command with the --delete-stack option"));
            }

        }


    }

    private Boolean isSameAccount(RemoveStackResourceRequest request) {
        return request.getAccountId()
                      .map(accountId -> awsAccountFacade.getAccount()
                                                        .equals(accountId)).orElse(true);
    }

    private String getStackId(String name, CloudFormationClient cloudFormationClient) {
        return getStack(name, cloudFormationClient).stackId();
    }

    private static Stack getStack(String name, CloudFormationClient cloudFormationClient) {
        return cloudFormationClient.describeStacks(
                                           DescribeStacksRequest.builder().stackName(name).build())
                                   .stacks()
                                   .get(0);
    }
}
