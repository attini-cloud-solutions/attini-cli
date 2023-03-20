package se.attini.deployment.file;

import static se.attini.deployment.file.ignore.FilePatterns.patternMatchPath;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import se.attini.deployment.DistributionValidationException;
import se.attini.deployment.file.ignore.FilePatterns;

public class FileUtil {

    public static void validateDirectory(Path path, List<String> ignores) {

        File dir = path.toFile();
        File[] files = dir.listFiles();

        if (files != null && files.length > 0) {
            List<File> filesList = Arrays.stream(files)
                                         .filter(file -> !patternMatchPath(ignores, file.toPath())).toList();

            for (File file : filesList) {
                if (patternMatchPath(FilePatterns.ILLEGAL_PATTERNS, file.toPath())) {
                    throw new DistributionValidationException("Could not deploy distribution. File " + file.getName() + " contains illegal characters");
                }
                if (file.isDirectory()) {
                    FileUtil.validateDirectory(file.toPath(), ignores);
                }
            }
        }

    }
}
