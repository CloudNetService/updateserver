package eu.cloudnetservice.cloudnet.repository;

import de.dytanic.cloudnet.common.logging.*;
import eu.cloudnetservice.cloudnet.repository.archiver.ReleaseArchiver;
import eu.cloudnetservice.cloudnet.repository.command.ConsoleCommandSender;
import eu.cloudnetservice.cloudnet.repository.command.DefaultCommandMap;
import eu.cloudnetservice.cloudnet.repository.command.ICommandMap;
import eu.cloudnetservice.cloudnet.repository.command.defaults.CommandUser;
import eu.cloudnetservice.cloudnet.repository.config.BasicConfiguration;
import eu.cloudnetservice.cloudnet.repository.console.ConsoleLogHandler;
import eu.cloudnetservice.cloudnet.repository.console.IConsole;
import eu.cloudnetservice.cloudnet.repository.console.JLine2Console;
import eu.cloudnetservice.cloudnet.repository.console.log.ColouredLogFormatter;
import eu.cloudnetservice.cloudnet.repository.database.Database;
import eu.cloudnetservice.cloudnet.repository.database.H2Database;
import eu.cloudnetservice.cloudnet.repository.database.statistics.StatisticsManager;
import eu.cloudnetservice.cloudnet.repository.github.GitHubReleaseInfo;
import eu.cloudnetservice.cloudnet.repository.loader.CloudNetVersionFileLoader;
import eu.cloudnetservice.cloudnet.repository.loader.JenkinsCloudNetVersionFileLoader;
import eu.cloudnetservice.cloudnet.repository.module.ModuleRepositoryProvider;
import eu.cloudnetservice.cloudnet.repository.endpoint.EndPoint;
import eu.cloudnetservice.cloudnet.repository.endpoint.discord.DiscordEndPoint;
import eu.cloudnetservice.cloudnet.repository.repository.CloudNetUpdateRepository;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import eu.cloudnetservice.cloudnet.repository.web.WebServer;
import org.fusesource.jansi.AnsiConsole;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class CloudNetUpdateServer {

    private ReleaseArchiver releaseArchiver;

    private final ModuleRepositoryProvider moduleRepositoryProvider;
    private final WebServer webServer;

    private Collection<EndPoint> endPoints = new ArrayList<>();

    private StatisticsManager statisticsManager;
    private Database database;
    private BasicConfiguration configuration;

    private ICommandMap commandMap;

    private Collection<CloudNetUpdateRepository> repositories = Collections.emptyList();

    private final ILogger logger;
    private final IConsole console;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private CloudNetUpdateServer() throws Exception {
        this.logger = new DefaultAsyncLogger();
        this.console = new JLine2Console();
        this.logger.addLogHandler(new DefaultFileLogHandler(new File("logs"), "cloudnet.repo.log", 8000000L).setFormatter(new DefaultLogFormatter()));
        this.logger.addLogHandler(new ConsoleLogHandler(this.console).setFormatter(new ColouredLogFormatter()));

        this.initCommands();

        AnsiConsole.systemInstall();

        System.setOut(new PrintStream(new LogOutputStream(this.logger, LogLevel.INFO), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new LogOutputStream(this.logger, LogLevel.ERROR), true, StandardCharsets.UTF_8));

        this.configuration = new BasicConfiguration();
        this.configuration.load();

        this.database = new H2Database(Paths.get("database"));
        this.statisticsManager = new StatisticsManager(this.database);

        this.registerEndPoint(new DiscordEndPoint());

        CloudNetVersionFileLoader versionFileLoader = new JenkinsCloudNetVersionFileLoader();
        this.releaseArchiver = new ReleaseArchiver(versionFileLoader);

        this.webServer = new WebServer(this);

        this.moduleRepositoryProvider = new ModuleRepositoryProvider(this);

        this.start();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stopWithoutShutdown));
    }

    private void initCommands() {
        this.commandMap = new DefaultCommandMap();
        this.commandMap.registerCommand(new CommandUser(this));
        // todo more commands (help, stop, reload)

        this.console.addCommandHandler(UUID.randomUUID(), input -> {
            try {
                if (input.trim().isEmpty()) {
                    return;
                }

                if (!this.commandMap.dispatchCommand(new ConsoleCommandSender(this.logger), input)) {
                    this.logger.warning("Command not found!");
                }

            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        });
    }

    public void registerEndPoint(EndPoint endPoint) {
        this.endPoints.add(endPoint);
    }

    public Collection<EndPoint> getEndPoints() {
        return this.endPoints;
    }

    public CloudNetVersion getCurrentLatestVersion(String parentVersionName) {
        return this.database.getLatestVersion(parentVersionName);
    }

    public Database getDatabase() {
        return this.database;
    }

    public StatisticsManager getStatisticsManager() {
        return this.statisticsManager;
    }

    public BasicConfiguration getConfiguration() {
        return this.configuration;
    }

    public ModuleRepositoryProvider getModuleRepositoryProvider() {
        return this.moduleRepositoryProvider;
    }

    public Collection<CloudNetParentVersion> getParentVersions() {
        return this.configuration.getParentVersions();
    }

    public Collection<String> getParentVersionNames() {
        return this.getParentVersions().stream().map(CloudNetParentVersion::getName).collect(Collectors.toList());
    }

    public Optional<CloudNetParentVersion> getParentVersion(String name) {
        return this.getParentVersions().stream().filter(parentVersion -> parentVersion.getName().equals(name)).findFirst();
    }

    public void start() throws IOException {

        if (!this.database.init()) {
            System.err.println("Failed to initialize the database");
            return;
        }

        for (EndPoint endPoint : this.endPoints) {
            Path configPath = Paths.get("endPoints", endPoint.getName() + ".json");
            Files.createDirectories(configPath.getParent());
            endPoint.setEnabled(endPoint.init(this, configPath));
            if (endPoint.isEnabled()) {
                System.out.println("Successfully initialized " + endPoint.getName() + " end point!");
            } else {
                System.err.println("Failed to initialize " + endPoint.getName() + " end point!");
            }
        }

        this.webServer.init();

        this.statisticsManager.init(this.executorService, this.webServer.getJavalin());

        this.repositories = new ArrayList<>();
        for (CloudNetParentVersion parentVersion : this.getParentVersions()) {
            CloudNetUpdateRepository repository = new CloudNetUpdateRepository(parentVersion.getName(), this.database, this.releaseArchiver);
            repository.init(parentVersion.getUpdateRepositoryPath(), this.webServer.getJavalin());
            repository.installVersion(this.getCurrentLatestVersion(parentVersion.getName()));
            this.repositories.add(repository);

            this.statisticsManager.getStatistics().registerVersion(parentVersion.getName());
        }
    }

    public void stop() {
        this.stopWithoutShutdown();
        System.exit(0);
    }

    private void stopWithoutShutdown() {
        this.statisticsManager.save();

        try {
            this.database.close();
            this.webServer.stop();
            this.logger.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        for (EndPoint endPoint : this.endPoints) {
            endPoint.close();
        }
    }

    public void installLatestRelease(CloudNetParentVersion parentVersion) {
        try {
            var version = this.releaseArchiver.installLatestRelease(parentVersion);
            this.invokeReleasePublished(parentVersion, version);
            this.database.registerVersion(version);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void installLatestRelease(CloudNetParentVersion parentVersion, GitHubReleaseInfo release) {
        try {
            var version = this.releaseArchiver.installLatestRelease(parentVersion, release);
            this.invokeReleasePublished(parentVersion, version);
            this.database.registerVersion(version);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void invokeReleasePublished(CloudNetParentVersion parentVersion, CloudNetVersion version) {
        for (EndPoint endPoint : this.endPoints) {
            endPoint.publishRelease(parentVersion, version);
        }
        for (CloudNetUpdateRepository repository : this.repositories) {
            if (repository.getTargetParentVersion().equals(parentVersion.getName())) {
                repository.installVersion(version);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new CloudNetUpdateServer();
    }

}
