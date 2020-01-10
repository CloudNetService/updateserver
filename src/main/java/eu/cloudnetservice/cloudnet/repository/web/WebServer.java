package eu.cloudnetservice.cloudnet.repository.web;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.Constants;
import eu.cloudnetservice.cloudnet.repository.faq.FAQEntry;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import eu.cloudnetservice.cloudnet.repository.web.handler.ArchivedVersionHandler;
import eu.cloudnetservice.cloudnet.repository.web.handler.GitHubWebHookReleaseEventHandler;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.plugin.json.JavalinJson;

import java.util.*;
import java.util.stream.Collectors;

public class WebServer {

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

            this.javalin.get("/api/faq/" + parentVersion.getName(), context -> context.json(this.server.getDatabase().getFAQEntries(parentVersion.getName())));
            this.javalin.get("/api/faq/" + parentVersion.getName() + "/:language", context -> context.json(
                    Arrays.stream(this.server.getDatabase().getFAQEntries(parentVersion.getName()))
                            .filter(entry -> entry.getLanguage().equalsIgnoreCase(context.pathParam("language")))
                            .toArray()
            ));

            this.javalin.get("/versions/" + parentVersion.getName() + "/:version/*", new ArchivedVersionHandler(Constants.VERSIONS_DIRECTORY, parentVersion, "CloudNet.zip", this.server));
            this.javalin.get("/docs/" + parentVersion.getName() + "/:version/*", new ArchivedVersionHandler(Constants.DOCS_DIRECTORY, parentVersion, "index.html", this.server));

            //todo fix "Ã¼" -> "ö"
            this.javalin.post("/admin/api/faq/" + parentVersion.getName(), context -> {
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

                System.out.println("FAQ entry inserted ");
            }, Set.of(WebPermissionRole.MODERATOR));
            this.javalin.patch("/admin/api/faq/" + parentVersion.getName(), context -> {
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
            }, Set.of(WebPermissionRole.MODERATOR));
            this.javalin.delete("/admin/api/faq/" + parentVersion.getName(), context -> {
                UUID uniqueId = UUID.fromString(Objects.requireNonNull(context.header("X-Entry-ID")));

                FAQEntry entry = this.server.getDatabase().getFAQEntry(uniqueId);
                if (entry == null) {
                    throw new BadRequestResponse("FAQ entry with that ID not found");
                }

                this.server.getDatabase().deleteFAQEntry(entry.getUniqueId());
            }, Set.of(WebPermissionRole.MODERATOR));
        }

        this.javalin.get("/admin/api", context -> context.result("{}"), Set.of(WebPermissionRole.MEMBER));

        this.javalin.start(this.server.getConfiguration().getWebPort());
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
