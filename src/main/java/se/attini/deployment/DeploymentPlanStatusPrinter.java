package se.attini.deployment;

import static java.util.Objects.requireNonNull;
import static se.attini.util.StringUtils.cut;
import static se.attini.util.StringUtils.pad;
import static se.attini.util.StringUtils.padStrict;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import se.attini.cli.ConsolePrinter;
import se.attini.cli.PrintItem;
import se.attini.cli.PrintUtil;
import se.attini.cli.global.GlobalConfig;
import se.attini.domain.Deployment;
import se.attini.domain.DeploymentPlanStatus;
import se.attini.domain.DeploymentPlanStepStatus;
import se.attini.domain.ExecutionArn;
import se.attini.domain.StepStatus;

public class DeploymentPlanStatusPrinter {

    private final static Set<String> EXCLUDED_STEPS = Set.of("AttiniPrepareDeployment","AttiniSamPackage?");

    private final Map<String, StepLogger> loggers;

    private final Set<String> completedSteps;

    private final Set<String> startedSteps;

    private final StepLoggerFactory stepLoggerFactory;
    private final GlobalConfig globalConfig;
    private final ConsolePrinter consolePrinter;
    private final ObjectMapper objectMapper;

    private int longestStep = 10;


    public DeploymentPlanStatusPrinter(StepLoggerFactory stepLoggerFactory,
                                       GlobalConfig globalConfig,
                                       ConsolePrinter consolePrinter) {
        this.stepLoggerFactory = requireNonNull(stepLoggerFactory, "stepLoggerFactory");
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
        this.completedSteps = new HashSet<>();
        this.startedSteps = new HashSet<>();
        this.loggers = new HashMap<>();
        this.objectMapper = new ObjectMapper();

    }

    public void configureStepLength(int value) {
        this.longestStep = value;
    }

    public void print(DeploymentPlanStatus deploymentPlanStatus,
                      ExecutionArn executionArn,
                      Deployment deployment) {

        Stream<Row> runningRows = resolveRunningRows(deploymentPlanStatus,
                                                     executionArn,
                                                     deployment);
        Stream.of(resolveNewStartedRowsToPrint(deploymentPlanStatus.getStartedSteps(),
                                               deployment),
                  runningRows,
                  resolveNewRowsToPrint(deploymentPlanStatus.getCompletedSteps(), completedSteps))
              .flatMap(Function.identity())
              .filter(row -> !EXCLUDED_STEPS.contains(row.stepName()))
              .sorted(Comparator.comparing(o -> o.timestamp))
              .forEach(row -> consolePrinter.print(createRow(row)));

    }

    private Stream<Row> resolveNewRowsToPrint(List<DeploymentPlanStepStatus> newStatuses,
                                              Set<String> alreadyPrintedSteps) {
        return newStatuses
                .stream()
                .filter(step -> alreadyPrintedSteps.add(step.getName()))
                .map(s -> new Row(s.getName(),
                                  s.getStepStatus(),
                                  s.getMessage().orElse(""),
                                  s.getTimestamp()));
    }

    private Stream<Row> resolveNewStartedRowsToPrint(List<DeploymentPlanStepStatus> newStatuses,
                                                     Deployment deployment) {
        return newStatuses
                .stream()
                .filter(step -> startedSteps.add(step.getName()))
                .flatMap(s -> {

                    List<Row> rows = new ArrayList<>();
                    rows.add(new Row(s.getName(),
                                     s.getStepStatus(),
                                     s.getMessage().orElse(""),
                                     s.getTimestamp()));

                    if ("AttiniManualApproval".equals(deployment.getAttiniSteps().get(s.getName()))) {
                        String command = "MANUAL APPROVAL NEEDED. Run the following command to continue: attini deploy continue -n %s -e %s --step-name '%s' %s %s".formatted(
                                deployment.getDistribution().getDistributionName().getName(),
                                deployment.getEnvironment().getName(),
                                s.getName(),
                                globalConfig.getProfile()
                                             .map(profile -> "-p " + profile.getProfileName())
                                             .orElse(""),
                                globalConfig.getRegion()
                                             .map(region -> "-r " + region.getName())
                                             .orElse(""));
                        rows.add(new Row(s.getName(), StepStatus.RUNNING, command, s.getTimestamp().plusNanos(1)));
                    }

                    return rows.stream();
                });
    }


    private record Row(String stepName, StepStatus status, String output, Instant timestamp) {
    }


    private PrintItem createRow(Row row) {
        if (globalConfig.printAsJson()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("timestamp", row.timestamp.toEpochMilli());
            objectNode.put("type", "object");
            ObjectNode dataNode = objectMapper.createObjectNode();
            dataNode.put("stepName", row.stepName);
            dataNode.put("status", row.status.name());
            dataNode.put("output", row.output);
            objectNode.set("data", dataNode);
            return PrintItem.message(objectNode.toString());

        }
        return PrintItem.message(stepColumn(row.stepName) + statusColumn(row.status.name()) + outputColumn(row.output == null ? "" : row.output));
    }

    public void printHeader() {
        if (!globalConfig.printAsJson()) {
            consolePrinter.print(PrintItem.message(stepColumn("[Step]") + statusColumn("[Status]") + outputColumn("[Output]")));
        }
    }

    private String stepColumn(String value) {
        int length = Math.min(longestStep + 2, 40);
        String cut = cut(value, length);
        return cut + " ".repeat(length - cut.length());
    }

    private String statusColumn(String value) {
        String croppedValue = pad(cut(value, 8), 10);

        return switch (value) {
            case "RUNNING" -> PrintUtil.toYellow(croppedValue);
            case "SUCCESS" -> PrintUtil.toGreen(croppedValue);
            case "FAILED", "RUNTIME_ERROR" -> PrintUtil.toRed(croppedValue);
            default -> croppedValue;
        };
    }

    private String outputColumn(String value) {
        return padStrict(value, 4);
    }

    private Stream<Row> resolveRunningRows(DeploymentPlanStatus deploymentPlanStatus,
                                           ExecutionArn executionArn,
                                           Deployment deployment) {

        return Stream.concat(deploymentPlanStatus.getStartedSteps()
                                                 .stream()
                                                 .map(DeploymentPlanStepStatus::getName),
                             deploymentPlanStatus.getCompletedSteps()
                                                 .stream()
                                                 .map(DeploymentPlanStepStatus::getName))
                     .distinct()
                     .filter(s -> !completedSteps.contains(s))
                     .parallel()
                     .map(s -> getLogger(executionArn,
                                         deployment,
                                         s).lines()
                                           .stream()
                                           .map(line -> new Row(s,
                                                                StepStatus.RUNNING,
                                                                line.data(),
                                                                line.timestamp())).toList())
                     .flatMap(Collection::stream);
    }

    private synchronized StepLogger getLogger(ExecutionArn executionArn,
                                              Deployment deployment,
                                              String stepName) {
        return loggers.computeIfAbsent(stepName, s -> stepLoggerFactory.getLogger(new StepLoggerFactory.GetLoggerRequest(
                                                                                          deployment.getDistribution()
                                                                                                    .getDistributionName(),
                                                                                          deployment.getDistribution()
                                                                                                    .getDistributionId(),
                                                                                          deployment.getEnvironment(),
                                                                                          executionArn,
                                                                                          deployment.getAttiniSteps()
                                                                                                    .get(stepName)),
                                                                                  stepName));
    }


}
