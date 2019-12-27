package eu.cloudnetservice.cloudnet.repository.publisher.discord.command;

import eu.cloudnetservice.cloudnet.repository.publisher.discord.DiscordUpdatePublisher;
import net.dv8tion.jda.api.entities.Member;

import java.util.HashMap;
import java.util.Map;

public enum DiscordPermissionState {
    OPERATOR, DEVELOPER, MODERATOR, EVERYONE;

    public static Map<DiscordPermissionState, String> getDefaultMappings() {
        Map<DiscordPermissionState, String> map = new HashMap<>();
        map.put(OPERATOR, "Operator");
        map.put(DEVELOPER, "Developer");
        map.put(MODERATOR, "Moderator");
        map.put(EVERYONE, "Member");
        return map;
    }

    public static DiscordPermissionState getState(DiscordUpdatePublisher updatePublisher, Member member) {
        for (DiscordPermissionState value : values()) {
            String roleId = updatePublisher.getPermissionStateRoles().get(value);
            if (roleId != null && member.getRoles().stream().anyMatch(role -> role.getId().equals(roleId))) {
                return value;
            }
        }
        return EVERYONE;
    }

    public boolean canInteract(DiscordUpdatePublisher updatePublisher, Member member) {
        return getState(updatePublisher, member).ordinal() <= this.ordinal();
    }
}
