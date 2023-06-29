package se.attini.cli;

import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import se.attini.cli.configure.ConfigCommand;
import se.attini.cli.deployment.DeploymentCommand;
import se.attini.cli.distribution.DistributionCommand;
import se.attini.cli.environment.EnvironmentCommand;
import se.attini.cli.init.InitProjectCommand;
import se.attini.cli.ops.OpsCommand;
import se.attini.cli.profile.ListProfileCommand;
import se.attini.cli.update.UpdateCommand;
import se.attini.domain.DistributionId;
import se.attini.domain.DistributionName;
import se.attini.domain.Email;
import se.attini.domain.EnvironmentName;
import se.attini.domain.FilePath;
import se.attini.domain.IamRoleArn;
import se.attini.domain.Profile;
import se.attini.domain.Region;
import se.attini.domain.StackName;

@Command(name = "attini", description = "Attini CLI application.", versionProvider = AttiniCliCommand.VersionProvider.class,
         mixinStandardHelpOptions = true, subcommands = {ListProfileCommand.class,
                                                         DeploymentCommand.class,
                                                         OnBoardingCommand.class,
                                                         GenerateCompletion.class,
                                                         ConfigCommand.class,
                                                         EnvironmentCommand.class,
                                                         InitProjectCommand.class,
                                                         OpsCommand.class,
                                                         UpdateCommand.class,
                                                         DistributionCommand.class})
public class AttiniCliCommand {

    public static void main(String[] args) {
        ApplicationContext applicationContext = ApplicationContext.builder().deduceEnvironment(false).build();
        if ("true".equalsIgnoreCase(System.getenv("ATTINI_DISABLE_ANSI_COLOR"))) {
            PrintUtil.clearColors();
        }
        CommandLine commandLine = new CommandLine(AttiniCliCommand.class, new MicronautFactory(applicationContext))
                .registerConverter(Region.class, Region::create)
                .registerConverter(Profile.class, Profile::create)
                .registerConverter(EnvironmentName.class, EnvironmentName::create)
                .registerConverter(DistributionName.class, DistributionName::create)
                .registerConverter(DistributionId.class, DistributionId::create)
                .registerConverter(FilePath.class, FilePath::create)
                .registerConverter(Email.class, Email::create)
                .registerConverter(StackName.class, StackName::create)
                .registerConverter(IamRoleArn.class, IamRoleArn::create);
        int execute = commandLine.execute(args);
        System.exit(execute);

    }


    public static class VersionProvider implements CommandLine.IVersionProvider {
        public final static int MAJOR = 2;
        public final static int MINOR = 6;
        public final static int PATCH = 0;

        public static final String VERSION_STRING = MAJOR + "." + MINOR + "." + PATCH;

        @Override
        public String[] getVersion() {

            return new String[]{VERSION_STRING};
        }


    }
}
