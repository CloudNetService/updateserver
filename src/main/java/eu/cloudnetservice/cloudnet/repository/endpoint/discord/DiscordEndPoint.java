package eu.cloudnetservice.cloudnet.repository.endpoint.discord;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.endpoint.EndPoint;
import eu.cloudnetservice.cloudnet.repository.endpoint.discord.command.DiscordCommandMap;
import eu.cloudnetservice.cloudnet.repository.endpoint.discord.command.DiscordPermissionState;
import eu.cloudnetservice.cloudnet.repository.endpoint.discord.command.defaults.DiscordCommandDependency;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;

import javax.security.auth.login.LoginException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class DiscordEndPoint implements EndPoint {

    private static final String PROPERTIES_KEY_MESSAGES = "DiscordMessageIDs";
    private static final String PROPERTIES_KEY_UPDATES_CHANNEL_ID = "DiscordUpdatesChannelId";

    private boolean enabled = false;

    private JDA jda;
    private DiscordCommandMap commandMap;

    private String token;
    private String commandPrefix;
    private Map<DiscordPermissionState, String> permissionStateRoles;
    private Guild guild;
    private long guildId;
    private DiscordPresence[] presences;

    private DiscordLoginManager loginManager = new DiscordLoginManager(this);

    public JDA getJda() {
        return this.jda;
    }

    public DiscordLoginManager getLoginManager() {
        return loginManager;
    }

    public Guild getGuild() {
        return this.guild;
    }

    public Map<DiscordPermissionState, String> getPermissionStateRoles() {
        return this.permissionStateRoles;
    }

    public String getCommandPrefix() {
        return this.commandPrefix;
    }

    @Override
    public String getName() {
        return "Discord";
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private void readConfig(Path configPath) {
        boolean exists = Files.exists(configPath);
        JsonDocument configDocument = exists ? JsonDocument.newDocument(configPath) : JsonDocument.newDocument();
        this.token = configDocument.getString("token", "");
        this.guildId = configDocument.getLong("guildId", 1L);
        this.commandPrefix = configDocument.getString("commandPrefix", "!");
        this.permissionStateRoles = configDocument.get(
                "permissionStateRoles",
                TypeToken.getParameterized(Map.class, DiscordPermissionState.class, String.class).getType(),
                DiscordPermissionState.getDefaultMappings()
        );
        this.presences = configDocument.get("presences", DiscordPresence[].class, new DiscordPresence[]{
                new DiscordPresence(OnlineStatus.ONLINE, Activity.ActivityType.WATCHING, "Rop beim Programmieren zu", null, 30000),
                new DiscordPresence(OnlineStatus.ONLINE, Activity.ActivityType.WATCHING, "Julian beim Pommesessen zu", null, 30000)
        });

        if (!exists) {
            configDocument.write(configPath);
        }
    }

    @Override
    public boolean init(CloudNetUpdateServer updateServer, Path configPath) {
        this.readConfig(configPath);

        if (this.token == null || this.token.isEmpty()) {
            return false;
        }
        try {
            this.jda = new JDABuilder(this.token)
                    .setAutoReconnect(true)
                    .build()
                    .awaitReady();

            this.guild = this.jda.getGuildById(this.guildId);
            if (this.guild == null) {
                System.err.println("Guild not found");
                this.jda.shutdown();
                return false;
            }

            for (CloudNetParentVersion parentVersion : updateServer.getParentVersions()) {
                if (!parentVersion.getProperties().containsKey(PROPERTIES_KEY_UPDATES_CHANNEL_ID)) {
                    parentVersion.getProperties().put(PROPERTIES_KEY_UPDATES_CHANNEL_ID, "1");
                    updateServer.getConfiguration().save();
                }
            }

            for (CloudNetParentVersion parentVersion : updateServer.getParentVersions()) {
                TextChannel channel = this.jda.getTextChannelById(String.valueOf(parentVersion.getProperties().get(PROPERTIES_KEY_UPDATES_CHANNEL_ID)));
                if (channel == null) {
                    System.err.println("Discord Updates Channel for parent version " + parentVersion.getName() + " not found!");
                    this.jda.shutdown();
                    return false;
                }
            }

            this.commandMap = new DiscordCommandMap(this, updateServer);
            this.commandMap.init(this.jda);
            this.commandMap.setCommandPrefix(this.commandPrefix);

            this.commandMap.registerCommand(new DiscordCommandDependency(
                    "dependency/dependencyMaven",
                    "dependency/repositoryMaven",
                    "maven",
                    "mvn"
            ));
            this.commandMap.registerCommand(new DiscordCommandDependency(
                    "dependency/dependencyGradle",
                    "dependency/repositoryGradle",
                    "gradle"
            ));

            if (this.presences != null && this.presences.length != 0) {
                if (this.presences.length == 1) {
                    this.jda.getPresence().setStatus(this.presences[0].getStatus());
                    this.presences[0].asActivity().ifPresent(activity -> this.jda.getPresence().setActivity(activity));
                } else {
                    updateServer.getExecutorService().execute(() -> {
                        int presenceIndex = 0;

                        while (!Thread.interrupted() && this.jda != null) {
                            if (presenceIndex >= this.presences.length) {
                                presenceIndex = 0;
                            }
                            DiscordPresence presence = this.presences[presenceIndex++];
                            if (this.jda.getPresence().getStatus() != presence.getStatus()) {
                                this.jda.getPresence().setStatus(presence.getStatus());
                            }
                            presence.asActivity().ifPresent(activity -> this.jda.getPresence().setActivity(activity));

                            try {
                                Thread.sleep(Math.max(5000, presence.getVisibleTimeMillis()));
                            } catch (InterruptedException exception) {
                                exception.printStackTrace();
                            }
                        }
                    });
                }
            }

            return true;
        } catch (InterruptedException | LoginException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    @Override
    public void close() {
        if (this.jda != null) {
            this.jda.shutdownNow();
            this.jda = null;
        }
    }

    @Override
    public void publishRelease(CloudNetParentVersion parentVersion, CloudNetVersion version) {
        Collection<String> messages = DiscordMessageSplitter.splitMessage(version.getRelease().getBody());

        Collection<String> messageIds = new ArrayList<>();
        ITask<Void> task = new ListenableTask<>(() -> {
            version.getProperties().put(PROPERTIES_KEY_MESSAGES, messageIds.toArray(String[]::new));
            return null;
        });

        TextChannel updatesChannel = this.jda.getTextChannelById(String.valueOf(parentVersion.getProperties().get(PROPERTIES_KEY_UPDATES_CHANNEL_ID)));

        Iterator<String> messageIterator = messages.iterator();
        this.sendMessages(updatesChannel, message -> messageIds.add(message.getId()), () -> {
            try {
                task.call();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }, messageIterator);

        try {
            task.get();
        } catch (InterruptedException | ExecutionException exception) {
            exception.printStackTrace();
        }
    }

    private void sendMessages(MessageChannel channel, Consumer<Message> messageConsumer, Runnable completionHandler, Iterator<String> messageIterator) {
        if (messageIterator.hasNext()) {
            channel.sendMessage(messageIterator.next()).queue(message -> {
                messageConsumer.accept(message);
                this.sendMessages(channel, messageConsumer, completionHandler, messageIterator);
            });
        } else {
            completionHandler.run();
        }
    }

    @Override
    public void updateRelease(CloudNetParentVersion parentVersion, CloudNetVersion version) {
        if (!version.getProperties().containsKey(PROPERTIES_KEY_MESSAGES)) {
            return;
        }
        String[] messageIds = ((Collection<String>) version.getProperties().get(PROPERTIES_KEY_MESSAGES)).toArray(String[]::new);
        Collection<String> newMessages = DiscordMessageSplitter.splitMessage(version.getRelease().getBody());
        if (newMessages.size() > messageIds.length) {
            System.err.println("Cannot update description because we cannot add more messages than we have!");
            return;
        }

        TextChannel updatesChannel = this.jda.getTextChannelById(String.valueOf(parentVersion.getProperties().get(PROPERTIES_KEY_UPDATES_CHANNEL_ID)));

        String[] messages = newMessages.toArray(String[]::new);

        for (int i = 0; i < messageIds.length; i++) {
            String messageId = messageIds[i];
            if (messages.length > i) {
                String message = messages[i];
                updatesChannel.editMessageById(messageId, message).queue();
            } else {
                updatesChannel.deleteMessageById(messageId).queue();
            }
        }
    }

    @Override
    public void deleteRelease(CloudNetParentVersion parentVersion, CloudNetVersion version) {
        if (!version.getProperties().containsKey(PROPERTIES_KEY_MESSAGES)) {
            return;
        }

        TextChannel updatesChannel = this.jda.getTextChannelById(String.valueOf(parentVersion.getProperties().get(PROPERTIES_KEY_UPDATES_CHANNEL_ID)));

        Collection<String> messageIds = ((Collection<String>) version.getProperties().get(PROPERTIES_KEY_MESSAGES));
        if (messageIds.size() > 1) {
            updatesChannel.deleteMessagesByIds(messageIds).queue();
        } else {
            for (String messageId : messageIds) {
                updatesChannel.deleteMessageById(messageId).queue();
            }
        }
        version.getProperties().remove(PROPERTIES_KEY_MESSAGES);
    }

}
