package se.attini;

public class AttiniNotInstalledException extends RuntimeException{

    private final static String MSG = "The Attini Framework is not installed in the given account or region. Install it using the \"attini setup\" command. For more information visit https://docs.attini.io/getting-started/installations/deploy-and-update-the-attini-framework.html";

    public AttiniNotInstalledException() {
        super(MSG);
    }

    public AttiniNotInstalledException(Throwable e) {
        super(MSG, e);
    }
}
