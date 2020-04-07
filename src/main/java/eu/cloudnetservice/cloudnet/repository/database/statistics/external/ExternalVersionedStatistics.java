package eu.cloudnetservice.cloudnet.repository.database.statistics.external;

import de.dytanic.cloudnet.common.JavaVersion;
import eu.cloudnetservice.cloudnet.repository.database.statistics.internal.OperatingSystem;
import eu.cloudnetservice.cloudnet.repository.database.statistics.internal.ServerVersion;

import java.util.Map;

public class ExternalVersionedStatistics {

    private long downloads;
    private Map<String, Long> cloudNetVersions;
    private Map<ServerVersion, Integer> serverVersions;
    private Map<JavaVersion, Long> javaVersions;
    private Map<OperatingSystem, Long> operatingSystem;
    private Map<String, Long> countries;

    public ExternalVersionedStatistics(long downloads, Map<String, Long> cloudNetVersions, Map<ServerVersion, Integer> serverVersions, Map<JavaVersion, Long> javaVersions, Map<OperatingSystem, Long> operatingSystem, Map<String, Long> countries) {
        this.downloads = downloads;
        this.cloudNetVersions = cloudNetVersions;
        this.serverVersions = serverVersions;
        this.javaVersions = javaVersions;
        this.operatingSystem = operatingSystem;
        this.countries = countries;
    }
}
