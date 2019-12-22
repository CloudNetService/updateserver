package eu.cloudnetservice.cloudnet.repository.version;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.github.GitHubCommitInfo;
import eu.cloudnetservice.cloudnet.repository.github.GitHubReleaseInfo;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode
@ToString
public class CloudNetVersion implements Serializable {

    private String name;
    private GitHubCommitInfo commit;
    private GitHubReleaseInfo release;
    private Date releaseDate;
    private CloudNetVersionFile[] files;
    private Map<String, String> properties;

    public CloudNetVersion(String name, GitHubCommitInfo commit, GitHubReleaseInfo release, Date releaseDate, CloudNetVersionFile[] files, Map<String, String> properties) {
        this.name = name;
        this.commit = commit;
        this.release = release;
        this.releaseDate = releaseDate;
        this.files = files;
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

    public Date getReleaseDate() {
        return this.releaseDate;
    }

    public CloudNetVersionFile[] getFiles() {
        return this.files;
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }
}
