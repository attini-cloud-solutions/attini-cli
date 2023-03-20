package se.attini.util;

import static org.junit.jupiter.api.Assertions.*;
import static se.attini.util.PathUtil.transformPathToRelative;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PathUtilTest {


    @Test
    public void shouldTransformPath() {

        assertEquals("./test.sh",
                     transformPathToRelative(Path.of("/Users/someuser/IdeaProjects/sol-infra"),
                                             Path.of("/Users/someuser/IdeaProjects/sol-infra/test.sh")));

        assertEquals("./test.sh",
                     transformPathToRelative(Path.of("/Users/someuser/IdeaProjects/./sol-infra"),
                                             Path.of("/Users/someuser/IdeaProjects/sol-infra/test.sh")));
        assertEquals("./test.sh",
                     transformPathToRelative(Path.of("/Users/someuser/IdeaProjects/sol-infra"),
                                             Path.of("/Users/someuser/IdeaProjects/./sol-infra/test.sh")));
        assertEquals("./test.sh",
                     transformPathToRelative(Path.of("/Users/someuser/IdeaProjects/sol-infra"),
                                             Path.of("/Users/someuser/IdeaProjects/./././sol-infra/test.sh")));
        assertEquals("./test.sh",
                     transformPathToRelative(Path.of("/Users/someuser/IdeaProjects/././sol-infra"),
                                             Path.of("/Users/someuser/IdeaProjects/././sol-infra/test.sh")));

    }

}
