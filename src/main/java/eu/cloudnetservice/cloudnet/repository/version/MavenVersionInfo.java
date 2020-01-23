package eu.cloudnetservice.cloudnet.repository.version;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class MavenVersionInfo {

    private String repositoryUrl;
    private String groupId;
    private String artifactId;

    public MavenVersionInfo(String repositoryUrl, String groupId, String artifactId) {
        this.repositoryUrl = repositoryUrl;
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public String getRepositoryUrl() {
        return this.repositoryUrl;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public String getArtifactId() {
        return this.artifactId;
    }

    public String getFullURL(String version) {
        return this.repositoryUrl + "/" + (this.groupId.replace('.', '/')) + "/" + this.artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
    }

}
