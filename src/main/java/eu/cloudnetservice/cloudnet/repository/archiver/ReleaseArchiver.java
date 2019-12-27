package eu.cloudnetservice.cloudnet.repository.archiver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import eu.cloudnetservice.cloudnet.repository.Constants;
import eu.cloudnetservice.cloudnet.repository.config.BasicConfiguration;
import eu.cloudnetservice.cloudnet.repository.exception.CloudNetVersionInstallException;
import eu.cloudnetservice.cloudnet.repository.github.GitHubCommitInfo;
import eu.cloudnetservice.cloudnet.repository.github.GitHubReleaseInfo;
import eu.cloudnetservice.cloudnet.repository.loader.CloudNetVersionFileLoader;
import eu.cloudnetservice.cloudnet.repository.loader.CloudNetVersionLoadException;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersionFile;
import eu.cloudnetservice.cloudnet.repository.version.VersionFileMappings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ReleaseArchiver {

    private String gitHubApiBaseUrl;
    private CloudNetVersionFileLoader versionFileLoader;
    private VersionFileMappings versionFileMappings;

    public ReleaseArchiver(String gitHubApiBaseUrl, CloudNetVersionFileLoader versionFileLoader, VersionFileMappings versionFileMappings) {
        this.gitHubApiBaseUrl = gitHubApiBaseUrl;
        this.versionFileLoader = versionFileLoader;
        this.versionFileMappings = versionFileMappings;
    }

    public CloudNetVersion installLatestRelease() throws IOException, CloudNetVersionLoadException, CloudNetVersionInstallException {
        var gitHubRelease = this.loadLatestRelease();
        if (gitHubRelease == null) {
            throw new CloudNetVersionInstallException("No github release found!");
        }
        return this.installLatestRelease(gitHubRelease);
    }

    public CloudNetVersion installLatestRelease(GitHubReleaseInfo gitHubRelease) throws IOException, CloudNetVersionLoadException, CloudNetVersionInstallException {
        var versionFiles = this.versionFileLoader.loadLastVersionFiles(this.versionFileMappings);

        var cloudNetVersion = gitHubRelease.getTagName(); //todo load Raw-Version out of cloudnet.jar instead of the tag

        System.out.println("Extracting docs for CloudNet " + cloudNetVersion + "...");
        this.generateDocs(cloudNetVersion, versionFiles);
        System.out.println("Successfully extracted the docs for CloudNet!");
        System.out.println("Archiving all CloudNet files for the AutoUpdater...");
        this.archiveFiles(cloudNetVersion, versionFiles);
        System.out.println("Successfully archived all CloudNet files!");

        var releaseCommitUrl = this.findCommitUrl(gitHubRelease.getTagName());
        var releaseCommit = this.loadCommit(releaseCommitUrl);

        return new CloudNetVersion(cloudNetVersion, releaseCommit, gitHubRelease, new Date(), versionFiles, new HashMap<>());
    }

    private GitHubCommitInfo loadCommit(String url) throws IOException {
        try (InputStream inputStream = new URL(url).openStream()) {
            return JsonDocument.newDocument().read(inputStream).get("commit", GitHubCommitInfo.class);
        }
    }

    private GitHubReleaseInfo loadLatestRelease() throws IOException {
        try (InputStream inputStream = new URL(this.gitHubApiBaseUrl + "releases/latest").openStream()) {
            return JsonDocument.newDocument()
                    .read(inputStream)
                    .toInstanceOf(GitHubReleaseInfo.class);
        }
    }

    private String findCommitUrl(String tag) throws IOException {
        try (InputStream inputStream = new URL(this.gitHubApiBaseUrl + "tags").openStream();
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement jsonElement : array) {
                if (jsonElement.getAsJsonObject().get("name").getAsString().equals(tag)) {
                    return jsonElement.getAsJsonObject().getAsJsonObject("commit").get("url").getAsString();
                }
            }
        }

        return null;
    }

    private void archiveFiles(String cloudNetVersion, CloudNetVersionFile[] versionFiles) throws IOException {
        var directory = Constants.VERSIONS_DIRECTORY.resolve(cloudNetVersion);

        if (Files.exists(directory)) {
            FileUtils.delete(directory.toFile());
        }

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

        if (Files.exists(docsDirectory)) {
            FileUtils.delete(docsDirectory.toFile());
        }

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

}
