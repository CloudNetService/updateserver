package eu.cloudnetservice.cloudnet.repository;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants {

    private Constants() {
        throw new UnsupportedOperationException();
    }

    public static final Path TEMP_DIRECTORY = Paths.get("temp");
    public static final Path DOCS_DIRECTORY = Paths.get("archive", "docs");
    public static final Path VERSIONS_DIRECTORY = Paths.get("archive", "versions");

}
