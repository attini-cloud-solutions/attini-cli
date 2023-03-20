package se.attini.setup;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.attini.cli.ConsolePrinter;
import se.attini.cli.UserInputReader;
import se.attini.cli.deployment.DataEmitter;
import se.attini.client.AwsClientFactory;
import se.attini.domain.Email;
import se.attini.domain.Region;
import se.attini.profile.ProfileFacade;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryResponse;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.ParameterDeclaration;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;

@ExtendWith(MockitoExtension.class)
class SetupAttiniServiceTest {

    @Mock
    ProfileFacade profileFacade;

    @Mock
    AwsClientFactory awsClientFactory;

    @Mock
    CloudFormationClient cloudFormationClient;

    @Mock
    UserInputReader userInputReader;

    @Mock
    DataEmitter dataEmitter;

    @Mock
    ConsolePrinter consolePrinter;

    SetupAttiniService setupAttiniService;

    @BeforeEach
    void setUp() {
        setupAttiniService = new SetupAttiniService(awsClientFactory, profileFacade, userInputReader, new GuidedSetup(userInputReader), dataEmitter, consolePrinter);
        when(awsClientFactory.cfnClient()).thenReturn(cloudFormationClient);
    }

    @Test
    void create() {

        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        SetupAttiniRequest request = SetupAttiniRequest.builder()
                                                       .setAcceptLicenseAgreement(true)
                                                       .setCreateInitDeployDefaultRole(true)
                                                       .build();
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenThrow(CloudFormationException.builder()
                                                  .awsErrorDetails(
                                                          AwsErrorDetails.builder()
                                                                         .errorCode("ValidationError")
                                                                         .build())
                                                  .build());
        setupAttiniService.setup(request, false);


        ArgumentCaptor<CreateStackRequest> createStackRequest = ArgumentCaptor.forClass(CreateStackRequest.class);
        verify(cloudFormationClient).createStack(createStackRequest.capture());

        CreateStackRequest value = createStackRequest.getValue();
        assertEquals(value.parameters().size(), 2);
        assertAcceptedLicenceAgreement(value);
    }

    @Test
    void invalidRequestDoesNothing_bothVersionAndKeepVersionSpecified() {

        SetupAttiniRequest request = SetupAttiniRequest.builder()
                                                       .setVersion("test")
                                                       .setUseExistingVersion(true)
                                                       .build();
        setupAttiniService.setup(request, false);


        verify(cloudFormationClient, never()).createStack(any(CreateStackRequest.class));
        verify(cloudFormationClient, never()).updateStack(any(UpdateStackRequest.class));

    }

    @Test
    void create_promptForAcceptUserAgreement() {

        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        when(userInputReader.getUserInput()).thenReturn("y");
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenThrow(CloudFormationException.builder()
                                                  .awsErrorDetails(
                                                          AwsErrorDetails.builder()
                                                                         .errorCode(
                                                                                 "ValidationError")
                                                                         .build())
                                                  .build());
        SetupAttiniRequest request = SetupAttiniRequest.builder().setCreateInitDeployDefaultRole(true).build();
        setupAttiniService.setup(request, false);

        ArgumentCaptor<CreateStackRequest> createStackRequest = ArgumentCaptor.forClass(CreateStackRequest.class);
        verify(cloudFormationClient).createStack(createStackRequest.capture());

        CreateStackRequest value = createStackRequest.getValue();
        assertEquals(2, value.parameters().size());
        assertAcceptedLicenceAgreement(value);
    }

    @Test
    void create_dontPromptForAcceptUserAgreementIfAcceptedViaCommand() {

        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));

        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenThrow(CloudFormationException.builder()
                                                  .awsErrorDetails(
                                                          AwsErrorDetails.builder()
                                                                         .errorCode("ValidationError")
                                                                         .build())
                                                  .build());

        SetupAttiniRequest request = SetupAttiniRequest.builder().setAcceptLicenseAgreement(true).setCreateInitDeployDefaultRole(true).build();
        setupAttiniService.setup(request, false);

        ArgumentCaptor<CreateStackRequest> createStackRequest = ArgumentCaptor.forClass(CreateStackRequest.class);
        verify(cloudFormationClient).createStack(createStackRequest.capture());

        CreateStackRequest value = createStackRequest.getValue();
        verify(userInputReader, never()).getUserInput();
        assertEquals(2, value.parameters().size());
        assertAcceptedLicenceAgreement(value);
    }

    @Test
    void create_withEnvironmentParam() {
        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        SetupAttiniRequest request = SetupAttiniRequest.builder()
                                                       .setEnvironmentVariable("test")
                                                       .setAcceptLicenseAgreement(true)
                                                       .setCreateInitDeployDefaultRole(true)
                                                       .build();
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenThrow(CloudFormationException.builder()
                                                  .awsErrorDetails(
                                                          AwsErrorDetails.builder()
                                                                         .errorCode("ValidationError")
                                                                         .build())
                                                  .build());
        setupAttiniService.setup(request, false);

        ArgumentCaptor<CreateStackRequest> createStackRequest = ArgumentCaptor.forClass(CreateStackRequest.class);
        verify(cloudFormationClient).createStack(createStackRequest.capture());

        CreateStackRequest value = createStackRequest.getValue();
        assertEquals(value.parameters().size(), 3);
        assertAcceptedLicenceAgreement(value);

    }

    @Test
    void create_withBooleanParameter_true() {
        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        SetupAttiniRequest request = SetupAttiniRequest.builder()
                                                       .setGiveAdminAccess(true)
                                                       .setAcceptLicenseAgreement(true)
                                                       .setCreateInitDeployDefaultRole(true)
                                                       .build();

        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenThrow(CloudFormationException.builder()
                                                  .awsErrorDetails(
                                                          AwsErrorDetails.builder()
                                                                         .errorCode("ValidationError")
                                                                         .build())
                                                  .build());
        setupAttiniService.setup(request, false);

        ArgumentCaptor<CreateStackRequest> createStackRequest = ArgumentCaptor.forClass(CreateStackRequest.class);
        verify(cloudFormationClient).createStack(createStackRequest.capture());

        CreateStackRequest value = createStackRequest.getValue();
        assertEquals(value.parameters().size(), 3);
        assertEquals("true", value.parameters().get(0).parameterValue());
    }

    @Test
    void create_withListParam() {
        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        SetupAttiniRequest request = SetupAttiniRequest.builder()
                                                       .setSubnetIds(List.of("test1", "test2"))
                                                       .setAcceptLicenseAgreement(true)
                                                       .setCreateInitDeployDefaultRole(true)
                                                       .build();

        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenThrow(CloudFormationException.builder()
                                                  .awsErrorDetails(
                                                          AwsErrorDetails.builder()
                                                                         .errorCode(
                                                                                 "ValidationError")
                                                                         .build())
                                                  .build());

        setupAttiniService.setup(request, false);

        ArgumentCaptor<CreateStackRequest> createStackRequest = ArgumentCaptor.forClass(CreateStackRequest.class);
        verify(cloudFormationClient).createStack(createStackRequest.capture());

        CreateStackRequest value = createStackRequest.getValue();
        assertEquals(value.parameters().size(), 3);
        assertEquals("test1,test2", value.parameters().get(0).parameterValue());
        assertAcceptedLicenceAgreement(value);

    }

    @Test
    void create_withListParamOneValue() {
        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        SetupAttiniRequest request = SetupAttiniRequest.builder()
                                                       .setSubnetIds(List.of("test1"))
                                                       .setAcceptLicenseAgreement(true)
                                                       .setCreateInitDeployDefaultRole(true)
                                                       .build();

        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenThrow(CloudFormationException.builder()
                                                  .awsErrorDetails(
                                                          AwsErrorDetails.builder()
                                                                         .errorCode("ValidationError")
                                                                         .build())
                                                  .build());
        setupAttiniService.setup(request, false);

        ArgumentCaptor<CreateStackRequest> createStackRequest = ArgumentCaptor.forClass(CreateStackRequest.class);
        verify(cloudFormationClient).createStack(createStackRequest.capture());

        CreateStackRequest value = createStackRequest.getValue();
        assertEquals(value.parameters().size(), 3);
        assertEquals("test1", value.parameters().get(0).parameterValue());
        assertAcceptedLicenceAgreement(value);
    }

    @Test
    void create_withIntegerParameter() {
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenThrow(CloudFormationException.builder()
                                                  .awsErrorDetails(
                                                          AwsErrorDetails.builder()
                                                                         .errorCode("ValidationError")
                                                                         .build())
                                                  .build());
        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        SetupAttiniRequest request = SetupAttiniRequest.builder()
                                                       .setRetainDistributionDays(10)
                                                       .setCreateInitDeployDefaultRole(true)
                                                       .setAcceptLicenseAgreement(true)
                                                       .build();


        setupAttiniService.setup(request, false);

        ArgumentCaptor<CreateStackRequest> createStackRequest = ArgumentCaptor.forClass(CreateStackRequest.class);
        verify(cloudFormationClient).createStack(createStackRequest.capture());

        CreateStackRequest value = createStackRequest.getValue();
        assertEquals(value.parameters().size(), 3);
        assertEquals("10", value.parameters().get(0).parameterValue());
        assertAcceptedLicenceAgreement(value);

    }

    @Test
    void create_withBooleanParameter_false() {
        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        SetupAttiniRequest request = SetupAttiniRequest.builder().setGiveAdminAccess(false)
                                                       .setAcceptLicenseAgreement(true)
                                                       .setCreateInitDeployDefaultRole(true)
                                                       .build();

        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenThrow(CloudFormationException.builder()
                                                  .awsErrorDetails(
                                                          AwsErrorDetails.builder()
                                                                         .errorCode(
                                                                                 "ValidationError")
                                                                         .build())
                                                  .build());
        setupAttiniService.setup(request, false);

        ArgumentCaptor<CreateStackRequest> createStackRequest = ArgumentCaptor.forClass(CreateStackRequest.class);
        verify(cloudFormationClient).createStack(createStackRequest.capture());

        CreateStackRequest value = createStackRequest.getValue();
        assertEquals(value.parameters().size(), 3);
        assertEquals("false", value.parameters().get(0).parameterValue());
        assertAcceptedLicenceAgreement(value);
    }

    @Test
    void create_dontSetDefaultsForPrimitives() {
        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenThrow(CloudFormationException.builder()
                                                  .awsErrorDetails(
                                                          AwsErrorDetails.builder()
                                                                         .errorCode("ValidationError")
                                                                         .build())
                                                  .build());
        SetupAttiniRequest request = SetupAttiniRequest.builder().setAcceptLicenseAgreement(true).setCreateInitDeployDefaultRole(true).build();
        setupAttiniService.setup(request, false);

        ArgumentCaptor<CreateStackRequest> createStackRequest = ArgumentCaptor.forClass(CreateStackRequest.class);
        verify(cloudFormationClient).createStack(createStackRequest.capture());

        CreateStackRequest value = createStackRequest.getValue();
        assertEquals(value.parameters().size(), 2);
        assertAcceptedLicenceAgreement(value);

    }


    @Test
    void create_withEnvironmentParamAndEmail() {
        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        SetupAttiniRequest request = SetupAttiniRequest.builder()
                                                       .setAcceptLicenseAgreement(true)
                                                       .setEnvironmentVariable("test")
                                                       .setEmail(Email.create("test@mail.com"))
                                                       .setCreateInitDeployDefaultRole(true)
                                                       .build();

        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(CloudFormationException.builder()
                                                                                                                     .awsErrorDetails(
                                                                                                                             AwsErrorDetails.builder()
                                                                                                                                            .errorCode(
                                                                                                                                                    "ValidationError")
                                                                                                                                            .build())
                                                                                                                     .build());

        setupAttiniService.setup(request, false);

        ArgumentCaptor<CreateStackRequest> createStackRequest = ArgumentCaptor.forClass(CreateStackRequest.class);
        verify(cloudFormationClient).createStack(createStackRequest.capture());

        CreateStackRequest value = createStackRequest.getValue();
        assertEquals(value.parameters().size(), 4);
        assertAcceptedLicenceAgreement(value);

    }

    @Test
    void update() {
        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenReturn(DescribeStacksResponse.builder()
                                                  .stacks(Stack.builder()
                                                               .parameters(AcceptLicenseAgreementParam())
                                                               .build())
                                                  .build());

        when(cloudFormationClient.getTemplateSummary(any(GetTemplateSummaryRequest.class))).thenReturn(
                GetTemplateSummaryResponse.builder()
                                          .parameters(List.of(ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "EnvironmentParameterName")
                                                                                  .parameterType("String")
                                                                                  .build(),
                                                              ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "AcceptLicenseAgreement")
                                                                                  .parameterType("String")
                                                                                  .build()))
                                          .build());
        SetupAttiniRequest request = SetupAttiniRequest.builder().build();
        setupAttiniService.setup(request, false);

        ArgumentCaptor<UpdateStackRequest> updateStackRequest = ArgumentCaptor.forClass(UpdateStackRequest.class);
        verify(cloudFormationClient).updateStack(updateStackRequest.capture());

        UpdateStackRequest value = updateStackRequest.getValue();
        verify(userInputReader, never()).getUserInput();
        assertEquals(value.parameters().size(), 1);
        assertNull(value.usePreviousTemplate());
        assertAcceptedLicenceAgreement(value);

    }

    @Test
    void update_shouldUseExistingTemplate() {
        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenReturn(DescribeStacksResponse.builder()
                                                  .stacks(Stack.builder()
                                                               .parameters(AcceptLicenseAgreementParam())
                                                               .build())
                                                  .build());

        when(cloudFormationClient.getTemplateSummary(any(GetTemplateSummaryRequest.class))).thenReturn(
                GetTemplateSummaryResponse.builder()
                                          .parameters(List.of(ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "EnvironmentParameterName")
                                                                                  .parameterType("String")
                                                                                  .build(),
                                                              ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "AcceptLicenseAgreement")
                                                                                  .parameterType("String")
                                                                                  .build()))
                                          .build());

        SetupAttiniRequest request = SetupAttiniRequest.builder().setUseExistingVersion(true).build();
        setupAttiniService.setup(request, false);

        ArgumentCaptor<UpdateStackRequest> updateStackRequest = ArgumentCaptor.forClass(UpdateStackRequest.class);
        verify(cloudFormationClient).updateStack(updateStackRequest.capture());

        UpdateStackRequest value = updateStackRequest.getValue();
        verify(userInputReader, never()).getUserInput();
        assertEquals(value.parameters().size(), 1);
        assertTrue(value.usePreviousTemplate());
        assertAcceptedLicenceAgreement(value);

    }


    @Test
    void update_promptForLicenceAgreementIfNotAccepted() {
        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        when(userInputReader.getUserInput()).thenReturn("Y");
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenReturn(DescribeStacksResponse.builder()
                                                  .stacks(Stack.builder()
                                                               .parameters(Parameter.builder()
                                                                                    .parameterKey("email")
                                                                                    .parameterValue("test@test.se")
                                                                                    .build())
                                                               .build())
                                                  .build());
        SetupAttiniRequest request = SetupAttiniRequest.builder().setCreateInitDeployDefaultRole(true).build();

        when(cloudFormationClient.getTemplateSummary(any(GetTemplateSummaryRequest.class))).thenReturn(
                GetTemplateSummaryResponse.builder()
                                          .parameters(List.of(ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "EnvironmentParameterName")
                                                                                  .parameterType("String")
                                                                                  .build(),
                                                              ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "AcceptLicenseAgreement")
                                                                                  .parameterType("String")
                                                                                  .build()))
                                          .build());
        setupAttiniService.setup(request, false);

        ArgumentCaptor<UpdateStackRequest> updateStackRequest = ArgumentCaptor.forClass(UpdateStackRequest.class);
        verify(cloudFormationClient).updateStack(updateStackRequest.capture());
        verify(userInputReader).getUserInput();
        UpdateStackRequest value = updateStackRequest.getValue();
        assertEquals(value.parameters().size(), 2);
        assertAcceptedLicenceAgreement(value);

    }

    @Test
    void update_promptForLicenceAgreementIfProvidedFalseByCommand() {
        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        when(userInputReader.getUserInput()).thenReturn("Y");
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenReturn(DescribeStacksResponse.builder()
                                                  .stacks(Stack.builder()
                                                               .parameters(Parameter.builder()
                                                                                    .parameterKey("email")
                                                                                    .parameterValue("test@test.se")
                                                                                    .build())
                                                               .build())
                                                  .build());
        when(cloudFormationClient.getTemplateSummary(any(GetTemplateSummaryRequest.class))).thenReturn(
                GetTemplateSummaryResponse.builder()
                                          .parameters(List.of(ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "EnvironmentParameterName")
                                                                                  .parameterType("String")
                                                                                  .build(),
                                                              ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "AcceptLicenseAgreement")
                                                                                  .parameterType("String")
                                                                                  .build()))
                                          .build());

        SetupAttiniRequest request = SetupAttiniRequest.builder()
                                                       .setCreateInitDeployDefaultRole(true)
                                                       .setAcceptLicenseAgreement(false)
                                                       .build();
        setupAttiniService.setup(request, false);

        ArgumentCaptor<UpdateStackRequest> updateStackRequest = ArgumentCaptor.forClass(UpdateStackRequest.class);
        verify(cloudFormationClient).updateStack(updateStackRequest.capture());
        verify(userInputReader).getUserInput();
        UpdateStackRequest value = updateStackRequest.getValue();
        assertEquals(value.parameters().size(), 2);
        assertAcceptedLicenceAgreement(value);

    }

    @Test
    void update_dont_promptForLicenceAgreementIfProvidedByCommand() {
        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenReturn(DescribeStacksResponse.builder()
                                                  .stacks(Stack.builder()
                                                               .parameters(Parameter.builder()
                                                                                    .parameterKey(
                                                                                            "CreateInitDeployDefaultRole")
                                                                                    .parameterValue("true")
                                                                                    .build())
                                                               .build())
                                                  .build());
        SetupAttiniRequest request = SetupAttiniRequest.builder().setAcceptLicenseAgreement(true).build();

        when(cloudFormationClient.getTemplateSummary(any(GetTemplateSummaryRequest.class))).thenReturn(
                GetTemplateSummaryResponse.builder()
                                          .parameters(List.of(ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "EnvironmentParameterName")
                                                                                  .parameterType("String")
                                                                                  .build(),
                                                              ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "AcceptLicenseAgreement")
                                                                                  .parameterType("String")
                                                                                  .build()))
                                          .build());
        setupAttiniService.setup(request, false);
        ArgumentCaptor<UpdateStackRequest> updateStackRequest = ArgumentCaptor.forClass(UpdateStackRequest.class);
        verify(cloudFormationClient).updateStack(updateStackRequest.capture());
        verify(userInputReader, never()).getUserInput();
        UpdateStackRequest value = updateStackRequest.getValue();
        assertEquals(value.parameters().size(), 1);
        assertAcceptedLicenceAgreement(value);

    }

    @Test
    void update_dont_promptForLicenceAgreementIfProvidedFalseByCommandButIsAccepted() {
        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenReturn(DescribeStacksResponse.builder()
                                                  .stacks(Stack.builder()
                                                               .parameters(AcceptLicenseAgreementParam())
                                                               .build())
                                                  .build());
        when(cloudFormationClient.getTemplateSummary(any(GetTemplateSummaryRequest.class))).thenReturn(
                GetTemplateSummaryResponse.builder()
                                          .parameters(List.of(ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "EnvironmentParameterName")
                                                                                  .parameterType("String")
                                                                                  .build(),
                                                              ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "AcceptLicenseAgreement")
                                                                                  .parameterType("String")
                                                                                  .build()))
                                          .build());

        SetupAttiniRequest request = SetupAttiniRequest.builder().setAcceptLicenseAgreement(false).build();
        setupAttiniService.setup(request, false);

        ArgumentCaptor<UpdateStackRequest> updateStackRequest = ArgumentCaptor.forClass(UpdateStackRequest.class);
        verify(cloudFormationClient).updateStack(updateStackRequest.capture());
        verify(userInputReader, never()).getUserInput();
        UpdateStackRequest value = updateStackRequest.getValue();
        assertEquals(value.parameters().size(), 1);
        assertAcceptedLicenceAgreement(value);

    }

    @Test
    void update_withEnvironmentParam() {

        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenReturn(DescribeStacksResponse.builder()
                                                  .stacks(Stack.builder()
                                                               .parameters(AcceptLicenseAgreementParam())
                                                               .build())
                                                  .build());

        when(cloudFormationClient.getTemplateSummary(any(GetTemplateSummaryRequest.class))).thenReturn(
                GetTemplateSummaryResponse.builder()
                                          .parameters(List.of(ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "EnvironmentParameterName")
                                                                                  .parameterType("String")
                                                                                  .build(),
                                                              ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "AcceptLicenseAgreement")
                                                                                  .parameterType("String")
                                                                                  .build()))
                                          .build());

        SetupAttiniRequest request = SetupAttiniRequest.builder().setEnvironmentVariable("test").build();
        setupAttiniService.setup(request, false);

        ArgumentCaptor<UpdateStackRequest> updateStackRequest = ArgumentCaptor.forClass(UpdateStackRequest.class);
        verify(cloudFormationClient).updateStack(updateStackRequest.capture());

        UpdateStackRequest value = updateStackRequest.getValue();
        verify(userInputReader, never()).getUserInput();
        assertEquals(value.parameters().size(), 2);
        assertAcceptedLicenceAgreement(value);

    }

    @Test
    void update_withEnvironmentParamFromExistingStack() {

        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        DescribeStacksResponse stacksResponse = DescribeStacksResponse.builder().stacks(
                Stack.builder()
                     .parameters(Parameter.builder()
                                          .parameterKey("EnvironmentParameterName")
                                          .parameterValue("fromExistingStack")
                                          .build(), AcceptLicenseAgreementParam())
                     .build()).build();
        when(cloudFormationClient.getTemplateSummary(any(GetTemplateSummaryRequest.class))).thenReturn(
                GetTemplateSummaryResponse.builder()
                                          .parameters(List.of(ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "EnvironmentParameterName")
                                                                                  .parameterType("String")
                                                                                  .build(),
                                                              ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "AcceptLicenseAgreement")
                                                                                  .parameterType("String")
                                                                                  .build()))
                                          .build());
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(stacksResponse);
        SetupAttiniRequest request = SetupAttiniRequest.builder().build();

        setupAttiniService.setup(request, false);

        ArgumentCaptor<UpdateStackRequest> updateStackRequest = ArgumentCaptor.forClass(UpdateStackRequest.class);
        verify(cloudFormationClient).updateStack(updateStackRequest.capture());

        UpdateStackRequest value = updateStackRequest.getValue();
        Optional<Parameter> environmentParameter = value.parameters()
                                                        .stream()
                                                        .filter(parameter -> parameter.parameterKey()
                                                                                      .equals("EnvironmentParameterName"))
                                                        .findAny();
        verify(userInputReader, never()).getUserInput();

        assertEquals(value.parameters().size(), 2);
        assertTrue(environmentParameter.isPresent());
        assertEquals("fromExistingStack", environmentParameter.get().parameterValue());
        assertAcceptedLicenceAgreement(value);

    }

    @Test
    void update_withEnvironmentParamFromRequestThatTakesPriority() {

        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        DescribeStacksResponse stacksResponse = DescribeStacksResponse.builder().stacks(
                Stack.builder()
                     .parameters(Parameter.builder()
                                          .parameterKey("EnvironmentParameterName")
                                          .parameterValue("fromExistingStack")
                                          .build(), AcceptLicenseAgreementParam())
                     .build()).build();

        when(cloudFormationClient.getTemplateSummary(any(GetTemplateSummaryRequest.class))).thenReturn(
                GetTemplateSummaryResponse.builder()
                                          .parameters(List.of(ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "EnvironmentParameterName")
                                                                                  .parameterType("String")
                                                                                  .build(),
                                                              ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "AcceptLicenseAgreement")
                                                                                  .parameterType("String")
                                                                                  .build()))
                                          .build());
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(stacksResponse);
        SetupAttiniRequest request = SetupAttiniRequest.builder().setEnvironmentVariable("fromRequest").build();

        setupAttiniService.setup(request, false);

        ArgumentCaptor<UpdateStackRequest> updateStackRequest = ArgumentCaptor.forClass(UpdateStackRequest.class);
        verify(cloudFormationClient).updateStack(updateStackRequest.capture());

        UpdateStackRequest value = updateStackRequest.getValue();
        Optional<Parameter> environmentParameter = value.parameters()
                                                        .stream()
                                                        .filter(parameter -> parameter.parameterKey()
                                                                                      .equals("EnvironmentParameterName"))
                                                        .findAny();

        assertEquals(value.parameters().size(), 2);
        assertTrue(environmentParameter.isPresent());
        assertEquals("fromRequest", environmentParameter.get().parameterValue());
        assertAcceptedLicenceAgreement(value);

    }


    @Test
    void update_withEnvironmentParamAndEmail() {
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(DescribeStacksResponse.builder()
                                                                                                                     .stacks(
                                                                                                                             Stack.builder()
                                                                                                                                  .parameters(
                                                                                                                                          AcceptLicenseAgreementParam())
                                                                                                                                  .build())
                                                                                                                     .build());

        when(profileFacade.getRegion()).thenReturn(Region.create("eu-west-1"));
        when(cloudFormationClient.getTemplateSummary(any(GetTemplateSummaryRequest.class))).thenReturn(
                GetTemplateSummaryResponse.builder()
                                          .parameters(List.of(ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "EnvironmentParameterName")
                                                                                  .parameterType("String")
                                                                                  .build(),
                                                              ParameterDeclaration.builder()
                                                                                  .parameterKey(
                                                                                          "AcceptLicenseAgreement")
                                                                                  .parameterType("String")
                                                                                  .build()))
                                          .build());
        SetupAttiniRequest request = SetupAttiniRequest.builder()
                                                       .setEnvironmentVariable("test")
                                                       .setEmail(Email.create("test@mail.com"))
                                                       .build();

        setupAttiniService.setup(request, false);

        ArgumentCaptor<UpdateStackRequest> updateStackRequest = ArgumentCaptor.forClass(UpdateStackRequest.class);
        verify(cloudFormationClient).updateStack(updateStackRequest.capture());

        UpdateStackRequest value = updateStackRequest.getValue();
        assertEquals(value.parameters().size(), 3);
        assertAcceptedLicenceAgreement(value);

    }

    private static Parameter AcceptLicenseAgreementParam() {
        return Parameter.builder()
                        .parameterKey(
                                "AcceptLicenseAgreement")
                        .parameterValue("true")
                        .build();
    }

    private void assertAcceptedLicenceAgreement(CreateStackRequest request) {
        Optional<Parameter> AcceptLicenseAgreement = request.parameters()
                                                            .stream()
                                                            .filter(parameter -> parameter.parameterKey()
                                                                                          .equals("AcceptLicenseAgreement"))
                                                            .findAny();

        assertTrue(AcceptLicenseAgreement.isPresent());
        assertEquals("true", AcceptLicenseAgreement.get().parameterValue());
    }

    private void assertAcceptedLicenceAgreement(UpdateStackRequest request) {
        Optional<Parameter> AcceptLicenseAgreement = request.parameters()
                                                            .stream()
                                                            .filter(parameter -> parameter.parameterKey()
                                                                                          .equals("AcceptLicenseAgreement"))
                                                            .findAny();

        assertTrue(AcceptLicenseAgreement.isPresent());
        assertEquals("true", AcceptLicenseAgreement.get().parameterValue());
    }
}
