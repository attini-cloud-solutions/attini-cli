package se.attini.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import se.attini.cli.global.GlobalConfig;
import se.attini.util.ObjectMapperFactory;


@ExtendWith(MockitoExtension.class)
class TransformSimpleSyntaxTest {


    @Mock
    GlobalConfig globalConfig;

    private TransformSimpleSyntax transformSimpleSyntax;

    @BeforeEach
    void setUp() {
        transformSimpleSyntax = new TransformSimpleSyntax(new ObjectMapperFactory(globalConfig));
    }

    @Test
    public void shouldTransformSuccessfully() {
        JsonNode transform = transformSimpleSyntax.transform(readTemplate(SimpleSyntaxPlan.INPUT));
        assertEquals(readTemplate(SimpleSyntaxPlan.EXPECTED_RESULT), transform);
    }

    @Test
    public void shouldFailIfNameIsMissingInFirstStep() {
        JsonNode template = readTemplate(SimpleSyntaxPlan.INPUT);
        ObjectNode node = (ObjectNode) template.get(0);
        node.remove("Name");

        assertThrows(IllegalArgumentException.class, () -> transformSimpleSyntax.transform(template));
    }

    @Test
    public void shouldFailIfNameIsDuplicated() {
        JsonNode template = readTemplate(SimpleSyntaxPlan.INPUT);
        ObjectNode node = (ObjectNode) template.get(0);
        node.set("Name", template.get(1).get("Name"));

        assertThrows(IllegalArgumentException.class, () -> transformSimpleSyntax.transform(template));
    }

    @Test
    public void shouldFailIfNameIsMissingInStep() {
        JsonNode template = readTemplate(SimpleSyntaxPlan.INPUT);
        ObjectNode node = (ObjectNode) template.get(1);
        node.remove("Name");

        assertThrows(IllegalArgumentException.class, () -> transformSimpleSyntax.transform(template));
    }

    @Test
    public void shouldFailIfStepContainsNext() {
        JsonNode template = readTemplate(SimpleSyntaxPlan.INPUT);
        ObjectNode node = (ObjectNode) template.get(1);
        node.put("Next", "Something");
        assertThrows(IllegalArgumentException.class, () -> transformSimpleSyntax.transform(template));
    }


    @Test
    public void shouldTransformParallelSuccessfully() {
        JsonNode transform = transformSimpleSyntax.transform(readTemplate(SimpleSyntaxParallelPlan.INPUT));

        System.out.println(transform.toPrettyString());
        assertEquals(readTemplate(SimpleSyntaxParallelPlan.EXPECTED_RESULT), transform);
    }

    @Test
    public void shouldTransformParallelSuccessfully_dontAddMergeStepIfPresent() {
        JsonNode steps = readTemplate(SimpleSyntaxParallelPlanWithMerge.INPUT);
        JsonNode transform = transformSimpleSyntax.transform(steps);
        assertEquals(readTemplate(SimpleSyntaxParallelPlanWithMerge.EXPECTED_RESULT), transform);
    }

    @Test
    public void shouldTransformChoiceSuccessfully() {
        JsonNode transform = transformSimpleSyntax.transform(readTemplate(SimpleSyntaxChoicePlan.INPUT));

        assertEquals(readTemplate(SimpleSyntaxChoicePlan.EXPECTED_RESULT), transform);
    }


    @Test
    public void shouldTransformChoiceFailIfNoTrueBlock() {
        JsonNode template = readTemplate(SimpleSyntaxChoicePlan.INPUT);
        ObjectNode node = (ObjectNode)template.get(1);
        node.remove("IsTrue");
        assertThrows(IllegalArgumentException.class, () -> transformSimpleSyntax.transform(template));
    }

    @Test
    public void shouldTransformChoiceFailITrueBlockIsNotList() {
        JsonNode template = readTemplate(SimpleSyntaxChoicePlan.INPUT);
        ObjectNode node = (ObjectNode)template.get(1);
        node.put("IsTrue","LOL");
        assertThrows(IllegalArgumentException.class, () -> transformSimpleSyntax.transform(template));
    }

    @Test
    public void shouldTransformChoiceFailIFalseBlockIsNotList() {
        JsonNode template = readTemplate(SimpleSyntaxChoicePlan.INPUT);
        ObjectNode node = (ObjectNode)template.get(1);
        node.put("IsFalse","LOL");
        assertThrows(IllegalArgumentException.class, () -> transformSimpleSyntax.transform(template));
    }

    @Test
    public void shouldTransformChoiceFailIfTrueBlockFirstStepIsMissingName() {
        JsonNode template = readTemplate(SimpleSyntaxChoicePlan.INPUT);
        ObjectNode node = (ObjectNode)template.get(1).get("IsTrue").get(0);
        node.remove("Name");
        assertThrows(IllegalArgumentException.class, () -> transformSimpleSyntax.transform(template));
    }

    @Test
    public void shouldTransformChoiceFailIfFalseBlockFirstStepIsMissingName() {
        JsonNode template = readTemplate(SimpleSyntaxChoiceWithFalsePlan.INPUT);
        ObjectNode node = (ObjectNode)template.get(1).get("IsFalse").get(0);
        node.remove("Name");
        assertThrows(IllegalArgumentException.class, () -> transformSimpleSyntax.transform(template));
    }

    @Test
    public void shouldTransformChoiceEndSuccessfully() {
        JsonNode transform = transformSimpleSyntax.transform(readTemplate(SimpleSyntaxChoiceEndPlan.INPUT));

        assertEquals(readTemplate(SimpleSyntaxChoiceEndPlan.EXPECTED_RESULT), transform);
    }

    @Test
    public void shouldTransformChoiceWithFalseSuccessfully() {
        JsonNode transform = transformSimpleSyntax.transform(readTemplate(SimpleSyntaxChoiceWithFalsePlan.INPUT));

        assertEquals(readTemplate(SimpleSyntaxChoiceWithFalsePlan.EXPECTED_RESULT), transform);
    }


    private JsonNode readTemplate(String template) {
        try {
            return new ObjectMapper(new YAMLFactory()).readTree(template);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
