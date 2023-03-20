package se.attini.deployment.file.ignore;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

public class FilePatterns {

    public static final List<String> ILLEGAL_PATTERNS = List.of("**[#%\\{}`~<>|^ &;?$,+=@]*",
                                                                "**\\[*",
                                                                "**\\]*");

    public static final List<String> DEFAULT_IGNORES = List.of("**[#%\\{}`~<>|^ &;?$,+=@]*",
                                                               "**\\[*",
                                                               "**\\]*",
                                                               "**/.*",
                                                               ".*",
                                                               "**/node_modules/**");


    public static final List<String> TEMP_DIR_IGNORES = List.of("**/node_modules/**",
                                                                "**/node_modules",
                                                                "**/.DS_Store/**",
                                                                "**/.DS_Store",
                                                                "**/.vscode/**",
                                                                "**/.vscode");

    public static boolean patternMatchPath(List<String> patterns, Path path) {
        FileSystem fileSystem = FileSystems.getDefault();
        long count = patterns.stream()
                             .map(s -> fileSystem.getPathMatcher("glob:" + s))
                             .filter(pathMatcher -> pathMatcher.matches(path))
                             .count();
        return count > 0;
    }


}
