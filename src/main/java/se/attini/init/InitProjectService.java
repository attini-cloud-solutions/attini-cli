package se.attini.init;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import se.attini.pack.AttiniFileSystemException;

public class InitProjectService {

    private static final Set<String> IGNORES = Set.of(".gitignore", "LICENSE", "README.md", "README.rst");

    public static void initHelloWorld() {
        initProject("init-project-hello-world");
    }

    public static void initSamProject(){
        initProject("sam-project");
    }

    public static void initSkeleton() {
        initProject("init-project-skeleton");
    }

    public static void initSimpleWebsite(){
        initProject("example-website");
    }

    private static void initProject(String project) {
        boolean containAttiniConfig = Arrays.stream(Objects.requireNonNull(new File(".").listFiles()))
                          .anyMatch(file -> file.getName().contains("attini-config"));
        if(containAttiniConfig){
            throw new RuntimeException("Could not init project, attini-config is already present in folder");
        }
        try {
            Path tempDirectory = createTempDirectory();
            File download = new File(tempDirectory.toFile(),"attini-hello-world.zip");
            FileUtils.copyURLToFile(new URL(
                                            "https://www.github.com/attini-cloud-solutions/" + project + "/archive/main.zip"),
                                    download,
                                    10000,
                                    100000);
            ZipFile zipFile = new ZipFile(download.getAbsolutePath());
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!containsIgnore(entry.getName())) {
                    File entryDestination = new File(tempDirectory.toFile(),entry.getName());
                    if (entry.isDirectory()) {
                        entryDestination.mkdirs();
                    } else {
                        entryDestination.getParentFile().mkdirs();
                        try (OutputStream out = new FileOutputStream(entryDestination)) {
                            zipFile.getInputStream(entry).transferTo(out);
                        }
                    }

                }
            }

            File zippedDir = new File(tempDirectory.toFile(), project + "-main");
            for (File file : FileUtils.listFiles(zippedDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
                File destination = new File(file.getPath().replace(tempDirectory+"/"+project + "-main/", ""));
                if (destination.exists()) {
                    cleanUp(tempDirectory);
                    throw new RuntimeException("Could not init project, " + destination.getPath() + " already exists");
                }
            }
            FileUtils.copyDirectory(zippedDir, new File("."));
            cleanUp(tempDirectory);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to init project due to IO exception, make sure you have the right permissions",
                    e);
        }
    }

    private static void cleanUp(Path tempDir) throws IOException {
        FileUtils.deleteDirectory(tempDir.toFile());
    }

    private static Path createTempDirectory() {
        try {
            return Files.createTempDirectory("attini-temp");
        } catch (IOException e) {
            throw new AttiniFileSystemException("Could not create temp directory", e);
        }
    }

    private static boolean containsIgnore(String name) {
        return IGNORES.stream().anyMatch(name::contains);
    }
}
