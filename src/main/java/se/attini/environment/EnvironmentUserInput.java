package se.attini.environment;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.stream.Collectors;

import se.attini.ClientWithEnvironmentRequest;
import se.attini.cli.UserInputReader;
import se.attini.cli.global.GlobalConfig;
import se.attini.domain.Environment;
import se.attini.domain.EnvironmentName;

public class EnvironmentUserInput {

    private final EnvironmentService environmentService;
    private final UserInputReader userInputReader;
    private final GlobalConfig globalConfig;

    public EnvironmentUserInput(EnvironmentService environmentService,
                                UserInputReader userInputReader, GlobalConfig globalConfig) {
        this.environmentService = requireNonNull(environmentService, "environmentService");
        this.userInputReader = requireNonNull(userInputReader, "userInputReader");
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
    }

    public Environment getEnvironment(ClientWithEnvironmentRequest request) {

        List<Environment> environments =
                environmentService.getEnvironments();

        if (request.getEnvironment().isEmpty() && environments.size() == 1) {
            return environments.get(0);
        }

        if (request.getEnvironment().isEmpty() && environments.size() > 1) {
            System.out.println(
                    "More then one environment detected in account." +
                    " Please enter what environment should be used. Environments in account: "
                    + environments.stream()
                                  .map(Environment::getName)
                                  .map(EnvironmentName::getName)
                                  .collect(Collectors.joining(",")));
            EnvironmentName environmentName = EnvironmentName.create(userInputReader.getUserInput());

            return environments.stream()
                               .filter(environment -> environment.getName().equals(environmentName))
                               .findFirst()
                               .orElseThrow(() -> new RuntimeException("Environment " + environmentName.getName() + " does not exist in account"));
        }

        if (environments.isEmpty()) {
            throw new RuntimeException(
                    "No environment is setup in the current account. create one with command:\n\n\t" + buildCreateEnvCommand(
                            request) + "\n");

        }

       return environments.stream()
                    .filter(environment -> environment.getName().equals(request.getEnvironment().get()))
                    .findAny()
                    .orElseThrow(() -> {
                        String message = "Current account and region does not contain environment = "
                                         + request.getEnvironment().get().getName() +
                                         ". Create it with command: \n\n\t" + buildCreateEnvCommand(request) + "\n";
                        throw new RuntimeException(message);
                    });
    }

    private String buildCreateEnvCommand(ClientWithEnvironmentRequest request) {
        return "attini environment create " + request.getEnvironment()
                                                     .map(EnvironmentName::getName)
                                                     .orElse("<environment-name>")
               + " " + globalConfig.getProfile()
                              .map(profile -> "-p " + profile.getProfileName())
                              .orElse("")
               + " " + globalConfig.getRegion()
                              .map(region -> "-r " + region.getName())
                              .orElse("");
    }

}
