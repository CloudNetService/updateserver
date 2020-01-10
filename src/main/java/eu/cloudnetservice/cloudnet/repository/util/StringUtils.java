package eu.cloudnetservice.cloudnet.repository.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class StringUtils {

    private StringUtils() {
        throw new UnsupportedOperationException();
    }

    public static String EMPTY_HASH_STRING = hashString("");

    public static boolean startsWithIgnoreCase(String str, String prefix) {
        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static String readStringFromClassPath(ClassLoader classLoader, String classPath) {
        try (InputStream inputStream = Objects.requireNonNull(classLoader.getResourceAsStream(classPath))) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[128];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static String hashString(String input) {
        if ((input == null || input.isEmpty()) && EMPTY_HASH_STRING != null) {
            return EMPTY_HASH_STRING;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(input.getBytes(StandardCharsets.UTF_8));
            return new String(digest.digest(), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException exception) {
            exception.printStackTrace();
            return input;
        }
    }

}
