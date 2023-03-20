package se.attini.deployment;

import static java.util.Objects.requireNonNull;
import static se.attini.domain.EnvironmentType.PRODUCTION;

import se.attini.DistributionDataFacade;
import se.attini.cli.PrintUtil;
import se.attini.cli.UserInputReader;
import se.attini.deployment.file.config.AttiniConfigFile;
import se.attini.domain.DistributionName;
import se.attini.domain.Environment;

public class ConfirmDeploymentUserInput {

    private final UserInputReader userInputReader;
    private final DistributionDataFacade distributionDataFacade;

    public ConfirmDeploymentUserInput(UserInputReader userInputReader, DistributionDataFacade distributionDataFacade) {
        this.userInputReader = requireNonNull(userInputReader, "userInputReader");
        this.distributionDataFacade = requireNonNull(distributionDataFacade, "distributionDataFacade");
    }

    public void confirmDeployment(Environment environment,
                                  DistributionName distributionName,
                                  AttiniConfigFile attiniConfigFile) {
        if (environment.getType() == PRODUCTION) {

            distributionDataFacade.getDistributionData(environment, distributionName)
                                  .ifPresent(distData -> {
                                      System.out.println();
                                      System.out.println("Current state for " + distributionName.getName() + " in " + environment.getName()
                                                                                                                                 .getName() + ":");
                                      System.out.println("\tDistributionId: " + PrintUtil.toBlue(distData.distId()
                                                                                                         .getId()));
                                      if (distData.version() != null) {
                                          System.out.println("\tVersion: " + PrintUtil.toBlue(distData.version()));
                                      }
                                      if (!distData.distTags().isEmpty()) {
                                          System.out.println("\tDistributionTags:");
                                          distData.distTags()
                                                  .forEach((s, s2) -> System.out.println("\t\t" + s + ": " + s2));
                                      }


                                  });

            System.out.println();
            System.out.println("Distribution being deployed:");
            attiniConfigFile.getDistributionId()
                            .ifPresent(distributionId -> System.out.println("\tDistributionId: " + PrintUtil.toBlue(
                                    distributionId.getId())));
            attiniConfigFile.getVersion()
                            .ifPresent(version -> System.out.println("\tVersion: " + PrintUtil.toBlue(version)));
            if (!attiniConfigFile.getDistributionTags().isEmpty()) {
                System.out.println("\tDistributionTags:");
                attiniConfigFile.getDistributionTags()
                                .forEach((s, s2) -> System.out.println("\t\t" + s + ": " + s2));
            }


            System.out.println();

            System.out.println("You are about to deploy " + PrintUtil.toBlue(distributionName.getName()) + " to " + PrintUtil.toBlue(
                    environment.getName()
                               .getName()) + ".");
            System.out.println("Continue? (Y/N)");

            String userInput = userInputReader.getUserInput();

            boolean confirmed = userInput.equalsIgnoreCase("Y");
            if (!confirmed) {
                throw new DeploymentAbortedException("Deployment aborted");
            }

            System.out.println("Continuing deployment");
        }
    }


}
