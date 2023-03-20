package se.attini.deployment.file.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.JsonNode;

import se.attini.domain.DistributionId;
import se.attini.domain.DistributionName;

public class AttiniConfigFiles {

    private static final Set<String> ATTINI_CONFIG_FILES = Set.of("attini-config.json",
                                                                  "attini-config.yml",
                                                                  "attini-config.yaml");


    /**
     * Get the attini config file. Should only be used for editing the file
     *
     * @param path to the distribution root
     * @return a java.io.File object
     */
    public Optional<File> getRawAttiniConfigFile(Path path) {
        File[] files = path.toFile().listFiles(file -> ATTINI_CONFIG_FILES.contains(file.getName()));
        if (files == null || files.length == 0) {
            return Optional.empty();
        }

        if (files.length > 1) {
            throw new AttiniConfigFileException("More then one attini-config file in directory");
        }
        return Optional.of(files[0]);
    }

    public AttiniConfigFile getAttiniConfigFile(Path path) {
        File[] files = path.toFile().listFiles(file -> ATTINI_CONFIG_FILES.contains(file.getName()));
        if (files == null || files.length == 0) {
            throw new AttiniConfigFileException("No attini-config file in directory");

        }

        if (files.length > 1) {
            throw new AttiniConfigFileException("More then one attini-config file in directory");
        }

        try {
            return AttiniConfigFile.create(Files.readAllBytes(files[0].toPath()));
        } catch (IOException e) {
            throw new AttiniConfigFileException("Could not read attini config file", e);

        }
    }

    public List<String> getLoginCommands(Path path) {
        JsonNode imageNode = getAttiniConfigFile(path)
                .getAsJson()
                .path("package")
                .path("container")
                .path("loginCommands");

        if (imageNode.isMissingNode()) {
            return Collections.emptyList();
        }
        if (!imageNode.isArray()) {
            throw new AttiniConfigFileException(
                    "Image login commands should be specified as a list");
        }
        return StreamSupport
                .stream(imageNode.spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList());
    }

    public List<String> getDockerOptions(Path path) {
        JsonNode optionsNode = getAttiniConfigFile(path)
                .getAsJson()
                .path("package")
                .path("container")
                .path("options");


        if (optionsNode.isMissingNode()) {
            return Collections.emptyList();
        }

        if (!optionsNode.isArray()) {
            throw new AttiniConfigFileException(
                    "Docker options should be specified as a list");
        }

        return StreamSupport
                .stream(optionsNode.spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toList());
    }

    public String getImageURI(Path path) {
        JsonNode imageNode =
                getAttiniConfigFile(path)
                        .getAsJson()
                        .path("package")
                        .path("container")
                        .path("image");
        if (imageNode.isMissingNode()) {
            throw new AttiniConfigFileException("image URI is missing from attini config");
        }
        if (!imageNode.isTextual()) {
            throw new AttiniConfigFileException("image URI is not a valid string");

        }
        return imageNode.asText();
    }


    public DistributionName getDistributionName(Path path) {
        return getAttiniConfigFile(path).getDistributionName();
    }

    public Optional<Path> getInitTemplatePath(Path path) {
        return getAttiniConfigFile(path)
                .getInitStackTemplatePath()
                .map(s -> Path.of(path.toAbsolutePath().toString(), s));

    }

    public Optional<DistributionId> getDistributionId(Path path) {
        return getAttiniConfigFile(path).getDistributionId();

    }

    /**
     * Will return an instance of AttiniConfigFile e if present in the provided zip archive.
     * Will throw an exception if no file is present or more then one file is present
     *
     * @param file a zipped distribution
     * @return AttiniConfigFile
     */
    public AttiniConfigFile getAttiniConfigFile(byte[] file) {
        try {
            return getAttiniConfigFileFromBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private AttiniConfigFile getAttiniConfigFileFromBytes(byte[] file) throws IOException {
        try (ZipInputStream zi = new ZipInputStream(new ByteArrayInputStream(file))) {
            ZipEntry zipEntry;
            while ((zipEntry = zi.getNextEntry()) != null) {
                if (ATTINI_CONFIG_FILES.contains(zipEntry.getName())) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[9000];
                    int len;
                    while ((len = zi.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, len);
                    }
                    byteArrayOutputStream.close();
                    return AttiniConfigFile.create(byteArrayOutputStream.toByteArray());
                }
            }
            throw new AttiniConfigFileException("No attini-config file is present in zip archive");
        }

    }
}
