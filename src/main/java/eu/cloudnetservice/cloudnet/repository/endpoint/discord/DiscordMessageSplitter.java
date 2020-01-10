package eu.cloudnetservice.cloudnet.repository.endpoint.discord;

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
            int bestSplitter = findBestSplitter(message, Math.min(2000, message.length()));

            targetMessages.add(message.substring(0, bestSplitter));
            message = message.substring(bestSplitter);
        }

        if (!message.isEmpty()) {
            targetMessages.add(message);
        }

        return targetMessages;
    }

    private static int findBestSplitter(String message, int maxLength) {
        int bestSplitter = message.lastIndexOf(' ', maxLength);
        if (bestSplitter == -1) {
            bestSplitter = message.lastIndexOf('\n', maxLength);
            if (bestSplitter == -1) {
                bestSplitter = maxLength;
            }
        }

        return bestSplitter;
    }

}
