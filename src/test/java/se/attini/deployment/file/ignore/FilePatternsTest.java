package se.attini.deployment.file.ignore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static se.attini.deployment.file.ignore.FilePatterns.TEMP_DIR_IGNORES;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class FilePatternsTest {


    @Test
    void tempDirPatterns() {
        Path okPath = Path.of("/test/my-file.json");
        Path okPath2= Path.of("/node_modules2");

        Path illegalPath= Path.of("/test/node_modules/my-file.json");
        Path illegalPath2= Path.of("/test/node_modules");
        Path illegalPath3= Path.of("/node_modules");
        Path illegalPath4= Path.of("/test/node_modules/my-file/test");



        assertFalse(FilePatterns.patternMatchPath(TEMP_DIR_IGNORES, okPath));
        assertFalse(FilePatterns.patternMatchPath(TEMP_DIR_IGNORES, okPath2));

        assertTrue(FilePatterns.patternMatchPath(TEMP_DIR_IGNORES, illegalPath));
        assertTrue(FilePatterns.patternMatchPath(TEMP_DIR_IGNORES, illegalPath2));
        assertTrue(FilePatterns.patternMatchPath(TEMP_DIR_IGNORES, illegalPath3));
        assertTrue(FilePatterns.patternMatchPath(TEMP_DIR_IGNORES, illegalPath4));



    }
}
