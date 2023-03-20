package se.attini.setup;

import static java.util.Objects.requireNonNull;
import static se.attini.setup.TemplateParameters.CREATE_DEPLOYMENT_PLAN_DEFAULT_ROLE;
import static se.attini.setup.TemplateParameters.CREATE_INIT_DEPLOY_DEFAULT_ROLE;
import static se.attini.setup.TemplateParameters.GIVE_ADMIN_ACCESS;
import static se.attini.setup.TemplateParameters.INIT_DEPLOY_ARN;
import static se.attini.setup.TemplateParameters.toParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import se.attini.cli.UserInputReader;
import se.attini.domain.IamRoleArn;
import software.amazon.awssdk.services.cloudformation.model.Parameter;

public class GuidedSetup {

    private final UserInputReader inputReader;

    public GuidedSetup(UserInputReader userInputReader) {
        this.inputReader = requireNonNull(userInputReader, "userInputReader");
    }

    List<Parameter> getParametersGuided(Map<String, Parameter> currentParams) {
        ArrayList<Parameter> parameters = new ArrayList<>();

        System.out.println("Would you like to give the Attini framework admin access? (Y/N)");
        parameters.add(getBooleanParameter(GIVE_ADMIN_ACCESS, currentParams));

        System.out.println("Would you like to use a default role the init deploy lambda? (Y/N)");

        Parameter useInitDefaultRole = getBooleanParameter(CREATE_INIT_DEPLOY_DEFAULT_ROLE, currentParams);
        parameters.add(useInitDefaultRole);
        if (useInitDefaultRole.parameterValue().equalsIgnoreCase("false")) {
            System.out.println("Please enter a role arn for the init deploy lambda");
            parameters.add(getIamArnParameter(INIT_DEPLOY_ARN, currentParams));
        }

        System.out.println("Would you like to use a default role for you deployment plans? (Y/N)");

        parameters.add(getBooleanParameter(CREATE_DEPLOYMENT_PLAN_DEFAULT_ROLE, currentParams));

        return parameters;
    }


    private Parameter getIamArnParameter(String parameter, Map<String, Parameter> currentParams) {
        if (currentParams.containsKey(parameter) && !currentParams.get(parameter).parameterValue().isEmpty()) {
            System.out.println("Current value: " + currentParams.get(parameter)
                                                                .parameterValue() + ", Press enter to keep");
        }

        Parameter readStringParameter = readIamArnParameter(parameter, currentParams);
        while (readStringParameter == null){
            System.out.println("Illegal value, please enter a valid role arn");
            readStringParameter = readIamArnParameter(parameter, currentParams);
        }

        return readStringParameter;
    }

    private Parameter readIamArnParameter(String parameter, Map<String, Parameter> currentParams) {
        try {
            String userInput = IamRoleArn.create(inputReader.getUserInput()).getValue();
            if (!userInput.isEmpty() && !userInput.isBlank()) {
                return toParameter(parameter, userInput);
            } else if (userInput.isEmpty() && currentParams.containsKey(parameter) && !currentParams.get(parameter).parameterValue().isEmpty()) {
                System.out.println("Keeping current setting");
                return currentParams.get(parameter);
            } else {
                return null;
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    private Parameter getBooleanParameter(String parameter, Map<String, Parameter> currentParams) {
        if (currentParams.containsKey(parameter)) {
            System.out.println("Current value: " + (currentParams.get(parameter)
                                                                 .parameterValue()
                                                                 .equalsIgnoreCase("true") ? "Y" : "N") + ", Press enter to keep");
        }

        Parameter readBooleanParameter = readBooleanParameter(parameter, currentParams);

        while (readBooleanParameter == null){
            System.out.println("Illegal value, please enter either Y or N");
            readBooleanParameter  = readBooleanParameter(parameter, currentParams);
        }

        return readBooleanParameter;
    }

    private Parameter readBooleanParameter(String parameter, Map<String, Parameter> currentParams) {
        String userInput = inputReader.getUserInput();
        if (userInput.equalsIgnoreCase("Y") || userInput.equalsIgnoreCase("N")) {
            return toParameter(parameter, userInput.equalsIgnoreCase("Y"));
        } else if (userInput.equalsIgnoreCase("") && currentParams.containsKey(parameter) && !currentParams.get(parameter).parameterValue().isEmpty()) {
            System.out.println("Keeping current setting");
            return currentParams.get(parameter);
        } else {
            return null;
        }
    }
}
