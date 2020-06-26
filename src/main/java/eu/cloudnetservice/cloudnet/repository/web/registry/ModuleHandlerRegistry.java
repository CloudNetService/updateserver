package eu.cloudnetservice.cloudnet.repository.web.registry;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.module.ModuleId;
import eu.cloudnetservice.cloudnet.repository.module.ModuleInstallException;
import eu.cloudnetservice.cloudnet.repository.module.RepositoryModuleInfo;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;
import eu.cloudnetservice.cloudnet.repository.web.WebPermissionRole;
import eu.cloudnetservice.cloudnet.repository.web.WebServer;
import io.javalin.Javalin;
import io.javalin.http.*;
import io.javalin.plugin.openapi.dsl.OpenApiUpdater;
import io.swagger.v3.oas.models.Operation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.plugin.openapi.dsl.OpenApiBuilder.document;
import static io.javalin.plugin.openapi.dsl.OpenApiBuilder.documented;

public class ModuleHandlerRegistry implements JavalinHandlerRegistry {

    public static final String MODULES_TAG = "Modules";
    private static final String SUCCESS_JSON = JsonDocument.newDocument("success", true).toPrettyJson();

    @Override
    public void init(CloudNetUpdateServer server, WebServer webServer, Javalin javalin) {
        javalin.routes(() -> {
            path("/api/:parent/modules", () -> {
                before(context -> {
                    if (server.getParentVersion(context.pathParam("parent")).isEmpty()) {
                        throw new NotFoundResponse();
                    }
                });
                get("/list", documented(
                        document()
                                .operation((OpenApiUpdater<Operation>) operation -> operation.summary("List all available modules for a specific parent version").addTagsItem(MODULES_TAG))
                                .jsonArray("200", RepositoryModuleInfo.class)
                                .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Parent version not found"))
                                .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                        (Handler) context -> context.json(server.getModuleRepositoryProvider().getModuleInfos(context.pathParam("parent")))
                ));
                get("/list/:group", documented(
                        document()
                                .operation((OpenApiUpdater<Operation>) operation -> operation.summary("List all available modules from the given group for a specific parent version").addTagsItem(MODULES_TAG))
                                .jsonArray("200", RepositoryModuleInfo.class)
                                .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Parent version or group and name combination not found"))
                                .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                        context -> {
                            Collection<RepositoryModuleInfo> moduleInfos = server.getModuleRepositoryProvider().getModuleInfos(context.pathParam("parent"), context.pathParam("group"));
                            if (moduleInfos.isEmpty()) {
                                context.status(404);
                            }
                            context.json(moduleInfos);
                        }
                ));
                get("/latest/:group/:name", documented(
                        document()
                                .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get the latest module from the given group with the given name for a specific parent version").addTagsItem(MODULES_TAG))
                                .json("200", RepositoryModuleInfo.class)
                                .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Parent version or group and name combination not found"))
                                .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                        context -> {
                            RepositoryModuleInfo moduleInfo = server.getModuleRepositoryProvider().getModuleInfoIgnoreVersion(
                                    context.pathParam("parent"),
                                    new ModuleId(context.pathParam("group"), context.pathParam("name"))
                            );
                            if (moduleInfo == null) {
                                context.status(404);
                            }
                            context.json(moduleInfo);
                        }
                ));
                get("/file/:group/:name", documented(
                        document()
                                .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Download the Jar of a module").addTagsItem(MODULES_TAG))
                                .result("200", null, "application/zip")
                                .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Parent version or group and name combination not found"))
                                .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                        context -> {
                            InputStream inputStream = server.getModuleRepositoryProvider().openLatestModuleStream(
                                    context.pathParam("parent"),
                                    new ModuleId(context.pathParam("group"), context.pathParam("name"))
                            );
                            if (inputStream == null) {
                                context.status(404);
                            } else {
                                context
                                        .contentType("application/zip")
                                        .header("Content-Disposition", "attachment; filename=" + context.pathParam("name") + ".jar")
                                        .result(inputStream);
                            }
                        }
                ));
            });

            path("/admin/api/modules/:parent", () -> {
                post("/create", context -> this.addVersion(server, context), Set.of(WebPermissionRole.MODERATOR));
                path("/:group/:name", () -> {
                    post("/modify", context -> this.updateVersion(server, context), Set.of(WebPermissionRole.MODERATOR));
                    delete("/delete", context -> {
                        String parentVersionName = context.pathParam("parent");
                        ModuleId moduleId = new ModuleId(context.pathParam("group"), context.pathParam("name"));
                        if (server.getModuleRepositoryProvider().getModuleInfoIgnoreVersion(parentVersionName, moduleId) == null) {
                            context.status(404);
                            return;
                        }
                        System.out.println("Module " + moduleId.getGroup() + ":" + moduleId.getName() + ":" + moduleId.getVersion() + " deleted by " + context.sessionAttribute("Username"));
                        server.getModuleRepositoryProvider().removeModule(parentVersionName, moduleId);
                    }, Set.of(WebPermissionRole.MODERATOR));
                });
            });
        });

        javalin.get("/api/modules/list", documented(
                document()
                        .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get a list of all modules").addTagsItem(MODULES_TAG))
                        .jsonArray("200", RepositoryModuleInfo.class)
                        .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                (Handler) context -> context.json(server.getModuleRepositoryProvider().getModuleInfos())
        ));
        javalin.exception(ModuleInstallException.class, (exception, context) -> context.status(400).contentType("application/json").result(JsonDocument.newDocument()
                .append("message", exception.getMessage())
                .append("status", 400)
                .toPrettyJson()
        ));
    }

    private void addVersion(CloudNetUpdateServer server, Context context) throws IOException {
        CloudNetParentVersion parentVersion = server.getParentVersion(context.pathParam("parent")).orElseThrow(NotFoundResponse::new);

        if (server.getCurrentLatestVersion(parentVersion.getName()) == null) {
            throw new BadRequestResponse("No versions have been released for this parent");
        }

        String[] authors = this.formParamOrElse(context, "authors", "Unknown").split(";");
        ModuleId[] depends = Arrays.stream(this.formParamOrElse(context, "depends", "").split(";"))
                .map(ModuleId::parse)
                .filter(Objects::nonNull)
                .toArray(ModuleId[]::new);
        ModuleId[] conflicts = Arrays.stream(this.formParamOrElse(context, "conflicts", "").split(";"))
                .map(ModuleId::parse)
                .filter(Objects::nonNull)
                .toArray(ModuleId[]::new);
        String description = this.formParamOrThrow(context, "description", () -> new BadRequestResponse("description is required"));
        String website = context.formParam("website");
        String sourceUrl = this.formParamOrThrow(context, "sourceURL", () -> new BadRequestResponse("SourceCode is required"));
        String supportUrl = context.formParam("supportURL");
        UploadedFile file = context.uploadedFile("modulefile");
        if (file == null) {
            throw new BadRequestResponse("No file uploaded");
        }
        InputStream inputStream = file.getContent();

        RepositoryModuleInfo moduleInfo = new RepositoryModuleInfo(
                null,
                authors,
                depends,
                conflicts,
                parentVersion.getName(),
                null,
                description,
                website,
                sourceUrl,
                supportUrl
        );
        server.getModuleRepositoryProvider().addModule(moduleInfo, inputStream);

        System.out.println("Module " + moduleInfo.getModuleId() + " added by " + context.sessionAttribute("Username"));
        context.result(SUCCESS_JSON);
    }

    private void updateVersion(CloudNetUpdateServer server, Context context) throws IOException {
        CloudNetParentVersion parentVersion = server.getParentVersion(context.pathParam("parent")).orElseThrow(NotFoundResponse::new);

        ModuleId moduleId = new ModuleId(context.pathParam("group"), context.pathParam("name"));
        RepositoryModuleInfo oldModuleInfo = server.getModuleRepositoryProvider().getModuleInfoIgnoreVersion(parentVersion.getName(), moduleId);

        if (oldModuleInfo == null) {
            throw new BadRequestResponse("Version not found");
        }

        String[] authors = context.formParam("authors") != null ? context.formParam("authors").split(";") : oldModuleInfo.getAuthors();
        ModuleId[] depends = context.formParam("depends") != null ? Arrays.stream(this.formParamOrElse(context, "depends", "").split(";"))
                .map(ModuleId::parse)
                .filter(Objects::nonNull)
                .toArray(ModuleId[]::new) : oldModuleInfo.getDepends();
        ModuleId[] conflicts = context.formParam("conflicts") != null ? Arrays.stream(this.formParamOrElse(context, "X-Conflicts", "").split(";"))
                .map(ModuleId::parse)
                .filter(Objects::nonNull)
                .toArray(ModuleId[]::new) : oldModuleInfo.getConflicts();
        String description = this.formParamOrElse(context, "description", oldModuleInfo.getDescription());
        String website = this.formParamOrElse(context, "website", oldModuleInfo.getWebsite());
        String sourceUrl = this.formParamOrElse(context, "sourceURL", oldModuleInfo.getSourceUrl());
        String supportUrl = this.formParamOrElse(context, "supportURL", oldModuleInfo.getSupportUrl());

        RepositoryModuleInfo moduleInfo = new RepositoryModuleInfo(
                oldModuleInfo.getModuleId(),
                authors,
                depends,
                conflicts,
                parentVersion.getName(),
                server.getCurrentLatestVersion(parentVersion.getName()).getName(),
                description,
                website,
                sourceUrl,
                supportUrl
        );

        UploadedFile file = context.uploadedFile("modulefile");

        if (file == null) {
            server.getModuleRepositoryProvider().updateModule(moduleInfo);

            System.out.println("Module " + moduleId + " updated by " + context.sessionAttribute("Username"));
            context.result(SUCCESS_JSON);
            return;
        }

        server.getModuleRepositoryProvider().updateModuleWithFile(moduleInfo, file.getContent());

        System.out.println("Module " + moduleId + " and file updated by " + context.sessionAttribute("Username"));
        context.result(SUCCESS_JSON);
    }

    private String formParamOrElse(Context context, String key, String def) {
        String result = context.formParam(key);
        return result != null ? result : def;
    }

    private String formParamOrThrow(Context context, String key, Supplier<RuntimeException> supplier) {
        String result = context.formParam(key);
        if (result == null) {
            throw supplier.get();
        }
        return result;
    }

}
