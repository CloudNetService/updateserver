package eu.cloudnetservice.cloudnet.repository.loader.jenkins;

public class JenkinsArtifact {

    private String displayPath;
    private String fileName;
    private String relativePath;

    public JenkinsArtifact(String displayPath, String fileName, String relativePath) {
        this.displayPath = displayPath;
        this.fileName = fileName;
        this.relativePath = relativePath;
    }

    public String getDisplayPath() {
        return this.displayPath;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getRelativePath() {
        return this.relativePath;
    }
}
