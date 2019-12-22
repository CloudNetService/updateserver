package eu.cloudnetservice.cloudnet.repository.publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class DiscordMessageSplitter {

    public static Collection<String> splitMessage(String message) {
        if (message.length() <= 2000) {
            return Arrays.asList(message);
        }

        Collection<String> targetMessages = new ArrayList<>();

        while (message.length() > 2000) {
            int currentMessageLength = Math.min(2000, message.length());

            int bestSplitter = message.lastIndexOf('\n', currentMessageLength);
            if (bestSplitter == -1) {
                bestSplitter = message.lastIndexOf(' ', currentMessageLength);
                if (bestSplitter == -1) {
                    bestSplitter = currentMessageLength;
                }
            }

            targetMessages.add(message.substring(0, bestSplitter));
            message = message.substring(bestSplitter);
        }

        if (!message.isEmpty()) {
            targetMessages.add(message);
        }

        return targetMessages;
    }

}
