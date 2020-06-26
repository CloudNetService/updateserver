package eu.cloudnetservice.cloudnet.repository.web.registry;

import com.google.gson.JsonObject;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.faq.FAQEntry;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;
import eu.cloudnetservice.cloudnet.repository.web.WebPermissionRole;
import eu.cloudnetservice.cloudnet.repository.web.WebServer;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.openapi.dsl.OpenApiUpdater;
import io.swagger.v3.oas.models.Operation;

import java.util.*;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.plugin.openapi.dsl.OpenApiBuilder.document;
import static io.javalin.plugin.openapi.dsl.OpenApiBuilder.documented;

public class FaqHandlerRegistry implements JavalinHandlerRegistry {

    public static final String FAQ_TAG = "FAQ";

    @Override
    public void init(CloudNetUpdateServer server, WebServer webServer, Javalin javalin) {
        javalin.routes(() -> {
            path("/api/:parent/faq", () -> {
                before(context -> {
                    if (server.getParentVersion(context.pathParam("parent")).isEmpty()) {
                        throw new NotFoundResponse();
                    }
                });
                get(documented(
                        document()
                                .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Get a list of all available faq entries for the specific parent version").addTagsItem(FAQ_TAG))
                                .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Parent version not found"))
                                .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                        (Handler) context -> context.json(Arrays.stream(server.getDatabase().getFAQEntries(context.pathParam("parent"))).map(this::serializeFAQEntryNonAuthorization).toArray(JsonObject[]::new))
                ));
                get("/:language", documented(
                        document()
                                .operation((OpenApiUpdater<Operation>) operation -> operation
                                        .summary("Get a list of all available faq entries for the specific parent version and language")
                                        .addTagsItem(FAQ_TAG)
                                )
                                .result("404", (Class<?>) null, apiResponse -> apiResponse.description("Parent version not found"))
                                .result("500", (Class<?>) null, apiResponse -> apiResponse.description("API not available")),
                        (Handler) context -> context.json(Arrays.stream(server.getDatabase().getFAQEntries(context.pathParam("parent")))
                                .filter(entry -> entry.getLanguage().equalsIgnoreCase(context.pathParam("language")))
                                .map(this::serializeFAQEntryNonAuthorization)
                                .toArray(JsonObject[]::new)
                        )
                ));
            });

            path("/admin/api/faq/:parent", () -> {
                before(context -> {
                    if (server.getParentVersion(context.pathParam("parent")).isEmpty()) {
                        throw new NotFoundResponse();
                    }
                });
                post(context -> this.addFAQEntry(server, context), Set.of(WebPermissionRole.MODERATOR));
                patch(context -> this.updateFAQEntry(server, context), Set.of(WebPermissionRole.MODERATOR));
                delete(context -> this.deleteFAQEntry(server, context), Set.of(WebPermissionRole.MODERATOR));
            });
        });
    }

    private JsonObject serializeFAQEntryNonAuthorization(FAQEntry entry) {
        JsonObject object = JsonDocument.GSON.toJsonTree(entry).getAsJsonObject();
        object.remove("creator");
        return object;
    }

    private void addFAQEntry(CloudNetUpdateServer server, Context context) {
        CloudNetParentVersion parentVersion = server.getParentVersion(context.pathParam("parent")).orElseThrow(NotFoundResponse::new);

        UUID uniqueId = UUID.randomUUID();
        String question = context.header("X-Question");
        String answer = context.header("X-Answer");
        String language = context.header("X-Language");
        if (language == null || language.isEmpty()) {
            language = "english";
        }

        if (server.getDatabase().getFAQEntry(uniqueId) != null) {
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
                context.sessionAttribute("Username"),
                new HashMap<>()
        );
        server.getDatabase().insertFAQEntry(entry);

        context.json(entry);

        System.out.println("FAQ entry " + uniqueId + " inserted by " + context.sessionAttribute("Username") + ": ");
        System.out.println(" - Question: " + question);
        System.out.println(" - Answer: " + answer);
    }

    private void updateFAQEntry(CloudNetUpdateServer server, Context context) {
        CloudNetParentVersion parentVersion = server.getParentVersion(context.pathParam("parent")).orElseThrow(NotFoundResponse::new);
        
        if (context.header("X-Entry-ID") == null) {
            throw new BadRequestResponse("Missing X-Entry-ID header");
        }
        UUID uniqueId = UUID.fromString(Objects.requireNonNull(context.header("X-Entry-ID")));
        String question = context.header("X-Question");
        String answer = context.header("X-Answer");

        FAQEntry entry = server.getDatabase().getFAQEntry(uniqueId);
        if (entry == null) {
            throw new BadRequestResponse("FAQ entry with that ID not found");
        }
        if (question != null) {
            entry.setQuestion(question);
        }
        if (answer != null) {
            entry.setAnswer(answer);
        }
        server.getDatabase().updateFAQEntry(entry);

        System.out.println("FAQEntry " + uniqueId + " updated by " + context.sessionAttribute("Username"));
    }

    private void deleteFAQEntry(CloudNetUpdateServer server, Context context) {
        CloudNetParentVersion parentVersion = server.getParentVersion(context.pathParam("parent")).orElseThrow(NotFoundResponse::new);

        if (context.header("X-Entry-ID") == null) {
            throw new BadRequestResponse("Missing X-Entry-ID header");
        }
        UUID uniqueId = UUID.fromString(Objects.requireNonNull(context.header("X-Entry-ID")));

        FAQEntry entry = server.getDatabase().getFAQEntry(uniqueId);
        if (entry == null) {
            throw new BadRequestResponse("FAQ entry with that ID not found");
        }

        server.getDatabase().deleteFAQEntry(entry.getUniqueId());

        System.out.println("FAQEntry " + uniqueId + " deleted by " + context.sessionAttribute("Username"));
    }
    
}
