package se.attini.domain;

import static java.util.Objects.requireNonNull;

public class FilePath {

    private final String path;
    private final SourceType sourceType;

    private FilePath(String path) {
        this.path = requireNonNull(path, "path");
        this.sourceType = resolveSourceType(path);

    }

    public static FilePath create(String path){
     return new FilePath(path);
    }

    public String getPath() {
        return path;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    private static SourceType resolveSourceType(String path){
        if (path.startsWith("https:")){
            return SourceType.HTTPS;
        }
        if (path.startsWith("s3://")){
            return SourceType.S3;
        }
        if (path.endsWith(".zip")){
            return SourceType.FILE_SYSTEM_ZIP;
        }
        return SourceType.FILE_SYSTEM_DIRECTORY;
    }

    public enum SourceType{
        S3, HTTPS, FILE_SYSTEM_DIRECTORY, FILE_SYSTEM_ZIP
    }
}
