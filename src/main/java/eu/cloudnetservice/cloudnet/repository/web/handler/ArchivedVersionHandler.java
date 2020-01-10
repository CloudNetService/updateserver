package eu.cloudnetservice.cloudnet.repository.web.handler;

import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;
import eu.cloudnetservice.cloudnet.repository.web.MimeTypes;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

public class ArchivedVersionHandler implements Handler {

    private Path baseDirectory;
    private String defaultFileName;
    private CloudNetUpdateServer updateServer;
    private CloudNetParentVersion parentVersion;

    public ArchivedVersionHandler(Path baseDirectory, CloudNetParentVersion parentVersion, CloudNetUpdateServer updateServer) {
        this.baseDirectory = baseDirectory;
        this.parentVersion = parentVersion;
        this.updateServer = updateServer;
    }

    public ArchivedVersionHandler(Path baseDirectory, CloudNetParentVersion parentVersion, String defaultFileName, CloudNetUpdateServer updateServer) {
        this.baseDirectory = baseDirectory;
        this.parentVersion = parentVersion;
        this.defaultFileName = defaultFileName;
        this.updateServer = updateServer;
    }

    @Override
    public void handle(@NotNull Context context) throws Exception {
        String version = context.pathParam("version");

        String baseVersion = version;

        int versionIndex = context.path().indexOf(version);
        if (versionIndex == -1) {
            context.status(404).result("");
            return;
        }

        if (version.equalsIgnoreCase("latest")) {
            var latestVersion = this.updateServer.getCurrentLatestVersion(this.parentVersion.getName());
            if (latestVersion == null) {
                context.status(404).result("");
                return;
            }
            version = latestVersion.getName();
        }

        Path file = this.baseDirectory.resolve(version).resolve(context.path().substring(versionIndex + baseVersion.length() + 1)); // 1 = the / at the end

        if (this.defaultFileName != null && Files.isDirectory(file)) {
            file = file.resolve(this.defaultFileName);
        }

        if (!Files.exists(file)) {
            context.status(404).result("");
            return;
        }

        context.status(202).contentType(MimeTypes.getTypeFromPath(file)).result(Files.newInputStream(file));
    }
}
