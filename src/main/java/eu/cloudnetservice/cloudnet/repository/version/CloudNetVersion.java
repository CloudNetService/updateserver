package eu.cloudnetservice.cloudnet.repository.version;

import eu.cloudnetservice.cloudnet.repository.github.GitHubCommitInfo;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

@EqualsAndHashCode
@ToString
public class CloudNetVersion implements Serializable {

    private String name;
    private GitHubCommitInfo commit;
    private Date releaseDate;
    private CloudNetVersionFile[] files;

    public CloudNetVersion(String name, GitHubCommitInfo commit, Date releaseDate, CloudNetVersionFile[] files) {
        this.name = name;
        this.commit = commit;
        this.releaseDate = releaseDate;
        this.files = files;
    }

    public String getName() {
        return this.name;
    }

    public GitHubCommitInfo getCommit() {
        return this.commit;
    }

    public Date getReleaseDate() {
        return this.releaseDate;
    }

    public CloudNetVersionFile[] getFiles() {
        return this.files;
    }
}
