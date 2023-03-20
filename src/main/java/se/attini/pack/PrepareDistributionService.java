package se.attini.pack;

import static java.util.Objects.requireNonNull;
import static se.attini.inittemplate.InitTemplateReader.DeploymentPlanFormat.SIMPLE;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import se.attini.EnvironmentVariables;
import se.attini.deployment.zip.ZipException;
import se.attini.deployment.zip.ZipUtil;
import se.attini.inittemplate.InitTemplateReader;
import se.attini.inittemplate.Steps;
import se.attini.pack.Metadata.SamProject;
import se.attini.util.ObjectMapperFactory;

public class PrepareDistributionService {


    private final ObjectMapperFactory objectMapperFactory;
    private final EnvironmentVariables environmentVariables;
    private final TransformSimpleSyntax transformSimpleSyntax;


    public PrepareDistributionService(ObjectMapperFactory objectMapperFactory,
                                      EnvironmentVariables environmentVariables,
                                      TransformSimpleSyntax transformSimpleSyntax) {
        this.objectMapperFactory = requireNonNull(objectMapperFactory, "objectMapperFactory");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.transformSimpleSyntax = requireNonNull(transformSimpleSyntax, "transformSimpleSyntax");
    }


    public void prepareDistribution(Path initTemplatePath, Path root) {
        try {

            InitTemplateReader initTemplateReader = InitTemplateReader.create(initTemplatePath.toFile(), objectMapperFactory.getYamlMapper());

            initTemplateReader.getDeploymentPlan()
                              .filter(deploymentPlanEntry -> deploymentPlanEntry.deploymentPlanFormat() == SIMPLE)
                              .ifPresent(deploymentPlanEntry -> {
                                  try {
                                      JsonNode deploymentPlan = deploymentPlanEntry.deploymentPlan()
                                                                         .get("Properties")
                                                                         .path("DeploymentPlan");
                                      JsonNode transformedSteps = transformSimpleSyntax.transform(deploymentPlan);
                                      JsonNode initTemplate = initTemplateReader.getInitTemplate();
                                      ObjectNode properties = (ObjectNode) deploymentPlanEntry.deploymentPlan()
                                                                                              .get("Properties");
                                      properties.set("DeploymentPlan", transformedSteps);
                                      objectMapperFactory.getJsonMapper()
                                                         .writeValue(initTemplatePath.toFile(), initTemplate);

                                  } catch (IOException e) {
                                      throw new UncheckedIOException(e);
                                  }
                              });


            Steps stepsByType = initTemplateReader.getStepsByType(Set.of("AttiniCfn",
                                                                         "AttiniMergeOutput",
                                                                         "AttiniRunnerJob",
                                                                         "AttiniSam"));

            Set<String> runners = stepsByType.get("AttiniRunnerJob")
                                             .stream()
                                             .map(step -> step.definition().path("Properties")
                                                              .path("Runner"))
                                             .filter(jsonNode -> !jsonNode.isMissingNode())
                                             .map(JsonNode::asText)
                                             .collect(Collectors.toSet());

            List<SamProject> samProjects = stepsByType.get("AttiniSam")
                                                      .stream()
                                                      .map(step -> {
                                                          JsonNode node = step.definition().path("Properties")
                                                                              .path("Project");
                                                          if (node.isMissingNode() && !node.isObject()) {
                                                              throw new IllegalArgumentException(
                                                                      "Missing Properties.Project in Sam step: " + step.name());
                                                          }

                                                          if (!node.isObject()) {
                                                              throw new IllegalArgumentException(
                                                                      " Illegal format for Properties.Project in Sam step: " + step.name() + ". Project should be an object");
                                                          }
                                                          return new SamProject(node.path("Path").textValue(),
                                                                                node.path("BuildDir").textValue(),
                                                                                node.path("Template").textValue(),
                                                                                step.name());
                                                      }).toList();

            Path attiniDataFolder = Files.createDirectories(Path.of(root.toString(), "attini_data"));

            if (!samProjects.isEmpty()) {
                Path samBuildFile = Files.createFile(Path.of(attiniDataFolder.toString(), "sam-package.sh"));
                Files.writeString(samBuildFile, SamPackageCommand.PACKAGE_COMMANDS);
            }

            samProjects.forEach(samProject -> {
                Path projectPath = Path.of(root.toString(), samProject.path());
                Path buildDir = Path.of(projectPath.toString(), samProject.buildDir());

                validateSamTemplate(samProject, projectPath);

                if (!buildDir.toFile().exists()) {
                    buildSamProject(samProject, projectPath);
                }
                zipSamProject(projectPath);

            });

            validateRunners(initTemplateReader, runners);

            Metadata metadata = new Metadata(runners, samProjects);

            objectMapperFactory.getJsonMapper()
                               .writeValue(new File(attiniDataFolder.toString(), "attini-metadata.json"), metadata);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }


    }

    private void validateSamTemplate(SamProject samProject, Path projectPath) {
        Path template = Path.of(projectPath.toString(), samProject.template());
        if (!template.toFile().exists()) {

            throw new IllegalArgumentException("template " + Path.of(samProject.path(),
                                                                     samProject.template()) + " in step " + samProject.stepName() + " does not exist");
        }

        try {


            objectMapperFactory.getYamlMapper()
                               .readTree(template.toFile())
                               .path("Resources")
                               .forEach(jsonNode -> {
                                   if ("AWS::Serverless::Function".equals(jsonNode.path("Type").asText())
                                       && "Image".equals(jsonNode.path("Properties").path("PackageType").asText())) {
                                       throw new IllegalArgumentException(
                                               "PackageType: Image found for AWS::Serverless::Function in template " + Path.of(
                                                       samProject.path(),
                                                       samProject.template()) + ". Image is not supported for AttiniSam steps. Step: " + samProject.stepName());
                                   }
                               });

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void buildSamProject(SamProject samProject, Path projectPath) {
        try {
            String buildDirOption = "--build-dir %s".formatted(samProject.buildDir());
            String templateOption = "--template %s".formatted(samProject.template());
            int exitCode = new ProcessBuilder()
                    .redirectErrorStream(true)
                    .inheritIO()
                    .command(List.of(environmentVariables.getShell(),
                                     "-c",
                                     "cd " + projectPath + "; sam build %s %s".formatted(buildDirOption,
                                                                                         templateOption)))
                    .start()
                    .waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Could not package sam, exit code: " + exitCode);
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void zipSamProject(Path path1) {
        try {
            byte[] bytes = ZipUtil.zipDirectory(path1, Collections.emptyList());
            Files.write(Path.of(path1.toString(), "attiniSamProject.zip"), bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ZipException e) {
            throw new RuntimeException("Could not zip sam project: " + e.getMessage(), e);
        }
    }

    private void validateRunners(InitTemplateReader initTemplateReader, Set<String> runners) {
        Set<String> presentRunners = initTemplateReader.getRunners();

        if (!presentRunners.containsAll(runners)) {
            List<String> missingRunners = runners.stream()
                                                 .filter(s -> !presentRunners.contains(s))
                                                 .toList();
            throw new IllegalStateException(
                    "There are runners used in the deployment plan that is not defined in the init stack. Missing runners: " + missingRunners);
        }

    }
}
