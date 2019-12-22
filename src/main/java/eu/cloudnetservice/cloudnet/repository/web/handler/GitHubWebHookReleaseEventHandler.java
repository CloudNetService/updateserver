package eu.cloudnetservice.cloudnet.repository.web.handler;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.github.webhook.GitHubReleaseAction;
import eu.cloudnetservice.cloudnet.repository.publisher.UpdatePublisher;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

public class GitHubWebHookReleaseEventHandler implements Handler {

    private CloudNetUpdateServer updateServer;

    public GitHubWebHookReleaseEventHandler(CloudNetUpdateServer updateServer) {
        this.updateServer = updateServer;
    }

    @Override
    public void handle(@NotNull Context context) throws Exception {
        var event = context.header("X-GitHub-Event");

        if (event != null && event.equals("release")) {
            var document = JsonDocument.newDocument(context.bodyAsBytes());

            var action = document.toInstanceOf(GitHubReleaseAction.class);
            var release = action.getRelease();

            switch (action.getAction()) {
                case "edited": {
                    var version = this.updateServer.getDatabase().getVersion(release);
                    if (version == null) {
                        throw new BadRequestResponse("No version for this release found");
                    }
                    for (UpdatePublisher publisher : this.updateServer.getUpdatePublishers()) {
                        publisher.updateRelease(version);
                    }
                }
                break;

                case "published": {
                    this.updateServer.installLatestRelease(release);
                }
                break;

                case "deleted": {
                    var version = this.updateServer.getDatabase().getVersion(release);
                    if (version == null) {
                        throw new BadRequestResponse("No version for this release found!");
                    }
                    for (UpdatePublisher publisher : this.updateServer.getUpdatePublishers()) {
                        publisher.deleteRelease(version);
                    }
                }
                break;
            }
        }
    }
}
