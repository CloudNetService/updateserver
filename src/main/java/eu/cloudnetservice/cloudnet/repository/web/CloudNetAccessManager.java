package eu.cloudnetservice.cloudnet.repository.web;

import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.endpoint.discord.DiscordEndPoint;
import eu.cloudnetservice.cloudnet.repository.endpoint.discord.DiscordLoginManager;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.Role;
import io.javalin.http.*;
import io.javalin.http.util.RateLimit;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CloudNetAccessManager implements AccessManager {

    private final CloudNetUpdateServer server;

    public CloudNetAccessManager(CloudNetUpdateServer server) {
        this.server = server;
    }

    @Override
    public void manage(@NotNull Handler handler, @NotNull Context ctx, @NotNull Set<Role> permittedRoles) throws Exception {
        if (!ctx.path().startsWith("/admin")) {
            handler.handle(ctx);
            return;
        }
        if (permittedRoles.isEmpty()) {
            throw new InternalServerErrorResponse("No roles for an admin action defined");
        }

        String authorization = ctx.header("Authorization");
        if (authorization == null) {
            ctx.header("WWW-Authenticate", "Basic realm=\"Administration\"");
            throw new UnauthorizedResponse();
        }

        new RateLimit(ctx).requestPerTimeUnit(15, TimeUnit.MINUTES);

        String[] authParts = authorization.split(" ");
        if (authParts.length != 2) {
            throw new ForbiddenResponse("Wrong authorization header");
        }

        WebPermissionRole role;

        if (authParts[0].equalsIgnoreCase("Basic")) {
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
            role = this.server.getDatabase().getRole(username);
            ctx.sessionAttribute("Username", "BASIC:" + username);
        } else if (authParts[0].equalsIgnoreCase("Bearer")) {

            String token = authParts[1];

            DiscordLoginManager loginManager = this.server.getEndPoint(DiscordEndPoint.class)
                    .orElseThrow(() -> new ForbiddenResponse("Discord not enabled"))
                    .getLoginManager();

            role = loginManager.getRole(ctx, token);
        } else {
            throw new ForbiddenResponse("Unsupported authorization");
        }

        if (permittedRoles.stream().noneMatch(permittedRole -> ((WebPermissionRole) permittedRole).canInteract(role))) {
            throw new ForbiddenResponse("Not enough permissions");
        }

        ctx.header("User-Role", role.name());
        ctx.header("User-Role-ID", String.valueOf(role.ordinal()));

        handler.handle(ctx);
    }
}
