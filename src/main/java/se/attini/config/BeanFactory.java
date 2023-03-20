package se.attini.config;


import com.fasterxml.jackson.databind.ObjectMapper;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import se.attini.AwsAccountFacade;
import se.attini.CheckVersionService;
import se.attini.DistributionDataFacade;
import se.attini.EnvironmentVariables;
import se.attini.GetDistributionOutputService;
import se.attini.cli.ConsolePrinter;
import se.attini.cli.RemoveStackService;
import se.attini.cli.UserInputReader;
import se.attini.cli.UserInputReaderScanner;
import se.attini.cli.deployment.DataEmitter;
import se.attini.cli.global.GlobalConfig;
import se.attini.client.AwsClientFactory;
import se.attini.context.ContextService;
import se.attini.deployment.ConfirmDeploymentUserInput;
import se.attini.deployment.ContinueDeploymentService;
import se.attini.deployment.DeployDistributionService;
import se.attini.deployment.DeploymentOrigin;
import se.attini.deployment.DeploymentPlanStatusFacade;
import se.attini.deployment.DeploymentPlanStatusPrinter;
import se.attini.deployment.FollowDeploymentService;
import se.attini.deployment.RedeployDistributionService;
import se.attini.deployment.StepLoggerFactory;
import se.attini.deployment.file.config.AttiniConfigFiles;
import se.attini.deployment.file.ignore.AttiniIgnoreFiles;
import se.attini.deployment.history.DeploymentHistoryFacade;
import se.attini.distribution.DownloadDistributionService;
import se.attini.environment.EnvironmentService;
import se.attini.environment.EnvironmentUserInput;
import se.attini.logs.ExportLogsService;
import se.attini.pack.EditAttiniConfigService;
import se.attini.pack.EditConfigService;
import se.attini.pack.LifeCycleHooksService;
import se.attini.pack.PackageDistributionService;
import se.attini.pack.PrepareDistributionService;
import se.attini.pack.TransformSimpleSyntax;
import se.attini.profile.ProfileFacade;
import se.attini.setup.GuidedSetup;
import se.attini.setup.SetupAttiniService;
import se.attini.setup.SetupVersionsService;
import se.attini.stackstatus.FindUnmanagedStackService;
import se.attini.util.ObjectMapperFactory;

@Factory
public class BeanFactory {

    @Singleton
    public ProfileFacade profileFacade(EnvironmentVariables environmentVariables, GlobalConfig globalConfig) {
        return new ProfileFacade(environmentVariables, globalConfig);
    }

    @Singleton
    public GlobalConfig globalConfig(){
        return new GlobalConfig();
    }

    @Singleton
    AwsClientFactory awsClientFactory(GlobalConfig globalConfig) {
        return new AwsClientFactory(globalConfig);
    }

    @Singleton
    public AttiniConfigFiles attiniConfigFiles() {
        return new AttiniConfigFiles();
    }

    @Singleton
    public AttiniIgnoreFiles attiniIgnoreFiles() {
        return new AttiniIgnoreFiles();
    }

    @Singleton
    public EnvironmentService environmentService(AwsClientFactory awsClientFactory) {
        return new EnvironmentService(awsClientFactory);
    }

    @Singleton
    public DownloadDistributionService downloadDistributionService(AwsClientFactory awsClientFactory,
                                                                   ProfileFacade profileFacade,
                                                                   EnvironmentUserInput environmentUserInput) {
        return new DownloadDistributionService(awsClientFactory, profileFacade, environmentUserInput);
    }

    @Singleton
    public ObjectMapperFactory objectMapperFactory(GlobalConfig globalConfig) {
        return new ObjectMapperFactory(globalConfig);
    }

    @Singleton
    public DistributionDataFacade distributionDataFacade(AwsClientFactory awsClientFactory) {
        return new DistributionDataFacade(awsClientFactory);
    }

    @Singleton
    ConfirmDeploymentUserInput confirmDeploymentUserInput(UserInputReader userInputReader,
                                                          DistributionDataFacade distributionDataFacade) {
        return new ConfirmDeploymentUserInput(userInputReader, distributionDataFacade);
    }

    @Singleton
    GetDistributionOutputService getDistributionOutputService(AwsClientFactory awsClientFactory,
                                                              ProfileFacade profileFacade,
                                                              EnvironmentUserInput environmentUserInput) {
        return new GetDistributionOutputService(awsClientFactory, profileFacade, environmentUserInput);
    }

    @Singleton
    CheckVersionService checkVersionService(EnvironmentVariables environmentVariables) {
        return new CheckVersionService(environmentVariables);
    }

    @Singleton
    public DeployDistributionService deployDistributionService(AwsClientFactory awsClientFactory,
                                                               AttiniConfigFiles attiniConfigFiles,
                                                               EnvironmentUserInput environmentUserInput,
                                                               ConfirmDeploymentUserInput confirmDeploymentUserInput,
                                                               EnvironmentVariables environmentVariables,
                                                               DeploymentOrigin deploymentOrigin) {
        return new DeployDistributionService(awsClientFactory,
                                             attiniConfigFiles,
                                             environmentUserInput,
                                             confirmDeploymentUserInput,
                                             environmentVariables,
                                             deploymentOrigin);
    }

    @Singleton
    public RemoveStackService removeStackService(AwsClientFactory awsClientFactory,
                                                 ProfileFacade profileFacade,
                                                 ConsolePrinter consolePrinter,
                                                 AwsAccountFacade awsAccountFacade) {
        return new RemoveStackService(awsClientFactory, profileFacade, consolePrinter, awsAccountFacade);

    }

    @Singleton
    public FindUnmanagedStackService findUnmanagedStackService(EnvironmentUserInput environmentUserInput,
                                                               AwsClientFactory awsClientFactory,
                                                               GlobalConfig globalConfig) {
        return new FindUnmanagedStackService(environmentUserInput, awsClientFactory, globalConfig);
    }

    @Singleton
    public ContextService contextService(AwsClientFactory awsClientFactory,
                                         ProfileFacade profileFacade,
                                         EnvironmentService environmentService) {
        return new ContextService(awsClientFactory, profileFacade, environmentService);
    }

    @Singleton
    public UserInputReader userInputReader() {
        return new UserInputReaderScanner();
    }

    @Singleton
    public DataEmitter dataEmitter(GlobalConfig globalConfig, ObjectMapper objectMapper, ConsolePrinter consolePrinter){
        return new DataEmitter(globalConfig, objectMapper, consolePrinter);
    }

    @Singleton
    public SetupAttiniService setupAttiniService(AwsClientFactory awsClientFactory,
                                                 ProfileFacade profileFacade,
                                                 UserInputReader userInputReader,
                                                 DataEmitter dataEmitter,
                                                 ConsolePrinter consolePrinter) {
        return new SetupAttiniService(awsClientFactory,
                                      profileFacade,
                                      userInputReader,
                                      new GuidedSetup(userInputReader),
                                      dataEmitter,
                                      consolePrinter);
    }

    @Singleton
    public SetupVersionsService setupVersionService(AwsClientFactory awsClientFactory, ProfileFacade profileFacade) {
        return new SetupVersionsService(awsClientFactory, profileFacade);
    }

    @Singleton
    public EnvironmentUserInput environmentUserInput(EnvironmentService environmentService,
                                                     UserInputReader userInputReader, GlobalConfig globalConfig) {
        return new EnvironmentUserInput(environmentService, userInputReader, globalConfig);
    }


    @Singleton
    public DeploymentHistoryFacade deploymentHistoryFacade(AwsClientFactory awsClientFactory,
                                                           EnvironmentUserInput environmentUserInput) {
        return new DeploymentHistoryFacade(awsClientFactory, environmentUserInput);
    }

    @Singleton
    public DeploymentOrigin deploymentOrigin(ProfileFacade profileFacade, AwsAccountFacade awsAccountFacade){
        return new DeploymentOrigin(profileFacade, awsAccountFacade);
    }

    @Singleton
    public RedeployDistributionService redeployDistributionService(AwsClientFactory awsClientFactory,
                                                                   EnvironmentUserInput environmentUserInput,
                                                                   ConfirmDeploymentUserInput confirmDeploymentUserInput,
                                                                   AttiniConfigFiles attiniConfigFiles,
                                                                   DeploymentOrigin deploymentOrigin) {
        return new RedeployDistributionService(awsClientFactory,
                                               deploymentOrigin,
                                               environmentUserInput,
                                               confirmDeploymentUserInput,
                                               attiniConfigFiles);
    }

    @Singleton
    public AwsAccountFacade awsAccountFacade(AwsClientFactory awsClientFactory, GlobalConfig globalConfig) {
        return new AwsAccountFacade(awsClientFactory, globalConfig);
    }

    @Singleton
    public ConsolePrinter consolePrinter(ObjectMapperFactory objectMapperFactory, GlobalConfig globalConfig){
        return new ConsolePrinter(objectMapperFactory, globalConfig);
    }

    @Singleton
    public DeploymentPlanStatusPrinter deploymentPlanStatusPrinter(AwsClientFactory awsClientFactory,
                                                                   ProfileFacade profileFacade,
                                                                   AwsAccountFacade awsAccountFacade,
                                                                   GlobalConfig globalConfig,
                                                                   ConsolePrinter consolePrinter) {
        return new DeploymentPlanStatusPrinter(new StepLoggerFactory(awsClientFactory,
                                                                     profileFacade,
                                                                     awsAccountFacade),
                                               globalConfig,
                                               consolePrinter);
    }

    @Singleton
    public FollowDeploymentService followDeploymentService(AwsClientFactory awsClientFactory,
                                                           DeploymentHistoryFacade deploymentHistoryFacade,
                                                           DeploymentPlanStatusPrinter deploymentPlanStatusPrinter,
                                                           ProfileFacade profileFacade,
                                                           DataEmitter dataEmitter,
                                                           GlobalConfig globalConfig,
                                                           ObjectMapper objectMapper,
                                                           ConsolePrinter consolePrinter) {
        return new FollowDeploymentService(awsClientFactory,
                                           new DeploymentPlanStatusFacade(awsClientFactory),
                                           deploymentHistoryFacade,
                                           deploymentPlanStatusPrinter,
                                           profileFacade,
                                           dataEmitter,
                                           globalConfig,
                                           objectMapper,
                                           consolePrinter);
    }

    @Singleton
    public ExportLogsService exportLogsService(AwsClientFactory awsClientFactory,
                                               ProfileFacade profileFacade,
                                               ConsolePrinter consolePrinter) {
        return new ExportLogsService(awsClientFactory, profileFacade, consolePrinter);
    }

    @Singleton
    public EditConfigService editConfigService() {
        return new EditConfigService();
    }

    @Singleton
    public EditAttiniConfigService editAttiniConfigService(AttiniConfigFiles attiniConfigFiles,
                                                           EditConfigService editConfigService) {
        return new EditAttiniConfigService(attiniConfigFiles, editConfigService);
    }

    @Singleton
    public LifeCycleHooksService lifeCycleHooksService(AttiniConfigFiles attiniConfigFiles,
                                                       EnvironmentVariables environmentVariables,
                                                       DataEmitter dataEmitter,
                                                       GlobalConfig globalConfig) {
        return new LifeCycleHooksService(attiniConfigFiles, environmentVariables, dataEmitter, globalConfig);
    }

    @Singleton
    public ContinueDeploymentService continueDeploymentService(AwsClientFactory awsClientFactory,
                                                               AwsAccountFacade awsAccountFacade,
                                                               ProfileFacade profileFacade,
                                                               EnvironmentUserInput environmentUserInput) {
        return new ContinueDeploymentService(awsClientFactory, awsAccountFacade, profileFacade, environmentUserInput);
    }

    @Singleton
    public EnvironmentVariables environmentVariables() {
        return new EnvironmentVariables();
    }

    @Singleton
    public PackageDistributionService packageDistributionService(LifeCycleHooksService lifeCycleHooksService,
                                                                 AttiniIgnoreFiles attiniIgnoreFiles,
                                                                 AttiniConfigFiles attiniConfigFiles,
                                                                 EditAttiniConfigService editAttiniConfigService,
                                                                 ObjectMapperFactory objectMapperFactory,
                                                                 EnvironmentVariables environmentVariables,
                                                                 DataEmitter dataEmitter,
                                                                 GlobalConfig globalConfig) {


        return new PackageDistributionService(lifeCycleHooksService,
                                              attiniIgnoreFiles,
                                              attiniConfigFiles,
                                              editAttiniConfigService,
                                              new PrepareDistributionService(objectMapperFactory,
                                                                             environmentVariables,
                                                                             new TransformSimpleSyntax(
                                                                                     objectMapperFactory)),
                                              environmentVariables,
                                              dataEmitter,
                                              globalConfig);

    }
}
