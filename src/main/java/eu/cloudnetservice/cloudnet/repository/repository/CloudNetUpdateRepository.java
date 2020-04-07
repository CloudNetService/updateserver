package eu.cloudnetservice.cloudnet.repository.repository;

import eu.cloudnetservice.cloudnet.repository.archiver.ReleaseArchiver;
import eu.cloudnetservice.cloudnet.repository.database.Database;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersionFile;
import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.openapi.dsl.OpenApiUpdater;
import io.swagger.v3.oas.models.Operation;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.plugin.openapi.dsl.OpenApiBuilder.document;
import static io.javalin.plugin.openapi.dsl.OpenApiBuilder.documented;

public class CloudNetUpdateRepository {

    private static final String WEB_API_DOCS_TAG = "Update Repository";

    private final String repositoryVersion = "1.0";
    private final String targetParentVersion;
    private final Database database;
    private final ReleaseArchiver releaseArchiver;

    private String repositoryInformation;

    public CloudNetUpdateRepository(String targetParentVersion, Database database, ReleaseArchiver releaseArchiver) {
        this.targetParentVersion = targetParentVersion;
        this.database = database;
        this.releaseArchiver = releaseArchiver;
    }

    public String getTargetParentVersion() {
        return this.targetParentVersion;
    }

    public void installVersion(CloudNetVersion version) {
        this.repositoryInformation = "repository-version=" + this.repositoryVersion + "\n";
        if (version != null) {
            this.repositoryInformation += "app-version=" + version.getName() + "\n" +
                    "git-commit=" + version.getCommit().fetchCommitHash() + "\n" +
                    "files=" + Arrays.stream(version.getFiles())
                    .filter(file -> file.getFileType() != CloudNetVersionFile.FileType.JAVA_DOCS)
                    .filter(file -> file.getFileType() != CloudNetVersionFile.FileType.CLOUDNET_ZIP)
                    .map(CloudNetVersionFile::getName)
                    .collect(Collectors.joining(";"));
       } else {
            this.repositoryInformation += "app-version=NONE";
        }
    }

    public void init(String pathPrefix, Javalin javalin) {
        javalin.routes(() -> path(pathPrefix, () -> {
            get("/repository", documented(
                    document()
                            .operation((OpenApiUpdater<Operation>) operation -> operation
                                    .summary("Get the repository version, cloudnet version (with git commit hash) and a list of all available files for this version")
                                    .addTagsItem(WEB_API_DOCS_TAG)
                            )
                            .result("200", String.class, "text/x-java-properties"),
                    (Handler) ctx -> ctx.result(this.repositoryInformation).contentType("text/x-java-properties")
            ));
            get("/versions/:version/:file", documented(
                    document()
                            .operation((OpenApiUpdater<Operation>) operation -> operation.summary("Download a file for a specific version").addTagsItem(WEB_API_DOCS_TAG))
                            .result("200", null, "application/zip")
                            .result("404", (Class<?>) null, apiResponse -> apiResponse.description("File or Version not found")),
                    ctx -> {
                        var version = this.database.getVersion(this.targetParentVersion, ctx.pathParam("version"));
                        if (version == null) {
                            throw new NotFoundResponse("Version not found");
                        }
                        var fileName = ctx.pathParam("file");
                        Arrays.stream(version.getFiles())
                                .filter(file -> file.getName().equals(fileName))
                                .filter(file -> file.getFileType() != CloudNetVersionFile.FileType.JAVA_DOCS)
                                .filter(file -> file.getFileType() != CloudNetVersionFile.FileType.CLOUDNET_ZIP)
                                .findFirst()
                                .ifPresentOrElse(file -> {
                                    try {
                                        ctx.result(this.releaseArchiver.openFileStream(version, file)).contentType("application/zip");
                                    } catch (IOException exception) {
                                        exception.printStackTrace();
                                    }
                                }, () -> {
                                    throw new NotFoundResponse("File not found");
                                });
                    }
            ));
        }));
    }

}
