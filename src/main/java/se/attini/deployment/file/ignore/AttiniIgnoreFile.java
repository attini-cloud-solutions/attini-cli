package se.attini.deployment.file.ignore;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class AttiniIgnoreFile {

    private final File file;


    private AttiniIgnoreFile(File file) {
        this.file = file;
    }

    public static AttiniIgnoreFile create(File file) {
        return new AttiniIgnoreFile(file);
    }

    public static AttiniIgnoreFile emptyFile() {
        return new AttiniIgnoreFile(null);
    }


    /**
     * Will return a list of path strings that should be ignored on the provided path
     *
     * @param path to a directory where the ignores should be applied
     * @return a list of paths that will be ignored
     */
    public List<String> getIgnores(Path path) {
        if (file == null) {
            return FilePatterns.DEFAULT_IGNORES;
        }
        try (Stream<String> lines = Files.lines(file.toPath())) {
            return lines.map(s -> path + getFileName(s))
                        .collect(toList());

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String getFileName(String fileName) {
        if (fileName.startsWith("/")) {
            return fileName;
        }
        return "/" + fileName;
    }

}
