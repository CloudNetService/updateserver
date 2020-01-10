package eu.cloudnetservice.cloudnet.repository.loader;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.Constants;
import eu.cloudnetservice.cloudnet.repository.config.CloudNetVersionFileInfo;
import eu.cloudnetservice.cloudnet.repository.loader.jenkins.JenkinsArtifact;
import eu.cloudnetservice.cloudnet.repository.loader.jenkins.JenkinsBuild;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersionFile;
import eu.cloudnetservice.cloudnet.repository.version.MavenVersionInfo;
import eu.cloudnetservice.cloudnet.repository.version.VersionFileMappings;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JenkinsCloudNetVersionFileLoader implements CloudNetVersionFileLoader {

    private String mavenRepositoryURL = System.getProperty("cloudnet.repository.maven.url", "https://cloudnetservice.eu/repositories");

    @Override
    public CloudNetVersionFile[] loadLastVersionFiles(CloudNetParentVersion parentVersion, VersionFileMappings versionFileMappings) throws IOException {
        var masterStatus = JsonDocument.newDocument();
        this.loadJson(masterStatus, new URL(parentVersion.getJenkinsBaseURL() + "api/json/"));

        var lastBuild = masterStatus.get("lastBuild", JenkinsBuild.class);
        var lastSuccessfulBuild = masterStatus.get("lastSuccessfulBuild", JenkinsBuild.class);

        if (lastBuild == null || lastSuccessfulBuild == null) {
            throw new CloudNetVersionLoadException("Jenkins \"master\" branch not found!", this);
        }

        if (lastBuild.getNumber() != lastSuccessfulBuild.getNumber()) {
            throw new CloudNetVersionLoadException("Jenkins last build on \"master\" branch was not successful!", this);
        }

        var lastBuildJson = JsonDocument.newDocument();
        this.loadJson(lastBuildJson, new URL(lastBuild.getApiUrl()));

        var artifacts = lastBuildJson.get("artifacts", JenkinsArtifact[].class);

        if (artifacts == null) {
            throw new CloudNetVersionLoadException("Jenkins last build on \"master\" branch has no artifacts object!", this);
        }

        var artifactsConnection = new URL(lastBuild.getUrl() + "artifact/AutoUpdater.zip").openConnection();

        var tempDirectory = Constants.TEMP_DIRECTORY.resolve("versions").resolve("jenkins").resolve(UUID.randomUUID().toString());

        Files.createDirectories(tempDirectory);

        Collection<Path> targetPaths = new ArrayList<>();

        try (var inputStream = artifactsConnection.getInputStream();
             var zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(tempDirectory.resolve(zipEntry.getName()));

                    zipInputStream.closeEntry();
                    continue;
                }

                Path path = tempDirectory.resolve(zipEntry.getName());
                Files.copy(zipInputStream, path);
                targetPaths.add(path);

                zipInputStream.closeEntry();
            }
        }

        Collection<CloudNetVersionFile> versionFiles = targetPaths.stream()
                .map(path -> {
                    try {
                        CloudNetVersionFile.FileType fileType = CloudNetVersionFile.FileType.CLOUDNET_JAR;
                        String artifactId = null;

                        if (path.getFileName().toString().endsWith(".cnl")) {
                            fileType = CloudNetVersionFile.FileType.CLOUDNET_CNL;
                        } else if (this.isCloudNetModule(parentVersion, path)) {
                            fileType = CloudNetVersionFile.FileType.MODULE;
                            artifactId = path.getFileName().toString();
                        } else {
                            artifactId = path.getFileName().toString();
                        }

                        if (artifactId != null && artifactId.endsWith(".jar")) {
                            artifactId = artifactId.substring(0, artifactId.length() - 4);
                        }

                        MavenVersionInfo versionInfo = artifactId != null ? new MavenVersionInfo(
                                this.mavenRepositoryURL,
                                parentVersion.getMavenGroupId(),
                                versionFileMappings.getVersionName(artifactId)
                        ) : null;

                        return new CloudNetVersionFile(path.toUri().toURL(), path.getFileName().toString(), fileType, versionInfo);
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for (CloudNetVersionFileInfo versionFile : parentVersion.getAdditionalVersionFiles()) {
            versionFiles.add(new CloudNetVersionFile(
                    new URL(lastBuild.getUrl() + "artifact/" + versionFile.getPath()),
                    versionFile.getName(),
                    versionFile.getFileType(),
                    null
            ));
        }

        return versionFiles.toArray(CloudNetVersionFile[]::new);
    }

    private void loadJson(JsonDocument targetDocument, URL url) throws IOException {
        var connection = url.openConnection();
        try (InputStream inputStream = connection.getInputStream()) {
            targetDocument.read(inputStream);
        }
    }

}
