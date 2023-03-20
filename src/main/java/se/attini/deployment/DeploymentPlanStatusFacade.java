package se.attini.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import se.attini.client.AwsClientFactory;
import se.attini.domain.DeploymentPlanStatus;
import se.attini.domain.DeploymentPlanStepStatus;
import se.attini.domain.ExecutionArn;
import se.attini.domain.StepStatus;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.DescribeExecutionRequest;
import software.amazon.awssdk.services.sfn.model.DescribeExecutionResponse;
import software.amazon.awssdk.services.sfn.model.DescribeStateMachineRequest;
import software.amazon.awssdk.services.sfn.model.GetExecutionHistoryRequest;
import software.amazon.awssdk.services.sfn.model.GetExecutionHistoryResponse;
import software.amazon.awssdk.services.sfn.model.HistoryEvent;
import software.amazon.awssdk.services.sfn.model.StateExitedEventDetails;

public class DeploymentPlanStatusFacade {

    private final AwsClientFactory awsClientFactory;
    private final ObjectMapper objectMapper;

    public DeploymentPlanStatusFacade(AwsClientFactory awsClientFactory) {
        this.awsClientFactory = awsClientFactory;
        this.objectMapper = new ObjectMapper();
    }

    public DeploymentPlanStatus getDeploymentPlanStatus(GetDeploymentPlanExecutionRequest request) {
        try(SfnClient sfnClient = awsClientFactory.sfnClient()) {
            List<HistoryEvent> historyEvents = getHistoryEvents(sfnClient, request.executionArn());

            List<DeploymentPlanStepStatus> completedSteps =
                    historyEvents
                            .stream()
                            .filter(historyEvent -> historyEvent.stateExitedEventDetails() != null)
                            .map(this::toCompletedStep)
                            .distinct()
                            .collect(Collectors.toList());

            List<DeploymentPlanStepStatus> failedSteps =
                    historyEvents
                            .stream()
                            .filter(historyEvent -> historyEvent.executionFailedEventDetails() != null)
                            .distinct()
                            .map(event -> findFailedTask(event, historyEvents))
                            .toList();

            completedSteps.addAll(failedSteps);


            List<DeploymentPlanStepStatus> startedSteps =
                    historyEvents
                            .stream()
                            .filter(historyEvent -> historyEvent.stateEnteredEventDetails() != null)
                            .map(historyEvent -> new DeploymentPlanStepStatus(historyEvent.stateEnteredEventDetails().name(), StepStatus.STARTED, historyEvent.timestamp().plusNanos(historyEvent.id())))
                            .distinct()
                            .collect(Collectors.toList());

            DescribeExecutionResponse describeExecutionResponse = sfnClient.describeExecution(DescribeExecutionRequest.builder()
                                                                                                                      .executionArn(
                                                                                                                              request.executionArn()
                                                                                                                                     .getValue())
                                                                                                                      .build());


            return DeploymentPlanStatus.create(completedSteps,
                                               startedSteps,
                                               describeExecutionResponse.status().name(),
                                               extractName(request.executionArn().getValue()),
                                               describeExecutionResponse.startDate(),
                                               describeExecutionResponse.stopDate());
        }

    }

    public int getLongestStepNameLength(ExecutionArn executionArn) {

        try (SfnClient sfnClient = awsClientFactory.sfnClient()) {
            String definition = sfnClient
                    .describeStateMachine(DescribeStateMachineRequest.builder()
                                                                     .stateMachineArn(
                                                                             executionArn.getSfnArn())
                                                                     .build()).definition();

            JsonNode jsonNode = new ObjectMapper().readTree(definition);
            return getNames(jsonNode.get("States"))
                    .stream()
                    .mapToInt(String::length)
                    .max()
                    .orElse(10);

        } catch (JsonProcessingException e) {
            return 10;
        }
    }

    private List<String> getNames(JsonNode jsonNode) {
        ArrayList<String> names = new ArrayList<>();
        for (JsonNode jsonNode1 : jsonNode) {
            if (jsonNode1.has("Branches")) {
                ArrayNode branches = (ArrayNode) jsonNode1.path("Branches");
                for (JsonNode jsonNode2 : branches) {
                    names.addAll(getNames(jsonNode2.get("States")));
                }
            }
        }

        Iterable<String> iterable = jsonNode::fieldNames;
        names.addAll(StreamSupport.stream(iterable.spliterator(), false).toList());
        return names;
    }

    private List<HistoryEvent> getHistoryEvents(SfnClient sfnClient, ExecutionArn executionArn) {

        GetExecutionHistoryResponse executionHistory =
                sfnClient.getExecutionHistory(GetExecutionHistoryRequest.builder()
                                                                        .executionArn(
                                                                                executionArn.getValue())
                                                                        .build());
        ArrayList<HistoryEvent> historyEvents = new ArrayList<>(executionHistory.events());

        while (executionHistory.nextToken() != null) {
            executionHistory = sfnClient.getExecutionHistory(GetExecutionHistoryRequest.builder()
                                                                                       .nextToken(executionHistory.nextToken())
                                                                                       .executionArn(
                                                                                               executionArn.getValue())
                                                                                       .build());
            historyEvents.addAll(executionHistory.events());
        }

        return historyEvents;
    }


    private DeploymentPlanStepStatus findFailedTask(HistoryEvent failedEvent, List<HistoryEvent> historyEvents) {
        if (failedEvent.previousEventId().equals(0L)) {
            return new DeploymentPlanStepStatus("Deployment plan",
                                                StepStatus.RUNTIME_ERROR,
                                                String.format("%sError: %s%sCause: %s",
                                                              System.lineSeparator(),
                                                              failedEvent.executionFailedEventDetails().error(),
                                                              System.lineSeparator(),
                                                              failedEvent.executionFailedEventDetails().cause()), failedEvent.timestamp().plusNanos(failedEvent.id()));
        }
        HistoryEvent currentEvent = getEventWithId(failedEvent.previousEventId(), historyEvents);
        while (currentEvent.stateEnteredEventDetails() == null) {

            currentEvent = getEventWithId(currentEvent.previousEventId(), historyEvents);
        }

        return new DeploymentPlanStepStatus(currentEvent.stateEnteredEventDetails().name(),
                                            StepStatus.FAILED,
                                            String.format("%sError: %s%sCause: %s",
                                                          System.lineSeparator(),
                                                          failedEvent.executionFailedEventDetails().error(),
                                                          System.lineSeparator(),
                                                          failedEvent.executionFailedEventDetails().cause()), failedEvent.timestamp().plusNanos(failedEvent.id()));

    }

    private HistoryEvent getEventWithId(Long eventId, List<HistoryEvent> historyEvents) {
        return historyEvents.stream()
                            .filter(event -> event.id().equals(eventId))
                            .findAny()
                            .orElseThrow(() -> new IllegalStateException("Could not find step function event with id =" + eventId));
    }

    private DeploymentPlanStepStatus toCompletedStep(HistoryEvent event) {
        return new DeploymentPlanStepStatus(event.stateExitedEventDetails().name(), StepStatus.SUCCESS, getOutput(event.stateExitedEventDetails()), event.timestamp().plusNanos(event.id()));
    }

    private String getOutput(StateExitedEventDetails stateExitedEventDetails){
        try {
            return objectMapper.readTree(stateExitedEventDetails.output()).path("output").path(stateExitedEventDetails.name()).toString();
        } catch (JsonProcessingException e) {
           return "";
        }

    }


    private String extractName(String executionArn) {
        return executionArn.split(":")[6]
                .split("-")[0]
                .replace("AttiniDeploymentPlanSfn", "");
    }

}
