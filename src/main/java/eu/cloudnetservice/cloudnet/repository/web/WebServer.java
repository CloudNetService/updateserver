package eu.cloudnetservice.cloudnet.repository.web;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.Constants;
import eu.cloudnetservice.cloudnet.repository.faq.FAQEntry;
import eu.cloudnetservice.cloudnet.repository.module.ModuleId;
import eu.cloudnetservice.cloudnet.repository.module.ModuleInstallException;
import eu.cloudnetservice.cloudnet.repository.module.RepositoryModuleInfo;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import eu.cloudnetservice.cloudnet.repository.web.handler.ArchivedVersionHandler;
import eu.cloudnetservice.cloudnet.repository.web.handler.GitHubWebHookReleaseEventHandler;
import io.javalin.Javalin;
import io.javalin.http.*;
import io.javalin.plugin.json.JavalinJson;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.OpenApiPlugin;
import io.javalin.plugin.openapi.dsl.OpenApiBuilder;
import io.javalin.plugin.openapi.dsl.OpenApiUpdater;
import io.javalin.plugin.openapi.ui.SwaggerOptions;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.plugin.openapi.dsl.OpenApiBuilder.document;
import static io.javalin.plugin.openapi.dsl.OpenApiBuilder.documented;

// todo some documentation entries have no 200 response
public class WebServer {

    private static final String SUCCESS_JSON = JsonDocument.newDocument("success", true).toPrettyJson();

    private Javalin javalin;
    private boolean apiAvailable = System.getProperty("cloudnet.repository.api.enabled", "true").equalsIgnoreCase("true");
    private CloudNetUpdateServer server;

    public WebServer(CloudNetUpdateServer server) {
        this.server = server;
    }

    public boolean isApiAvailable() {
        return this.apiAvailable;
    }

    public void setApiAvailable(boolean apiAvailable) {
        this.apiAvailable = apiAvailable;
    }

    public Javalin getJavalin() {
        return this.javalin;
    }

    public void init() {
        this.javalin = Javalin.create(config -> {
            config.enableWebjars();

            OpenApiOptions options = new OpenApiOptions(() -> new OpenAPI()
                    .info(new Info()
                            .version("1.0")
                            .description("CloudNet 2/3 UpdateServer API")
                            .title("CloudNet Update"))
                    .addServersItem(new Server().url("https://update.cloudnetservice.eu").description("CloudNetService")))
                    .path("/api/swagger-docs")
                    .ignorePath("/docs/*")
                    .ignorePath("/versions/*")
                    .ignorePath("/admin/*")
                    .activateAnnotationScanningFor(this.getClass().getPackageName())
                    .swagger(new SwaggerOptions("/api/docs").title("CloudNet Updates"));

            for (CloudNetParentVersion parentVersion : this.server.getParentVersions()) {
                options.ignorePath(parentVersion.getGitHubWebHookPath());
            }

            config.registerPlugin(new OpenApiPlugin(options));

            config.enableCorsForAllOrigins();
            config.requestCacheSize = 16384L;
        });

        JavalinJson.setToJsonMapper(JsonDocument.GSON::toJson);
        JavalinJson.setFromJsonMapper(JsonDocument.GSON::fromJson);

        this.javalin.config.accessManager((handler, ctx, permittedRoles) -> {
            if (!ctx.path().startsWith("/admin") || permittedRoles.isEmpty()) {
                handler.handle(ctx);
                return;
            }

            String username;
            String password;
            try {
                username = ctx.basicAuthCredentials().getUsername();
                password = ctx.basicAuthCredentials().getPassword();
            } catch (IllegalArgumentException exception) {
                ctx.header("WWW-Authenticate", "Basic realm=\"Administration\"");
                throw new UnauthorizedResponse(exception.getMessage());
            }

            if (!this.server.getDatabase().checkUserPassword(username, password)) {
                throw new ForbiddenResponse("Invalid credentials");
            }
            WebPermissionRole role = this.server.getDatabase().getRole(username);
            if (permittedRoles.stream().noneMatch(permittedRole -> ((WebPermissionRole) permittedRole).canInteract(role))) {
                throw new ForbiddenResponse("Not enough permissions");
            }

            ctx.header("User-Role", role.name());
            ctx.header("User-Role-ID", String.valueOf(role.ordinal()));

            handler.handle(ctx);
        });

        // sometimes we can't use the Context#json methods to set the response because they don't accept null input

        this.javalin.get("/api", documented(
                document()
                        .operation(operation -> {
                            operation
                                    .summary("Check if the API is available")
                                    .operationId("apiAvailable");
                        })
                        .json("200", APIAvailableResponse.class),
                (Handler) context -> context.json(new APIAvailableResponse(this.apiAvailable))
        ));

        this.javalin.before("/api/*", context -> {
            if (!this.apiAvailable && !context.path().equalsIgnoreCase("/api/")) {
                throw new InternalServerErrorResponse("API currently not available");
            }
        });
        this.javalin.get("/api/parentVersions", documented(
                document()
                        .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get the names of all parent versions"))
                        .jsonArray("200", String.class)
                        .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                (Handler) context -> context.result(JsonDocument.GSON.toJson(this.server.getParentVersionNames()))
        ));
        this.javalin.get("/api/versions", documented(
                document()
                        .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get the names of all versions"))
                        .jsonArray("200", String.class)
                        .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                (Handler) context -> context.result(JsonDocument.GSON.toJson(Arrays.stream(this.server.getDatabase().getAllVersions()).map(CloudNetVersion::getName).collect(Collectors.toList())))
        ));
        this.javalin.get("/api/versions/:parent", documented(
                document()
                        .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get the names of all versions available for a specific parent version"))
                        .pathParam("parent", String.class, parameter -> parameter.description("The name of the parent version"))
                        .jsonArray("200", CloudNetVersion.class)
                        .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Parent version not found"))
                        .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                context -> {
                    if (this.server.getParentVersion(context.pathParam("parent")).isEmpty()) {
                        context.status(404);
                        return;
                    }
                    context.result(JsonDocument.GSON.toJson(Arrays.stream(this.server.getDatabase().getAllVersions(context.pathParam("parent"))).map(CloudNetVersion::getName).collect(Collectors.toList())));
                }
        ));
        this.javalin.get("/api/versions/:parent/:version", documented(
                document()
                        .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get all available information for a specific version"))
                        .pathParam("parent", String.class, parameter -> parameter.description("The name of the parent version"))
                        .pathParam("version", String.class, parameter -> parameter.description("The name of the version"))
                        .json("200", CloudNetVersion.class)
                        .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Version not found"))
                        .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                context -> {
                    CloudNetVersion version = this.server.getDatabase().getVersion(
                            context.pathParam("parent"),
                            this.getVersionOrLatest(context.pathParam("parent"), context.pathParam("version"))
                    );
                    context.status(version != null ? 200 : 404).result(JsonDocument.GSON.toJson(version));
                }
        ));

        this.javalin.get("/api/languages", documented(
                document()
                        .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get all available languages"))
                        .jsonArray("200", String.class)
                        .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                (Handler) context -> context.json(Arrays.asList("english", "german")) //todo config
        ));

        for (CloudNetParentVersion parentVersion : this.server.getConfiguration().getParentVersions()) {
            this.javalin.post(parentVersion.getGitHubWebHookPath(), new GitHubWebHookReleaseEventHandler(this.server, parentVersion));

            this.javalin.get("/versions/" + parentVersion.getName() + "/:version/*", new ArchivedVersionHandler(Constants.VERSIONS_DIRECTORY.resolve(parentVersion.getName()), parentVersion, "CloudNet.zip", this.server));
            this.javalin.get("/docs/" + parentVersion.getName() + "/:version/*", new ArchivedVersionHandler(Constants.DOCS_DIRECTORY.resolve(parentVersion.getName()), parentVersion, "index.html", this.server));

        }

        this.initFAQAPI();
        this.initModuleAPI();

        this.javalin.get("/api/modules/list", documented(
                document()
                        .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get a list of all modules"))
                        .jsonArray("200", RepositoryModuleInfo.class)
                        .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                (Handler) context -> context.json(this.server.getModuleRepositoryProvider().getModuleInfos())
        ));
        this.javalin.exception(ModuleInstallException.class, (exception, context) -> context.status(400).contentType("application/json").result(JsonDocument.newDocument()
                .append("message", exception.getMessage())
                .append("status", 400)
                .toPrettyJson()
        ));

        this.javalin.get("/admin/api", context -> context.result("{}"), Set.of(WebPermissionRole.MEMBER));

        this.javalin.start(this.server.getConfiguration().getWebPort());
    }

    private void initFAQAPI() {
        this.javalin.routes(() -> {
            path("/api/:parent/faq", () -> {
                before(context -> {
                    if (this.server.getParentVersion(context.pathParam("parent")).isEmpty()) {
                        throw new NotFoundResponse();
                    }
                });
                get(documented(
                        document()
                                .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get a list of all available faq entries for the specific parent version"))
                                .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Parent version not found"))
                                .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                        (Handler) context -> context.json(this.server.getDatabase().getFAQEntries(context.pathParam("parent")))
                ));
                get("/:language", documented(
                        document()
                                .operation((OpenApiUpdater<Operation>) operation -> operation
                                        .summary("Get a list of all available faq entries for the specific parent version and language")
                                )
                                .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Parent version not found"))
                                .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                        (Handler) context -> context.json(Arrays.stream(this.server.getDatabase().getFAQEntries(context.pathParam("parent")))
                                .filter(entry -> entry.getLanguage().equalsIgnoreCase(context.pathParam("language")))
                                .toArray(FAQEntry[]::new)
                        )
                ));
            });

            path("/admin/api/faq/:parent", () -> {
                before(context -> {
                    if (this.server.getParentVersion(context.pathParam("parent")).isEmpty()) {
                        throw new NotFoundResponse();
                    }
                });
                post(context -> this.addFAQEntry(this.server.getParentVersion(context.pathParam("parent")).orElseThrow(NotFoundResponse::new), context), Set.of(WebPermissionRole.MODERATOR));
                patch(context -> this.updateFAQEntry(this.server.getParentVersion(context.pathParam("parent")).orElseThrow(NotFoundResponse::new), context), Set.of(WebPermissionRole.MODERATOR));
                delete(context -> this.deleteFAQEntry(this.server.getParentVersion(context.pathParam("parent")).orElseThrow(NotFoundResponse::new), context), Set.of(WebPermissionRole.MODERATOR));
            });
        });
    }

    private void addFAQEntry(CloudNetParentVersion parentVersion, Context context) {
        UUID uniqueId = UUID.randomUUID();
        String question = context.header("X-Question");
        String answer = context.header("X-Answer");
        String language = context.header("X-Language");
        if (language == null || language.isEmpty()) {
            language = "english";
        }

        if (this.server.getDatabase().getFAQEntry(uniqueId) != null) {
            throw new BadRequestResponse("An FAQ entry with that id already exists");
        }
        if (question == null || answer == null) {
            throw new BadRequestResponse("Missing question or answer header");
        }

        FAQEntry entry = new FAQEntry(
                uniqueId,
                language,
                parentVersion.getName(),
                System.currentTimeMillis(),
                question,
                answer,
                context.basicAuthCredentials().getUsername(),
                new HashMap<>()
        );
        this.server.getDatabase().insertFAQEntry(entry);

        context.json(entry);

        System.out.println("FAQ entry " + uniqueId + " inserted by " + context.basicAuthCredentials().getUsername() + ": ");
        System.out.println(" - Question: " + question);
        System.out.println(" - Answer: " + answer);
    }

    private void updateFAQEntry(CloudNetParentVersion parentVersion, Context context) {
        if (context.header("X-Entry-ID") == null) {
            throw new BadRequestResponse("Missing X-Entry-ID header");
        }
        UUID uniqueId = UUID.fromString(Objects.requireNonNull(context.header("X-Entry-ID")));
        String question = context.header("X-Question");
        String answer = context.header("X-Answer");

        FAQEntry entry = this.server.getDatabase().getFAQEntry(uniqueId);
        if (entry == null) {
            throw new BadRequestResponse("FAQ entry with that ID not found");
        }
        if (question != null) {
            entry.setQuestion(question);
        }
        if (answer != null) {
            entry.setAnswer(answer);
        }
        this.server.getDatabase().updateFAQEntry(entry);

        System.out.println("FAQEntry " + uniqueId + " updated by " + context.basicAuthCredentials().getUsername());
    }

    private void deleteFAQEntry(CloudNetParentVersion parentVersion, Context context) {
        if (context.header("X-Entry-ID") == null) {
            throw new BadRequestResponse("Missing X-Entry-ID header");
        }
        UUID uniqueId = UUID.fromString(Objects.requireNonNull(context.header("X-Entry-ID")));

        FAQEntry entry = this.server.getDatabase().getFAQEntry(uniqueId);
        if (entry == null) {
            throw new BadRequestResponse("FAQ entry with that ID not found");
        }

        this.server.getDatabase().deleteFAQEntry(entry.getUniqueId());

        System.out.println("FAQEntry " + uniqueId + " deleted by " + context.basicAuthCredentials().getUsername());
    }

    private void initModuleAPI() {
        this.javalin.routes(() -> {
            path("/api/:parent/modules", () -> {
                before(context -> {
                    if (this.server.getParentVersion(context.pathParam("parent")).isEmpty()) {
                        throw new NotFoundResponse();
                    }
                });
                get("/list", documented(
                        document()
                                .operation((OpenApiUpdater<Operation>) operation -> operation.summary("List all available modules for a specific parent version"))
                                .jsonArray("200", RepositoryModuleInfo.class)
                                .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Parent version not found"))
                                .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                        (Handler) context -> context.json(this.server.getModuleRepositoryProvider().getModuleInfos(context.pathParam("parent")))
                ));
                get("/list/:group", documented(
                        document()
                                .operation((OpenApiUpdater<Operation>) operation -> operation.summary("List all available modules from the given group for a specific parent version"))
                                .jsonArray("200", RepositoryModuleInfo.class)
                                .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Parent version or group and name combination not found"))
                                .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                        context -> {
                            Collection<RepositoryModuleInfo> moduleInfos = this.server.getModuleRepositoryProvider().getModuleInfos(context.pathParam("parent"), context.pathParam("group"));
                            if (moduleInfos.isEmpty()) {
                                context.status(404);
                            }
                            context.json(moduleInfos);
                        }
                ));
                get("/latest/:group/:name", documented(
                        document()
                                .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get the latest module from the given group with the given name for a specific parent version"))
                                .json("200", RepositoryModuleInfo.class)
                                .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Parent version or group and name combination not found"))
                                .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                        context -> {
                            RepositoryModuleInfo moduleInfo = this.server.getModuleRepositoryProvider().getModuleInfoIgnoreVersion(
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
                                .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Download the Jar of a module"))
                                .result("200", null, "application/zip")
                                .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Parent version or group and name combination not found"))
                                .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                        context -> {
                            InputStream inputStream = this.server.getModuleRepositoryProvider().openLatestModuleStream(
                                    context.pathParam("parent"),
                                    new ModuleId(context.pathParam("group"), context.pathParam("name"))
                            );
                            if (inputStream == null) {
                                context.status(404);
                            } else {
                                context.contentType("application/zip").result(inputStream);
                            }
                        }
                ));
            });

            path("/admin/api/:parent/modules", () -> {
                path("/:group/:name", () -> {
                    path(":version", () -> {
                        post(context -> this.addVersion(this.server.getParentVersion(context.pathParam("parent")).orElseThrow(NotFoundResponse::new), context));
                        patch(context -> this.updateVersion(this.server.getParentVersion(context.pathParam("parent")).orElseThrow(NotFoundResponse::new), context));
                    });
                    delete(context -> {
                        String parentVersionName = context.pathParam(":parent");
                        ModuleId moduleId = new ModuleId(context.pathParam("group"), context.pathParam("name"));
                        if (this.server.getModuleRepositoryProvider().getModuleInfoIgnoreVersion(parentVersionName, moduleId) == null) {
                            context.status(404);
                            return;
                        }
                        this.server.getModuleRepositoryProvider().removeModule(parentVersionName, moduleId);
                    });
                });
            });
        });


    }

    private void addVersion(CloudNetParentVersion parentVersion, Context context) throws IOException {
        if (this.server.getCurrentLatestVersion(parentVersion.getName()) == null) {
            throw new BadRequestResponse("No versions have been released for this parent");
        }

        Map<String, String> headers = context.headerMap();
        String[] authors = headers.getOrDefault("X-Authors", "Unknown").split(";");
        ModuleId[] depends = Arrays.stream(headers.getOrDefault("X-Depends", "").split(";"))
                .map(ModuleId::parse)
                .filter(Objects::nonNull)
                .toArray(ModuleId[]::new);
        ModuleId[] conflicts = Arrays.stream(headers.getOrDefault("X-Conflicts", "").split(";"))
                .map(ModuleId::parse)
                .filter(Objects::nonNull)
                .toArray(ModuleId[]::new);
        ModuleId moduleId = new ModuleId(context.pathParam("group"), context.pathParam("name"), context.pathParam("version"));
        String description = headers.computeIfAbsent("X-Description", s -> {
            throw new BadRequestResponse("description is required");
        });
        String website = headers.get("X-Website");
        String sourceUrl = headers.computeIfAbsent("X-SourceURL", s -> {
            throw new BadRequestResponse("SourceCode is required");
        });
        String supportUrl = headers.get("X-SupportURL");

        RepositoryModuleInfo moduleInfo = new RepositoryModuleInfo(
                moduleId,
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
        this.server.getModuleRepositoryProvider().addModule(moduleInfo, new ByteArrayInputStream(context.bodyAsBytes()));

        System.out.println("Module added by " + context.basicAuthCredentials().getUsername());
        context.result(SUCCESS_JSON);
    }

    private void updateVersion(CloudNetParentVersion parentVersion, Context context) throws IOException {
        ModuleId moduleId = new ModuleId(context.pathParam("group"), context.pathParam("name"), context.pathParam("version"));
        RepositoryModuleInfo oldModuleInfo = this.server.getModuleRepositoryProvider().getModuleInfoIgnoreVersion(parentVersion.getName(), moduleId);

        if (oldModuleInfo == null) {
            throw new BadRequestResponse("Version not found");
        }

        Map<String, String> headers = context.headerMap();
        String[] authors = headers.containsKey("X-Authors") ? headers.get("X-Authors").split(";") : oldModuleInfo.getAuthors();
        ModuleId[] depends = headers.containsKey("X-Depends") ? Arrays.stream(headers.getOrDefault("X-Depends", "").split(";"))
                .map(ModuleId::parse)
                .filter(Objects::nonNull)
                .toArray(ModuleId[]::new) : oldModuleInfo.getDepends();
        ModuleId[] conflicts = headers.containsKey("X-Conflicts") ? Arrays.stream(headers.getOrDefault("X-Conflicts", "").split(";"))
                .map(ModuleId::parse)
                .filter(Objects::nonNull)
                .toArray(ModuleId[]::new) : oldModuleInfo.getConflicts();
        String description = headers.getOrDefault("X-Description", oldModuleInfo.getDescription());
        String website = headers.getOrDefault("X-Website", oldModuleInfo.getWebsite());
        String sourceUrl = headers.getOrDefault("X-SourceURL", oldModuleInfo.getSourceUrl());
        String supportUrl = headers.getOrDefault("X-SupportURL", oldModuleInfo.getSupportUrl());

        RepositoryModuleInfo moduleInfo = new RepositoryModuleInfo(
                moduleId,
                authors,
                depends,
                conflicts,
                parentVersion.getName(),
                this.server.getCurrentLatestVersion(parentVersion.getName()).getName(),
                description,
                website,
                sourceUrl,
                supportUrl
        );

        if (oldModuleInfo.getModuleId().getVersion().equals(moduleId.getVersion())) {
            this.server.getModuleRepositoryProvider().updateModule(moduleInfo);

            System.out.println("Module updated by " + context.basicAuthCredentials().getUsername());
            context.result(SUCCESS_JSON);
            return;
        }

        this.server.getModuleRepositoryProvider().updateModuleWithFile(moduleInfo, new ByteArrayInputStream(context.bodyAsBytes()));

        System.out.println("Module updated by " + context.basicAuthCredentials().getUsername());
        context.result(SUCCESS_JSON);
    }

    private String getVersionOrLatest(String parentVersionName, String version) {
        if (version.equalsIgnoreCase("latest")) {
            CloudNetVersion latestVersion = this.server.getCurrentLatestVersion(parentVersionName);
            return latestVersion != null ? latestVersion.getName() : "";
        }
        return version;
    }

    public void stop() {
        if (this.javalin != null) {
            this.javalin.stop();
        }
    }

}
