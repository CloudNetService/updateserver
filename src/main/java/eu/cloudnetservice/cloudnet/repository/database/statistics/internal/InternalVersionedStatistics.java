package eu.cloudnetservice.cloudnet.repository.database.statistics.internal;

import de.dytanic.cloudnet.common.JavaVersion;
import eu.cloudnetservice.cloudnet.repository.database.statistics.external.ExternalVersionedStatistics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InternalVersionedStatistics {

    private long downloads;
    private Map<CloudId, String> installedVersions;
    private Map<ServerVersion, Set<CloudId>> serverVersions;
    private Map<CloudId, JavaVersion> javaVersions;
    private Map<CloudId, OperatingSystem> operatingSystems;
    private Map<CloudId, String> countries;

    public InternalVersionedStatistics() {
        this.downloads = 0;
        this.installedVersions = new HashMap<>();
        this.serverVersions = new HashMap<>();
        this.javaVersions = new HashMap<>();
        this.operatingSystems = new HashMap<>();
        this.countries = new HashMap<>();

        for (ServerVersion version : ServerVersion.values()) {
            this.serverVersions.put(version, new HashSet<>());
        }
    }

    public long getDownloads() {
        return downloads;
    }

    public Map<CloudId, String> getInstalledVersions() {
        return installedVersions;
    }

    public Map<ServerVersion, Set<CloudId>> getServerVersions() {
        return serverVersions;
    }

    public Map<CloudId, JavaVersion> getJavaVersions() {
        return javaVersions;
    }

    public Map<CloudId, OperatingSystem> getOperatingSystems() {
        return operatingSystems;
    }

    public Map<CloudId, String> getCountries() {
        return countries;
    }

    public void increaseDownloads() {
        ++this.downloads;
    }

    public void setCloudNetVersion(CloudId id, String version) {
        this.installedVersions.put(id, version);
    }

    public void addServerVersion(CloudId id, ServerVersion version) {
        this.serverVersions.get(version).add(id);
    }

    public void setJavaVersion(CloudId id, JavaVersion version) {
        this.javaVersions.put(id, version);
    }

    public void setOperatingSystem(CloudId id, OperatingSystem system) {
        this.operatingSystems.put(id, system);
    }

    public void setCountry(CloudId id, String country) {
        this.countries.put(id, country);
    }

    public ExternalVersionedStatistics toExternal() {
        Map<String, Long> cloudNetVersions = this.installedVersions.values().stream()
                .collect(Collectors.toMap(Function.identity(), version -> 1L, Long::sum));
        Map<ServerVersion, Integer> serverVersions = this.serverVersions.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
        Map<JavaVersion, Long> javaVersions = this.javaVersions.values().stream()
                .collect(Collectors.toMap(Function.identity(), version -> 1L, Long::sum));
        Map<OperatingSystem, Long> operatingSystems = this.operatingSystems.values().stream()
                .collect(Collectors.toMap(Function.identity(), os -> 1L, Long::sum));
        Map<String, Long> countries = this.countries.values().stream()
                .collect(Collectors.toMap(Function.identity(), country -> 1L, Long::sum));

        return new ExternalVersionedStatistics(
                this.downloads, cloudNetVersions, serverVersions, javaVersions, operatingSystems, countries
        );
    }

}
