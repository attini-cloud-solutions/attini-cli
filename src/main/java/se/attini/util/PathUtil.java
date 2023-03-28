package se.attini.util;

import java.nio.file.Path;

public class PathUtil {


    /**
     * Transform an absolut path to a relative path.
     * @param root The location from which the path should be relative to.
     * @param path The absolut path should be made relative
     * @return A relative path from the given root.
     */
    public static String transformPathToRelative(Path root, Path path) {
        if (path.isAbsolute()) {
            return path.normalize().toString().trim().replace(root.toAbsolutePath().normalize().toString().trim(), ".");
        }

        return path.toString();
    }
}
