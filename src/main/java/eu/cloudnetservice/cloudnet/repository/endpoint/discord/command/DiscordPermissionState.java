package eu.cloudnetservice.cloudnet.repository.endpoint.discord.command;

import eu.cloudnetservice.cloudnet.repository.endpoint.discord.DiscordEndPoint;
import eu.cloudnetservice.cloudnet.repository.web.WebPermissionRole;
import net.dv8tion.jda.api.entities.Member;

import java.util.HashMap;
import java.util.Map;

public enum DiscordPermissionState {
    OPERATOR(WebPermissionRole.OPERATOR),
    DEVELOPER(WebPermissionRole.DEVELOPER),
    MODERATOR(WebPermissionRole.MODERATOR),
    EVERYONE(WebPermissionRole.MEMBER);

    private WebPermissionRole web;

    DiscordPermissionState(WebPermissionRole web) {
        this.web = web;
    }

    public WebPermissionRole toWeb() {
        return this.web;
    }

    public static Map<DiscordPermissionState, String> getDefaultMappings() {
        Map<DiscordPermissionState, String> map = new HashMap<>();
        map.put(OPERATOR, "OperatorID");
        map.put(DEVELOPER, "DeveloperID");
        map.put(MODERATOR, "ModeratorID");
        map.put(EVERYONE, "MemberID");
        return map;
    }

    public static DiscordPermissionState getState(DiscordEndPoint endPoint, Member member) {
        for (DiscordPermissionState value : values()) {
            String roleId = endPoint.getPermissionStateRoles().get(value);
            if (roleId != null && member.getRoles().stream().anyMatch(role -> role.getId().equals(roleId))) {
                return value;
            }
        }
        return EVERYONE;
    }

    public boolean canInteract(DiscordEndPoint endPoint, Member member) {
        return getState(endPoint, member).ordinal() <= this.ordinal();
    }
}
