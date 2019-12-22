package eu.cloudnetservice.cloudnet.repository.web.handler;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.github.webhook.GitHubReleaseAction;
import eu.cloudnetservice.cloudnet.repository.github.webhook.GitHubWebHookAuthenticator;
import eu.cloudnetservice.cloudnet.repository.publisher.UpdatePublisher;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import org.apache.commons.codec.DecoderException;
import org.jetbrains.annotations.NotNull;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class GitHubWebHookReleaseEventHandler implements Handler {

    private String gitHubSecret;
    private CloudNetUpdateServer updateServer;

    public GitHubWebHookReleaseEventHandler(String gitHubSecret, CloudNetUpdateServer updateServer) {
        this.gitHubSecret = gitHubSecret;
        this.updateServer = updateServer;
    }

    @Override
    public void handle(@NotNull Context context) throws Exception {
        var hubSignature = context.header("X-Hub-Signature");
        var body = context.bodyAsBytes();

        try {
            if (!GitHubWebHookAuthenticator.validateSignature(hubSignature, this.gitHubSecret, body)) {
                throw new ForbiddenResponse();
            }
        } catch (DecoderException | IllegalArgumentException | InvalidKeyException | NoSuchAlgorithmException exception) {
            throw new BadRequestResponse(exception.getMessage());
        }

        var event = context.header("X-GitHub-Event");
        if (event == null) {
            throw new BadRequestResponse("Missing X-GitHub-Event header");
        }

        if (event.equals("release")) {
            var document = JsonDocument.newDocument(body);

            var action = document.toInstanceOf(GitHubReleaseAction.class);
            var release = action.getRelease();

            switch (action.getAction()) {
                case "edited": {
                    var version = this.updateServer.getDatabase().getVersion(release);
                    if (version == null) {
                        throw new BadRequestResponse("No version for this release found");
                    }
                    System.out.println("Updating version " + version.getName() + "...");
                    version.setRelease(release);
                    for (UpdatePublisher publisher : this.updateServer.getUpdatePublishers()) {
                        publisher.updateRelease(version);
                    }
                    this.updateServer.getDatabase().updateVersion(version);
                }
                break;

                case "published": {
                    System.out.println("Publishing github release " + release.getTagName() + "...");
                    this.updateServer.installLatestRelease(release);
                }
                break;

                case "deleted": {
                    var version = this.updateServer.getDatabase().getVersion(release);
                    if (version == null) {
                        throw new BadRequestResponse("No version for this release found!");
                    }
                    System.out.println("Deleting version " + version.getName() + "...");
                    for (UpdatePublisher publisher : this.updateServer.getUpdatePublishers()) {
                        publisher.deleteRelease(version);
                    }
                    this.updateServer.getDatabase().updateVersion(version);
                }
                break;
            }
        }
    }
}
