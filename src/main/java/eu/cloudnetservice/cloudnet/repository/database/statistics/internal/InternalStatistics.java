package eu.cloudnetservice.cloudnet.repository.database.statistics.internal;

import eu.cloudnetservice.cloudnet.repository.database.statistics.external.ExternalStatistics;
import eu.cloudnetservice.cloudnet.repository.database.statistics.external.ExternalVersionedStatistics;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class InternalStatistics {

    private InternalVersionedStatistics globalStatistics;
    private Map<String, InternalVersionedStatistics> versionedStatistics;

    public InternalStatistics() {
        this.globalStatistics = new InternalVersionedStatistics();
        this.versionedStatistics = new HashMap<>();
    }

    public void registerVersion(String version) {
        if (!this.versionedStatistics.containsKey(version)) {
            this.versionedStatistics.put(version, new InternalVersionedStatistics());
        }
    }

    public Map<String, InternalVersionedStatistics> getVersionedStatistics() {
        return this.versionedStatistics;
    }

    public Optional<InternalVersionedStatistics> getVersionedStatistics(String version) {
        return Optional.ofNullable(this.versionedStatistics.get(version));
    }

    public InternalVersionedStatistics getGlobalStatistics() {
        return this.globalStatistics;
    }

    public ExternalStatistics toExternal() {
        Map<String, ExternalVersionedStatistics> versionedStatistics = new HashMap<>(this.versionedStatistics.size());
        this.versionedStatistics.forEach((version, statistics) -> versionedStatistics.put(version, statistics.toExternal()));
        return new ExternalStatistics(this.globalStatistics.toExternal(), versionedStatistics);
    }

    public void acceptStatistics(String version, Consumer<InternalVersionedStatistics> consumer, Runnable orElse) {
        this.getVersionedStatistics(version).ifPresentOrElse(versionedStatistics -> {
            consumer.accept(versionedStatistics);
            consumer.accept(this.globalStatistics);
        }, orElse);
    }

    public void acceptStatistics(String version, Consumer<InternalVersionedStatistics> consumer) {
        this.acceptStatistics(version, consumer, () -> {});
    }

}
