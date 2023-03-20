package se.attini.cli;

import static se.attini.cli.PrintItem.PrintType.ERROR;
import static se.attini.cli.PrintItem.PrintType.NEW_LINE;
import static se.attini.cli.PrintItem.PrintType.NORMAL;
import static se.attini.cli.PrintItem.PrintType.SUCCESS;

public class PrintItem {

    private final PrintType printType;
    private final String message;

    private PrintItem(PrintType printType, String message) {
        this.printType = printType;
        this.message = message;
    }

    public PrintType getPrintType() {
        return printType;
    }

    public String getMessage() {
        return message;
    }

    public static PrintItem successMessage(String message){
        return new PrintItem(SUCCESS, message);
    }

    public static PrintItem errorMessage(String message){
        return new PrintItem(ERROR, message);
    }

    public static PrintItem message(String message){
        return new PrintItem(NORMAL, message);
    }

    public static PrintItem newLine(){
        return new PrintItem(NEW_LINE, "");
    }

    public static PrintItem message(PrintType printType, String message){
        return new PrintItem(printType, message);
    }

    public enum PrintType{
        SUCCESS,
        ERROR,
        ERROR_DEBUG,
        NORMAL,
        NORMAL_NO_TIMESTAMP,
        NORMAL_SAME_LINE,
        NEW_LINE
    }
}
