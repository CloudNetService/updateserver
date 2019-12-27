package eu.cloudnetservice.cloudnet.repository.version;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class VersionFileMappings {

    private static final Map<String, String> DEFAULT_VERSION_MAPPINGS = new HashMap<>();
    private static final Map<String, String> DEFAULT_VERSION_TARGETS = new HashMap<>();

    static {
        DEFAULT_VERSION_MAPPINGS.put("driver", "cloudnet-driver");

        Arrays.asList("cloudnet-driver", "cloudnet-bridge", "cloudnet-cloudperms", "cloudnet-common")
                .forEach(key -> DEFAULT_VERSION_TARGETS.put(key, "Service and Node"));
        Arrays.asList("cloudnet", "cloudnet-report", "cloudnet-smart", "cloudnet-storage-ftp", "cloudnet-cloudflare", "cloudnet-report", "cloudnet-database-mysql")
                .forEach(key -> DEFAULT_VERSION_TARGETS.put(key, "Only Node"));
        DEFAULT_VERSION_TARGETS.put("cloudnet-syncproxy", "All supported Proxies");
        DEFAULT_VERSION_TARGETS.put("cloudnet-signs", "Sign-Enabled Bukkit/Nukkit Services");
    }

    private Map<String, String> versionMappings = DEFAULT_VERSION_MAPPINGS;
    private Map<String, String> versionTargets = DEFAULT_VERSION_TARGETS;

    public VersionFileMappings() {
    }

    public VersionFileMappings(Map<String, String> versionMappings, Map<String, String> versionTargets) {
        this.versionMappings = versionMappings != null ? versionMappings : DEFAULT_VERSION_MAPPINGS;
        this.versionTargets = versionTargets != null ? versionTargets : DEFAULT_VERSION_TARGETS;
    }

    public String getVersionName(String fileName) {
        return this.versionMappings.getOrDefault(fileName, fileName);
    }

    public String getVersionTarget(String versionName) {
        return this.versionTargets.get(versionName);
    }

}
