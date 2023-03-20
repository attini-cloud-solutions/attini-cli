package se.attini.cli;

import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;

import se.attini.cli.global.GlobalConfig;
import se.attini.util.ObjectMapperFactory;

public class ConsolePrinter {

    private final ObjectMapperFactory objectMapperFactory;
    private final GlobalConfig globalConfig;

    public ConsolePrinter(ObjectMapperFactory objectMapperFactory, GlobalConfig globalConfig) {
        this.objectMapperFactory = requireNonNull(objectMapperFactory, "objectMapperFactory");
        this.globalConfig = requireNonNull(globalConfig, "globalConfig");
    }

    public void print(PrintItem message) {
        switch (message.getPrintType()) {
            case ERROR -> System.err.println(PrintUtil.toRed(message.getMessage()));
            case SUCCESS -> System.out.println(PrintUtil.toRed(message.getMessage()));
            case NORMAL_SAME_LINE -> System.out.print('\r' + message.getMessage() + " ".repeat(20));
            case NEW_LINE -> System.out.println();
            default -> System.out.println(message.getMessage());
        }
    }

    public void printError(CliError cliError, ErrorPrintType errorPrintType) {
        if (errorPrintType == ErrorPrintType.DATA){
            try {
                System.err.println(objectMapperFactory.getObjectMapper()
                                                            .writerWithDefaultPrettyPrinter()
                                                            .writeValueAsString(cliError));
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        }else {
            System.err.println(PrintUtil.toRed(getErrorMessage(cliError.getMessage())));
        }

        if (globalConfig.isDebug()){
            cliError.getOriginalException().ifPresent(Throwable::printStackTrace);
        }
    }
    public void printError(CliError cliError) {
        printError(cliError, ErrorPrintType.DATA);
    }

    private static String getErrorMessage(String message) {
        return message != null ? message : "An unknown error occurred, if the problem persist please contact Attini support, " +
                                                                         "to help us locate the issue run the command again with the --debug flag and attach the printed " +
                                                                         "stacktrace when contacting support";
    }

    public enum ErrorPrintType{
        DATA,TEXT
    }

}
