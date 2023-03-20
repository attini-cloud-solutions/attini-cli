package se.attini.cli;

import se.attini.AttiniNotInstalledException;
import se.attini.InvalidCredentialsException;
import se.attini.deployment.file.config.AttiniConfigFileException;

public class ErrorResolver {


    public static CliError resolve(Throwable e){
        return CliError.create(getErrorCode(e), getErrorMessage(e), e);
    }

    private static ErrorCode getErrorCode(Throwable e){
        if(e instanceof AttiniNotInstalledException){
            return ErrorCode.AttiniFrameworkNotInstalled;
        }
        if (e instanceof IllegalArgumentException){
            return ErrorCode.IllegalArgument;
        }
        if (e instanceof InvalidCredentialsException){
            return ErrorCode.InvalidCredentials;
        }
        if (e instanceof AttiniConfigFileException){
            return ErrorCode.AttiniConfigError;
        }
        return ErrorCode.ExecutionError;
    }

    private static String getErrorMessage(Throwable throwable) {
        return throwable.getMessage() != null ? throwable.getMessage() : "An unknown error occurred, if the problem persist please contact Attini support, " +
                                                                         "to help us locate the issue run the command again with the --debug flag and attach the printed " +
                                                                         "stacktrace when contacting support";
    }
}
