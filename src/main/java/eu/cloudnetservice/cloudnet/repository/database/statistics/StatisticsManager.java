package eu.cloudnetservice.cloudnet.repository.database.statistics;

import de.dytanic.cloudnet.common.JavaVersion;
import eu.cloudnetservice.cloudnet.repository.database.Database;
import eu.cloudnetservice.cloudnet.repository.database.statistics.external.ExternalStatistics;
import eu.cloudnetservice.cloudnet.repository.database.statistics.internal.*;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import eu.cloudnetservice.cloudnet.repository.web.WebServer;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.util.RateLimit;
import io.javalin.plugin.openapi.dsl.OpenApiUpdater;
import io.swagger.v3.oas.models.Operation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.plugin.openapi.dsl.OpenApiBuilder.document;
import static io.javalin.plugin.openapi.dsl.OpenApiBuilder.documented;

public class StatisticsManager {

    private final Database database;

    private InternalStatistics currentStatistics;
    private boolean modified = false;

    public StatisticsManager(Database database) {
        this.database = database;
    }

    public void init(ExecutorService executorService, Javalin javalin) {
        this.currentStatistics = this.database.getStatistics();

        executorService.execute(() -> {
            while (!Thread.interrupted()) {
                if (this.modified) {
                    this.database.updateStatistics(this.currentStatistics);
                    this.modified = false;
                }

                try {
                    Thread.sleep(30000);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
        });

        this.initWeb(executorService, javalin);
    }

    private void initWeb(ExecutorService executorService, Javalin javalin) {
        javalin.get("/api/statistics", documented(
                document()
                        .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get the global statistics of CloudNet").addTagsItem(WebServer.GENERAL_TAG))
                        .json("200", ExternalStatistics.class)
                        .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                (Handler) ctx -> ctx.json(this.getExternalStatistics())
        ));

        javalin.routes(() -> path("/internal/statistics", () -> {
            post("serverVersion", ctx -> {
                new RateLimit(ctx).requestPerTimeUnit(20, TimeUnit.DAYS);

                String rawVersion = ctx.header("X-Server-Version");
                if (rawVersion == null || rawVersion.length() >= 10) {
                    throw new BadRequestResponse();
                }

                ServerVersion version = ServerVersion.parseServerVersion(rawVersion).orElseThrow(() -> new BadRequestResponse("Version not found"));

                CloudId id = this.getId(ctx);

                this.getStatistics().acceptStatistics(id.getParentVersion(), statistics -> statistics.getServerVersions().get(version).add(id));

            });
            post("javaVersion", ctx -> {
                new RateLimit(ctx).requestPerTimeUnit(3, TimeUnit.DAYS);

                String rawVersion = ctx.header("X-Java-Version");
                if (rawVersion == null || rawVersion.length() >= 3) {
                    throw new BadRequestResponse();
                }

                int parsedVersion;
                try {
                    parsedVersion = Integer.parseInt(rawVersion);
                } catch (NumberFormatException exception) {
                    throw new BadRequestResponse();
                }

                JavaVersion version = JavaVersion.fromVersionId(parsedVersion).orElseThrow(() -> new BadRequestResponse("JavaVersion not found"));

                CloudId id = this.getId(ctx);

                this.getStatistics().acceptStatistics(id.getParentVersion(), statistics -> statistics.getJavaVersions().put(id, version));
            });
            post("cloudNetVersion", ctx -> {
                new RateLimit(ctx).requestPerTimeUnit(3, TimeUnit.DAYS);

                String rawVersion = ctx.header("X-CloudNet-Version");
                if (rawVersion == null || rawVersion.length() >= 20) {
                    throw new BadRequestResponse();
                }

                CloudId id = this.getId(ctx);

                CloudNetVersion version = this.database.getVersion(id.getParentVersion(), rawVersion);
                if (version == null) {
                    throw new BadRequestResponse();
                }

                this.getStatistics().acceptStatistics(id.getParentVersion(), statistics -> statistics.getInstalledVersions().put(id, version.getName()));
            });

            StatisticsRateLimiter countryLimiter = new StatisticsRateLimiter(executorService, TimeUnit.DAYS, 2);
            post("country", ctx -> {
                new RateLimit(ctx).requestPerTimeUnit(2, TimeUnit.DAYS);

                String country = ctx.header("X-Country");
                if (country == null || country.length() >= 50) {
                    throw new BadRequestResponse();
                }

                CloudId id = this.getId(ctx);
                countryLimiter.test(id);

                this.getStatistics().acceptStatistics(id.getParentVersion(), statistics -> {
                    // todo check if country exists?
                    statistics.getCountries().put(id, country);
                }, () -> countryLimiter.block(id));

            });
            post("operatingSystem", ctx -> {
                new RateLimit(ctx).requestPerTimeUnit(2, TimeUnit.DAYS);

                String rawOperatingSystem = ctx.header("X-Operating-System");
                if (rawOperatingSystem == null) {
                    throw new BadRequestResponse();
                }

                OperatingSystem operatingSystem = OperatingSystem.parseOperatingSystem(rawOperatingSystem);
                if (operatingSystem == null) {
                    throw new NotFoundResponse("OperatingSystem not found");
                }

                CloudId id = this.getId(ctx);

                this.getStatistics().acceptStatistics(id.getParentVersion(), statistics -> statistics.getOperatingSystems().put(id, operatingSystem));

            });
        }));
    }

    private CloudId getId(Context ctx) {
        CloudId id = CloudId.parse(ctx.header("CloudNet-ID"), ctx.ip());

        if (id == null) {
            throw new BadRequestResponse();
        }
        return id;
    }

    public void save() {
        if (this.modified) {
            this.database.updateStatistics(this.currentStatistics);
        }
    }

    public InternalStatistics getStatistics() {
        return this.currentStatistics;
    }

    public ExternalStatistics getExternalStatistics() {
        return this.currentStatistics.toExternal();
    }

    public void increaseDownloads(String version) {
        this.currentStatistics.getVersionedStatistics(version).ifPresent(InternalVersionedStatistics::increaseDownloads);
        this.currentStatistics.getGlobalStatistics().increaseDownloads();
        this.modified = true;
    }

}
