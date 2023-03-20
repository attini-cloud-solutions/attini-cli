package se.attini.pack;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import se.attini.deployment.file.config.AttiniConfigFileException;
import se.attini.deployment.file.config.AttiniConfigFiles;

public class EditAttiniConfigService {

    private final AttiniConfigFiles attiniConfigFiles;
    private final EditConfigService editConfigService;

    public EditAttiniConfigService(AttiniConfigFiles attiniConfigFiles, EditConfigService editConfigService) {
        this.attiniConfigFiles = requireNonNull(attiniConfigFiles, "attiniConfigFiles");
        this.editConfigService = requireNonNull(editConfigService, "editConfigService");
    }

    public void setProperty(Path path,
                            List<String> propertyPath,
                            String key,
                            String value,
                            boolean override) {

        File configFile = attiniConfigFiles.getRawAttiniConfigFile(path)
                                           .orElseThrow(() -> new AttiniConfigFileException(
                                                   "No attini-config file found on given path"));

        editConfigService.setProperty(configFile, propertyPath, key, value, override);


    }



}
