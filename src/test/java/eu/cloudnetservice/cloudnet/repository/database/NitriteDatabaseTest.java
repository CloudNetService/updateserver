package eu.cloudnetservice.cloudnet.repository.database;

import eu.cloudnetservice.cloudnet.repository.github.GitHubAuthorInfo;
import eu.cloudnetservice.cloudnet.repository.github.GitHubCommitInfo;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersionFile;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.*;

public class NitriteDatabaseTest {

    @Test
    public void testNitriteDatabase() throws IOException {
        Files.deleteIfExists(Paths.get("target", "h2database.db"));
        Database database = new NitriteDatabase(Paths.get("target", "h2database.db"));

        assertTrue(database.init());

        assertNull(database.getVersion("3.0.0"));
        assertNull(database.getLatestVersion());
        assertEquals(0, database.getAllVersions().length);

        var oldVersion = new CloudNetVersion(
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
                                CloudNetVersionFile.FileType.CLOUDNET_JAR
                        )
                },
                new HashMap<>()
        );

        var latestVersion = new CloudNetVersion(
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
                                CloudNetVersionFile.FileType.CLOUDNET_JAR
                        )
                },
                new HashMap<>()
        );

        database.registerVersion(oldVersion);

        assertEquals(oldVersion, database.getLatestVersion());
        assertEquals(oldVersion, database.getVersion("3.0.0"));
        assertEquals(1, database.getAllVersions().length);

        database.registerVersion(latestVersion);

        assertEquals(oldVersion, database.getVersion("3.0.0"));
        assertEquals(latestVersion, database.getVersion("3.1.0"));
        assertEquals(2, database.getAllVersions().length);
        assertEquals(latestVersion, database.getLatestVersion());

        database.close();

    }

}
