package se.attini.deployment.zip;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import se.attini.deployment.file.ignore.FilePatterns;

public final class ZipUtil {

    public static byte[] zipDirectory(Path path, List<String> ignores) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(baos);

            File[] fileArray = path.toFile().listFiles();
            if (fileArray == null){
                throw new ZipException("no files in directory " + path);
            }

            for (File file : fileArray) {
                zipFile(file, file.getName(), zipOut, ignores);
            }

            zipOut.close();
            baos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ZipException(e);
        }

    }

    private static void zipFile(File fileToZip,
                                String fileName,
                                ZipOutputStream zipOut,
                                List<String> ignores) throws IOException {

        if (FilePatterns.patternMatchPath(ignores, fileToZip.toPath())) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
            }
            zipOut.closeEntry();
            File[] children = requireNonNull(fileToZip.listFiles());
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut, ignores);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

}
