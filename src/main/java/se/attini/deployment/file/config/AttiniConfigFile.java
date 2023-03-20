package se.attini.deployment.file.config;

import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.yaml.snakeyaml.error.MarkedYAMLException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import se.attini.domain.DistributionId;
import se.attini.domain.DistributionName;

public class AttiniConfigFile {

    private static final String ATTINI_DISTRIBUTION_NAME = "distributionName";
    private static final String ATTINI_DISTRIBUTION_ID = "distributionId";

    private static final String ATTINI_VERSION_ID = "version";

    private static final String ATTINI_DISTRIBUTION_TAGS = "distributionTags";

    private static final List<String> ILLEGAL_CHARS =
            List.of("[", "#", "%", "\\", "{", "}", "`", "~", "<", ">",
                    "|", "^", " ", "&", ";", "?", "$", ",", "+", "=", "@", "]");

    private final byte[] configFile;

    private final JsonNode jsonNode;
    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    private AttiniConfigFile(byte[] configFile) {
        this.configFile = requireNonNull(configFile, "configFile");
        this.jsonNode = getAsJson();
        JsonNode distributionName = jsonNode.path(ATTINI_DISTRIBUTION_NAME);
        if (distributionName.isMissingNode()) {
            throw new AttiniConfigFileException("distributionName is missing");
        }
        if (!distributionName.isValueNode()) {
            throw new AttiniConfigFileException("distributionName should be a string");
        }

        if (!distributionName.asText().matches("[a-zA-Z][-a-zA-Z0-9]*")) {
            throw new AttiniConfigFileException(
                    "invalid distributionName. Distribution name should match regular expression pattern: [a-zA-Z][-a-zA-Z0-9]*");
        }
        validateCharacters(ATTINI_DISTRIBUTION_NAME, distributionName.asText());
        getDistributionId()
                .map(DistributionId::getId)
                .ifPresent((value) -> validateCharacters(ATTINI_DISTRIBUTION_ID, value));

    }

    private static void validateCharacters(String fieldName,
                                           String value) {
        ILLEGAL_CHARS.forEach(s -> {
            if (value.contains(s)) {
                throw new AttiniConfigFileException("Illegal character in " + fieldName);
            }
        });
    }

    protected static AttiniConfigFile create(byte[] file) {
        return new AttiniConfigFile(file);
    }

    public Map<String, String> getDistributionTags() {


        LinkedHashMap<String, String> value = objectMapper.convertValue(jsonNode.get(ATTINI_DISTRIBUTION_TAGS),
                                                                        new TypeReference<>() {
                                                                        });

        return value != null ? value : Collections.emptyMap();

    }

    public DistributionName getDistributionName() {
        return DistributionName.create(getAsJson().path(ATTINI_DISTRIBUTION_NAME).asText());
    }

    public Optional<DistributionId> getDistributionId() {
        return getValue(ATTINI_DISTRIBUTION_ID).map(DistributionId::create);

    }

    public Optional<String> getVersion() {
        return getValue(ATTINI_VERSION_ID);

    }

    public Optional<String> getInitStackTemplatePath() {

        JsonNode templatePath = jsonNode
                .path("initDeployConfig")
                .path("template");

        if (templatePath.isMissingNode()) {
            return Optional.empty();
        }

        return Optional.of(templatePath.asText());
    }

    public JsonNode getAsJson() {
        try {
            return objectMapper.readTree(new String(configFile, StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            if (e.getCause() instanceof MarkedYAMLException) {
                throw new AttiniConfigFileException("Invalid yaml format: " + e.getCause().getMessage(), e);
            }
            throw new AttiniConfigFileException("Json Processing Exception when reading attini config", e);

        }
    }

    private Optional<String> getValue(String key) {

        JsonNode node = jsonNode.path(key);
        if (node.isMissingNode()) {
            return Optional.empty();
        }

        if (!node.isValueNode()) {
            throw new AttiniConfigFileException("Invalid format for " + key + ", value must be a string.");

        }

        if (node.asText().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(node.asText());

    }


}
