package se.attini.inittemplate;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class InitTemplateReader {

    private final JsonNode initTemplate;
    private final ObjectMapper objectMapper;

    private InitTemplateReader(File file, ObjectMapper objectMapper) {
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        if (file.getName().endsWith(".json")) {
            initTemplate = readJsonFile(file);
        } else {
            initTemplate = readYamlFile(file);
        }
    }

    public JsonNode getInitTemplate() {
        return initTemplate;
    }

    public static InitTemplateReader create(File initTemplate, ObjectMapper objectMapper) {
        return new InitTemplateReader(initTemplate, objectMapper);
    }

    private JsonNode readYamlFile(File file) {
        try {
            Yaml yaml = new Yaml(new CloudformationConstructor(new LoaderOptions()),
                                 new Representer(new DumperOptions()),
                                 new DumperOptions(),
                                 new CustomResolver());
            Object load = yaml.load(new FileInputStream(file));
            return objectMapper.valueToTree(load);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode readJsonFile(File file) {
        try {
            return objectMapper.readTree(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Set<String> getRunners() {
        return StreamSupport.stream(getFieldIterable(initTemplate.path("Resources")).spliterator(),
                                    false)
                            .filter(entry -> entry.getValue().path("Type")
                                                  .asText()
                                                  .equals("Attini::Deploy::Runner"))
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toSet());
    }


    public Optional<DeploymentPlanEntry> getDeploymentPlan() {

        return StreamSupport.stream(getFieldIterable(initTemplate.path("Resources")).spliterator(), false)
                            .filter(isDeploymentPlan())
                            .map(entry -> new DeploymentPlanEntry(entry.getKey(),
                                                                  entry.getValue(),
                                                                  entry.getValue()
                                                                       .path("Properties")
                                                                       .path("DeploymentPlan")
                                                                       .isArray() ? DeploymentPlanFormat.SIMPLE : DeploymentPlanFormat.STATE_LANGUAGE))
                            .findAny();
    }


    public record DeploymentPlanEntry(String name,
                                      JsonNode deploymentPlan,
                                      DeploymentPlanFormat deploymentPlanFormat) {

    }

    public enum DeploymentPlanFormat {
        SIMPLE, STATE_LANGUAGE
    }


    public Steps getStepsByType(Set<String> types) {

        Map<String, List<Step>> stepsByType =
                getDeploymentPlan()
                        .map(DeploymentPlanEntry::deploymentPlan)
                        .map(jsonNode -> jsonNode.path("Properties")
                                                 .path("DeploymentPlan"))
                        .map(jsonNode -> findSteps(jsonNode, types))
                        .orElse(Stream.empty())
                        .map(entry -> new Step(entry.getKey(), entry.getValue()))
                        .collect(Collectors.groupingBy(step -> step.definition()
                                                                   .get("Type")
                                                                   .asText()));

        return new Steps(stepsByType);
    }

    private static Predicate<Map.Entry<String, JsonNode>> isDeploymentPlan() {
        return entry -> entry.getValue().path("Type")
                             .asText()
                             .equals("Attini::Deploy::DeploymentPlan");
    }


    private static Stream<Map.Entry<String, JsonNode>> findSteps(JsonNode source, Set<String> types) {

        Iterable<Map.Entry<String, JsonNode>> iterable = getFieldIterable(source.path("States"));

        return StreamSupport.stream(iterable.spliterator(), false)
                            .flatMap(entry -> {
                                JsonNode node = entry.getValue();
                                if (node.path("Type").asText().equals("Parallel")) {
                                    return StreamSupport.stream(node.path("Branches").spliterator(), false)
                                                        .flatMap(jsonNode -> findSteps(jsonNode, types));

                                } else if (types.contains(node.path("Type").asText())) {
                                    return Stream.of(entry);
                                }
                                return Stream.empty();
                            });

    }

    private static class CustomResolver extends Resolver {
        @Override
        protected void addImplicitResolvers() {
            addImplicitResolver(Tag.BOOL, BOOL, "yYnNtTfFoO");
            /*
             * INT must be before FLOAT because the regular expression for FLOAT
             * matches INT (see issue 130)
             * http://code.google.com/p/snakeyaml/issues/detail?id=130
             */
            addImplicitResolver(Tag.INT, INT, "-+0123456789");
            addImplicitResolver(Tag.FLOAT, FLOAT, "-+0123456789.");
            addImplicitResolver(Tag.MERGE, MERGE, "<");
            addImplicitResolver(Tag.NULL, NULL, "~nN\0");
            addImplicitResolver(Tag.NULL, EMPTY, null);
        }
    }

    private static class CloudformationConstructor extends SafeConstructor {
        public CloudformationConstructor(LoaderOptions loaderOptions) {
            super(loaderOptions);
            this.yamlConstructors.put(new Tag("!Sub"), intrinsicFunction("Fn::Sub"));
            this.yamlConstructors.put(new Tag("!GetAtt"), intrinsicFunction("Fn::GetAtt"));
            this.yamlConstructors.put(new Tag("!Ref"), intrinsicFunction("Ref"));
            this.yamlConstructors.put(new Tag("!Split"), intrinsicFunction("Fn::Split"));
            this.yamlConstructors.put(new Tag("!Select"), intrinsicFunction("Fn::Select"));
            this.yamlConstructors.put(new Tag("!Join"), intrinsicFunction("Fn::Join"));
            this.yamlConstructors.put(new Tag("!ImportValue"), intrinsicFunction("Fn::ImportValue"));
            this.yamlConstructors.put(new Tag("!GetAZs"), intrinsicFunction("Fn::GetAZs"));
            this.yamlConstructors.put(new Tag("!FindInMap"), intrinsicFunction("Fn::FindInMap"));
            this.yamlConstructors.put(new Tag("!Cidr"), intrinsicFunction("Fn::Cidr"));
            this.yamlConstructors.put(new Tag("!Base64"), intrinsicFunction("Fn::Base64"));
            this.yamlConstructors.put(new Tag("!Equals"), intrinsicFunction("Fn::Equals"));
            this.yamlConstructors.put(new Tag("!And"), intrinsicFunction("Fn::And"));
            this.yamlConstructors.put(new Tag("!Or"), intrinsicFunction("Fn::Or"));
            this.yamlConstructors.put(new Tag("!Not"), intrinsicFunction("Fn::Not"));
            this.yamlConstructors.put(new Tag("!If"), intrinsicFunction("Fn::If"));


        }

        private AbstractConstruct intrinsicFunction(String function) {
            return new AbstractConstruct() {
                @Override
                public Object construct(Node node) {
                    if (node instanceof MappingNode) {
                        return Map.of(function, constructMapping((MappingNode) node));
                    }
                    if (node instanceof SequenceNode) {
                        return Map.of(function, constructSequence((SequenceNode) node));
                    }

                    return Map.of(function, constructScalar((ScalarNode) node));
                }
            };
        }
    }

    private static Iterable<Map.Entry<String, JsonNode>> getFieldIterable(JsonNode source) {
        return source::fields;
    }
}
