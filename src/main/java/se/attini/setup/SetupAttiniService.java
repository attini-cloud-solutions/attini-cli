package se.attini.setup;

import static java.lang.Boolean.FALSE;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static se.attini.cli.PrintUtil.Color.GREEN;
import static se.attini.cli.PrintUtil.Color.RED;
import static se.attini.cli.PrintUtil.Color.YELLOW;
import static se.attini.setup.TemplateParameters.ACCEPT_LICENSE_AGREEMENT;
import static se.attini.setup.TemplateParameters.ATTACH_LEAST_PRIVILEGE_INIT_DEPLOY_POLICY;
import static se.attini.setup.TemplateParameters.AUTO_UPDATE;
import static se.attini.setup.TemplateParameters.CREATE_DEPLOYMENT_PLAN_DEFAULT_ROLE;
import static se.attini.setup.TemplateParameters.CREATE_INIT_DEPLOY_DEFAULT_ROLE;
import static se.attini.setup.TemplateParameters.EMAIL;
import static se.attini.setup.TemplateParameters.ENVIRONMENT_PARAMETER_NAME;
import static se.attini.setup.TemplateParameters.GIVE_ADMIN_ACCESS;
import static se.attini.setup.TemplateParameters.INIT_DEPLOY_ARN;
import static se.attini.setup.TemplateParameters.LICENCE_TOKEN;
import static se.attini.setup.TemplateParameters.LOG_LVL;
import static se.attini.setup.TemplateParameters.RESOURCE_ALLOCATION;
import static se.attini.setup.TemplateParameters.RETAIN_DISTRIBUTION_DAYS;
import static se.attini.setup.TemplateParameters.RETAIN_DISTRIBUTION_VERSIONS;
import static se.attini.setup.TemplateParameters.SUBNET_IDS;
import static se.attini.setup.TemplateParameters.VPC_ID;
import static se.attini.setup.TemplateParameters.toParameter;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import se.attini.InvalidCredentialsException;
import se.attini.cli.PrintItem;
import se.attini.cli.PrintUtil;
import se.attini.cli.UserInputReader;
import se.attini.cli.deployment.DataEmitter;
import se.attini.client.AwsClientFactory;
import se.attini.domain.IamRoleArn;
import se.attini.domain.Region;
import se.attini.domain.StackName;
import se.attini.profile.ProfileFacade;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryRequest;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.ParameterDeclaration;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;

public class SetupAttiniService {

    private final static String URL_TEMPLATE = "https://attini-artifacts-%s.s3.%s.amazonaws.com/attini-setup/%s/attini-setup.yaml";
    private static final StackName STACK_NAME = StackName.create("attini-setup");
    private final AwsClientFactory awsClientFactory;
    private final ProfileFacade profileFacade;
    private final UserInputReader inputReader;
    private final GuidedSetup guidedSetup;
    private final DataEmitter dataEmitter;

    public SetupAttiniService(AwsClientFactory awsClientFactory,
                              ProfileFacade profileFacade,
                              UserInputReader inputReader,
                              GuidedSetup guidedSetup,
                              DataEmitter dataEmitter) {
        this.awsClientFactory = requireNonNull(awsClientFactory, "awsClientFactory");
        this.profileFacade = requireNonNull(profileFacade, "profileFacade");
        this.inputReader = requireNonNull(inputReader, "inputReader");
        this.guidedSetup = requireNonNull(guidedSetup, "guidedSetup");
        this.dataEmitter = requireNonNull(dataEmitter, "dataEmitter");
    }

    public void setup(SetupAttiniRequest request,
                                       boolean follow) {


            try(CloudFormationClient cloudFormationClient = awsClientFactory.cfnClient()) {
                if (request.getUseExistingVersion().orElse(FALSE) && request.getVersion().isPresent()) {
                    dataEmitter.emitString("Both --keep-version and --version has been specified. Select one and try again");
                    return;
                }

                Map<String, Parameter> existingParams = getExistingParams(cloudFormationClient);


                List<Parameter> parameters = request.isGuided() ? guidedSetup.getParametersGuided(existingParams) : allGivenParams(
                        request);

                validateParameters(parameters);


                String url = createUrl(profileFacade.getRegion(), request.getVersion().orElse(
                        "latest"));
                Boolean acceptedInCommand = request.getAcceptLicenceAgreement()
                                                   .orElse(FALSE);


                if (acceptedInCommand) {
                    parameters.add(toParameter(ACCEPT_LICENSE_AGREEMENT, true));
                } else if (!hasAlreadyAcceptedLicenceAgreement(existingParams)) {
                    dataEmitter.emitString(
                            "This is the first time you use Attini in this account and region. Therefore you need to accept the Attini licence agreement" +
                            " Read more here: https://docs.attini.io/pricing-and-license/product-offering.html");
                    dataEmitter.emitString("Accept Attini licence agreement? (Y/N)");
                    String input = inputReader.getUserInput();
                    if (input.equalsIgnoreCase("y") || input.equalsIgnoreCase("Yes")) {
                        parameters.add(toParameter(ACCEPT_LICENSE_AGREEMENT, true));
                    } else {
                        dataEmitter.emitPrintItem(PrintItem.errorMessage("Aborted deployment"));
                        return;
                    }
                }
                if (existingParams.isEmpty()) {
                    dataEmitter.emitString("Setting up Attini");
                    cloudFormationClient.createStack(createStackRequest(url, parameters));
                } else {
                    dataEmitter.emitString("Updating Attini");
                    updateStack(request, cloudFormationClient, parameters, url, existingParams);
                }

                if (follow) {
                    pollDeploymentPlanCloudFormation(cloudFormationClient, dataEmitter);
                } else {
                    dataEmitter.emitPrintItem(PrintItem.successMessage("The Attini stack has been deployed!"));
                }
            }
    }


    private void updateStack(SetupAttiniRequest request,
                             CloudFormationClient cloudFormationClient,
                             List<Parameter> parameters,
                             String url,
                             Map<String, Parameter> existingParams) {
        try {

            Set<String> paramsForNewStack = cloudFormationClient.getTemplateSummary(
                                                                        GetTemplateSummaryRequest.builder()
                                                                                                 .templateURL(url)
                                                                                                 .build())
                                                                .parameters()
                                                                .stream()
                                                                .map(ParameterDeclaration::parameterKey)
                                                                .collect(toSet());


            existingParams.entrySet().removeIf(entry -> !paramsForNewStack.contains(entry.getKey()));

            cloudFormationClient.updateStack(updateStackRequest(url, getFinalParams(
                                                                        existingParams,
                                                                        parameters),
                                                                request.getUseExistingVersion().orElse(FALSE)));
        } catch (CloudFormationException e) {
            if (e.awsErrorDetails().errorCode().equals("ValidationError")) {
                throw new IllegalArgumentException("Invalid attini version", e);
            }
        }
    }

    private void validateParameters(List<Parameter> parameters) {

        Map<String, Parameter> givenParameters = parameters.stream().collect(toParamMap());
        if (givenParameters.containsKey(INIT_DEPLOY_ARN) && givenParameters.containsKey(CREATE_INIT_DEPLOY_DEFAULT_ROLE) && givenParameters.get(
                CREATE_INIT_DEPLOY_DEFAULT_ROLE).parameterValue().equalsIgnoreCase("true")) {
            throw new IllegalStateException(
                    "Both --create-init-deploy-default-role and --init-deploy-role-arn was specified. Only one of these parameters can be set.");
        }

    }

    private void validateCreateParameters(List<Parameter> parameters) {

        Map<String, Parameter> givenParameters = parameters.stream().collect(toParamMap());
        if (!givenParameters.containsKey(INIT_DEPLOY_ARN) && !givenParameters.containsKey(
                CREATE_INIT_DEPLOY_DEFAULT_ROLE)) {
            throw new IllegalArgumentException(
                    "Either --create-init-deploy-default-role or --init-deploy-role-arn needs to be specified. Use the --guided option for a guided installation.");
        }

    }

    private boolean hasAlreadyAcceptedLicenceAgreement(Map<String, Parameter> existingParams) {
        return existingParams.containsKey(ACCEPT_LICENSE_AGREEMENT)
               && existingParams.get(ACCEPT_LICENSE_AGREEMENT)
                                .parameterValue()
                                .equalsIgnoreCase("true");

    }

    private Collection<Parameter> getFinalParams(Map<String, Parameter> existingParams,
                                                 List<Parameter> parameters) {

        HashMap<String, Parameter> finalParams = new HashMap<>();
        Map<String, Parameter> givenParameters = parameters.stream().collect(toParamMap());

        finalParams.putAll(existingParams);
        finalParams.putAll(givenParameters);

        if (givenParameters.containsKey(INIT_DEPLOY_ARN)) {
            finalParams.put(CREATE_INIT_DEPLOY_DEFAULT_ROLE, toParameter(CREATE_INIT_DEPLOY_DEFAULT_ROLE, false));
        }

        if (givenParameters.containsKey(CREATE_INIT_DEPLOY_DEFAULT_ROLE) && givenParameters.get(
                CREATE_INIT_DEPLOY_DEFAULT_ROLE).parameterValue().equalsIgnoreCase("true")) {
            finalParams.remove(INIT_DEPLOY_ARN);
        }

        return finalParams.values();
    }

    private static Collector<Parameter, ?, Map<String, Parameter>> toParamMap() {
        return Collectors.toMap(Parameter::parameterKey,
                                parameter -> parameter);
    }

    private Map<String, Parameter> getExistingParams(CloudFormationClient cloudFormationClient) {

        try {
            DescribeStacksResponse describeStacksResponse = cloudFormationClient
                    .describeStacks(DescribeStacksRequest.builder()
                                                         .stackName(STACK_NAME.getName())
                                                         .build());

            return describeStacksResponse.stacks()
                                         .get(0)
                                         .parameters()
                                         .stream()
                                         .collect(toParamMap());
        } catch (CloudFormationException e) {
            if (e.awsErrorDetails().errorCode().equals("ValidationError")) {
                return Collections.emptyMap();
            }
            if (e.awsErrorDetails().errorCode().equals("InvalidClientTokenId")) {
                throw new InvalidCredentialsException(e.awsErrorDetails().errorMessage(), e);
            }
            throw e;
        }

    }

    private static List<Parameter> allGivenParams(SetupAttiniRequest request) {
        Optional<Parameter> environmentParameter = request.getEnvironmentVariable()
                                                          .map(s -> toParameter(ENVIRONMENT_PARAMETER_NAME, s));

        Optional<Parameter> emailParameter = request.getEmail()
                                                    .map(email -> toParameter(EMAIL, email.getEmail()));

        Optional<Parameter> retainDistributionDays = request.getRetainDistributionDays()
                                                            .map(s -> toParameter(RETAIN_DISTRIBUTION_DAYS, s));
        Optional<Parameter> giveAdminAccess = request.getGiveAdminAccess()
                                                     .map(aBoolean -> toParameter(GIVE_ADMIN_ACCESS, aBoolean));

        Optional<Parameter> initDeployArn = request.getInitDeployArn()
                                                   .map(IamRoleArn::getValue)
                                                   .map(s -> toParameter(INIT_DEPLOY_ARN, s));
        Optional<Parameter> logLvl = request.getLogLevel().map(logLevel -> toParameter(LOG_LVL, logLevel.name()));
        Optional<Parameter> retainDistVersions = request.getRetainDistributionVersions()
                                                        .map(integer -> toParameter(RETAIN_DISTRIBUTION_VERSIONS,
                                                                                    integer));

        Optional<Parameter> subnetIds =
                request.getSubnetIds().isEmpty() ? Optional.empty() : Optional.of(toParameter(SUBNET_IDS,
                                                                                              request.getSubnetIds()));
        Optional<Parameter> deploymentPlanDefaultRole = request.getCreateDeploymentPlanDefaultRole()
                                                               .map(aBoolean -> toParameter(
                                                                       CREATE_DEPLOYMENT_PLAN_DEFAULT_ROLE,
                                                                       aBoolean));
        Optional<Parameter> vpcId = request.getVpcId().map(s -> toParameter(VPC_ID, s));

        Optional<Parameter> autoUpdate = request.getAutoUpdate().map(s -> toParameter(AUTO_UPDATE, s));

        Optional<Parameter> token = request.getToken().map(s -> toParameter(LICENCE_TOKEN, s));

        Optional<Parameter> initDeployDefaultRole = request.getCreateInitDeployDefaultRole()
                                                           .map(aBoolean -> toParameter(CREATE_INIT_DEPLOY_DEFAULT_ROLE,
                                                                                        aBoolean));

        Optional<Parameter> resourceAllocation = request.getResourceAllocation()
                                                        .map(s -> toParameter(RESOURCE_ALLOCATION, s));

        Optional<Parameter> leastPrivilegePolicy = request.isDisableLeastPrivilegeInitDeployPolicy()
                                               .map(aBoolean -> toParameter(ATTACH_LEAST_PRIVILEGE_INIT_DEPLOY_POLICY,
                                                                            aBoolean));


        return Stream.of(environmentParameter,
                         emailParameter,
                         retainDistributionDays,
                         giveAdminAccess,
                         initDeployArn,
                         logLvl,
                         retainDistVersions,
                         subnetIds,
                         deploymentPlanDefaultRole,
                         vpcId,
                         autoUpdate,
                         token,
                         initDeployDefaultRole,
                         resourceAllocation,
                         leastPrivilegePolicy)
                     .filter(Optional::isPresent)
                     .map(Optional::get)
                     .collect(Collectors.toList());

    }

    private UpdateStackRequest updateStackRequest(String url,
                                                  Collection<Parameter> parameters,
                                                  boolean keepVersion) {


        UpdateStackRequest.Builder builder = UpdateStackRequest.builder()
                                                               .stackName(STACK_NAME.getName())
                                                               .capabilities(Capability.CAPABILITY_IAM,
                                                                             Capability.CAPABILITY_NAMED_IAM,
                                                                             Capability.CAPABILITY_AUTO_EXPAND);

        if (keepVersion) {
            builder.usePreviousTemplate(true);
        } else {
            builder.templateURL(url);
        }

        if (!parameters.isEmpty()) {
            builder.parameters(parameters);
        }
        return builder.build();
    }

    private CreateStackRequest createStackRequest(String url,
                                                  List<Parameter> parameters) {

        validateCreateParameters(parameters);

        CreateStackRequest.Builder builder = CreateStackRequest.builder()
                                                               .templateURL(url)
                                                               .stackName(STACK_NAME.getName())
                                                               .capabilities(Capability.CAPABILITY_IAM,
                                                                             Capability.CAPABILITY_NAMED_IAM,
                                                                             Capability.CAPABILITY_AUTO_EXPAND);
        if (!parameters.isEmpty()) {
            builder.parameters(parameters);
        }
        return builder.build();
    }

    private void pollDeploymentPlanCloudFormation(CloudFormationClient cloudFormationClient,
                                                  DataEmitter emitter) {

        EnumSet<StackStatus> completedStatuses = EnumSet.of(StackStatus.ROLLBACK_COMPLETE,
                                                            StackStatus.CREATE_COMPLETE,
                                                            StackStatus.UPDATE_ROLLBACK_COMPLETE,
                                                            StackStatus.UPDATE_COMPLETE,
                                                            StackStatus.UPDATE_ROLLBACK_FAILED);

        waitFor(3000);
        StackStatus stackStatus = getStackStatus(cloudFormationClient);
        while (!completedStatuses.contains(stackStatus)) {
            waitFor(2000);
            stackStatus = getStackStatus(cloudFormationClient);
            emitter.emitKeyValueSameLine("StackStatus", stackStatus.name(), getColorForStatus(stackStatus));
        }
        emitter.emitNewLine();
        switch (stackStatus) {
            case CREATE_COMPLETE -> emitter.emitString("The Attini framework was successfully installed!");
            case UPDATE_COMPLETE -> emitter.emitString("The Attini framework was successfully updated!");
            default -> throw new IllegalStateException("Attini setup failed, please check cloudformation logs");
        }
    }

    private StackStatus getStackStatus(CloudFormationClient cloudFormationClient) {
        return cloudFormationClient
                .describeStacks(DescribeStacksRequest.builder()
                                                     .stackName(STACK_NAME.getName())
                                                     .build())
                .stacks()
                .get(0)
                .stackStatus();
    }

    private PrintUtil.Color getColorForStatus(StackStatus stackStatus) {
        return switch (stackStatus) {
            case CREATE_COMPLETE, UPDATE_COMPLETE -> GREEN;
            case ROLLBACK_FAILED, DELETE_FAILED, CREATE_FAILED, UPDATE_ROLLBACK_FAILED, UPDATE_FAILED, IMPORT_ROLLBACK_FAILED,
                    UPDATE_ROLLBACK_COMPLETE, UPDATE_ROLLBACK_IN_PROGRESS -> RED;
            default -> YELLOW;
        };
    }


    private static void waitFor(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String createUrl(Region givenRegion, String version) {
        return String.format(URL_TEMPLATE, givenRegion.getName(), givenRegion.getName(), version);
    }


}
