package se.attini.logs;

import static java.util.Objects.requireNonNull;
import static se.attini.cli.PrintItem.PrintType.NORMAL_SAME_LINE;
import static se.attini.cli.PrintItem.newLine;
import static software.amazon.awssdk.services.cloudwatchlogs.model.ExportTaskStatusCode.CANCELLED;
import static software.amazon.awssdk.services.cloudwatchlogs.model.ExportTaskStatusCode.COMPLETED;
import static software.amazon.awssdk.services.cloudwatchlogs.model.ExportTaskStatusCode.FAILED;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import se.attini.cli.ConsolePrinter;
import se.attini.cli.PrintItem;
import se.attini.client.AwsClientFactory;
import se.attini.profile.ProfileFacade;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateExportTaskRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateExportTaskResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeExportTasksRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.ExportTaskStatusCode;
import software.amazon.awssdk.services.sts.StsClient;

public class ExportLogsService {

    private final AwsClientFactory awsClientFactory;
    private final ProfileFacade profileFacade;
    private final ConsolePrinter consolePrinter;

    private static final List<String> LAMBDAS = List.of("attini-action",
                                                        "attini-init-deploy",
                                                        "attini-step-guard",
                                                        "attini-deployment-plan-setup");

    private static final EnumSet<ExportTaskStatusCode> COMPLETED_STATES = EnumSet.of(COMPLETED,
                                                                                     CANCELLED,
                                                                                     FAILED);

    public ExportLogsService(AwsClientFactory awsClientFactory,
                             ProfileFacade profileFacade,
                             ConsolePrinter consolePrinter) {
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
        this.profileFacade = requireNonNull(profileFacade, "profileFacade");
        this.consolePrinter = requireNonNull(consolePrinter, "consolePrinter");
    }

    public void exportLogs(ExportLogsRequest request) {
            try(CloudWatchLogsClient cloudWatchClient = awsClientFactory.cloudWatchClient();
                StsClient stsClient = awsClientFactory.stsClient()){
                String accountId = stsClient.getCallerIdentity().account();
                String region = profileFacade.getRegion().getName();
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                for (String lambda : LAMBDAS){
                    exportLogAndWait(request, cloudWatchClient, accountId, region, lambda, timestamp);

                }
                consolePrinter.print(newLine());
                consolePrinter.print(PrintItem.successMessage("Exported all logs in timespan"));
            }

    }

    private void exportLogAndWait(ExportLogsRequest request,
                                  CloudWatchLogsClient cloudWatchClient,
                                  String accountId,
                                  String region,
                                  String lambdaName,
                                  String timestamp) {
        String taskId;
        taskId = exportLogs(request, cloudWatchClient, accountId, region, lambdaName, timestamp);
        pollForComplete(cloudWatchClient, taskId);
    }

    private void pollForComplete(CloudWatchLogsClient cloudWatchClient,
                                 String taskId) {
        ExportTaskStatusCode code = cloudWatchClient.describeExportTasks(
                                                            DescribeExportTasksRequest.builder().taskId(taskId).build())
                                                    .exportTasks()
                                                    .get(0)
                                                    .status()
                                                    .code();
        while (!COMPLETED_STATES.contains(code)) {
            consolePrinter.print(PrintItem.message(NORMAL_SAME_LINE, "Exporting logs, this may take few minutes"));
            try {
                TimeUnit.SECONDS.sleep(2);
                code = cloudWatchClient.describeExportTasks(
                                               DescribeExportTasksRequest.builder().taskId(taskId).build())
                                       .exportTasks()
                                       .get(0)
                                       .status()
                                       .code();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while polling cloud watch", e);
            }
        }
    }

    private String exportLogs(ExportLogsRequest request,
                              CloudWatchLogsClient cloudWatchClient,
                              String accountId,
                              String region,
                              String lambdaName,
                              String timestamp) {
        String destinationPrefix = String.format("%s-%s-%s",
                                                 accountId,
                                                 timestamp,
                                                 lambdaName);
        CreateExportTaskResponse exportTask =
                cloudWatchClient.createExportTask(CreateExportTaskRequest.builder()
                                                                         .logGroupName(
                                                                                 "/aws/lambda/" + lambdaName)
                                                                         .from(request.getStartTime())
                                                                         .to(request.getEndTime())
                                                                         .destinationPrefix(
                                                                                 destinationPrefix)
                                                                         .taskName(String.format(
                                                                                 "export-%s-logs",
                                                                                 lambdaName))
                                                                         .destination(
                                                                                 "attini-support-logs-" + region)
                                                                         .build());

        return exportTask.taskId();
    }
}
