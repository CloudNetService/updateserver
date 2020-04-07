package eu.cloudnetservice.cloudnet.repository.database.statistics.internal;

import java.util.Objects;

public class CloudId {

    private String parentVersion;
    private String uniqueId;
    private transient String latestIp;
    private long timestamp;

    public CloudId(String parentVersion, String uniqueId, String latestIp) {
        this.parentVersion = parentVersion;
        this.uniqueId = uniqueId;
        this.latestIp = latestIp;
        this.timestamp = System.currentTimeMillis();
    }

    public String getParentVersion() {
        return this.parentVersion;
    }

    public String getUniqueId() {
        return this.uniqueId;
    }

    public String getLatestIp() {
        return this.latestIp;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CloudId cloudId = (CloudId) o;
        return uniqueId.equals(cloudId.uniqueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }
// todo equals/hashcode methods

    public static CloudId parse(String line, String ip) {
        if (line == null) {
            return null;
        }
        // todo Hardware ID?
        String[] split = line.split(":");
        if (split.length != 2) {
            return null;
        }
        return new CloudId(split[0], split[1], ip);
    }
}
