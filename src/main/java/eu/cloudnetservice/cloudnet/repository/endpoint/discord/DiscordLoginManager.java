package eu.cloudnetservice.cloudnet.repository.endpoint.discord;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.endpoint.discord.command.DiscordPermissionState;
import eu.cloudnetservice.cloudnet.repository.web.WebPermissionRole;
import io.javalin.http.ForbiddenResponse;
import net.dv8tion.jda.api.entities.Member;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class DiscordLoginManager {

    private static final String INFO_URI = "http://discordapp.com/api/users/@me";

    private final HttpClient httpClient;
    private DiscordEndPoint endPoint;

    public DiscordLoginManager(DiscordEndPoint endPoint) {
        this.endPoint = endPoint;
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    }

    public WebPermissionRole getRole(String authToken) throws IOException, InterruptedException {
        HttpResponse<String> response = this.httpClient.send(
                HttpRequest.newBuilder()
                        .GET()
                        .uri(URI.create(INFO_URI))
                        .header("Authorization", "Bearer " + authToken)
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() != 200) {
            throw new ForbiddenResponse("Error from Discord: " + response.statusCode());
        }

        JsonDocument document = JsonDocument.newDocument(response.body());

        long id = document.getLong("id");

        Member member = this.endPoint.getGuild().getMemberById(id);
        if (member == null) {
            throw new ForbiddenResponse("Not on CloudNet guild");
        }

        return this.endPoint.getPermissionStateRoles().entrySet().stream()
                .filter(entry -> member.getRoles().stream().anyMatch(role -> role.getId().equals(entry.getValue())))
                .findFirst()
                .map(Map.Entry::getKey)
                .map(DiscordPermissionState::toWeb)
                .orElse(WebPermissionRole.MEMBER);
    }

}
