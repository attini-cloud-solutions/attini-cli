package se.attini;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import se.attini.client.AwsClientFactory;
import se.attini.domain.DistributionId;
import se.attini.domain.DistributionName;
import se.attini.domain.Environment;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

public class DistributionDataFacade {

    private final AwsClientFactory awsClientFactory;

    public DistributionDataFacade(AwsClientFactory awsClientFactory) {
        this.awsClientFactory = awsClientFactory;
    }

    public Optional<DistData> getDistributionData(Environment environment,
                                                   DistributionName distributionName) {

        String s = environment.getName().getName() + "-" + distributionName.getName();
        try( DynamoDbClient dynamoDbClient = awsClientFactory.dynamoClient()) {
            Map<String, AttributeValue> item = dynamoDbClient
                    .getItem(GetItemRequest.builder().tableName(
                                                   "AttiniResourceStatesV1")
                                           .key(Map.of("resourceType",
                                                       AttributeValue.builder()
                                                                     .s("Distribution")
                                                                     .build(),
                                                       "name",
                                                       AttributeValue.builder()
                                                                     .s(s)
                                                                     .build()))
                                           .build())
                    .item();
            AttributeValue attributeValue = item.get("distributionId");

            if (attributeValue != null) {

                AttributeValue distributionTags = item.get("distributionTags");

                Map<String, String> distTags = distributionTags == null ? Collections.emptyMap() : distributionTags.m()
                                                                                                                   .entrySet()
                                                                                                                   .stream()
                                                                                                                   .collect(
                                                                                                                           Collectors.toMap(
                                                                                                                                   Map.Entry::getKey,
                                                                                                                                   entry -> entry.getValue()
                                                                                                                                                 .s()));

                String version = item.get("version") != null ? item.get("version").s() : null;
                return Optional.of(new DistData(DistributionId.create(attributeValue.s()), distTags, version));
            }
            return Optional.empty();
        }
    }

    public record DistData(DistributionId distId, Map<String, String> distTags, String version) {

    }
}
