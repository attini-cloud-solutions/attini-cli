package se.attini.pack;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import se.attini.deployment.file.config.AttiniConfigFileException;

public class EditConfigService {

    private static final Set<String> VALID_FILE_EXTENSIONS = Set.of("yml", "yaml", "json");

    public void setProperty(File configFile,
                            List<String> propertyPath,
                            String key,
                            String value,
                            boolean override) {

        if (!validFileExtension(configFile)) {
            throw new AttiniConfigFileException("The following file extensions are supported="+VALID_FILE_EXTENSIONS);
        }


        ObjectMapper objectMapper = getObjectMapper(configFile.getName());
        try {
            JsonNode startNode = objectMapper.readTree(configFile);
            ObjectNode node = getNode(propertyPath, startNode);
            if (override || node.path(key).isMissingNode()) {
                node.put(key, value);
                objectMapper.writeValue(configFile, startNode);
            } else if (!node.path(key).isMissingNode()) {
                throw new PropertyExistsException("Property with key: "+ key +" already exists");
            }


        } catch (IOException e) {
            throw new AttiniConfigFileException("Could not edit config file =" + configFile.getPath());
        }

    }

    private boolean validFileExtension(File file) {
        return VALID_FILE_EXTENSIONS.contains(FilenameUtils.getExtension(file.getName()));
    }

    private ObjectMapper getObjectMapper(String fileName) {
        if (fileName.endsWith(".json")) {
            return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        }
        return new ObjectMapper(new YAMLFactory());
    }

    private ObjectNode getNode(List<String> path, JsonNode startNode) {
        ObjectNode node = (ObjectNode) startNode;
        for (String value : path) {
            if (node.path(value).isMissingNode()) {
                node.putObject(value);
            }
            node = (ObjectNode) node.path(value);
        }
        return node;

    }

}
