package eu.cloudnetservice.cloudnet.repository.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Statistics {

    private VersionedStatistics globalStatistics;
    private Map<String, VersionedStatistics> versionedStatistics;

    public Statistics() {
        this.globalStatistics = new VersionedStatistics();
        this.versionedStatistics = new HashMap<>();
    }

    public void registerVersion(String version) {
        if (!this.versionedStatistics.containsKey(version)) {
            this.versionedStatistics.put(version, new VersionedStatistics());
        }
    }

    public Map<String, VersionedStatistics> getVersionedStatistics() {
        return this.versionedStatistics;
    }

    public Optional<VersionedStatistics> getVersionedStatistics(String version) {
        return Optional.ofNullable(this.versionedStatistics.get(version));
    }

    public VersionedStatistics getGlobalStatistics() {
        return this.globalStatistics;
    }

}
