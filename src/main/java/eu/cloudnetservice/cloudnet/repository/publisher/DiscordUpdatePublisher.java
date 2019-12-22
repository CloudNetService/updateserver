package eu.cloudnetservice.cloudnet.repository.publisher;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.security.auth.login.LoginException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class DiscordUpdatePublisher implements UpdatePublisher {

    private static final String PROPERTIES_KEY_MESSAGES = "DiscordMessageIDs";

    private boolean enabled = false;

    private JDA jda;

    private String token;
    private long updatesChannelId;
    private TextChannel updatesChannel;

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
        this.updatesChannelId = configDocument.getLong("updatesChannelId", 0L);

        if (!exists) {
            configDocument.write(configPath);
        }
    }

    @Override
    public boolean init(Path configPath) {
        this.readConfig(configPath);

        if (this.token == null || this.token.isEmpty() || this.updatesChannelId <= 0) {
            return false;
        }
        try {
            this.jda = new JDABuilder(this.token)
                    .setAutoReconnect(true)
                    .build()
                    .awaitReady();

            this.updatesChannel = this.jda.getTextChannelById(this.updatesChannelId);
            if (this.updatesChannel == null) {
                System.err.println("Discord Updates Channel not found!");
                this.jda.shutdown();
                return false;
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
        }
    }

    @Override
    public void publishRelease(CloudNetVersion version) {
        Collection<String> messages = DiscordMessageSplitter.splitMessage(version.getRelease().getBody());

        Collection<String> messageIds = new ArrayList<>();
        ITask<Void> task = new ListenableTask<>(() -> {
            version.getProperties().put(PROPERTIES_KEY_MESSAGES, messageIds.toArray(String[]::new));
            return null;
        });

        Iterator<String> messageIterator = messages.iterator();
        this.sendMessages(this.updatesChannel, message -> messageIds.add(message.getId()), () -> {
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
    public void updateRelease(CloudNetVersion version) {
        if (!version.getProperties().containsKey(PROPERTIES_KEY_MESSAGES)) {
            return;
        }
        String[] messageIds = ((Collection<String>) version.getProperties().get(PROPERTIES_KEY_MESSAGES)).toArray(String[]::new);
        Collection<String> newMessages = DiscordMessageSplitter.splitMessage(version.getRelease().getBody());
        if (newMessages.size() > messageIds.length) {
            System.err.println("Cannot update description because we cannot add more messages than we have!");
            return;
        }
        String[] messages = newMessages.toArray(String[]::new);
        for (int i = 0; i < messageIds.length; i++) {
            String messageId = messageIds[i];
            if (messages.length > i) {
                String message = messages[i];
                this.updatesChannel.editMessageById(messageId, message).queue();
            } else {
                this.updatesChannel.deleteMessageById(messageId).queue();
            }
        }
    }

    @Override
    public void deleteRelease(CloudNetVersion version) {
        if (!version.getProperties().containsKey(PROPERTIES_KEY_MESSAGES)) {
            return;
        }
        Collection<String> messageIds = ((Collection<String>) version.getProperties().get(PROPERTIES_KEY_MESSAGES));
        if (messageIds.size() > 1) {
            this.updatesChannel.deleteMessagesByIds(messageIds).queue();
        } else {
            for (String messageId : messageIds) {
                this.updatesChannel.deleteMessageById(messageId).queue();
            }
        }
        version.getProperties().remove(PROPERTIES_KEY_MESSAGES);
    }

}
