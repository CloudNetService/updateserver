package eu.cloudnetservice.cloudnet.repository.database;

public class VersionedStatistics {

    private long downloads;

    public VersionedStatistics() {
        this.downloads = 0;
    }

    public long getDownloads() {
        return downloads;
    }

    public void increaseDownloads() {
        ++this.downloads;
    }

}
