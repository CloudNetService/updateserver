package eu.cloudnetservice.cloudnet.repository.database;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;

import java.util.concurrent.ExecutorService;

public class StatisticsManager {

    private final Database database;

    private Statistics currentStatistics;
    private boolean modified = false;

    public StatisticsManager(Database database) {
        this.database = database;
    }

    public void init(ExecutorService executorService) {
        this.currentStatistics = this.database.getStatistics().toInstanceOf(Statistics.class);

        executorService.execute(() -> {
            while (!Thread.interrupted()) {
                if (this.modified) {
                    this.database.updateStatistics(new JsonDocument(this.currentStatistics));
                    this.modified = false;
                }

                try {
                    Thread.sleep(30000);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
        });
    }

    public void save() {
        if (this.modified) {
            this.database.updateStatistics(new JsonDocument(this.currentStatistics));
        }
    }

    public Statistics getStatistics() {
        return this.currentStatistics;
    }

    public void increaseDownloads(String version) {
        this.currentStatistics.getVersionedStatistics(version).ifPresent(VersionedStatistics::increaseDownloads);
        this.currentStatistics.getGlobalStatistics().increaseDownloads();
        this.modified = true;
    }

}
