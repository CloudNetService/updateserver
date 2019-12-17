package eu.cloudnetservice.cloudnet.repository;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.exception.CloudNetVersionInstallException;
import eu.cloudnetservice.cloudnet.repository.github.GitHubReleaseInfo;
import eu.cloudnetservice.cloudnet.repository.loader.CloudNetVersionFile;
import eu.cloudnetservice.cloudnet.repository.loader.CloudNetVersionFileLoader;
import eu.cloudnetservice.cloudnet.repository.loader.CloudNetVersionLoadException;
import eu.cloudnetservice.cloudnet.repository.loader.JenkinsCloudNetVersionFileLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CloudNetUpdateServer {


    private String gitHubApiBaseUrl = System.getProperty("cloudnet.repository.github.baseUrl", "https://api.github.com/repos/CloudNetService/CloudNet-v3/");

    private CloudNetVersionFileLoader versionFileLoader;

    private CloudNetUpdateServer() {
        this.versionFileLoader = new JenkinsCloudNetVersionFileLoader();
    }

    public void start() {

    }

    public void installLatestRelease() throws IOException, CloudNetVersionLoadException, CloudNetVersionInstallException {
        var versionFiles = this.versionFileLoader.loadLastVersionFiles();

        GitHubReleaseInfo gitHubRelease;
        try (InputStream inputStream = new URL(this.gitHubApiBaseUrl + "releases/latest").openStream()) {
            gitHubRelease = JsonDocument.newDocument()
                    .read(inputStream)
                    .toInstanceOf(GitHubReleaseInfo.class);
        }
        if (gitHubRelease == null) {
            throw new CloudNetVersionInstallException("No github release found!");
        }

        var cloudNetVersion = gitHubRelease.getTagName();

        System.out.println("Extracting docs for CloudNet " + cloudNetVersion + "...");
        this.generateDocs(cloudNetVersion, versionFiles);
        System.out.println("Successfully extracted the docs for CloudNet!");
        System.out.println("Archiving all CloudNet files for the AutoUpdater...");
        this.archiveFiles(cloudNetVersion, versionFiles);
        System.out.println("Successfully archived all CloudNet files!");
    }

    private void archiveFiles(String cloudNetVersion, CloudNetVersionFile[] versionFiles) throws IOException {
        var directory = Constants.VERSIONS_DIRECTORY.resolve(cloudNetVersion);

        Files.createDirectories(directory);

        for (CloudNetVersionFile versionFile : versionFiles) {
            if (versionFile.getFileType().equals(CloudNetVersionFile.FileType.JAVA_DOCS)) {
                continue;
            }

            var path = directory.resolve(versionFile.getName());

            try (InputStream inputStream = versionFile.getDownloadURL().openStream()) {
                Files.copy(inputStream, path);
            }
        }

    }

    private void generateDocs(String cloudNetVersion, CloudNetVersionFile[] versionFiles) throws IOException {
        var docsDirectory = Constants.DOCS_DIRECTORY.resolve(cloudNetVersion);

        Optional<CloudNetVersionFile> optionalVersionFile = Arrays.stream(versionFiles)
                .filter(versionFile -> versionFile.getFileType().equals(CloudNetVersionFile.FileType.JAVA_DOCS))
                .findFirst();

        if (optionalVersionFile.isEmpty()) {
            return;
        }

        var javaDocsZipFile = optionalVersionFile.get();

        Files.createDirectories(docsDirectory);

        URLConnection connection = javaDocsZipFile.getDownloadURL().openConnection();

        try (var inputStream = connection.getInputStream();
             var zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    Files.createDirectories(docsDirectory.resolve(entry.getName()));

                    zipInputStream.closeEntry();
                    continue;
                }

                var path = docsDirectory.resolve(entry.getName());
                var parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                Files.copy(zipInputStream, path);

                zipInputStream.closeEntry();
            }

        }
    }

    public static void main(String[] args) {
        new CloudNetUpdateServer();
    }

}
