package eu.cloudnetservice.cloudnet.repository.config;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersionFile;
import eu.cloudnetservice.cloudnet.repository.version.VersionFileMappings;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class BasicConfiguration {

    private static final Path CONFIG_PATH = Paths.get("config.json");

    private int webPort;
    private Collection<CloudNetParentVersion> parentVersions;

    public int getWebPort() {
        return this.webPort;
    }

    public Collection<CloudNetParentVersion> getParentVersions() {
        return this.parentVersions;
    }

    public void load() {
        JsonDocument document = JsonDocument.newDocument();
        document.read(CONFIG_PATH);

        this.webPort = document.getInt("webPort", 1430);
        this.parentVersions = document.get("parentVersions", TypeToken.getParameterized(Collection.class, CloudNetParentVersion.class).getType(),
                Collections.singletonList(
                        new CloudNetParentVersion(
                                "v3",
                                "de.dytanic.cloudnet",
                                "/updates/v3",
                                "https://ci.cloudnetservice.eu/job/CloudNetService/job/CloudNet-v3/job/master/",
                                "https://api.github.com/repos/CloudNetService/CloudNet-v3/",
                                "123",
                                "/github/v3",
                                Arrays.asList(
                                        new CloudNetVersionFileInfo("Javadoc.zip", "docs", CloudNetVersionFile.FileType.JAVA_DOCS),
                                        new CloudNetVersionFileInfo("CloudNet.zip", "CloudNet.zip", CloudNetVersionFile.FileType.CLOUDNET_ZIP),
                                        new CloudNetVersionFileInfo("cloudnet-launcher/build/libs/launcher.jar", "launcher.jar", CloudNetVersionFile.FileType.CLOUDNET_JAR)
                                ),
                                "module.json",
                                new VersionFileMappings()
                        )
                )
        );

        document.write(CONFIG_PATH);
    }

    public void save() {
        JsonDocument.newDocument()
                .append("webPort", this.webPort)
                .append("parentVersions", this.parentVersions)
                .write(CONFIG_PATH);
    }

}
