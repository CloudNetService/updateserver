package eu.cloudnetservice.cloudnet.repository.loader;

import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersionFile;
import eu.cloudnetservice.cloudnet.repository.version.VersionFileMappings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public interface CloudNetVersionFileLoader {

    // Loads all available version files (cloudnet.jar, driver.jar, cloudnet.cnl, driver.cnl and all modules) from the source (e.g. Jenkins)
    CloudNetVersionFile[] loadLastVersionFiles(CloudNetParentVersion parentVersion, VersionFileMappings versionFileMappings) throws IOException, CloudNetVersionLoadException;


    default boolean isCloudNetModule(CloudNetParentVersion parentVersion, Path path) {
        try (var inputStream = Files.newInputStream(path);
             var zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {

                if (entry.getName().equals(parentVersion.getModuleFileName())) {
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
