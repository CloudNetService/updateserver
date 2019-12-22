package eu.cloudnetservice.cloudnet.repository.database;

import eu.cloudnetservice.cloudnet.repository.github.GitHubReleaseInfo;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Objects;

public interface Database extends Closeable {

    boolean init();

    void registerVersion(CloudNetVersion version);

    void updateVersion(CloudNetVersion version);

    CloudNetVersion getVersion(String name);

    default CloudNetVersion getVersion(GitHubReleaseInfo release) {
        return Arrays.stream(this.getAllVersions())
                .filter(Objects::nonNull)
                .filter(version -> version.getRelease().getId() == release.getId())
                .findFirst()
                .orElse(null);
    }

    CloudNetVersion getLatestVersion();

    CloudNetVersion[] getAllVersions();

}
