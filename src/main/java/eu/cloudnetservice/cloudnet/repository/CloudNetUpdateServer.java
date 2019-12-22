package eu.cloudnetservice.cloudnet.repository;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.logging.*;
import eu.cloudnetservice.cloudnet.repository.archiver.ReleaseArchiver;
import eu.cloudnetservice.cloudnet.repository.config.BasicConfiguration;
import eu.cloudnetservice.cloudnet.repository.console.ConsoleLogHandler;
import eu.cloudnetservice.cloudnet.repository.database.Database;
import eu.cloudnetservice.cloudnet.repository.database.NitriteDatabase;
import eu.cloudnetservice.cloudnet.repository.loader.CloudNetVersionFileLoader;
import eu.cloudnetservice.cloudnet.repository.loader.JenkinsCloudNetVersionFileLoader;
import eu.cloudnetservice.cloudnet.repository.module.ModuleRepositoryProvider;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import eu.cloudnetservice.cloudnet.repository.web.handler.ArchivedVersionHandler;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJson;
import org.fusesource.jansi.AnsiConsole;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;

public class CloudNetUpdateServer {


    private boolean apiAvailable = true;

    private ReleaseArchiver releaseArchiver;

    private final ModuleRepositoryProvider moduleRepositoryProvider;
    private final Javalin webServer;

    private Database database;

    private BasicConfiguration configuration;

    private final ILogger logger;

    private CloudNetUpdateServer() {
        this.logger = new DefaultAsyncLogger();
        this.logger.addLogHandler(new DefaultFileLogHandler(new File("logs"), "cloudnet.repo.log", 8000000L).setFormatter(new DefaultLogFormatter()));
        this.logger.addLogHandler(new ConsoleLogHandler(System.out, System.err).setFormatter(new DefaultLogFormatter()));

        AnsiConsole.systemInstall();

        System.setOut(new PrintStream(new LogOutputStream(this.logger, LogLevel.INFO), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new LogOutputStream(this.logger, LogLevel.ERROR), true, StandardCharsets.UTF_8));

        this.configuration = new BasicConfiguration();
        this.configuration.load();

        this.database = new NitriteDatabase(Paths.get("nitrite.db"));

        CloudNetVersionFileLoader versionFileLoader = new JenkinsCloudNetVersionFileLoader();
        String gitHubApiBaseUrl = System.getProperty("cloudnet.repository.github.baseUrl", "https://api.github.com/repos/CloudNetService/CloudNet-v3/");
        this.releaseArchiver = new ReleaseArchiver(gitHubApiBaseUrl, versionFileLoader);

        this.webServer = Javalin.create();

        this.moduleRepositoryProvider = new ModuleRepositoryProvider(this.webServer);

        this.start();

        this.installLatestRelease();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stopWithoutShutdown));
    }

    public CloudNetVersion getCurrentLatestVersion() {
        return this.database.getLatestVersion();
    }

    public void start() {

        if (!this.database.init()) {
            System.err.println("Failed to initialize nitrite");
            return;
        }

        JavalinJson.setToJsonMapper(JsonDocument.GSON::toJson);
        JavalinJson.setFromJsonMapper(JsonDocument.GSON::fromJson);

        this.webServer.config.addStaticFiles("/web");

        this.webServer.get("/versions/:version/*", new ArchivedVersionHandler(Constants.VERSIONS_DIRECTORY, "CloudNet.zip", this));
        this.webServer.get("/docs/:version/*", new ArchivedVersionHandler(Constants.DOCS_DIRECTORY, "index.html", this));

        this.webServer.get("/api/status", context -> context.result("{\"available\":" + this.apiAvailable + "}"));

        this.webServer.get("/api/versions", context -> context.result(JsonDocument.newDocument().append("versions", Arrays.stream(this.database.getAllVersions()).map(CloudNetVersion::getName)).toPrettyJson()));

        this.webServer.start(this.configuration.getWebPort());
    }

    public void stop() {
        this.stopWithoutShutdown();
        System.exit(0);
    }

    private void stopWithoutShutdown() {
        try {
            this.database.close();
            this.webServer.stop();
            this.logger.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void installLatestRelease() {
        try {
            this.database.registerVersion(this.releaseArchiver.installLatestRelease());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new CloudNetUpdateServer();
    }

}
