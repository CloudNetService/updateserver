package eu.cloudnetservice.cloudnet.repository.web;

import io.javalin.core.security.Role;

public enum WebPermissionRole implements Role {
    OPERATOR, DEVELOPER, MODERATOR, MEMBER;

    public boolean canInteract(WebPermissionRole otherPermissionRole) {
        return otherPermissionRole.ordinal() <= this.ordinal();
    }
}
