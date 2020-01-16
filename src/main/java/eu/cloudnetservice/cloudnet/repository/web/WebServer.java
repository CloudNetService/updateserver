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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.*;

public class WebServer {

    private static final String SUCCESS_JSON = JsonDocument.newDocument("success", true).toPrettyJson();

    private Javalin javalin;
    private boolean apiAvailable = System.getProperty("cloudnet.repository.api.enabled", "true").equalsIgnoreCase("true");
    private CloudNetUpdateServer server;

    public WebServer(Javalin javalin, CloudNetUpdateServer server) {
        this.javalin = javalin;
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
        JavalinJson.setToJsonMapper(JsonDocument.GSON::toJson);
        JavalinJson.setFromJsonMapper(JsonDocument.GSON::fromJson);

        this.javalin.config.requestCacheSize = 16384L;

        this.javalin.config.addStaticFiles("/web");

        this.javalin.config.accessManager((handler, ctx, permittedRoles) -> {
            /*if (!ctx.path().startsWith("/admin") || permittedRoles.isEmpty()) {
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
            ctx.header("User-Role-ID", String.valueOf(role.ordinal()));*/

            handler.handle(ctx);
        });

        // sometimes we can't use the Context#json methods to set the response because they don't accept null input

        this.javalin.get("/api", context -> context.result("{\"available\":" + this.apiAvailable + "}"));

        this.javalin.before("/api/*", context -> {
            if (!this.apiAvailable && !context.path().equalsIgnoreCase("/api/")) {
                throw new InternalServerErrorResponse("API currently not available");
            }
        });
        this.javalin.get("/api/parentVersions", context -> context.result(JsonDocument.GSON.toJson(this.server.getParentVersionNames())));
        this.javalin.get("/api/versions", context -> context.result(JsonDocument.GSON.toJson(Arrays.stream(this.server.getDatabase().getAllVersions()).map(CloudNetVersion::getName).collect(Collectors.toList()))));
        this.javalin.get("/api/versions/:parent", context -> context.result(JsonDocument.GSON.toJson(Arrays.stream(this.server.getDatabase().getAllVersions(context.pathParam("parent"))).map(CloudNetVersion::getName).collect(Collectors.toList()))));
        this.javalin.get("/api/versions/:parent/:version", context -> context.result(JsonDocument.GSON.toJson(this.server.getDatabase().getVersion(
                context.pathParam("parent"),
                this.getVersionOrLatest(context.pathParam("parent"), context.pathParam("version"))
        ))));

        this.javalin.get("/api/languages", context -> context.result(
                JsonDocument.newDocument()
                        .append("availableLanguages", Arrays.asList("english", "german"))
                        .toPrettyJson()
        ));

        for (CloudNetParentVersion parentVersion : this.server.getConfiguration().getParentVersions()) {
            this.javalin.post(parentVersion.getGitHubWebHookPath(), new GitHubWebHookReleaseEventHandler(this.server, parentVersion));

            this.javalin.get("/versions/" + parentVersion.getName() + "/:version/*", new ArchivedVersionHandler(Constants.VERSIONS_DIRECTORY.resolve(parentVersion.getName()), parentVersion, "CloudNet.zip", this.server));
            this.javalin.get("/docs/" + parentVersion.getName() + "/:version/*", new ArchivedVersionHandler(Constants.DOCS_DIRECTORY.resolve(parentVersion.getName()), parentVersion, "index.html", this.server));

            this.initFAQAPI(parentVersion);
            this.initModuleAPI(parentVersion);
        }

        this.javalin.get("/api/modules/list", context -> context.json(this.server.getModuleRepositoryProvider().getModuleInfos()));
        this.javalin.exception(ModuleInstallException.class, (exception, context) -> context.status(400).contentType("application/json").result(JsonDocument.newDocument()
                .append("message", exception.getMessage())
                .append("status", 400)
                .toPrettyJson()
        ));

        this.javalin.get("/admin/api", context -> context.result("{}"), Set.of(WebPermissionRole.MEMBER));

        this.javalin.start(this.server.getConfiguration().getWebPort());
    }

    private void initFAQAPI(CloudNetParentVersion parentVersion) {
        this.javalin.routes(() -> {
            path("/api/faq/" + parentVersion.getName(), () -> {
                get(context -> context.json(this.server.getDatabase().getFAQEntries(parentVersion.getName())));
                get("/:language", context -> context.json(
                        Arrays.stream(this.server.getDatabase().getFAQEntries(parentVersion.getName()))
                                .filter(entry -> entry.getLanguage().equalsIgnoreCase(context.pathParam("language")))
                                .toArray()
                ));
            });

            path("/admin/api/faq/" + parentVersion.getName(), () -> {
                //todo fix "Ã¼" -> "ö"
                post(context -> this.addFAQEntry(parentVersion, context), Set.of(WebPermissionRole.MODERATOR));
                patch(context -> this.updateFAQEntry(parentVersion, context), Set.of(WebPermissionRole.MODERATOR));
                delete(context -> this.deleteFAQEntry(parentVersion, context), Set.of(WebPermissionRole.MODERATOR));
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
        this.server.getDatabase().insertFAQEntry(new FAQEntry(
                uniqueId,
                language,
                parentVersion.getName(),
                System.currentTimeMillis(),
                question,
                answer,
                context.basicAuthCredentials().getUsername(),
                new HashMap<>()
        ));

        System.out.println("FAQ entry inserted by " + context.basicAuthCredentials().getUsername());
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
        entry.setQuestion(question);
        entry.setAnswer(answer);
        this.server.getDatabase().updateFAQEntry(entry);

        System.out.println("FAQEntry updated by " + context.basicAuthCredentials().getUsername());
    }

    private void deleteFAQEntry(CloudNetParentVersion parentVersion, Context context) {
        UUID uniqueId = UUID.fromString(Objects.requireNonNull(context.header("X-Entry-ID")));

        FAQEntry entry = this.server.getDatabase().getFAQEntry(uniqueId);
        if (entry == null) {
            throw new BadRequestResponse("FAQ entry with that ID not found");
        }

        this.server.getDatabase().deleteFAQEntry(entry.getUniqueId());

        System.out.println("FAQEntry deleted by " + context.basicAuthCredentials().getUsername());
    }

    private void initModuleAPI(CloudNetParentVersion parentVersion) {
        this.javalin.routes(() -> {
            path("/api/" + parentVersion.getName() + "/modules", () -> {
                get("/list", context -> context.json(this.server.getModuleRepositoryProvider().getModuleInfos(parentVersion.getName())));
                get("/list/:group",
                        context -> context.json(this.server.getModuleRepositoryProvider().getModuleInfos(parentVersion.getName(), context.pathParam("group")))
                );
                get("/list/:group/:name",
                        context -> context.json(this.server.getModuleRepositoryProvider().getModuleInfos(parentVersion.getName(), context.pathParam("group"), context.pathParam("name")))
                );
                get("/file/:group/:name",
                        context -> context.contentType("application/zip").result(this.server.getModuleRepositoryProvider().openLatestModuleStream(
                                parentVersion.getName(),
                                new ModuleId(context.pathParam("group"), context.pathParam("name"))
                        ))
                );
                get("/file/:group/:name/:version",
                        context -> context.contentType("application/zip").result(this.server.getModuleRepositoryProvider().openModuleStream(
                                parentVersion.getName(),
                                new ModuleId(context.pathParam("group"), context.pathParam("name"), context.pathParam("version"))
                        ))
                );
            });

            path("/admin/api/" + parentVersion.getName() + "/modules", () -> {
                path("/:group/:name/:version", () -> {
                    post(context -> this.addVersion(parentVersion, context));
                    patch(context -> this.updateVersion(parentVersion, context));
                });
            });
        });


    }

    private void addVersion(CloudNetParentVersion parentVersion, Context context) throws IOException {
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
                this.server.getCurrentLatestVersion(parentVersion.getName()).getName(),
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
