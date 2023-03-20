package se.attini.domain;

import picocli.CommandLine;

public class DomainConversionError extends CommandLine.TypeConversionException {

    public DomainConversionError(String msg) {
        super(msg);
    }
}
