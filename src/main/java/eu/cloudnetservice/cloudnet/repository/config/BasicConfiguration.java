package eu.cloudnetservice.cloudnet.repository.config;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.version.VersionFileMappings;

import java.nio.file.Path;
import java.nio.file.Paths;

public class BasicConfiguration {

    private static final Path CONFIG_PATH = Paths.get("config.json");

    private int webPort;
    private String gitHubSecret;
    private VersionFileMappings versionFileMappings;

    public int getWebPort() {
        return this.webPort;
    }

    public String getGitHubSecret() {
        return this.gitHubSecret;
    }

    public VersionFileMappings getVersionFileMappings() {
        return this.versionFileMappings;
    }

    public void load() {
        JsonDocument document = JsonDocument.newDocument();
        document.read(CONFIG_PATH);

        this.webPort = document.getInt("webPort", 1430);
        this.gitHubSecret = document.getString("gitHubSecret");
        this.versionFileMappings = document.get("versionFileMappings", VersionFileMappings.class, new VersionFileMappings());

        document.write(CONFIG_PATH);
    }

}
