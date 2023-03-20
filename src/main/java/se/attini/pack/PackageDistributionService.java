package se.attini.pack;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static se.attini.cli.PrintUtil.Color.BLUE;
import static se.attini.deployment.file.ignore.FilePatterns.TEMP_DIR_IGNORES;
import static se.attini.deployment.zip.ZipUtil.zipDirectory;
import static se.attini.util.PathUtil.transformPathToRelative;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import se.attini.EnvironmentVariables;
import se.attini.cli.PrintUtil;
import se.attini.cli.deployment.DataEmitter;
import se.attini.cli.global.GlobalConfig;
import se.attini.deployment.file.FileUtil;
import se.attini.deployment.file.config.AttiniConfigFileException;
import se.attini.deployment.file.config.AttiniConfigFiles;
import se.attini.deployment.file.ignore.AttiniIgnoreFile;
import se.attini.deployment.file.ignore.AttiniIgnoreFiles;
import se.attini.deployment.file.ignore.FilePatterns;
import se.attini.domain.DistributionId;

public class PackageDistributionService {

    private final LifeCycleHooksService lifeCycleHooksService;
    private final AttiniIgnoreFiles attiniIgnoreFiles;
    private final AttiniConfigFiles attiniConfigFiles;
    private final EditAttiniConfigService editAttiniConfigService;
    private final DataEmitter dataEmitter;
    private final PrepareDistributionService prepareDistributionService;
    private final EnvironmentVariables environmentVariables;
    private final GlobalConfig globalConfig;

    public PackageDistributionService(LifeCycleHooksService lifeCycleHooksService,
                                      AttiniIgnoreFiles attiniIgnoreFiles,
                                      AttiniConfigFiles attiniConfigFiles,
                                      EditAttiniConfigService editAttiniConfigService,
                                      PrepareDistributionService prepareDistributionService,
                                      EnvironmentVariables environmentVariables,
                                      DataEmitter dataEmitter,
                                      GlobalConfig globalConfig) {
        this.lifeCycleHooksService = requireNonNull(lifeCycleHooksService, "lifeCycleHooksService");
        this.attiniIgnoreFiles = requireNonNull(attiniIgnoreFiles, "attiniIgnoreFiles");
        this.attiniConfigFiles = requireNonNull(attiniConfigFiles, "attiniConfigFiles");
        this.editAttiniConfigService = requireNonNull(editAttiniConfigService, "editAttiniConfigService");
        this.dataEmitter = requireNonNull(dataEmitter, "dataEmitter");
        this.prepareDistributionService = requireNonNull(prepareDistributionService, "initStackStateService");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
    }

    public void packageDistribution(Path path,
                                    Path destPath,
                                    Path envConfigPath,
                                    boolean containerBuild,
                                    boolean runLoginCommands,
                                    DistributionId distributionId,
                                    boolean skipCommands,
                                    String version) {


        File distFolder = new File(path + "/attini_dist");
        if (distFolder.exists() && distFolder.isDirectory()) {
            deleteDirectory(distFolder);
        }

        if (containerBuild) {
            String loginCommandsString = runLoginCommands ? getLoginCommands(path) : "";

            String envConfigPathFlag = envConfigPath == null ? "" : " --environment-config-script " + transformPathToRelative(
                    path,
                    envConfigPath);


            String distIdFlag = distributionId == null ? "" : " -i " + distributionId.getId();

            String skippCommands = skipCommands ? " --skip-commands " : "";

            String debugCommand = globalConfig.isDebug() ? " --debug " : "";

            String command = loginCommandsString + "docker run -v " + path.toAbsolutePath() + ":/mnt/attini " + getDockerOptions(
                    path) + "-w /mnt/attini " + attiniConfigFiles.getImageURI(path) + " /bin/bash -c \"attini distribution package ." + envConfigPathFlag + distIdFlag + skippCommands + debugCommand + "\"";

            dataEmitter.emitKeyValue("Package command", command);
            int exitCode = runCommand(command);

            if (exitCode != 0) {
                throw new ScriptExecutionException("Failed to run container build command");
            }

        } else {

            dataEmitter.emitKeyValue("Packaging distribution",
                                     attiniConfigFiles.getDistributionName(path).getName(),
                                     BLUE);

            File destFile = destPath.toFile();
            if (destFile.delete()) {
                dataEmitter.emitString("Deleted existing distribution in directory");
            }
            dataEmitter.emitString("Creating temp directory");

            Path tempDirectory = createTempDirectory();


            addCleanupHook(tempDirectory);
            copyDirectory(path, tempDirectory);

            if (version != null) {
                validateVersion(version);
                editAttiniConfigService.setProperty(tempDirectory, emptyList(), "version", version, true);
                dataEmitter.emitKeyValue("Distribution version set to", version, BLUE);

            }

            if (distributionId != null) {
                editAttiniConfigService.setProperty(tempDirectory,
                                                    emptyList(),
                                                    "distributionId",
                                                    distributionId.getId(),
                                                    true);

            }

            if (!skipCommands) {
                lifeCycleHooksService.runPreBuildCommands(tempDirectory, envConfigPath);
            }

            if (attiniConfigFiles.getDistributionId(tempDirectory).isEmpty()) {
                dataEmitter.emitString("No distributionId set, consider setting it via a pre-package command. " +
                                       "Setting a random id");
                editAttiniConfigService.setProperty(tempDirectory,
                                                    emptyList(),
                                                    "distributionId",
                                                    UUID.randomUUID().toString(),
                                                    false);


            }

            attiniConfigFiles.getInitTemplatePath(tempDirectory)
                             .ifPresent(initTemplatePath -> prepareDistributionService.prepareDistribution(
                                     initTemplatePath, tempDirectory));

            AttiniIgnoreFile attiniIgnoreFile = attiniIgnoreFiles.getAttiniIgnoreFile(tempDirectory);
            FileUtil.validateDirectory(tempDirectory, attiniIgnoreFile.getIgnores(tempDirectory));

            byte[] bytes = zipDirectory(tempDirectory, attiniIgnoreFile.getIgnores(tempDirectory));

            File zip = createZip(bytes, destPath.getFileName().toString(), tempDirectory);


            if (!skipCommands) {
                lifeCycleHooksService.runPostBuildCommands(tempDirectory, envConfigPath);
            }

            moveFile(destFile, zip);
            dataEmitter.emitString("Distribution created: " + PrintUtil.toGreen(destFile.toPath().toString()));

        }


    }

    private static void moveFile(File destFile, File zip) {
        try {
            FileUtils.moveFile(zip,
                               destFile,
                               StandardCopyOption.REPLACE_EXISTING);

        }catch (IOException e){
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteDirectory(File distFolder) {
        try{
            FileUtils.deleteDirectory(distFolder);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void validateVersion(String version) {
        String regex = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";
        if (!version.matches(regex)) {
            throw new IllegalArgumentException(
                    "Invalid format for version. Version should follow the semantic version format.");
        }
    }

    private int runCommand(String command) {
        try {

            Process process = new ProcessBuilder()
                    .redirectErrorStream(true)
                    .inheritIO()
                    .command(List.of(environmentVariables.getShell(),
                                     "-c",
                                     command))
                    .start();
            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new ScriptExecutionException("There was en error executing commands", e);
        }
    }

    private String getLoginCommands(Path path) {

        List<String> loginCommands = attiniConfigFiles.getLoginCommands(path);

        if (loginCommands.isEmpty()) {
            throw new AttiniConfigFileException(
                    "--container-repository-login was specified but no login commands are present in the attini-config file");
        }

        return String.join(" && ", loginCommands) + " && ";

    }


    private String getDockerOptions(Path path) {
        List<String> options = attiniConfigFiles.getDockerOptions(path);
        if (options.isEmpty()) {
            return " ";
        }
        return String.join(" ", options) + " ";
    }

    private File createZip(byte[] bytes, String fileName, Path path) {
        try {
            return Files.write(Path.of(path.toString() + "/" + fileName), bytes).toFile();
        } catch (IOException e) {
            throw new AttiniFileSystemException("Could not create final package", e);
        }
    }

    private Path createTempDirectory() {
        try {
            return Files.createTempDirectory("attini-temp");
        } catch (IOException e) {
            throw new AttiniFileSystemException("Could not create temp directory", e);
        }
    }

    private void copyDirectory(Path path, Path tempDirectory) {
        try {
            FileUtils.copyDirectory(path.toFile(),
                                    tempDirectory.toFile(),
                                    pathname -> !FilePatterns.patternMatchPath(TEMP_DIR_IGNORES,
                                                                               pathname.toPath()) && !Files.isSymbolicLink(
                                            pathname.toPath()),
                                    true,
                                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AttiniFileSystemException("Could not copy files", e);
        }
    }

    private void addCleanupHook(Path tempDirectory) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                FileUtils.deleteDirectory(tempDirectory.toFile());
            } catch (IOException e) {
                System.out.println("Could not clean up directory =" + tempDirectory);
            }
        }));
    }
}
