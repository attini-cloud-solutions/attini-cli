package se.attini.deployment;

import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.attini.domain.Region;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public class AttiniRunnerLogger implements StepLogger {

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    private final String key;

    private final String bucket;

    private int printedLines = 0;

    public AttiniRunnerLogger(S3Client s3Client,
                              StepLoggerFactory.GetLoggerRequest getLoggerRequest,
                              String stepName,
                              String accountId,
                              Region region, ObjectMapper objectMapper) {
        this.s3Client = requireNonNull(s3Client, "s3Client");
        this.objectMapper = objectMapper;
        this.key = createKey(getLoggerRequest, stepName);
        bucket = "attini-artifact-store-%s-%s".formatted(region.getName(), accountId);
    }

    @Override
    public List<Line> lines() {
        try {
            String[] split = new String(s3Client.getObject(GetObjectRequest.builder()
                                                                           .key(key)
                                                                           .bucket(bucket)
                                                                           .build(),
                                                           ResponseTransformer.toBytes())
                                                .asByteArray())
                    .split("\n");
            List<Line> lines = Stream.of(split)
                                     .skip(printedLines)
                                     .map(s -> {
                                         try {
                                             return objectMapper.readTree(s);
                                         } catch (JsonProcessingException e) {
                                             throw new UncheckedIOException(e);
                                         }
                                     })
                                     .map(jsonNode -> new Line(Instant.ofEpochMilli(jsonNode.get("timestamp").asLong()),
                                                               jsonNode.get("data").asText()))
                                     .toList();

            printedLines = printedLines + lines.size();

            return lines;
        } catch (NoSuchKeyException e) {
            return Collections.emptyList();
        }
    }

    private String createKey(StepLoggerFactory.GetLoggerRequest getLoggerRequest, String stepName) {
        return "attini/deployment/logs/runner/" + getLoggerRequest.environment().getName() +
               "/" + getLoggerRequest.distributionName().getName() +
               "/" + getLoggerRequest.distributionId().getId() +
               "/" + stepName +
               "/" + getLoggerRequest.executionArn()
                                     .getValue()
                                     .substring(getLoggerRequest.executionArn().getValue().lastIndexOf(":") + 1);

    }
}
