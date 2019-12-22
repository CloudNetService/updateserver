package eu.cloudnetservice.cloudnet.repository.web.handler;

import eu.cloudnetservice.cloudnet.repository.github.webhook.GitHubWebHookAuthenticator;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import org.apache.commons.codec.DecoderException;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class GitHubWebHookAuthenticationHandler implements Handler {

    private String gitHubSecret;

    public GitHubWebHookAuthenticationHandler(String gitHubSecret) {
        this.gitHubSecret = gitHubSecret;
    }

    @Override
    public void handle(@NotNull Context context) throws Exception {
        String hubSignature = context.header("X-Hub-Signature");
        byte[] body = context.bodyAsBytes();

        try {
            if (!GitHubWebHookAuthenticator.validateSignature(hubSignature, this.gitHubSecret, body)) {
                throw new ForbiddenResponse();
            }
        } catch (DecoderException | IllegalArgumentException | InvalidKeyException | NoSuchAlgorithmException exception) {
            throw new BadRequestResponse(exception.getMessage());
        }

        String event = context.header("X-GitHub-Event");
        if (event == null) {
            throw new BadRequestResponse("Missing X-GitHub-Event header");
        }
    }
}
