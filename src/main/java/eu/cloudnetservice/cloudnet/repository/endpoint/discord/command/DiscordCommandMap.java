package eu.cloudnetservice.cloudnet.repository.endpoint.discord.command;

import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.endpoint.discord.DiscordEndPoint;
import eu.cloudnetservice.cloudnet.repository.util.StringUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DiscordCommandMap {

    private String commandPrefix;
    private Map<String, DiscordCommand> commandMap = new HashMap<>();
    private DiscordEndPoint endPoint;
    private CloudNetUpdateServer updateServer;

    public DiscordCommandMap(DiscordEndPoint endPoint, CloudNetUpdateServer updateServer) {
        this.endPoint = endPoint;
        this.updateServer = updateServer;
    }

    public void init(JDA jda) {
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
                dispatchCommand(event.getMessage());
            }
        });
    }

    public void setCommandPrefix(String commandPrefix) {
        this.commandPrefix = commandPrefix;
    }

    public String getCommandPrefix() {
        return this.commandPrefix;
    }

    public void registerCommand(DiscordCommand command) {
        for (String name : command.getNames()) {
            this.commandMap.put(name.toLowerCase(), command);
        }

        command.setEndPoint(this.endPoint);
        command.setUpdateServer(this.updateServer);
        command.setCommandMap(this);
    }

    public DiscordCommand getCommand(String line) {
        line = line.toLowerCase();
        if (!line.startsWith(this.commandPrefix)) {
            return null;
        }
        return this.commandMap.get(line.substring(this.commandPrefix.length()).split(" ")[0]);
    }

    public boolean dispatchCommand(Message message) {
        String line = message.getContentRaw();
        if (!StringUtils.startsWithIgnoreCase(line, this.commandPrefix)) {
            return false;
        }
        line = line.substring(this.commandPrefix.length());
        String[] args = line.split(" ");
        DiscordCommand command = this.commandMap.get(args[0].toLowerCase());
        if (command == null) {
            return false;
        }
        if (!command.canExecute(message.getMember())) {
            return false;
        }
        command.execute(message.getMember(), message.getChannel(), message, args[0], Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

}
