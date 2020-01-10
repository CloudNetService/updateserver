package eu.cloudnetservice.cloudnet.repository.database;

import eu.cloudnetservice.cloudnet.repository.github.GitHubAuthorInfo;
import eu.cloudnetservice.cloudnet.repository.github.GitHubCommitInfo;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersionFile;
import eu.cloudnetservice.cloudnet.repository.version.MavenVersionInfo;
import eu.cloudnetservice.cloudnet.repository.version.VersionFileMappings;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.*;

public class H2DatabaseTest {

    @Test
    public void testH2Database() throws IOException {
        Files.deleteIfExists(Paths.get("target", "h2.mv.db"));
        Database database = new H2Database(Paths.get("target", "h2"));

        assertTrue(database.init());

        assertNull(database.getVersion("v3", "3.0.0"));
        assertNull(database.getLatestVersion("v3"));
        assertEquals(0, database.getAllVersions().length);

        var oldVersion = new CloudNetVersion(
                "v3",
                "3.0.0",
                new GitHubCommitInfo(
                        new GitHubAuthorInfo(
                                "test",
                                "test@test",
                                new Date()
                        ),
                        new GitHubAuthorInfo(
                                "test",
                                "test@test",
                                new Date()
                        ),
                        "Release 3.0.0",
                        "https://api.github.com/repos/CloudNetService/CloudNet-v3/git/commits/TEST",
                        0
                ),
                null,
                new Date(),
                new CloudNetVersionFile[]{
                        new CloudNetVersionFile(
                                new URL("https://null.null"),
                                "launcher.jar",
                                CloudNetVersionFile.FileType.CLOUDNET_JAR,
                                new MavenVersionInfo(
                                        "https://cloudnetservice.eu/repositories",
                                        "de.dytanic.cloudnet",
                                        "cloudnet-TARGET"
                                )
                        )
                },
                new VersionFileMappings(),
                new HashMap<>()
        );

        var latestVersion = new CloudNetVersion(
                "v3",
                "3.1.0",
                new GitHubCommitInfo(
                        new GitHubAuthorInfo(
                                "test",
                                "test@test",
                                new Date()
                        ),
                        new GitHubAuthorInfo(
                                "test",
                                "test@test",
                                new Date()
                        ),
                        "Release 3.1.0",
                        "https://api.github.com/repos/CloudNetService/CloudNet-v3/git/commits/TEST",
                        0
                ),
                null,
                new Date(),
                new CloudNetVersionFile[]{
                        new CloudNetVersionFile(
                                new URL("https://null.null"),
                                "launcher.jar",
                                CloudNetVersionFile.FileType.CLOUDNET_JAR,
                                new MavenVersionInfo(
                                        "https://cloudnetservice.eu/repositories",
                                        "de.dytanic.cloudnet",
                                        "cloudnet-TARGET"
                                )
                        )
                },
                new VersionFileMappings(),
                new HashMap<>()
        );

        database.registerVersion(oldVersion);

        assertEquals(oldVersion, database.getLatestVersion("v3"));
        assertEquals(oldVersion, database.getVersion("v3", "3.0.0"));
        assertEquals(1, database.getAllVersions().length);

        database.registerVersion(latestVersion);

        assertEquals(oldVersion, database.getVersion("v3", "3.0.0"));
        assertEquals(latestVersion, database.getVersion("v3", "3.1.0"));
        assertEquals(2, database.getAllVersions().length);
        assertNotEquals(oldVersion, database.getLatestVersion("v3"));
        assertEquals(latestVersion, database.getLatestVersion("v3"));

        database.close();

    }

}
