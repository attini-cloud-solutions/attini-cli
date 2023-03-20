package se.attini.util;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import se.attini.cli.global.GlobalConfig;

public class ObjectMapperFactory {

    private ObjectMapper jsonMapper;
    private ObjectMapper yamlMapper;
    private final GlobalConfig globalConfig;

    public ObjectMapperFactory(GlobalConfig globalConfig) {
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
    }

    public ObjectMapper getObjectMapper() {
       return globalConfig.printAsJson() ? getJsonMapper() : getYamlMapper();
    }
    public ObjectMapper getYamlMapper(){
        if (yamlMapper == null){
            yamlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER).disable(
                    YAMLGenerator.Feature.SPLIT_LINES).enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        }

        return yamlMapper;
    }

    public ObjectMapper getJsonMapper(){
        if (jsonMapper == null){
            jsonMapper = new ObjectMapper();
        }

        return jsonMapper;
    }
}
