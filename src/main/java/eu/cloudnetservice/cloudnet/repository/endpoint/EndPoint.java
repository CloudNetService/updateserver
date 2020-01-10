package eu.cloudnetservice.cloudnet.repository.endpoint;

import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;

import java.nio.file.Path;

public interface EndPoint {

    String getName();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean init(CloudNetUpdateServer updateServer, Path configPath);

    void close();

    void publishRelease(CloudNetParentVersion parentVersion, CloudNetVersion version);

    void updateRelease(CloudNetParentVersion parentVersion, CloudNetVersion version);

    void deleteRelease(CloudNetParentVersion parentVersion, CloudNetVersion version);

}
