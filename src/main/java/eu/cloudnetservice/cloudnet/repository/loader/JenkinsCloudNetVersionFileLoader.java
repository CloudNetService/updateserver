package eu.cloudnetservice.cloudnet.repository.loader;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.Constants;
import eu.cloudnetservice.cloudnet.repository.loader.jenkins.JenkinsArtifact;
import eu.cloudnetservice.cloudnet.repository.loader.jenkins.JenkinsBuild;

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

    private String jenkinsJobURL = System.getProperty("cloudnet.repository.versions.source.jenkins", "https://ci.cloudnetservice.eu/job/CloudNetService/job/CloudNet-v3/job/master/");

    @Override
    public CloudNetVersionFile[] loadLastVersionFiles() throws IOException {
        var masterStatus = JsonDocument.newDocument();
        this.loadJson(masterStatus, new URL(this.jenkinsJobURL + "api/json/"));

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
                        if (path.getFileName().toString().endsWith(".cnl")) {
                            fileType = CloudNetVersionFile.FileType.CLOUDNET_CNL;
                        } else if (this.isCloudNetModule(path)) {
                            fileType = CloudNetVersionFile.FileType.MODULE;
                        }

                        return new CloudNetVersionFile(path.toUri().toURL(), path.getFileName().toString(), fileType);
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        versionFiles.add(new CloudNetVersionFile(
                new URL(lastBuild.getUrl() + "artifact/Javadoc.zip"),
                "docs",
                CloudNetVersionFile.FileType.JAVA_DOCS
        ));

        return versionFiles.toArray(CloudNetVersionFile[]::new);
    }

    private void loadJson(JsonDocument targetDocument, URL url) throws IOException {
        var connection = url.openConnection();
        try (InputStream inputStream = connection.getInputStream()) {
            targetDocument.read(inputStream);
        }
    }

}
