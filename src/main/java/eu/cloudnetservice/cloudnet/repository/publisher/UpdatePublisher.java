package eu.cloudnetservice.cloudnet.repository.publisher;

import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;

import java.nio.file.Path;

public interface UpdatePublisher {

    String getName();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean init(Path configPath);

    void close();

    void publishRelease(CloudNetVersion version);

    void updateRelease(CloudNetVersion version);

    void deleteRelease(CloudNetVersion version);

}
