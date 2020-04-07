package eu.cloudnetservice.cloudnet.repository.database.statistics.internal;

import java.util.Optional;

public enum ServerVersion {

    V1_7,
    V1_8,
    V1_9,
    V1_10,
    V1_11,
    V1_12,
    V1_13,
    V1_14,
    V1_15;

    public static Optional<ServerVersion> parseServerVersion(String raw) {
        String[] split = raw.split("\\.");
        if (split.length < 2) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(String.format("V%s_%s", split[0], split[1])));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

}
