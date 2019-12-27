package eu.cloudnetservice.cloudnet.repository.publisher.discord.command;

import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.publisher.discord.DiscordUpdatePublisher;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.Arrays;

public abstract class DiscordCommand {

    private CloudNetUpdateServer updateServer;
    private String[] names;
    private DiscordPermissionState[] allowedRoles;

    private DiscordUpdatePublisher updatePublisher;

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

    public DiscordUpdatePublisher getUpdatePublisher() {
        return this.updatePublisher;
    }

    public CloudNetUpdateServer getServer() {
        return this.updateServer;
    }

    public void setUpdateServer(CloudNetUpdateServer updateServer) {
        this.updateServer = updateServer;
    }

    public void setUpdatePublisher(DiscordUpdatePublisher updatePublisher) {
        this.updatePublisher = updatePublisher;
    }

    public boolean canExecute(Member member) {
        if (this.allowedRoles.length == 0) {
            return true;
        }
        return Arrays.stream(this.allowedRoles).anyMatch(state -> state.canInteract(this.updatePublisher, member));
    }

    public abstract void execute(Member sender, MessageChannel channel, Message message, String[] args);

}
