package eu.cloudnetservice.cloudnet.repository.web.handler;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.github.webhook.GitHubReleaseAction;
import eu.cloudnetservice.cloudnet.repository.github.webhook.GitHubWebHookAuthenticator;
import eu.cloudnetservice.cloudnet.repository.endpoint.EndPoint;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.annotations.OpenApi;
import org.apache.commons.codec.DecoderException;
import org.jetbrains.annotations.NotNull;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class GitHubWebHookReleaseEventHandler implements Handler {

    private CloudNetUpdateServer updateServer;
    private CloudNetParentVersion parentVersion;

    public GitHubWebHookReleaseEventHandler(CloudNetUpdateServer updateServer, CloudNetParentVersion parentVersion) {
        this.updateServer = updateServer;
        this.parentVersion = parentVersion;
    }

    @OpenApi(ignore = true)
    @Override
    public void handle(@NotNull Context context) throws Exception {
        var hubSignature = context.header("X-Hub-Signature");
        var body = context.bodyAsBytes();

        try {
            if (!GitHubWebHookAuthenticator.validateSignature(hubSignature, this.parentVersion.getGitHubSecret(), body)) {
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
                    var version = this.updateServer.getDatabase().getVersion(this.parentVersion.getName(), release);
                    if (version == null) {
                        throw new BadRequestResponse("No version for this release found");
                    }
                    System.out.println("Updating version " + version.getName() + "...");
                    version.setRelease(release);
                    for (EndPoint endPoint : this.updateServer.getEndPoints()) {
                        endPoint.updateRelease(this.parentVersion, version);
                    }
                    this.updateServer.getDatabase().updateVersion(version);
                }
                break;

                case "published": {
                    System.out.println("Publishing github release " + release.getTagName() + "...");
                    this.updateServer.installLatestRelease(this.parentVersion, release);
                }
                break;

                case "deleted": {
                    var version = this.updateServer.getDatabase().getVersion(this.parentVersion.getName(), release);
                    if (version == null) {
                        throw new BadRequestResponse("No version for this release found!");
                    }
                    System.out.println("Deleting version " + version.getName() + "...");
                    for (EndPoint endPoint : this.updateServer.getEndPoints()) {
                        endPoint.deleteRelease(this.parentVersion, version);
                    }
                    this.updateServer.getDatabase().updateVersion(version);
                }
                break;
            }
        }
    }
}
