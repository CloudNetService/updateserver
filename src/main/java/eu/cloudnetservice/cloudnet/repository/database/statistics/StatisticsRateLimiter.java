package eu.cloudnetservice.cloudnet.repository.database.statistics;

import eu.cloudnetservice.cloudnet.repository.database.statistics.internal.CloudId;
import io.javalin.http.HttpResponseException;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class StatisticsRateLimiter {

    private Map<CloudId, Long> requestsById = new ConcurrentHashMap<>();
    private TimeUnit unit;
    private long maxRequestsPerUnit;

    public StatisticsRateLimiter(ExecutorService executorService, TimeUnit unit, long maxRequestsPerUnit) {
        this.unit = unit;
        this.maxRequestsPerUnit = maxRequestsPerUnit;

        executorService.execute(() -> {
            while (!Thread.interrupted()) {
                requestsById.clear();

                try {
                    unit.sleep(1);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
        });
    }

    public void test(CloudId id) {
        Long value = this.requestsById.get(id);
        long newValue = value == null ? 1L : value + 1;

        this.requestsById.put(id, newValue);

        if (newValue >= this.maxRequestsPerUnit) {
            String unit = this.unit.toString().toLowerCase();
            unit = unit.substring(0, unit.length() - 1);
            throw new HttpResponseException(429, "Rate limit exceeded - Server allows " + this.maxRequestsPerUnit + " requests per " + unit + ".", Collections.emptyMap());
        }
    }

    public void block(CloudId id) {
        this.requestsById.put(id, this.maxRequestsPerUnit);
    }

}
