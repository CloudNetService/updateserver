package eu.cloudnetservice.cloudnet.repository.web;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MimeTypes {

    private static final Map<String, String> TYPES = new HashMap<>();

    static {
        registerMany("text/html", "htm", "html");
        registerMany("image/jpeg", "jpg", "jpeg");
        register("application/javascript", "js");
        register("text/css", "css");
        register("application/zip", "zip");
        register("application/java-archive", "jar");

    }

    public static void register(String typeValue, String fileSuffix) {
        TYPES.put(fileSuffix, typeValue);
    }

    public static void registerMany(String typeValue, String... fileSuffixes) {
        for (String fileSuffix : fileSuffixes) {
            register(typeValue, fileSuffix);
        }
    }

    public static String getTypeFromPath(Path path) {
        String name = path.getFileName().toString();
        int pointIndex = name.indexOf('.');
        if (pointIndex == -1) {
            return "text/plain";
        }
        return TYPES.getOrDefault(name.substring(pointIndex + 1), "text/plain");
    }

    public static Map<String, String> getTypes() {
        return TYPES;
    }
}
