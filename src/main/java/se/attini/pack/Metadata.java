package se.attini.pack;


import java.util.List;
import java.util.Set;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;

@Introspected
@ReflectiveAccess
@SuppressWarnings("unused")
public class Metadata {

    private final Set<String> usedRunners;
    private final List<SamProject> samProjects;

    public Metadata(Set<String> usedRunners, List<SamProject> samProjects) {
        this.usedRunners = usedRunners;
        this.samProjects = samProjects;
    }

    public Set<String> getUsedRunners() {
        return usedRunners;
    }

    public List<SamProject> getSamProjects() {
        return samProjects;
    }

    @Introspected
    @ReflectiveAccess
    public record SamProject(String path, String buildDir, String template, String stepName) {

        public SamProject(String path, String buildDir, String template, String stepName) {
            this.path =path.startsWith("/") ? path : "/"+path;
            this.buildDir = buildDir == null ? ".aws-sam/build" : buildDir;
            this.template = template == null ? "template.yaml" : template;
            this.stepName = stepName;
        }
    }
}
