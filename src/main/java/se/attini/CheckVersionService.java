package se.attini;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.attini.cli.AttiniCliCommand;


public class CheckVersionService {


    private final EnvironmentVariables environmentVariables;

    public CheckVersionService(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    private String getCpuArch(){
        String architecture = System.getProperty("os.arch");
        if(architecture.equals("amd64")){
            return "x86_64";
        }
        return architecture;
    }

    public String getOS(){
        return System.getProperty("os.name").startsWith("MAC") ? "Darwin" : "Linux";
    }

    public void checkVersion() {

        if (environmentVariables.isDisableCliVersionCheck()) {
            return;
        }
        try {
            String body = HttpClient.newHttpClient()
                                    .send(HttpRequest.newBuilder()
                                                     .GET()
                                                     .timeout(Duration.ofSeconds(1))
                                                     .uri(URI.create(
                                                             "https://docs.attini.io/api/v1/cli/get-cli-versions/" + getCpuArch() + "/" + getOS()))
                                                     .build(),
                                          HttpResponse.BodyHandlers.ofString())
                                    .body();

            new ObjectMapper().readTree(body).spliterator();
            Optional<Version> latest = StreamSupport.stream(
                    new ObjectMapper().readTree(body).spliterator(),
                    false).map(JsonNode::asText).filter(s -> !s.equals("latest")).map(s -> {
                String[] split = s.split("\\.");
                return new Version(Integer.parseInt(split[0]),
                                   Integer.parseInt(split[1]),
                                   Integer.parseInt(split[2]),
                                   null,
                                   null,
                                   null);

            }).max(Version::compareTo);

            Version currentVersion = new Version(AttiniCliCommand.VersionProvider.MAJOR,
                                                 AttiniCliCommand.VersionProvider.MINOR,
                                                 AttiniCliCommand.VersionProvider.PATCH,
                                                 null,
                                                 null,
                                                 null);

            latest.ifPresent(latestVersion -> {
                if (latestVersion.compareTo(currentVersion) > 0) {
                    System.out.println(
                            "There is a new version of the Attini CLI. You are currently using version " + AttiniCliCommand.VersionProvider.VERSION_STRING + ". The latest version is " + latestVersion + ". To update, use the Attini update CLI command:");
                    System.out.println();
                    System.out.println("\t attini update-cli");
                    System.out.println();
                }
            });

        } catch (Exception e) {
            //do nothing
        }
    }
}
