package eu.cloudnetservice.cloudnet.repository.version;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.*;

@EqualsAndHashCode
@ToString
public class VersionFileMappings {

    private static final Map<String, String> DEFAULT_VERSION_MAPPINGS = new HashMap<>();
    private static final Map<VersionEnvironment, Collection<String>> DEFAULT_SUPPORTED_ENVIRONMENTS = new HashMap<>();

    static {
        DEFAULT_VERSION_MAPPINGS.put("driver", "cloudnet-driver");


        DEFAULT_SUPPORTED_ENVIRONMENTS.put(VersionEnvironment.SPIGOT, Arrays.asList("cloudnet-signs", "cloudnet-wrapper-jvm"));
        DEFAULT_SUPPORTED_ENVIRONMENTS.put(VersionEnvironment.NUKKIT, Arrays.asList("cloudnet-signs", "cloudnet-wrapper-jvm"));
        DEFAULT_SUPPORTED_ENVIRONMENTS.put(VersionEnvironment.SPONGE, Collections.singletonList("cloudnet-wrapper-jvm"));
        DEFAULT_SUPPORTED_ENVIRONMENTS.put(VersionEnvironment.BUNGEE, Arrays.asList("cloudnet-syncproxy", "cloudnet-wrapper-jvm"));
        DEFAULT_SUPPORTED_ENVIRONMENTS.put(VersionEnvironment.VELOCITY, Arrays.asList("cloudnet-syncproxy", "cloudnet-wrapper-jvm"));
        DEFAULT_SUPPORTED_ENVIRONMENTS.put(VersionEnvironment.NODE, Arrays.asList(
                "cloudnet", "cloudnet-report", "cloudnet-smart", "cloudnet-storage-ftp", "cloudnet-cloudflare", "cloudnet-report", "cloudnet-database-mysql"
        ));

        DEFAULT_SUPPORTED_ENVIRONMENTS.put(VersionEnvironment.ALL, Arrays.asList("cloudnet-driver", "cloudnet-bridge", "cloudnet-cloudperms", "cloudnet-common"));
    }

    private Map<String, String> versionMappings = DEFAULT_VERSION_MAPPINGS;
    private Map<VersionEnvironment, Collection<String>> supportedEnvironments = DEFAULT_SUPPORTED_ENVIRONMENTS;

    public VersionFileMappings() {
    }

    public VersionFileMappings(Map<String, String> versionMappings, Map<VersionEnvironment, Collection<String>> supportedEnvironments) {
        this.versionMappings = versionMappings != null ? versionMappings : DEFAULT_VERSION_MAPPINGS;
        this.supportedEnvironments = supportedEnvironments != null ? supportedEnvironments : DEFAULT_SUPPORTED_ENVIRONMENTS;
    }

    public String getVersionName(String fileName) {
        return this.versionMappings.getOrDefault(fileName, fileName);
    }

    public Collection<String> getSupportedDependencies(VersionEnvironment environment) {
        Collection<String> dependencies = new ArrayList<>(this.supportedEnvironments.get(environment));
        dependencies.addAll(this.supportedEnvironments.get(VersionEnvironment.ALL));
        return dependencies;
    }

}
