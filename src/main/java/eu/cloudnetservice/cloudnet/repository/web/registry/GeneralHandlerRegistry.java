package eu.cloudnetservice.cloudnet.repository.web.registry;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.Constants;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;
import eu.cloudnetservice.cloudnet.repository.version.service.ServiceVersion;
import eu.cloudnetservice.cloudnet.repository.version.service.ServiceVersionType;
import eu.cloudnetservice.cloudnet.repository.web.APIAvailableResponse;
import eu.cloudnetservice.cloudnet.repository.web.WebPermissionRole;
import eu.cloudnetservice.cloudnet.repository.web.WebServer;
import eu.cloudnetservice.cloudnet.repository.web.handler.ArchivedVersionHandler;
import eu.cloudnetservice.cloudnet.repository.web.handler.GitHubWebHookReleaseEventHandler;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Handler;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.plugin.openapi.dsl.OpenApiUpdater;
import io.swagger.v3.oas.models.Operation;

import java.util.Set;

import static io.javalin.plugin.openapi.dsl.OpenApiBuilder.document;
import static io.javalin.plugin.openapi.dsl.OpenApiBuilder.documented;

public class GeneralHandlerRegistry implements JavalinHandlerRegistry {

    public static final String GENERAL_TAG = "General";

    @Override
    public void init(CloudNetUpdateServer server, WebServer webServer, Javalin javalin) {
        javalin.get("/admin/api", context -> context.result("{}"), Set.of(WebPermissionRole.MEMBER));
        javalin.get("/api", documented(
                document()
                        .operation(operation -> {
                            operation
                                    .summary("Check if the API is available")
                                    .addTagsItem(GENERAL_TAG)
                                    .operationId("apiAvailable");
                        })
                        .json("200", APIAvailableResponse.class),
                (Handler) context -> context.json(new APIAvailableResponse(webServer.isApiAvailable()))
        ));

        javalin.before("/api/*", context -> {
            if (!webServer.isApiAvailable() && !context.path().equalsIgnoreCase("/api/")) {
                throw new InternalServerErrorResponse("API currently not available");
            }
        });
        javalin.get("/api/languages", documented(
                document()
                        .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get all available languages").addTagsItem(GENERAL_TAG))
                        .jsonArray("200", String.class)
                        .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                (Handler) context -> context.json(server.getConfiguration().getAvailableLanguages())
        ));

        for (CloudNetParentVersion parentVersion : server.getConfiguration().getParentVersions()) {
            javalin.post(parentVersion.getGitHubWebHookPath(), new GitHubWebHookReleaseEventHandler(server, parentVersion));

            javalin.get("/versions/" + parentVersion.getName() + "/:version/*", new ArchivedVersionHandler(Constants.VERSIONS_DIRECTORY.resolve(parentVersion.getName()), parentVersion, "CloudNet.zip", server));
            javalin.get("/docs/" + parentVersion.getName() + "/:version/*", new ArchivedVersionHandler(Constants.DOCS_DIRECTORY.resolve(parentVersion.getName()), parentVersion, "index.html", server));
        }

        javalin.get("/api/:parent/serviceversions", documented(
                document()
                        .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get all available service versions for a specific parent version").addTagsItem(GENERAL_TAG))
                        .jsonArray("200", ServiceVersionType[].class)
                        .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                (Handler) context -> context.json(server.getDatabase().getServiceVersionTypes(context.pathParam("parent")))
        ));

        javalin.post("/admin/api/:parent/serviceversions", context -> {
            ServiceVersionType version = context.bodyAsClass(ServiceVersionType.class);
            this.validateServiceVersionType(version);

            version.setParentVersionName(context.pathParam("parent"));

            if (server.getParentVersion(version.getParentVersionName()).isEmpty()) {
                throw new BadRequestResponse("That parent version doesn't exist");
            }

            if (server.getDatabase().containsServiceVersionType(version.getParentVersionName(), version.getName())) {
                throw new BadRequestResponse("That version type already exists");
            }

            server.getDatabase().insertServiceVersionType(version);
        }, Set.of(WebPermissionRole.DEVELOPER));

        javalin.patch("/admin/api/:parent/serviceversions", context -> {
            ServiceVersionType version = context.bodyAsClass(ServiceVersionType.class);
            this.validateServiceVersionType(version);

            version.setParentVersionName(context.pathParam("parent"));

            if (server.getParentVersion(version.getParentVersionName()).isEmpty()) {
                throw new BadRequestResponse("That parent version doesn't exist");
            }

            if (!server.getDatabase().containsServiceVersionType(version.getParentVersionName(), version.getName())) {
                throw new BadRequestResponse("That version type already exists");
            }

            server.getDatabase().updateServiceVersionType(version);
        }, Set.of(WebPermissionRole.DEVELOPER));
    }

    private void validateServiceVersionType(ServiceVersionType version) {
        if (version == null || version.getName() == null || version.getVersions() == null ||
                version.getInstallerType() == null || version.getTargetEnvironment() == null) {
            throw new BadRequestResponse("Invalid version type provided");
        }

        int i = 0;
        for (ServiceVersion serviceVersion : version.getVersions()) {
            if (serviceVersion.getName() == null || serviceVersion.getUrl() == null) {
                throw new BadRequestResponse("Invalid version at index " + i + " provided");
            }
            if (serviceVersion.getProperties() == null) {
                serviceVersion.setProperties(JsonDocument.newDocument());
            }
            ++i;
        }
    }

}
