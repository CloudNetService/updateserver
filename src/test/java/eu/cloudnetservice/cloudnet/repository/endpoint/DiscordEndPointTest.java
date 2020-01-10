package eu.cloudnetservice.cloudnet.repository.endpoint;

import eu.cloudnetservice.cloudnet.repository.endpoint.discord.DiscordMessageSplitter;
import org.junit.Test;

import java.util.Collection;
import java.util.Random;

import static org.junit.Assert.*;

public class DiscordEndPointTest {

    private int ranInt(Random random, int min, int max) {
        return random.nextInt(max - min) + min;
    }

    private String buildTestMessage(int length) {
        Random random = new Random();
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            messageBuilder.append((char) this.ranInt(random, 33, 126));
            if (i % 100 == 0) {
                messageBuilder.append('\n');
            } else if (i % 10 == 0) {
                messageBuilder.append(' ');
            }
        }
        return messageBuilder.toString();
    }

    @Test
    public void testDiscordMessageSplitter() {
        String message = this.buildTestMessage(10001);
        Collection<String> splitMessages = DiscordMessageSplitter.splitMessage(message);

        assertEquals(6, splitMessages.size());

        assertEquals(message, String.join("", splitMessages));
    }

}
