package eu.cloudnetservice.cloudnet.repository.web.registry;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import eu.cloudnetservice.cloudnet.repository.web.WebServer;
import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.dsl.OpenApiUpdater;
import io.swagger.v3.oas.models.Operation;

import java.util.Arrays;
import java.util.stream.Collectors;

import static io.javalin.plugin.openapi.dsl.OpenApiBuilder.document;
import static io.javalin.plugin.openapi.dsl.OpenApiBuilder.documented;

public class VersionsHandlerRegistry implements JavalinHandlerRegistry {

    public static final String VERSIONS_TAG = "Versions";

    @Override
    public void init(CloudNetUpdateServer server, WebServer webServer, Javalin javalin) {
        javalin.get("/api/parentVersions", documented(
                document()
                        .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get the names of all parent versions").addTagsItem(VERSIONS_TAG))
                        .jsonArray("200", String.class)
                        .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                (Handler) context -> context.result(JsonDocument.GSON.toJson(server.getParentVersionNames()))
        ));
        javalin.get("/api/versions", documented(
                document()
                        .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get the names of all versions").addTagsItem(VERSIONS_TAG))
                        .jsonArray("200", String.class)
                        .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                (Handler) context -> context.result(JsonDocument.GSON.toJson(Arrays.stream(server.getDatabase().getAllVersions()).map(CloudNetVersion::getName).collect(Collectors.toList())))
        ));
        javalin.get("/api/versions/:parent", documented(
                document()
                        .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get the names of all versions available for a specific parent version").addTagsItem(VERSIONS_TAG))
                        .pathParam("parent", String.class, parameter -> parameter.description("The name of the parent version"))
                        .jsonArray("200", CloudNetVersion.class)
                        .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Parent version not found"))
                        .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                context -> {
                    if (server.getParentVersion(context.pathParam("parent")).isEmpty()) {
                        context.status(404);
                        return;
                    }
                    context.result(JsonDocument.GSON.toJson(Arrays.stream(server.getDatabase().getAllVersions(context.pathParam("parent"))).map(CloudNetVersion::getName).collect(Collectors.toList())));
                }
        ));
        javalin.get("/api/versions/:parent/:version", documented(
                document()
                        .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get all available information for a specific version").addTagsItem(VERSIONS_TAG))
                        .pathParam("parent", String.class, parameter -> parameter.description("The name of the parent version"))
                        .pathParam("version", String.class, parameter -> parameter.description("The name of the version"))
                        .json("200", CloudNetVersion.class)
                        .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Version not found"))
                        .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                context -> {
                    CloudNetVersion version = server.getDatabase().getVersion(
                            context.pathParam("parent"),
                            this.getVersionOrLatest(server, context.pathParam("parent"), context.pathParam("version"))
                    );
                    context.status(version != null ? 200 : 404).result(JsonDocument.GSON.toJson(version));
                }
        ));
    }

    private String getVersionOrLatest(CloudNetUpdateServer server, String parentVersionName, String version) {
        if (version.equalsIgnoreCase("latest")) {
            CloudNetVersion latestVersion = server.getCurrentLatestVersion(parentVersionName);
            return latestVersion != null ? latestVersion.getName() : "";
        }
        return version;
    }

}
