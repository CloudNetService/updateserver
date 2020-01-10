package eu.cloudnetservice.cloudnet.repository.endpoint.discord.command;

import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.endpoint.discord.DiscordEndPoint;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.Arrays;

public abstract class DiscordCommand {

    private String[] names;
    private DiscordPermissionState[] allowedRoles;

    private CloudNetUpdateServer updateServer;
    private DiscordEndPoint endPoint;
    private DiscordCommandMap commandMap;

    public DiscordCommand(String[] names, DiscordPermissionState[] allowedRoles) {
        this.names = names;
        this.allowedRoles = allowedRoles;
    }

    public String[] getNames() {
        return this.names;
    }

    public DiscordPermissionState[] getAllowedRoles() {
        return this.allowedRoles;
    }

    public DiscordEndPoint getEndPoint() {
        return this.endPoint;
    }

    public CloudNetUpdateServer getServer() {
        return this.updateServer;
    }

    public DiscordCommandMap getCommandMap() {
        return this.commandMap;
    }

    public void setUpdateServer(CloudNetUpdateServer updateServer) {
        this.updateServer = updateServer;
    }

    public void setEndPoint(DiscordEndPoint endPoint) {
        this.endPoint = endPoint;
    }

    public void setCommandMap(DiscordCommandMap commandMap) {
        this.commandMap = commandMap;
    }

    public boolean canExecute(Member member) {
        if (this.allowedRoles.length == 0) {
            return true;
        }
        return Arrays.stream(this.allowedRoles).anyMatch(state -> state.canInteract(this.endPoint, member));
    }

    public abstract void execute(Member sender, MessageChannel channel, Message message, String label, String[] args);

}
