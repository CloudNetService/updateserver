package eu.cloudnetservice.cloudnet.repository.module;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.Constants;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModuleRepositoryProvider {

    private CloudNetUpdateServer server;

    public ModuleRepositoryProvider(CloudNetUpdateServer server) {
        this.server = server;
    }

    public Collection<RepositoryModuleInfo> getModuleInfos() {
        return Arrays.asList(this.server.getDatabase().getModuleInfos());
    }

    public Collection<RepositoryModuleInfo> getModuleInfos(String parentVersionName) {
        return this.getModuleInfos().stream()
                .filter(moduleInfo -> moduleInfo.getParentVersionName().equals(parentVersionName))
                .collect(Collectors.toList());
    }

    public Collection<RepositoryModuleInfo> getModuleInfos(String parentVersionName, String group) {
        return this.getModuleInfos().stream()
                .filter(moduleInfo -> moduleInfo.getParentVersionName().equals(parentVersionName))
                .filter(moduleInfo -> moduleInfo.getModuleId().getGroup().equalsIgnoreCase(group))
                .collect(Collectors.toList());
    }

    public Collection<RepositoryModuleInfo> getModuleInfos(String parentVersionName, String group, String name) {
        return this.getModuleInfos().stream()
                .filter(moduleInfo -> moduleInfo.getParentVersionName().equals(parentVersionName))
                .filter(moduleInfo -> moduleInfo.getModuleId().getGroup().equalsIgnoreCase(group))
                .filter(moduleInfo -> moduleInfo.getModuleId().getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
    }

    public RepositoryModuleInfo getModuleInfo(String parentVersionName, ModuleId moduleId) {
        return this.getModuleInfos().stream()
                .filter(moduleInfo -> moduleInfo.getParentVersionName().equals(parentVersionName))
                .filter(moduleInfo -> moduleId.equals(moduleInfo.getModuleId()))
                .findFirst().orElse(null);
    }

    public RepositoryModuleInfo getModuleInfoIgnoreVersion(String parentVersionName, ModuleId moduleId) {
        return this.getModuleInfos().stream()
                .filter(moduleInfo -> moduleInfo.getParentVersionName().equals(parentVersionName))
                .filter(moduleInfo -> moduleId.equalsIgnoreVersion(moduleInfo.getModuleId()))
                .findFirst().orElse(null);
    }

    public void addModule(RepositoryModuleInfo moduleInfo, InputStream inputStream) throws IOException {
        if (moduleInfo.getModuleId().getVersion().equals("latest")) {
            throw new ModuleInstallException("latest as the version on install is not allowed");
        }
        if (this.getModuleInfoIgnoreVersion(moduleInfo.getParentVersionName(), moduleInfo.getModuleId()) != null) {
            throw new ModuleInstallException("a Module with that ID already exists");
        }

        this.installModuleFile(moduleInfo, inputStream);

        this.server.getDatabase().insertModuleInfo(moduleInfo);
    }

    public void updateModule(RepositoryModuleInfo moduleInfo) {
        RepositoryModuleInfo oldModuleInfo = this.getModuleInfoIgnoreVersion(moduleInfo.getParentVersionName(), moduleInfo.getModuleId());
        if (oldModuleInfo == null) {
            throw new ModuleInstallException("Module not found");
        }

        this.removeModule(oldModuleInfo.getParentVersionName(), oldModuleInfo.getModuleId());
        this.server.getDatabase().insertModuleInfo(moduleInfo);
    }

    public void updateModuleWithFile(RepositoryModuleInfo moduleInfo, InputStream inputStream) throws IOException {
        this.updateModule(moduleInfo);
        this.installModuleFile(moduleInfo, inputStream);
    }

    private void installModuleFile(RepositoryModuleInfo moduleInfo, InputStream inputStream) throws IOException {
        CloudNetParentVersion parentVersion = this.server.getParentVersion(moduleInfo.getParentVersionName()).orElseThrow(() -> new ModuleInstallException("ParentVersion not found"));

        Files.createDirectories(this.getModuleDirectory(moduleInfo.getParentVersionName(), moduleInfo.getModuleId()));

        Path[] paths = new Path[]{
                this.getPath(moduleInfo.getParentVersionName(), moduleInfo.getModuleId()),
                this.getLatestPath(moduleInfo.getParentVersionName(), moduleInfo.getModuleId())
        };
        Path tempPath = Constants.TEMP_DIRECTORY.resolve(UUID.randomUUID().toString());
        Files.createDirectories(tempPath.getParent());

        Files.copy(inputStream, tempPath);

        try (InputStream tempInputStream = Files.newInputStream(tempPath)) {
            this.validateModule(parentVersion, moduleInfo.getModuleId(), tempInputStream);
        }

        for (Path path : paths) {
            Files.copy(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void validateModule(CloudNetParentVersion parentVersion, ModuleId moduleId, InputStream inputStream) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().equals(parentVersion.getModuleFileName())) {
                    JsonDocument document = JsonDocument.newDocument().read(zipInputStream);
                    ModuleId realModuleId = document.toInstanceOf(ModuleId.class);
                    if (!realModuleId.equals(moduleId)) {
                        throw new ModuleInstallException("Given moduleId and module.json don't match");
                    }
                    return;
                }

                zipInputStream.closeEntry();
            }
        }
    }

    public void removeModule(String parentVersionName, ModuleId moduleId) {
        this.server.getDatabase().removeModuleInfo(parentVersionName, moduleId);
    }

    public InputStream openLatestModuleStream(String parentVersionName, ModuleId moduleId) throws IOException {
        if (moduleId == null) {
            return null;
        }
        Path path = this.getLatestPath(parentVersionName, moduleId);
        return Files.exists(path) ? Files.newInputStream(path) : null;
    }

    public InputStream openModuleStream(String parentVersionName, ModuleId moduleId) throws IOException {
        if (moduleId == null) {
            return null;
        }
        Path path = this.getPath(parentVersionName, moduleId);
        return Files.exists(path) ? Files.newInputStream(path) : null;
    }

    public Path getModuleDirectory(String parentVersionName, ModuleId moduleId) {
        return Constants.MODULES_DIRECTORY.resolve(parentVersionName).resolve(moduleId.getGroup()).resolve(moduleId.getName());
    }

    public Path getPath(String parentVersionName, ModuleId moduleId) {
        return this.validatePath(this.getModuleDirectory(parentVersionName, moduleId).resolve((moduleId.getVersion() != null ? moduleId.getVersion() : "latest") + ".jar"));
    }

    public Path getLatestPath(String parentVersionName, ModuleId moduleId) {
        return this.validatePath(this.getModuleDirectory(parentVersionName, moduleId).resolve("latest.jar"));
    }

    private Path validatePath(Path path) {
        if (!path.normalize().startsWith(Constants.MODULES_DIRECTORY)) {
            throw new ModuleInstallException("Given module path is not in the internal modules directory (does it contain '../'?)");
        }
        return path;
    }

}
