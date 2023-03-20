package se.attini.deployment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.attini.cli.ConsolePrinter;
import se.attini.cli.PrintItem;
import se.attini.cli.global.GlobalConfig;
import se.attini.domain.Deployment;
import se.attini.domain.DeploymentPlanStatus;
import se.attini.domain.DeploymentPlanStepStatus;
import se.attini.domain.Distribution;
import se.attini.domain.DistributionId;
import se.attini.domain.DistributionName;
import se.attini.domain.EnvironmentName;
import se.attini.domain.ExecutionArn;
import se.attini.domain.StepStatus;


@ExtendWith(MockitoExtension.class)
class DeploymentPlanStatusPrinterTest {

    private static final Instant START = Instant.ofEpochSecond(LocalDateTime.of(2022, 1, 20, 10, 10, 10)
                                                                            .atZone(ZoneId.systemDefault())
                                                                            .toEpochSecond());

    private static final EnvironmentName ENVIRONMENT = EnvironmentName.create("test");

    private static final DistributionName DISTRIBUTION_NAME = DistributionName.create("infra");

    private static final DistributionId DISTRIBUTION_ID = DistributionId.create("test-id");

    private static final ExecutionArn EXECUTION_ARN = ExecutionArn.create(
            "arn:aws:states:eu-west-1:855066048591:execution:AttiniDeploymentPlanSfnHelloWorldDeploymentPlan-4JAilOXIuONL:12ca1790-9395-4477-a0c7-9b8b11320e6b");

    @Mock()
    StepLoggerFactory stepLoggerFactory;

    @Mock
    ConsolePrinter consolePrinter;

    @Mock
    GlobalConfig globalConfig;
    DeploymentPlanStatusPrinter deploymentPlanStatusPrinter;

    @BeforeEach
    void setUp() {
        deploymentPlanStatusPrinter = new DeploymentPlanStatusPrinter(stepLoggerFactory, globalConfig, consolePrinter);
    }

    @Test
    void shouldPrintStartAndFinnish() {
        DeploymentPlanStepStatus testStep1 = new DeploymentPlanStepStatus("TestStep1",
                                                                          StepStatus.SUCCESS,
                                                                          Instant.now());
        DeploymentPlanStepStatus testStep2 = new DeploymentPlanStepStatus("TestStep1",
                                                                          StepStatus.STARTED,
                                                                          Instant.now());

        DeploymentPlanStatus deploymentPlanStatus = DeploymentPlanStatus.create(List.of(testStep1),
                                                                                List.of(testStep2),
                                                                                "Running",
                                                                                "MyDeploymentPlan",
                                                                                START);

        when(stepLoggerFactory.getLogger(any(StepLoggerFactory.GetLoggerRequest.class),
                                         anyString())).thenReturn(Collections::emptyList);

        Deployment deployment = getDeployment().build();
        print(deploymentPlanStatus, deployment);
        print(deploymentPlanStatus, deployment);
        print(deploymentPlanStatus, deployment);


        verify(consolePrinter, times(2)).print(any(PrintItem.class));
    }

    @Test
    void shouldPrintStartAndFinnishAndRunning() {
        DeploymentPlanStepStatus testStep1 = new DeploymentPlanStepStatus("TestStep1",
                                                                          StepStatus.SUCCESS,
                                                                          Instant.now());
        DeploymentPlanStepStatus testStep2 = new DeploymentPlanStepStatus("TestStep1",
                                                                          StepStatus.STARTED,
                                                                          Instant.now());

        DeploymentPlanStatus deploymentPlanStatus = DeploymentPlanStatus.create(List.of(testStep1),
                                                                                List.of(testStep2),
                                                                                "Running",
                                                                                "MyDeploymentPlan",
                                                                                START);

        when(stepLoggerFactory.getLogger(new StepLoggerFactory.GetLoggerRequest(DISTRIBUTION_NAME,
                                                                                DISTRIBUTION_ID,
                                                                                ENVIRONMENT,
                                                                                EXECUTION_ARN,
                                                                                "AttiniImport"),
                                         "TestStep1")).thenReturn(new FakeLogger());


        Deployment deployment = getDeployment().setAttiniSteps(Map.of("TestStep1",
                                                                      "AttiniImport")).build();

        print(deploymentPlanStatus, deployment);
        print(deploymentPlanStatus, deployment);
        print(deploymentPlanStatus, deployment);


        verify(consolePrinter, times(5)).print(any(PrintItem.class));
    }

    private static class FakeLogger implements StepLogger {
        boolean hasReturned = false;

        @Override
        public List<Line> lines() {
            if (hasReturned) {
                return Collections.emptyList();
            } else {
                hasReturned = true;
                return List.of(new Line(Instant.ofEpochMilli(1665056835113L), "test1"),
                               new Line(Instant.ofEpochMilli(1665056835113L), "test2"),
                               new Line(Instant.ofEpochMilli(1665056835113L), "test3"));
            }
        }
    }

    private Deployment.Builder getDeployment() {
        return Deployment.builder()
                         .setEnvironment(ENVIRONMENT)
                         .setDistribution(Distribution.builder()
                                                      .setDistributionId(DISTRIBUTION_ID)
                                                      .setDistributionName(DISTRIBUTION_NAME)
                                                      .build());
    }


    private void print(DeploymentPlanStatus deploymentPlanStatus, Deployment deployment) {
        deploymentPlanStatusPrinter.print(deploymentPlanStatus,
                                          EXECUTION_ARN,
                                          deployment);

    }
}
