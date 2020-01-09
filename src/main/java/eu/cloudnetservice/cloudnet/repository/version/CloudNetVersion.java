package eu.cloudnetservice.cloudnet.repository.version;

import eu.cloudnetservice.cloudnet.repository.github.GitHubCommitInfo;
import eu.cloudnetservice.cloudnet.repository.github.GitHubReleaseInfo;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Date;
import java.util.Map;

@EqualsAndHashCode
@ToString
public class CloudNetVersion {

    private String name;
    private GitHubCommitInfo commit;
    private GitHubReleaseInfo release;
    private Date releaseDate;
    private CloudNetVersionFile[] files;
    private VersionFileMappings versionFileMappings;
    private Map<String, Object> properties;

    public CloudNetVersion(String name, GitHubCommitInfo commit, GitHubReleaseInfo release, Date releaseDate, CloudNetVersionFile[] files, VersionFileMappings versionFileMappings, Map<String, Object> properties) {
        this.name = name;
        this.commit = commit;
        this.release = release;
        this.releaseDate = releaseDate;
        this.files = files;
        this.versionFileMappings = versionFileMappings;
        this.properties = properties;
    }

    public String getName() {
        return this.name;
    }

    public GitHubCommitInfo getCommit() {
        return this.commit;
    }

    public GitHubReleaseInfo getRelease() {
        return this.release;
    }

    public void setRelease(GitHubReleaseInfo release) {
        this.release = release;
    }

    public Date getReleaseDate() {
        return this.releaseDate;
    }

    public CloudNetVersionFile[] getFiles() {
        return this.files;
    }

    public VersionFileMappings getVersionFileMappings() {
        return this.versionFileMappings;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }
}
