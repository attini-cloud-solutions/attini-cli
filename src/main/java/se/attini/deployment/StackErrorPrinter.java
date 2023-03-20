package se.attini.deployment;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import se.attini.cli.ConsolePrinter;
import se.attini.cli.PrintItem;
import se.attini.domain.Deployment;
import se.attini.domain.DeploymentPlanStepError;

public class StackErrorPrinter {


    private final boolean printAsJson;
    private final ObjectMapper objectMapper;
    private final ConsolePrinter consolePrinter;
    private final Set<DeploymentPlanStepError> errorSet = new HashSet<>();


    public StackErrorPrinter(boolean printAsJson,
                             ObjectMapper objectMapper, ConsolePrinter consolePrinter) {
        this.printAsJson = printAsJson;
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    public boolean noErrorsPrinted() {
        return errorSet.isEmpty();
    }

    public void printStackError(Deployment deployment) {
        deployment.getDeploymentPlanStepErrors()
                  .stream()
                  .filter(errorSet::add)
                  .forEach(deploymentPlanStepError -> {
                      String url = String.format(
                              "https://%s.console.aws.amazon.com/cloudformation/home?region=%s#/stacks?filteringStatus=active&filteringText=%s&viewNested=true&hideStacks=false",
                              deploymentPlanStepError.region().getName(),
                              deploymentPlanStepError.region().getName(),
                              deploymentPlanStepError.stackName().getName());
                      if (printAsJson) {
                          String errorData = objectMapper.createObjectNode()
                                                         .set("data",
                                                              objectMapper
                                                                      .createObjectNode()
                                                                      .put("Result", "Error")
                                                                      .put("Step", deploymentPlanStepError.error())
                                                                      .put("Stack",
                                                                           deploymentPlanStepError.stackName()
                                                                                                  .getName())
                                                                      .put("Resource",
                                                                           deploymentPlanStepError.resourceName())
                                                                      .put("Cause", deploymentPlanStepError.error())
                                                                      .put("url", url)).toString();
                          consolePrinter.print(PrintItem.message(errorData));

                      } else {
                          consolePrinter.print(PrintItem.errorMessage("Error in step: " + deploymentPlanStepError.stepName()));

                          consolePrinter.print(PrintItem.errorMessage("Stack: " + deploymentPlanStepError.stackName()
                                                                                                                    .getName()));
                          consolePrinter.print(PrintItem.errorMessage("Resource: " + deploymentPlanStepError.resourceName()));
                          consolePrinter.print(PrintItem.errorMessage("Cause: " + deploymentPlanStepError.error()));
                          consolePrinter.print(PrintItem.message("Stack url: " + url));
                      }
                  });
    }
}
