package eu.cloudnetservice.cloudnet.repository.database.statistics.external;

import java.util.Map;
import java.util.Optional;

public class ExternalStatistics {

    private ExternalVersionedStatistics global;
    private Map<String, ExternalVersionedStatistics> versioned;

    public ExternalStatistics(ExternalVersionedStatistics global, Map<String, ExternalVersionedStatistics> versioned) {
        this.global = global;
        this.versioned = versioned;
    }

    public Map<String, ExternalVersionedStatistics> getVersionedStatistics() {
        return this.versioned;
    }

    public Optional<ExternalVersionedStatistics> getVersionedStatistics(String version) {
        return Optional.ofNullable(this.versioned.get(version));
    }

    public ExternalVersionedStatistics getGlobalStatistics() {
        return this.global;
    }

}
