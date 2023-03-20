package se.attini.cli;

public class UserInputReaderScanner implements UserInputReader{
    @Override
    public String getUserInput() {
        return System.console().readLine();

    }
}
