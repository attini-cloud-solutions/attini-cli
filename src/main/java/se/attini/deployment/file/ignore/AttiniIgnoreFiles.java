package se.attini.deployment.file.ignore;

import java.io.File;
import java.nio.file.Path;

public class AttiniIgnoreFiles {

    private static final String ATTINI_IGNORE_FILE = ".attini-ignore";


    public AttiniIgnoreFile getAttiniIgnoreFile(Path path){
        File[] files = path.toFile().listFiles(name -> name.getName().equals(ATTINI_IGNORE_FILE));
        if (files == null || files.length == 0) {
          return AttiniIgnoreFile.emptyFile();
        }
        if (files.length > 1) {
            throw new AttiniIgnoreFileException("There is more then one attini.ignore file on the given path");
        }
        return  AttiniIgnoreFile.create(files[0]);
    }
}
