package eu.cloudnetservice.cloudnet.repository.database;

import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;

import java.io.Closeable;

public interface Database extends Closeable {

    boolean init();

    void registerVersion(CloudNetVersion version);

    CloudNetVersion getLatestVersion();

    CloudNetVersion[] getAllVersions();

}
