package se.attini.inittemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

class InitTemplateReaderTest {

    InitTemplateReader initTemplateReader;

    @BeforeEach
    void setUp() {
        File initFile = Paths.get("src", "test", "resources", "init-templates", "template.yaml").toFile();
        initTemplateReader = InitTemplateReader.create(initFile, new ObjectMapper(new YAMLFactory()));
    }

    @Test
    void shouldReturnAllSamSteps() {
        Steps steps = initTemplateReader.getStepsByType(Set.of("AttiniSam"));
        assertEquals(1, steps.get("AttiniSam").size());
    }

    @Test
    void shouldReturnAllSamAndRunnerJobs() {
        Steps steps = initTemplateReader.getStepsByType(Set.of("AttiniSam", "AttiniRunnerJob"));
        assertEquals(1, steps.get("AttiniSam").size());
        assertEquals(8, steps.get("AttiniRunnerJob").size());
    }

    @Test
    void shouldReturnEmptyListIfNoneExists() {
        Steps steps = initTemplateReader.getStepsByType(Set.of("AttiniCfn"));
        assertTrue(steps.get("AttiniCfn").isEmpty());
    }

    @Test
    void shouldReturnAllRunners() {
        Set<String> runners = initTemplateReader.getRunners();
        assertEquals(1, runners.size());
    }
}
