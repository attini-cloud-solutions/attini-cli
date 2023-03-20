package se.attini.pack;

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import se.attini.EnvironmentVariables;
import se.attini.cli.deployment.DataEmitter;
import se.attini.cli.global.GlobalConfig;
import se.attini.deployment.file.config.AttiniConfigFiles;

public class LifeCycleHooksService {

    private final AttiniConfigFiles attiniConfigFiles;
    private final EnvironmentVariables environmentVariables;
    private final DataEmitter emitter;
    private final GlobalConfig globalConfig;

    public LifeCycleHooksService(AttiniConfigFiles attiniConfigFiles,
                                 EnvironmentVariables environmentVariables,
                                 DataEmitter dataEmitter,
                                 GlobalConfig globalConfig) {
        this.attiniConfigFiles = requireNonNull(attiniConfigFiles, "attiniConfigFiles");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.emitter = requireNonNull(dataEmitter, "dataEmitter");
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
    }

    public void runPreBuildCommands(Path path, Path envConfigPath) {
        runCommandsForPhase(path, "prePackage", envConfigPath);

    }

    public void runPostBuildCommands(Path path, Path envConfigPath) {
        runCommandsForPhase(path, "postPackage", envConfigPath);
    }

    public void runCommandsForPhase(Path path, String phase, Path envConfigPath) {
        final String marker = "#######";

        getCommandsForPhase(attiniConfigFiles.getAttiniConfigFile(path).getAsJson(), phase)
                .ifPresent(arrayNode -> {
                    emitter.emitString(marker + " Executing " + phase + " commands " + marker);
                    emitter.emitNewLine();
                    File file = new File(path.toAbsolutePath() + "/attini-" + phase + ".sh");
                    createNewFile(file);
                    runCommands(arrayNode, emitter, file, path.toAbsolutePath(), envConfigPath);
                    emitter.emitNewLine();
                    emitter.emitString(marker + " Done with " + phase + " commands " + marker);

                });
    }

    private void createNewFile(File file) {
        try {
            boolean newFile = file.createNewFile();
            if (!newFile) {
                throw new AttiniFileSystemException("Could not create script file, file with name" + file.getName() + " already exists");

            }
        } catch (IOException e) {
            throw new AttiniFileSystemException("Could not create script file", e);
        }
    }

    private void runCommands(ArrayNode arrayNode,
                             DataEmitter emitter,
                             File file,
                             Path workingDir,
                             Path envConfigPath) {

        try {
            Iterator<JsonNode> iterator = arrayNode.iterator();

            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(file));
            fileWriter.write("set -e");
            fileWriter.newLine();
            fileWriter.write("export ATTINI_WORK_DIR=" + workingDir);
            fileWriter.newLine();
            fileWriter.write("cd $ATTINI_WORK_DIR");
            fileWriter.newLine();
            if (globalConfig.printAsJson()){
                fileWriter.write("export ATTINI_DISABLE_ANSI_COLOR=true");
                fileWriter.newLine();
            }
            if (envConfigPath != null) {
                Path absolutePath = envConfigPath.isAbsolute() ? envConfigPath : Path.of(workingDir.toString(),
                                                                                         envConfigPath.toString());
                if (!absolutePath.toFile().exists()) {
                    throw new AttiniFileSystemException("Could not find file: " + absolutePath);

                }
                emitter.emitKeyValue("Sourcing environment config", absolutePath.toString());
                fileWriter.write("source '" + absolutePath + "'");
                fileWriter.newLine();
                giveExecuteAccess(absolutePath);
            }
            while (iterator.hasNext()) {
                String str = iterator.next().asText();
                fileWriter.write(str);
                fileWriter.newLine();
            }

            fileWriter.close();

            giveExecuteAccess(file.toPath());

            runScriptFile(file, emitter);
            if (!file.delete()) {
                emitter.emitString("Could not delete script file");
            }

        } catch (IOException e) {
            throw new AttiniFileSystemException("Could not run commands", e);
        }
    }

    private void giveExecuteAccess(Path file) {

        String command = chmodFileCommand(file.toString());

        try {
            int exitCode = new ProcessBuilder()
                    .redirectErrorStream(true)
                    .inheritIO()
                    .command(List.of(environmentVariables.getShell(),
                                     "-c",
                                     command))
                    .start()
                    .waitFor();

            if (exitCode != 0) {
                throw new ScriptExecutionException("Failed to give execute access to script, exit code=" + exitCode + ", command=" + command);
            }

        } catch (InterruptedException | IOException e) {
            throw new ScriptExecutionException("Failed to give execute access to script, error=" + e.getMessage());

        }

    }

    private static String chmodFileCommand(String file) {
        return "chmod +x " + file;
    }

    private void runScriptFile(File file, DataEmitter dataEmitter) {
        try {


            int exitCode = runCommands(file, dataEmitter);

            if (exitCode != 0) {
                throw new ScriptExecutionException("Failed to to run script, exit code=" + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            throw new ScriptExecutionException("could not run script file: " + e.getMessage());
        }
    }

    private int runCommands(File file, DataEmitter dataEmitter) throws IOException, InterruptedException {
        if (globalConfig.printAsJson()) {
            Process process = new ProcessBuilder()
                    .redirectErrorStream(true)
                    .command(List.of(environmentVariables.getShell(),
                                     "-c",
                                     file.getPath()))
                    .start();

            try (BufferedReader reader = process.inputReader()) {
                reader.lines()
                      .forEach(dataEmitter::emitString);
            }
            return process.waitFor();
        }
        return new ProcessBuilder().redirectErrorStream(true)
                                   .inheritIO()
                                   .command(file.getPath())
                                   .start()
                                   .waitFor();
    }

    private static Optional<ArrayNode> getCommandsForPhase(JsonNode jsonNode, String phase) {

        JsonNode commands = jsonNode.path("package").path(phase).path("commands");
        if (commands.isMissingNode() || !(commands instanceof ArrayNode)) {
            return Optional.empty();
        }
        return Optional.of((ArrayNode) commands);
    }
}
