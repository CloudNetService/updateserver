package eu.cloudnetservice.cloudnet.repository.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public interface CloudNetVersionFileLoader {

    // Loads all available version files (cloudnet.jar, driver.jar, cloudnet.cnl, driver.cnl and all modules) from the source (e.g. Jenkins)
    CloudNetVersionFile[] loadLastVersionFiles() throws IOException, CloudNetVersionLoadException;


    default boolean isCloudNetModule(Path path) {
        try (var inputStream = Files.newInputStream(path);
             var zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {

                if (entry.getName().equals("module.json")) {
                    zipInputStream.closeEntry();
                    return true;
                }

                zipInputStream.closeEntry();
            }
        } catch (IOException ignored) { //most likely the given file is no jar/zip file and therefore cannot be a module
            return false;
        }

        return false;
    }

}
