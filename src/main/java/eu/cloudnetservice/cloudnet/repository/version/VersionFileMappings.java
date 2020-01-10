package eu.cloudnetservice.cloudnet.repository.version;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.*;

@EqualsAndHashCode
@ToString
public class VersionFileMappings {

    private static final Map<String, String> DEFAULT_VERSION_MAPPINGS = new HashMap<>();
    private static final Map<String, Collection<String>> DEFAULT_SUPPORTED_ENVIRONMENTS = new HashMap<>();

    static {
        DEFAULT_VERSION_MAPPINGS.put("driver", "cloudnet-driver");


        DEFAULT_SUPPORTED_ENVIRONMENTS.put("SPIGOT", Arrays.asList("cloudnet-signs", "wrapper"));
        DEFAULT_SUPPORTED_ENVIRONMENTS.put("NUKKIT", Arrays.asList("cloudnet-signs", "wrapper"));
        DEFAULT_SUPPORTED_ENVIRONMENTS.put("SPONGE", Collections.singletonList("wrapper"));
        DEFAULT_SUPPORTED_ENVIRONMENTS.put("BUNGEE", Arrays.asList("cloudnet-syncproxy", "wrapper"));
        DEFAULT_SUPPORTED_ENVIRONMENTS.put("VELOCITY", Arrays.asList("cloudnet-syncproxy", "wrapper"));
        DEFAULT_SUPPORTED_ENVIRONMENTS.put("NODE", Arrays.asList(
                "cloudnet", "cloudnet-report", "cloudnet-smart", "cloudnet-storage-ftp", "cloudnet-cloudflare", "cloudnet-report", "cloudnet-database-mysql"
        ));

        DEFAULT_SUPPORTED_ENVIRONMENTS.put("ALL", Arrays.asList("cloudnet-driver", "cloudnet-bridge", "cloudnet-cloudperms", "cloudnet-common"));
    }

    private Map<String, String> versionMappings = DEFAULT_VERSION_MAPPINGS;
    private Map<String, Collection<String>> supportedEnvironments = DEFAULT_SUPPORTED_ENVIRONMENTS;

    public VersionFileMappings() {
    }

    public VersionFileMappings(Map<String, String> versionMappings, Map<String, Collection<String>> supportedEnvironments) {
        this.versionMappings = versionMappings != null ? versionMappings : DEFAULT_VERSION_MAPPINGS;
        this.supportedEnvironments = supportedEnvironments != null ? supportedEnvironments : DEFAULT_SUPPORTED_ENVIRONMENTS;
    }

    public Collection<String> getAvailableEnvironments() {
        return this.supportedEnvironments.keySet();
    }

    public String getVersionName(String fileName) {
        return this.versionMappings.getOrDefault(fileName, fileName);
    }

    public Collection<String> getSupportedDependencies(String environment) {
        environment = environment.toUpperCase();
        Collection<String> dependencies = new ArrayList<>(this.supportedEnvironments.getOrDefault(environment, Collections.emptyList()));
        dependencies.addAll(this.supportedEnvironments.getOrDefault("ALL", Collections.emptyList()));
        return dependencies;
    }

}
